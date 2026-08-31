package net.createmod.ponder.api.client;

import net.createmod.ponder.impl.client.tooltip.PonderTooltipHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/// Determines the item that the player is currently hovering over in their inventory.
/// Additional providers may be registered if Ponder's built-in handling is insufficient.
@FunctionalInterface
public interface HoveredItemProvider {
	/// Attempt to determine the item the player is currently hovering over.
	/// If nothing is currently hovered over, or if the hovered item cannot be determined,
	/// return [Items#AIR]. Ponder will fall back and query the other registered providers.
	Item determineHoveredItem();

	/// Register a new provider for Ponder to query. Providers are invoked in reverse-registration
	/// order, so additional providers will take priority over Ponder's built-in ones.
	///
	/// This is threadsafe on Neoforge.
	static void register(HoveredItemProvider provider) {
		PonderTooltipHandler.registerHoveredItemProvider(provider);
	}
}
