package com.simibubi.create.content.processing.sequenced;

import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.recipe.CreateRecipeClientCache;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class SequencedAssemblyClient {
	private SequencedAssemblyClient() {}

	public static void addToTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		SequencedAssemblyRecipe.SequencedAssembly assembly =
			stack.get(AllDataComponents.SEQUENCED_ASSEMBLY);
		if (assembly == null)
			return;

		var holder = CreateRecipeClientCache.getRecipe(assembly.id());
		if (holder == null || !(holder.value() instanceof SequencedAssemblyRecipe recipe))
			return;

		int length = recipe.getSequence().size();
		if (length == 0)
			return;

		int step = assembly.step();
		int total = length * recipe.getLoops();
		List<Component> tooltip = event.getToolTip();
		tooltip.add(CommonComponents.EMPTY);
		tooltip.add(CreateLang.translateDirect("recipe.sequenced_assembly")
			.withStyle(ChatFormatting.GRAY));
		tooltip.add(CreateLang.translateDirect("recipe.assembly.progress", step, total)
			.withStyle(ChatFormatting.DARK_GRAY));

		int remaining = total - step;
		for (int i = 0; i < length && i < remaining; i++) {
			SequencedRecipe<?> sequencedRecipe = recipe.getSequence()
				.get((i + step) % length);
			Component description = sequencedRecipe.getAsAssemblyRecipe()
				.getDescriptionForAssembly();
			if (i == 0) {
				tooltip.add(CreateLang.translateDirect("recipe.assembly.next", description)
					.withStyle(ChatFormatting.AQUA));
			} else {
				tooltip.add(Component.literal("-> ")
					.append(description)
					.withStyle(ChatFormatting.DARK_AQUA));
			}
		}
	}
}
