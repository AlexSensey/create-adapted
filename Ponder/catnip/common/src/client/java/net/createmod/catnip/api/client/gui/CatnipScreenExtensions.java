package net.createmod.catnip.api.client.gui;

import net.minecraft.client.gui.screens.Screen;

/// Screens may choose to implement this interface for additional functionality.
public interface CatnipScreenExtensions {
	/// @return true if this screen should close when `E` is pressed
	/// @see Screen#shouldCloseOnEsc()
	default boolean shouldCloseOnE() {
		return false;
	}
}
