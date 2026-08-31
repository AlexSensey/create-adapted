package net.createmod.ponder.neoforge;

import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.impl.command.PonderCommands;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Ponder.MOD_ID)
public class NeoForgePonder {
	public NeoForgePonder(ModContainer container) {
		registerConfigs(container);
	}

	private static void registerConfigs(ModContainer container) {
		// FIXME: config
		// Set<Map.Entry<ModConfig.Type, ConfigBase>> entries = PonderConfig.registerConfigs();
		// for (Map.Entry<ModConfig.Type, ConfigBase> entry : entries) {
		// 	continue;.registerConfig(entry.getKey(), entry.getValue().specification);
		// }
	}

	@EventBusSubscriber
	public static class Events {
		@SubscribeEvent
		public static void registerCommands(RegisterCommandsEvent event) {
			PonderCommands.register(event.getDispatcher());
		}
	}

	// FIXME: config
	// @EventBusSubscriber(bus = Bus.MOD)
	// public static class ModBusEvents {
	// 	@SubscribeEvent
	// 	public static void onLoad(ModConfigEvent.Loading event) {
	// 		PonderConfig.onLoad(event.getConfig());
	// 	}
	//
	// 	@SubscribeEvent
	// 	public static void onReload(ModConfigEvent.Reloading event) {
	// 		PonderConfig.onReload(event.getConfig());
	// 	}
	// }
}
