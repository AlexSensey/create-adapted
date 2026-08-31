package net.createmod.ponder.api.client.event;

import java.util.List;

import net.createmod.catnip.api.event.CatnipEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/// Invoked whenever an item's tooltip is queried. Client-side only.
@FunctionalInterface
public interface TooltipQueryCallback {
	CatnipEvent<TooltipQueryCallback> EVENT = CatnipEvent.create(callbacks -> (stack, context, flag, tooltip) -> {
		for (TooltipQueryCallback callback : callbacks) {
			callback.onTooltipQuery(stack, context, flag, tooltip);
		}
	});

	void onTooltipQuery(ItemStack stack, TooltipContext context, TooltipFlag flag, List<Component> tooltip);
}
