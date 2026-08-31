package net.createmod.ponder.fabric;

import net.createmod.catnip.impl.command.CatnipCommands;
import net.createmod.ponder.impl.command.PonderCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class FabricPonder implements ModInitializer {
	@Override
	public void onInitialize() {
		registerConfigs();

		// FIXME: config
		// ArgumentTypeRegistry.registerArgumentType(Ponder.id("config_path"), ConfigPathArgument.class,
		// 	SingletonArgumentInfo.contextFree(ConfigPathArgument::new));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			PonderCommands.register(dispatcher);
			CatnipCommands.register(dispatcher);
		});

		// FIXME: config
		// ModConfigEvents.loading(Ponder.MOD_ID).register(PonderConfig::onLoad);
		// ModConfigEvents.reloading(Ponder.MOD_ID).register(PonderConfig::onReload);
	}

	private static void registerConfigs() {
		// Set<Map.Entry<ModConfig.Type, ConfigBase>> entries = PonderConfig.registerConfigs();
		// for (Map.Entry<ModConfig.Type, ConfigBase> entry : entries) {
		// 	ConfigRegistry.INSTANCE.register(Ponder.MOD_ID, entry.getKey(), entry.getValue().specification);
		// }
	}
}
