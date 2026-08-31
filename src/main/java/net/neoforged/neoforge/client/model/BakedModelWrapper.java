package net.neoforged.neoforge.client.model;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;

public class BakedModelWrapper<T extends BakedModel> implements BakedModel {
	protected final T originalModel;

	public BakedModelWrapper(T originalModel) {
		this.originalModel = originalModel;
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data,
		RenderType renderType) {
		return originalModel == null ? List.of() : originalModel.getQuads(state, side, rand, data, renderType);
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		return originalModel == null ? null : originalModel.getParticleIcon(data);
	}

	@Override
	public boolean useAmbientOcclusion() {
		return originalModel != null && originalModel.useAmbientOcclusion();
	}

	@Override
	public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
		return originalModel == null ? TriState.DEFAULT : originalModel.useAmbientOcclusion(state, data, renderType);
	}

	@Override
	public boolean usesBlockLight() {
		return originalModel != null && originalModel.usesBlockLight();
	}

	@Override
	public boolean isGui3d() {
		return originalModel != null && originalModel.isGui3d();
	}

	@Override
	public boolean isCustomRenderer() {
		return originalModel != null && originalModel.isCustomRenderer();
	}

	@Override
	public ItemTransforms getTransforms() {
		return originalModel == null ? ItemTransforms.NO_TRANSFORMS : originalModel.getTransforms();
	}

	@Override
	public ItemOverrides getOverrides() {
		return originalModel == null ? ItemOverrides.EMPTY : originalModel.getOverrides();
	}

	@Override
	public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
		return originalModel == null ? List.of(this) : originalModel.getRenderPasses(stack, fabulous);
	}

	@Override
	public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
		return originalModel == null ? this : originalModel.applyTransform(context, poseStack, leftHand);
	}

	public ModelData getModelData(net.minecraft.client.renderer.block.BlockAndTintGetter world, BlockPos pos,
		BlockState state, ModelData blockEntityData) {
		return blockEntityData;
	}
}
