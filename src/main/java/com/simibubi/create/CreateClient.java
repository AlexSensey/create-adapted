package com.simibubi.create;

import java.util.function.Supplier;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.jei.CreateJEIBackButton;
import com.simibubi.create.compat.jei.CreateJEIKeyBridge;
import com.simibubi.create.compat.ftb.FTBIntegration;
import com.simibubi.create.compat.pojav.PojavChecker;
import com.simibubi.create.compat.jei.category.animations.BlazeBurnerGuiRenderState;
import com.simibubi.create.compat.jei.category.animations.BlazeBurnerGuiRenderer;
import com.simibubi.create.compat.sodium.SodiumCompat;
import com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHandler;
import com.simibubi.create.content.decoration.encasing.CasingConnectivity;
import com.simibubi.create.content.equipment.bell.SoulPulseEffectHandler;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonRenderHandler;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItemRenderer;
import com.simibubi.create.content.equipment.zapper.terrainzapper.WorldshaperItemRenderer;
import com.simibubi.create.content.equipment.zapper.ZapperRenderHandler;
import com.simibubi.create.content.fluids.particle.FluidParticleData;
import com.simibubi.create.content.fluids.particle.FluidStackParticle;
import com.simibubi.create.content.kinetics.base.RotationIndicatorParticle;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.fan.AirFlowParticle;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticle;
import com.simibubi.create.content.schematics.client.ClientSchematicLoader;
import com.simibubi.create.content.schematics.client.SchematicAndQuillHandler;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import com.simibubi.create.content.trains.CubeParticle;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.foundation.ClientResourceReloadListener;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsClient;
import com.simibubi.create.foundation.events.ClientEvents;
import com.simibubi.create.foundation.model.ModelSwapper;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.particle.AirParticle;
import com.simibubi.create.foundation.ponder.CreatePonderPlugin;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.assets.ExternalCreateAssets;
import com.simibubi.create.infrastructure.gui.OpenCreateMenuButton;

import dev.engine_room.flywheel.lib.model.baked.PartialModelEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.createmod.catnip.api.client.platform.ModClientHooksHelper;
import net.createmod.ponder.api.client.PonderIndex;

@Mod(value = Create.ID, dist = Dist.CLIENT)
public class CreateClient {

	static {
		Create.startupDebug("client static clinit start");
	}

	public static final ModelSwapper MODEL_SWAPPER = initClientStatic("model swapper", ModelSwapper::new);
	public static final CasingConnectivity CASING_CONNECTIVITY = initClientStatic("casing connectivity", CasingConnectivity::new);

	public static final ClientSchematicLoader SCHEMATIC_SENDER = initClientStatic("schematic sender", ClientSchematicLoader::new);
	public static final SchematicHandler SCHEMATIC_HANDLER = initClientStatic("schematic handler", SchematicHandler::new);
	public static final SchematicAndQuillHandler SCHEMATIC_AND_QUILL_HANDLER = initClientStatic("schematic and quill handler", SchematicAndQuillHandler::new);
	public static final SuperGlueSelectionHandler GLUE_HANDLER = initClientStatic("super glue handler", SuperGlueSelectionHandler::new);

	public static final ZapperRenderHandler ZAPPER_RENDER_HANDLER = initClientStatic("zapper render handler", ZapperRenderHandler::new);
	public static final PotatoCannonRenderHandler POTATO_CANNON_RENDER_HANDLER = initClientStatic("potato cannon render handler", PotatoCannonRenderHandler::new);
	public static final SoulPulseEffectHandler SOUL_PULSE_EFFECT_HANDLER = initClientStatic("soul pulse effect handler", SoulPulseEffectHandler::new);
	public static final GlobalRailwayManager RAILWAYS = initClientStatic("railway manager", GlobalRailwayManager::new);
	public static final ValueSettingsClient VALUE_SETTINGS_HANDLER = initClientStatic("value settings handler", ValueSettingsClient::new);

	public static final ClientResourceReloadListener RESOURCE_RELOAD_LISTENER = initClientStatic("resource reload listener", ClientResourceReloadListener::new);

	public CreateClient(IEventBus modEventBus) {
		Create.startupDebug("client constructor start");
		try {
			onCtorClient(modEventBus);
		} catch (Throwable t) {
			Create.startupDebug("client constructor failed", t);
			throw t;
		}
	}

	private static <T> T initClientStatic(String name, Supplier<T> supplier) {
		Create.startupDebug("client static init: " + name);
		try {
			return supplier.get();
		} catch (Throwable t) {
			Create.startupDebug("client static init failed: " + name, t);
			throw t;
		}
	}

	public static void onCtorClient(IEventBus modEventBus) {
		ExternalCreateAssets.initialize();
		modEventBus.addListener(ExternalCreateAssets::addPackFinders);
		ModClientHooksHelper.INSTANCE.registerPictureInPictureRenderer(BlazeBurnerGuiRenderState.class,
			BlazeBurnerGuiRenderer::new);
		IEventBus neoEventBus = NeoForge.EVENT_BUS;

		modEventBus.addListener(CreateClient::clientInit);
		modEventBus.addListener(CreateClient::registerFluidClientExtensions);
		modEventBus.addListener(CreateClient::registerFluidModels);
		modEventBus.addListener(CreateClient::registerParticleFactories);
		modEventBus.addListener(CreateClient::registerItemDecorations);
		modEventBus.addListener(AllBlockEntityRenderers::register);
		modEventBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) ->
			com.simibubi.create.content.equipment.armor.BacktankArmorLayer.register(event));
		modEventBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) ->
			com.simibubi.create.content.equipment.hats.CreateHatArmorLayer.register(event));
		modEventBus.addListener(ClientEvents::registerGuiOverlays);
		modEventBus.addListener(CreateStandaloneModels::register);
		// Flywheel's own mod bus cannot see partials created by Create in time on NeoForge 26.2.
		modEventBus.addListener(PartialModelEventHandler::onRegisterStandalone);

		AllInstanceTypes.init();
		// Partial models must exist before ModelEvent.RegisterStandalone is fired.
		AllPartialModels.init();
		AllPacketsClient.registerHandlers();

		MODEL_SWAPPER.registerListeners(modEventBus);

		ZAPPER_RENDER_HANDLER.registerListeners(neoEventBus);
		POTATO_CANNON_RENDER_HANDLER.registerListeners(neoEventBus);
		neoEventBus.addListener(OpenCreateMenuButton.OpenConfigButtonHandler::onGuiInit);

		Mods.FTBLIBRARY.executeIfInstalled(() -> () -> FTBIntegration.init(modEventBus, neoEventBus));
		Mods.SODIUM.executeIfInstalled(() -> () -> SodiumCompat.init(modEventBus, neoEventBus));
		Mods.JEI.executeIfInstalled(() -> () -> {
			CreateJEIKeyBridge.register(neoEventBus);
			CreateJEIBackButton.register(neoEventBus);
		});
		PojavChecker.init();
	}

	public static void clientInit(final FMLClientSetupEvent event) {
		//BUFFER_CACHE.registerCompartment(CachedBufferer.GENERIC_BLOCK);
		//BUFFER_CACHE.registerCompartment(CachedPartialBuffers.partial);
		//BUFFER_CACHE.registerCompartment(CachedBufferer.DIRECTIONAL_PARTIAL);
		//BUFFER_CACHE.registerCompartment(KineticBlockEntityRenderer.KINETIC_BLOCK);
		//BUFFER_CACHE.registerCompartment(WaterWheelRenderer.WATER_WHEEL);
		//BUFFER_CACHE.registerCompartment(ContraptionRenderInfo.CONTRAPTION, 20);
		//BUFFER_CACHE.registerCompartment(WorldSectionElement.DOC_WORLD_SECTION, 20);

		PonderIndex.addPlugin(new CreatePonderPlugin());

	}

	private static void registerFluidClientExtensions(
		net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
		registerFluidClientExtension(event, AllFluids.HONEY.get().getFluidType());
		registerFluidClientExtension(event, AllFluids.CHOCOLATE.get().getFluidType());
	}

	private static void registerFluidClientExtension(
		net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event,
		net.neoforged.neoforge.fluids.FluidType fluidType) {
		if (fluidType instanceof AllFluids.TintedFluidType tinted)
			tinted.initializeClient(extension -> event.registerFluidType(extension, fluidType));
	}

	private static void registerItemDecorations(RegisterItemDecorationsEvent event) {
		event.register(AllItems.POTATO_CANNON, PotatoCannonItemRenderer.DECORATOR);
		event.register(AllItems.WORLDSHAPER, WorldshaperItemRenderer.DECORATOR);
	}

	private static void registerFluidModels(RegisterFluidModelsEvent event) {
		registerFluidModel(event, "potion", AllFluids.POTION.getSource(), new FluidTintSource() {
			@Override
			public int color(FluidState state) {
				return 0xffffffff;
			}

			@Override
			public int colorAsStack(FluidStack stack) {
				return 0xff000000 | stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
			}
		});
		registerFluidModel(event, "tea", AllFluids.TEA.getSource());
		registerFluidModel(event, "honey", AllFluids.HONEY.getSource());
		registerFluidModel(event, "chocolate", AllFluids.CHOCOLATE.getSource());
	}

	private static void registerFluidModel(RegisterFluidModelsEvent event, String name, BaseFlowingFluid source) {
		registerFluidModel(event, name, source, state -> 0xffffffff);
	}

	private static void registerFluidModel(RegisterFluidModelsEvent event, String name, BaseFlowingFluid source,
		FluidTintSource tintSource) {
		FluidModel.Unbaked model = new FluidModel.Unbaked(
			new Material(Create.asResource("fluid/" + name + "_still")),
			new Material(Create.asResource("fluid/" + name + "_flow")),
			null,
			tintSource
		);
		Fluid flowing = source.getFlowing();
		event.register(model, source, flowing);
	}

	private static void registerParticleFactories(RegisterParticleProvidersEvent event) {
		AllParticleTypes.registerFactories(event);
		registerBuiltInParticleFactories(event);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void registerBuiltInParticleFactories(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet((ParticleType) AllParticleTypes.ROTATION_INDICATOR.get(),
			RotationIndicatorParticle.Factory::new);
		event.registerSpriteSet((ParticleType) AllParticleTypes.AIR_FLOW.get(), AirFlowParticle.Factory::new);
		event.registerSpriteSet((ParticleType) AllParticleTypes.AIR.get(), AirParticle.Factory::new);
		event.registerSpriteSet((ParticleType) AllParticleTypes.STEAM_JET.get(), SteamJetParticle.Factory::new);
		event.registerSpriteSet((ParticleType) AllParticleTypes.CUBE.get(), CubeParticle.Factory::new);
		event.registerSpecial((ParticleType<FluidParticleData>) AllParticleTypes.FLUID_PARTICLE.get(),
			CreateClient::createFluidParticle);
		event.registerSpecial((ParticleType<FluidParticleData>) AllParticleTypes.BASIN_FLUID.get(),
			CreateClient::createFluidParticle);
		event.registerSpecial((ParticleType<FluidParticleData>) AllParticleTypes.FLUID_DRIP.get(),
			CreateClient::createFluidParticle);
	}

	private static FluidStackParticle createFluidParticle(FluidParticleData data,
		net.minecraft.client.multiplayer.ClientLevel world, double x, double y, double z, double vx, double vy,
		double vz, net.minecraft.util.RandomSource random) {
		return FluidStackParticle.create(data.getParticleType(), world, data.getFluid(), x, y, z, vx, vy, vz);
	}

	public static void invalidateRenderers() {
		SCHEMATIC_HANDLER.updateRenderers();
	}

	public static void checkGraphicsFanciness() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;

		if (mc.options.graphicsPreset().get() != GraphicsPreset.FABULOUS)
			return;

		if (AllConfigs.client().ignoreFabulousWarning.get())
			return;

		mc.player.sendSystemMessage(Component.literal(
			"Some of Create's visual features will not be available while Fabulous graphics are enabled!")
			.withStyle(ChatFormatting.GOLD));
	}

}
