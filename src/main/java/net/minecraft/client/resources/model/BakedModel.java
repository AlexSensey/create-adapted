package net.minecraft.client.resources.model;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.model.data.ModelData;

public interface BakedModel {
	default List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
		return getQuads(state, side, rand, ModelData.EMPTY, null);
	}

	default List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data,
		RenderType renderType) {
		return List.of();
	}

	default TextureAtlasSprite getParticleIcon(ModelData data) {
		return null;
	}

	default boolean useAmbientOcclusion() {
		return false;
	}

	default TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
		return TriState.DEFAULT;
	}

	default boolean usesBlockLight() {
		return false;
	}

	default boolean isGui3d() {
		return false;
	}

	default boolean isCustomRenderer() {
		return false;
	}

	default ItemTransforms getTransforms() {
		return ItemTransforms.NO_TRANSFORMS;
	}

	default ItemOverrides getOverrides() {
		return ItemOverrides.EMPTY;
	}

	default List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
		return List.of(this);
	}

	default BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
		return this;
	}

	default ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		return ChunkRenderTypeSet.all();
	}

	default ModelData getModelData(net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos,
		BlockState state, ModelData data) {
		return data;
	}
}
