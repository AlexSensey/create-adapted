package net.createmod.catnip.api.client.gui;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

/// Extension of [GuiEventListener] that also listens to ticks.
///
/// This is supported automatically on all screens.
public interface TickableGuiEventListener extends GuiEventListener {
	/// Tick this listener. This is invoked after [the screen ticks][Screen#tick()].
	void tick();
}
