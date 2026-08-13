package com.simibubi.create.foundation.codec;

import java.util.function.Function;

import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.simibubi.create.foundation.fluid.FluidIngredientOld;
import com.simibubi.create.foundation.item.ItemSlots;

import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CreateCodecs {
	public static final Codec<Integer> INT_STR = Codec.STRING.comapFlatMap(
		string -> {
			try {
				return DataResult.success(Integer.parseInt(string));
			} catch (NumberFormatException ignored) {
				return DataResult.error(() -> "Not an integer: " + string);
			}
		},
		String::valueOf
	);

	public static final Codec<ItemStackHandler> ITEM_STACK_HANDLER = Codec.lazyInitialized(() -> ItemSlots.CODEC.xmap(
		slots -> slots.toHandler(ItemStackHandler::new), ItemSlots::fromHandler
	));

	public static Codec<Integer> boundedIntStr(int min) {
		return INT_STR.validate(i -> i >= min ? DataResult.success(i) : DataResult.error(() -> "Value under minimum of " + min));
	}

	public static final Codec<Double> NON_NEGATIVE_DOUBLE = doubleRangeWithMessage(0, Double.MAX_VALUE,
		i -> "Value must be non-negative: " + i);
	public static final Codec<Double> POSITIVE_DOUBLE = doubleRangeWithMessage(1, Double.MAX_VALUE,
		i -> "Value must be positive: " + i);

	private static Codec<Double> doubleRangeWithMessage(double min, double max, Function<Double, String> errorMessage) {
		return Codec.DOUBLE.validate(i ->
			i.compareTo(min) >= 0 && i.compareTo(max) <= 0 ? DataResult.success(i) : DataResult.error(() ->
				errorMessage.apply(i)
			)
		);
	}

	public static Codec<SizedFluidIngredient> FLAT_SIZED_FLUID_INGREDIENT_WITH_TYPE = SizedFluidIngredient.CODEC;

	@ScheduledForRemoval(inVersion = "1.21.1+ Port")
	@Deprecated(since = "6.0.7", forRemoval = true)
	public static Codec<SizedFluidIngredient> SIZED_FLUID_INGREDIENT = Codec.withAlternative(FLAT_SIZED_FLUID_INGREDIENT_WITH_TYPE, FluidIngredientOld.CODEC);
}
