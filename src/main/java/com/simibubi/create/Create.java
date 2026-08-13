package com.simibubi.create;

import java.util.Random;
import java.util.function.Supplier;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.registrate.CreateRegistrateRegistrationCallback;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.curios.Curios;
import com.simibubi.create.compat.inventorySorter.InventorySorterCompat;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.content.equipment.armor.AllArmorMaterials;
import com.simibubi.create.content.equipment.potatoCannon.AllPotatoProjectileBlockHitActions;
import com.simibubi.create.content.equipment.potatoCannon.AllPotatoProjectileEntityHitActions;
import com.simibubi.create.content.equipment.potatoCannon.AllPotatoProjectileRenderModes;
import com.simibubi.create.content.fluids.tank.BoilerHeaters;
import com.simibubi.create.content.kinetics.TorquePropagator;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.logistics.item.filter.attribute.AllItemAttributeTypes;
import com.simibubi.create.content.logistics.packagePort.AllPackagePortTargetTypes;
import com.simibubi.create.content.logistics.packager.AllInventoryIdentifiers;
import com.simibubi.create.content.logistics.packager.AllUnpackingHandlers;
import com.simibubi.create.content.logistics.packagerLink.GlobalLogisticsManager;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.schematics.ServerSchematicLoader;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.track.AllPortalTracks;
import com.simibubi.create.foundation.CreateNBTProcessors;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.recipe.RecipeData26;
import com.simibubi.create.infrastructure.data.CreateDataGen26;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.recipe.AllIngredients;
import com.simibubi.create.infrastructure.command.ServerLagger;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.worldgen.AllFeatures;
import com.simibubi.create.infrastructure.worldgen.AllPlacementModifiers;

import net.createmod.catnip.api.client.lang.FontHelper;
import net.createmod.catnip.api.lang.LangBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Create.ID)
public class Create {
	static {
		startupDebug("static clinit start");
	}

	public static final String ID = "create";
	public static final String NAME = "Create";

	public static final Logger LOGGER = initLogger();

	private static final StackWalker STACK_WALKER = initStackWalker();

	public static final Gson GSON = initGson();

	/**
	 * Use the {@link Random} of a local {@link Level} or {@link Entity} or create one
	 */
	@Deprecated
	public static final Random RANDOM = initRandom();

	/**
	 * <b>Other mods should not use this field!</b> If you are an addon developer, create your own instance of
	 * {@link CreateRegistrate}.
	 * </br
	 * If you were using this instance to register a callback listener use {@link CreateRegistrateRegistrationCallback#register} instead.
	 */
	private static final CreateRegistrate REGISTRATE = initRegistrate();

	public static final ServerSchematicLoader SCHEMATIC_RECEIVER = initStatic("schematic receiver", ServerSchematicLoader::new);
	public static final RedstoneLinkNetworkHandler REDSTONE_LINK_NETWORK_HANDLER = initStatic("redstone link network", RedstoneLinkNetworkHandler::new);
	public static final TorquePropagator TORQUE_PROPAGATOR = initStatic("torque propagator", TorquePropagator::new);
	public static final GlobalRailwayManager RAILWAYS = initStatic("railway manager", GlobalRailwayManager::new);
	public static final GlobalLogisticsManager LOGISTICS = initStatic("logistics manager", GlobalLogisticsManager::new);
	public static final ServerLagger LAGGER = initStatic("server lagger", ServerLagger::new);

	private static <T> T initStatic(String name, Supplier<T> supplier) {
		startupDebug("static init: " + name);
		try {
			LOGGER.info("Create static init: {}", name);
			return supplier.get();
		} catch (Throwable t) {
			startupDebug("static init failed: " + name, t);
			LOGGER.error("Create static init failed: {}", name, t);
			throw t;
		}
	}

	private static Logger initLogger() {
		startupDebug("static init: logger");
		try {
			return LogUtils.getLogger();
		} catch (Throwable t) {
			startupDebug("static init failed: logger", t);
			throw t;
		}
	}

	private static StackWalker initStackWalker() {
		startupDebug("static init: stack walker");
		try {
			return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		} catch (Throwable t) {
			startupDebug("static init failed: stack walker", t);
			throw t;
		}
	}

	private static Gson initGson() {
		startupDebug("static init: gson");
		try {
			return new GsonBuilder().setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
		} catch (Throwable t) {
			startupDebug("static init failed: gson", t);
			throw t;
		}
	}

	private static Random initRandom() {
		startupDebug("static init: random");
		try {
			return new Random();
		} catch (Throwable t) {
			startupDebug("static init failed: random", t);
			throw t;
		}
	}

	private static CreateRegistrate initRegistrate() {
		startupDebug("static init: registrate");
		try {
			return CreateRegistrate.create(ID)
				.defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
				.setTooltipModifierFactory(item ->
					new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
						.andThen(TooltipModifier.mapNull(KineticStats.create(item)))
				);
		} catch (Throwable t) {
			startupDebug("static init failed: registrate", t);
			throw t;
		}
	}

	public Create(IEventBus eventBus, ModContainer modContainer) {
		startupDebug("constructor start");
		try {
			onCtor(eventBus, modContainer);
		} catch (Throwable t) {
			startupDebug("constructor failed", t);
			LOGGER.error("Create failed during mod construction", t);
			throw t;
		}
	}

	public static void startupDebug(String message) {
		startupDebug(message, null);
	}

	public static void startupDebug(String message, Throwable throwable) {
		try {
			java.nio.file.Path path = java.nio.file.Path.of(System.getProperty("user.dir", "."), "create-startup-debug.log");
			StringBuilder builder = new StringBuilder();
			builder.append("[CREATE] ").append(message).append(System.lineSeparator());
			if (throwable != null) {
				java.io.StringWriter writer = new java.io.StringWriter();
				throwable.printStackTrace(new java.io.PrintWriter(writer));
				builder.append(writer);
			}
			java.nio.file.Files.writeString(path, builder.toString(), java.nio.charset.StandardCharsets.UTF_8,
				java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		} catch (Throwable ignored) {
		}
	}

	public static void onCtor(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(EventPriority.HIGHEST, CreateDataGen26::prepareRegistrateData);
		modEventBus.addListener(RecipeData26::gatherData);
		modEventBus.addListener(CreateDataGen26::gatherData);
		LOGGER.info("{} {} initializing! Commit hash: {}", NAME, CreateBuildInfo.VERSION, CreateBuildInfo.GIT_COMMIT);
		ModLoadingContext modLoadingContext = ModLoadingContext.get();

		LOGGER.info("Create bootstrap: registrate listeners");
		REGISTRATE.registerEventListeners(modEventBus);

		LOGGER.info("Create bootstrap: core registrations");
		AllSoundEvents.prepare();
		AllCreativeModeTabs.register(modEventBus);
		AllArmorMaterials.register(modEventBus);
		AllDisplaySources.register();
		AllDisplayTargets.register();
		AllBlocks.register();
		AllItems.register();
		AllFluids.register();
		AllPaletteBlocks.register();
		AllMenuTypes.register();
		AllEntityTypes.register();
		AllBlockEntityTypes.register();
		AllRecipeTypes.register(modEventBus);
		AllParticleTypes.register(modEventBus);
		AllStructureProcessorTypes.register(modEventBus);
		AllEntityDataSerializers.register(modEventBus);
		AllPackets.register();
		AllFeatures.register(modEventBus);
		AllPlacementModifiers.register(modEventBus);
		AllIngredients.register(modEventBus);
		AllAttachmentTypes.register(modEventBus);
		AllDataComponents.register(modEventBus);
		AllMapDecorationTypes.register(modEventBus);
		AllMountedStorageTypes.register();

		LOGGER.info("Create bootstrap: configs");
		AllConfigs.register(modLoadingContext, modContainer);

		// TODO - Make these use Registry.register and move them into the RegisterEvent
		LOGGER.info("Create bootstrap: package port targets");
		AllPackagePortTargetTypes.register(modEventBus);

		LOGGER.info("Create bootstrap: schematic filters");
		AllSchematicStateFilters.registerDefaults();

		// FIXME: some of these registrations are not thread-safe
		LOGGER.info("Create bootstrap: bogey init");
		BogeySizes.init();
		AllBogeyStyles.init();
		// ----

		LOGGER.info("Create bootstrap: compat");
		ComputerCraftProxy.register();

		LOGGER.info("Create bootstrap: neoforge hooks");
		NeoForgeMod.enableMilkFluid();

		LOGGER.info("Create bootstrap: event listeners");
		modEventBus.addListener(Create::init);
		modEventBus.addListener(Create::onRegister);
		modEventBus.addListener(AllEntityTypes::registerEntityAttributes);
		modEventBus.addListener(AllSoundEvents::register);

		// FIXME: this is not thread-safe
		Mods.CURIOS.executeIfInstalled(() -> () -> Curios.init(modEventBus));
		Mods.INVENTORYSORTER.executeIfInstalled(() -> () -> InventorySorterCompat.init(modEventBus));
	}

	public static void init(final FMLCommonSetupEvent event) {
		AllFluids.registerFluidInteractions();
		CreateNBTProcessors.register();

		event.enqueueWork(() -> {
			// TODO: custom registration should all happen in one place
			// Most registration happens in the constructor.
			// These registrations use Create's registered objects directly so they must run after registration has finished.
			BoilerHeaters.registerDefaults();
			AllPortalTracks.registerDefaults();
			AllBlockSpoutingBehaviours.registerDefaults();
			AllMovementBehaviours.registerDefaults();
			AllInteractionBehaviours.registerDefaults();
			AllContraptionMovementSettings.registerDefaults();
			AllOpenPipeEffectHandlers.registerDefaults();
			AllMountedDispenseItemBehaviors.registerDefaults();
			AllUnpackingHandlers.registerDefaults();
			AllInventoryIdentifiers.registerDefaults();
			// --
		});
	}

	public static void onRegister(final RegisterEvent event) {
		AllArmInteractionPointTypes.init();
		AllFanProcessingTypes.init();
		AllItemAttributeTypes.init();
		AllContraptionTypes.init();
		AllPotatoProjectileRenderModes.init();
		AllPotatoProjectileEntityHitActions.init();
		AllPotatoProjectileBlockHitActions.init();

		if (event.getRegistry() == BuiltInRegistries.TRIGGER_TYPES) {
			AllAdvancements.register();
			AllTriggers.register();
		}
	}

	public static LangBuilder lang() {
		return new LangBuilder(ID);
	}

	public static Identifier asResource(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}

	public static CreateRegistrate registrate() {
		if (!STACK_WALKER.getCallerClass().getPackageName().startsWith("com.simibubi.create"))
			throw new UnsupportedOperationException("Other mods are not permitted to use create's registrate instance.");
		return REGISTRATE;
	}
}
