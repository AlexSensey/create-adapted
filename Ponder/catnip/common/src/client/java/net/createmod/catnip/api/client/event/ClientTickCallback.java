package net.createmod.catnip.api.client.event;

import net.createmod.catnip.api.event.CatnipEvent;
import net.minecraft.client.Minecraft;

/// Invoked as the [Minecraft] client ticks.
@FunctionalInterface
public interface ClientTickCallback {
	CatnipEvent.Biphasic<ClientTickCallback> EVENT = CatnipEvent.biphasic(callbacks -> () -> callbacks.forEach(ClientTickCallback::onTick));

	void onTick();
}
