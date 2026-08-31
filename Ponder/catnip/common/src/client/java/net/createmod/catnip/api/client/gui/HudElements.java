package net.createmod.catnip.api.client.gui;

import net.createmod.catnip.api.platform.ServiceHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/// Registry of custom HUD elements. Threadsafe on Neoforge.
public interface HudElements {
	HudElements INSTANCE = ServiceHelper.load(HudElements.class);

	/// Register a new HUD element with the given ID.
	void register(Identifier id, Element element);

	@FunctionalInterface
	interface Element {
		void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
	}
}
