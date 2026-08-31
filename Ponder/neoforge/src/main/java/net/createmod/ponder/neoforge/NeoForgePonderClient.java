package net.createmod.ponder.neoforge;

import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.api.client.event.TooltipQueryCallback;
import net.createmod.ponder.impl.client.PonderClient;
import net.createmod.ponder.impl.client.PonderKeybinds;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Ponder.MOD_ID, dist = Dist.CLIENT)
public class NeoForgePonderClient {
	public NeoForgePonderClient(IEventBus modEventBus) {
		PonderClient.init();
		modEventBus.addListener(NeoForgePonderClient::init);
		// Ponder is shipped as a nested jar. In 26.2, automatic subscriber
		// discovery does not reliably attach nested-mod classes to the game bus.
		NeoForge.EVENT_BUS.addListener(ClientEvents::onItemTooltip);
	}

	public static void init(FMLClientSetupEvent event) {
	}

	public static class ClientEvents {
		@SubscribeEvent
		public static void onItemTooltip(ItemTooltipEvent event) {
			TooltipQueryCallback.EVENT.invoker().onTooltipQuery(event.getItemStack(), event.getContext(), event.getFlags(), event.getToolTip());
		}
	}

	@EventBusSubscriber(value = Dist.CLIENT)
	public static class ModBusClientEvents {
		@SubscribeEvent
		public static void loadCompleted(FMLLoadCompleteEvent event) {
			PonderClient.modLoadCompleted();
		}

		@SubscribeEvent
		public static void register(RegisterKeyMappingsEvent event) {
			PonderKeybinds.register(event::register);
		}
	}

}
