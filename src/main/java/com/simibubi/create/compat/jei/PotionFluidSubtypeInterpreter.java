package com.simibubi.create.compat.jei;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;

import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;

/* From JEI's Potion item subtype interpreter */
public class PotionFluidSubtypeInterpreter implements ISubtypeInterpreter<FluidStack> {
	@Override
	public @Nullable Object getSubtypeData(FluidStack ingredient, UidContext context) {
		if (ingredient.getComponentsPatch().isEmpty())
			return null;

		String bottleType = ingredient
			.getOrDefault(AllDataComponents.POTION_FLUID_BOTTLE_TYPE, BottleType.REGULAR)
			.name();
		PotionContents contents = ingredient.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		StringBuilder id = new StringBuilder(ingredient.getDescriptionId())
			.append(';')
			.append(bottleType);
		contents.potion().ifPresent(potion -> {
			for (MobEffectInstance effect : potion.value().getEffects())
				id.append(';').append(effect);
		});
		for (MobEffectInstance effect : contents.customEffects())
			id.append(';').append(effect);
		return id.toString();
	}
}
