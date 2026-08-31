package dev.engine_room.flywheel.impl;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.UnknownNullability;

import dev.engine_room.flywheel.api.Flywheel;
import dev.engine_room.flywheel.api.event.EndClientResourceReloadEvent;
import dev.engine_room.flywheel.api.event.ReloadLevelRendererEvent;
import dev.engine_room.flywheel.backend.compile.FlwProgramsReloader;
import dev.engine_room.flywheel.backend.engine.uniform.FrameUniforms;
import dev.engine_room.flywheel.backend.engine.uniform.Uniforms;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.event.RenderContextImpl;
import dev.engine_room.flywheel.impl.event.LevelRenderMatrices;
import dev.engine_room.flywheel.impl.visualization.VisualizationEventHandler;
import dev.engine_room.flywheel.lib.model.baked.PartialModelEventHandler;
import dev.engine_room.flywheel.lib.util.LevelAttached;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.CrashReportCallables;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@Mod(value = Flywheel.ID, dist = Dist.CLIENT)
public final class FlywheelNeoForge {
	@UnknownNullability
	private static ArtifactVersion version;

	public FlywheelNeoForge(IEventBus modEventBus, ModContainer modContainer) {
		version = modContainer
				.getModInfo()
				.getVersion();

		IEventBus gameEventBus = NeoForge.EVENT_BUS;

		NeoForgeFlwConfig.INSTANCE.registerSpecs(modContainer);

		// Model and resource caches must be invalidated before visualization managers
		// recreate their visuals at the end of a resource reload. Registering the
		// implementation listener first made language changes rebuild visuals from
		// the old caches and then immediately discard the models they referenced.
		registerLibEventListeners(gameEventBus, modEventBus);
		registerImplEventListeners(gameEventBus, modEventBus);
		registerBackendEventListeners(gameEventBus, modEventBus);

		CrashReportCallables.registerCrashCallable("Flywheel Backend", BackendManagerImpl::getBackendString);
		FlwImpl.init();

	}

	private static void registerImplEventListeners(IEventBus gameEventBus, IEventBus modEventBus) {
		gameEventBus.addListener((ReloadLevelRendererEvent e) -> BackendManagerImpl.onReloadLevelRenderer(e.level()));

		gameEventBus.addListener((LevelTickEvent.Post e) -> {
			// Make sure we don't tick on the server somehow.
			if (e.getLevel().isClientSide()) {
				FrameUniforms.onClientTick();
				VisualizationEventHandler.onClientTick(Minecraft.getInstance(), e.getLevel());
			}
		});
		gameEventBus.addListener((EntityJoinLevelEvent e) -> VisualizationEventHandler.onEntityJoinLevel(e.getLevel(), e.getEntity()));
		gameEventBus.addListener((EntityLeaveLevelEvent e) -> VisualizationEventHandler.onEntityLeaveLevel(e.getLevel(), e.getEntity()));

		gameEventBus.addListener(FlwCommands::registerClientCommands);

		gameEventBus.addListener(FlywheelNeoForge::afterSky);
		gameEventBus.addListener(FlywheelNeoForge::afterOpaqueFeatures);

		modEventBus.addListener((EndClientResourceReloadEvent e) -> BackendManagerImpl.onEndClientResourceReload(e.error().isPresent()));

		modEventBus.addListener((FMLCommonSetupEvent e) -> {
			// We can't register anything to Registries.COMMAND_ARGUMENT_TYPE because it is a synced registry but
			// Flywheel is a client-side only mod.
			ArgumentTypeInfos.registerByClass(BackendArgument.class, BackendArgument.INFO);
			ArgumentTypeInfos.registerByClass(DebugModeArgument.class, DebugModeArgument.INFO);
			ArgumentTypeInfos.registerByClass(LightSmoothnessArgument.class, LightSmoothnessArgument.INFO);
		});
	}

	private static void registerLibEventListeners(IEventBus gameEventBus, IEventBus modEventBus) {
		gameEventBus.addListener((LevelEvent.Unload e) -> LevelAttached.invalidateLevel(e.getLevel()));

		modEventBus.addListener(PartialModelEventHandler::onRegisterStandalone);
	}

	private static void registerBackendEventListeners(IEventBus gameEventBus, IEventBus modEventBus) {
		gameEventBus.addListener((ReloadLevelRendererEvent e) -> Uniforms.onReloadLevelRenderer());

		modEventBus.addListener((AddClientReloadListenersEvent e) -> {
			e.addListener(Identifier.fromNamespaceAndPath(Flywheel.ID, "programs"), FlwProgramsReloader.INSTANCE);
		});
	}

	private static void afterSky(RenderLevelStageEvent.AfterSky event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}

		VisualizationManager manager = VisualizationManager.get(minecraft.level);
		if (manager != null) {
			manager.renderDispatcher().onStartLevelRender(createRenderContext(event, minecraft));
		}
	}

	private static void afterOpaqueFeatures(RenderLevelStageEvent.AfterOpaqueFeatures event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}

		VisualizationManager manager = VisualizationManager.get(minecraft.level);
		if (manager != null) {
			manager.renderDispatcher().afterEntities(createRenderContext(event, minecraft));
		}
	}

	private static RenderContextImpl createRenderContext(RenderLevelStageEvent event, Minecraft minecraft) {
		var cameraState = event.getLevelRenderState().cameraRenderState;
		return RenderContextImpl.create(event.getLevelRenderer(), minecraft.level,
			// Chunk geometry in 26.2 is rendered with viewRotationMatrix. The event's
			// model-view matrix contains additional camera motion, which makes Flywheel
			// block-entity parts drift relative to their chunk while the player moves.
			minecraft.gameRenderer.renderBuffers(), cameraState.viewRotationMatrix,
			LevelRenderMatrices.projection(cameraState.projectionMatrix),
			minecraft.gameRenderer.mainCamera(), cameraState.pos,
			minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
	}

	public static ArtifactVersion version() {
		return version;
	}
}
