package com.simibubi.create.foundation.item;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.simibubi.create.AllKeys;

import net.createmod.catnip.api.client.lang.FontHelper.Palette;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ItemDescription {
	private static final Map<Item, Supplier<String>> CUSTOM_TOOLTIP_KEYS = new IdentityHashMap<>();

	public static void useKey(Item item, Supplier<String> supplier) {
		CUSTOM_TOOLTIP_KEYS.put(item, supplier);
	}

	public static void useKey(ItemLike item, String string) {
		useKey(item.asItem(), () -> string);
	}

	public static void referKey(ItemLike item, Supplier<? extends ItemLike> otherItem) {
		useKey(item.asItem(), () -> otherItem.get()
			.asItem()
			.getDescriptionId());
	}

	public static String getTooltipTranslationKey(Item item) {
		if (CUSTOM_TOOLTIP_KEYS.containsKey(item))
			return CUSTOM_TOOLTIP_KEYS.get(item).get() + ".tooltip";
		return item.getDescriptionId() + ".tooltip";
	}

	public static class Modifier implements TooltipModifier {
		private final Item item;
		private final Palette palette;

		public Modifier(Item item, Palette palette) {
			this.item = item;
			this.palette = palette;
		}

		@Override
		public void modify(ItemTooltipEvent context) {
			String tooltipKey = getTooltipTranslationKey(item);
			String summaryKey = tooltipKey + ".summary";
			boolean hasSummary = translationExists(summaryKey);
			boolean hasBehaviour = translationExists(tooltipKey + ".behaviour1");
			if (!hasSummary && !hasBehaviour)
				return;

			List<Component> tooltip = context.getToolTip();
			tooltip.add(CommonComponents.EMPTY);
			if (!AllKeys.shiftDown()) {
				tooltip.add(TooltipHelper.holdShift(palette, false));
				return;
			}

			if (hasSummary)
				tooltip.addAll(TooltipHelper.cutTextComponent(Component.translatable(summaryKey), palette));

			for (int i = 1; translationExists(tooltipKey + ".behaviour" + i); i++) {
				String conditionKey = tooltipKey + ".condition" + i;
				String behaviourKey = tooltipKey + ".behaviour" + i;
				tooltip.add(CommonComponents.EMPTY);
				if (translationExists(conditionKey))
					tooltip.add(Component.translatable(conditionKey)
						.withStyle(palette.highlight()));
				tooltip.addAll(TooltipHelper.cutTextComponent(Component.translatable(behaviourKey), palette));
			}
		}

		private static boolean translationExists(String key) {
			return !I18n.get(key)
				.equals(key);
		}
	}
}
