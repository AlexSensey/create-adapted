package com.simibubi.create.content.processing.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecs;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;

public record ProcessingFluidOutput(Fluid fluid, int amount, DataComponentPatch components) {
	public static final Codec<ProcessingFluidOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		BuiltInRegistries.FLUID.byNameCodec().fieldOf("id").forGetter(ProcessingFluidOutput::fluid),
		ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(ProcessingFluidOutput::amount),
		DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
			.forGetter(ProcessingFluidOutput::components)
	).apply(instance, ProcessingFluidOutput::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingFluidOutput> STREAM_CODEC = StreamCodec.composite(
		CatnipStreamCodecs.FLUID, ProcessingFluidOutput::fluid,
		ByteBufCodecs.VAR_INT, ProcessingFluidOutput::amount,
		DataComponentPatch.STREAM_CODEC, ProcessingFluidOutput::components,
		ProcessingFluidOutput::new
	);

	public static ProcessingFluidOutput of(FluidStack stack) {
		return new ProcessingFluidOutput(stack.getFluid(), stack.getAmount(), stack.getComponentsPatch());
	}

	public FluidStack getStack() {
		return new FluidStack(fluid, amount, components);
	}
}
