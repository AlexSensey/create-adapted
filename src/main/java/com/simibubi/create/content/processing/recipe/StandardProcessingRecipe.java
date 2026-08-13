package com.simibubi.create.content.processing.recipe;

import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class StandardProcessingRecipe<T extends RecipeInput> extends ProcessingRecipe<T, ProcessingRecipeParams> {
	public StandardProcessingRecipe(IRecipeTypeInfo typeInfo, ProcessingRecipeParams params) {
		super(typeInfo, params);
	}

	@FunctionalInterface
	public interface Factory<R extends StandardProcessingRecipe<?>> extends ProcessingRecipe.Factory<ProcessingRecipeParams, R> {
		R create(ProcessingRecipeParams params);
	}

	public static class Builder<R extends StandardProcessingRecipe<?>>
		extends ProcessingRecipeBuilder<ProcessingRecipeParams, R, Builder<R>> {

		public Builder(Factory<R> factory, Identifier recipeId) {
			super(factory, recipeId);
		}

		@Override
		protected ProcessingRecipeParams createParams() {
			return new ProcessingRecipeParams();
		}

		@Override
		public Builder<R> self() {
			return this;
		}
	}

	public static class Serializer<R extends StandardProcessingRecipe<?>> {
		private static final Map<RecipeSerializer<?>, Serializer<?>> BY_SERIALIZER = new IdentityHashMap<>();

		private final Factory<R> factory;
		private final MapCodec<R> codec;
		private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;
		private final RecipeSerializer<R> serializer;

		public Serializer(Factory<R> factory) {
			this.factory = factory;
			this.codec = ProcessingRecipe.codec(factory, ProcessingRecipeParams.CODEC);
			this.streamCodec = ProcessingRecipe.streamCodec(factory, ProcessingRecipeParams.STREAM_CODEC);
			this.serializer = new RecipeSerializer<>(codec, streamCodec);
			BY_SERIALIZER.put(serializer, this);
		}

		public MapCodec<R> codec() {
			return codec;
		}

		public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
			return streamCodec;
		}

		public Factory<R> factory() {
			return factory;
		}

		public RecipeSerializer<R> asRecipeSerializer() {
			return serializer;
		}

		@SuppressWarnings("unchecked")
		public static <R extends StandardProcessingRecipe<?>> Serializer<R> from(RecipeSerializer<?> serializer) {
			return (Serializer<R>) BY_SERIALIZER.get(serializer);
		}
	}
}
