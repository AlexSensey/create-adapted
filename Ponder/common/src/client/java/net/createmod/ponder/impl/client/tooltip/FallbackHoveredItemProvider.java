package net.createmod.ponder.impl.client.tooltip;

import net.createmod.catnip.api.client.event.ClientTickCallback;
import net.createmod.ponder.api.client.HoveredItemProvider;
import net.createmod.ponder.api.client.event.TooltipQueryCallback;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/// When all else fails, fall back to whatever item had its tooltip queried last this tick.
enum FallbackHoveredItemProvider implements HoveredItemProvider {
	INSTANCE;

	private static Item lastQueriedItem = Items.AIR;

	@Override
	public Item determineHoveredItem() {
		return lastQueriedItem;
	}

	public static void init() {
		// the order of events is preTick, postTick, tooltip
		// we want this to happen:
		// - reset lastQueriedItem
		// - possibly update lastQueriedItem
		// - add ponder tooltip
		// to do this, we need to shift things a bit, and treat the order of operations as postTick, tooltip, preTick
		ClientTickCallback.EVENT.post().subscribe(() -> lastQueriedItem = Items.AIR);
		TooltipQueryCallback.EVENT.subscribe((stack, _, _, _) -> lastQueriedItem = stack.getItem());
		HoveredItemProvider.register(INSTANCE);
	}
}
