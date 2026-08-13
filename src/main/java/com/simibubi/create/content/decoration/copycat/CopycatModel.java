package com.simibubi.create.content.decoration.copycat;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelData.Builder;
import net.neoforged.neoforge.model.data.ModelProperty;

public abstract class CopycatModel extends BakedModelWrapperWithData {

	public static final ModelProperty<BlockState> MATERIAL_PROPERTY = CopycatModelData.MATERIAL_PROPERTY;
	private static final ModelProperty<OcclusionData> OCCLUSION_PROPERTY = new ModelProperty<>();
	private static final ModelProperty<ModelData> WRAPPED_DATA_PROPERTY = new ModelProperty<>();
	private static final ModelProperty<Boolean> IS_EMISSIVE_PROPERTY = new ModelProperty<>();

	public CopycatModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	protected Builder gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
		ModelData blockEntityData) {
		BlockState material = getMaterial(blockEntityData);
		builder.with(MATERIAL_PROPERTY, material);
		return builder;
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
		return super.getQuads(state, side, rand);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
		return super.getQuads(state, side, rand, data, renderType);
	}

	/**
	 * The returned list must not be mutated.
	 */
	protected abstract List<BakedQuad> getCroppedQuads(BlockState state, Direction side, RandomSource rand,
		BlockState material, ModelData wrappedData, RenderType renderType);

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		return super.getParticleIcon(data);
	}

	@NotNull
	public static BlockState getMaterial(ModelData data) {
		BlockState material = data == null ? null : data.get(MATERIAL_PROPERTY);
		return material == null ? AllBlocks.COPYCAT_BASE.getDefaultState() : material;
	}

	public static BakedModel getModelOf(BlockState state) {
		return null;
	}

	private static class OcclusionData {
		private final boolean[] occluded;

		public OcclusionData() {
			occluded = new boolean[6];
		}

		public void occlude(Direction face) {
			occluded[face.get3DDataValue()] = true;
		}

		public boolean isOccluded(Direction face) {
			return face != null && occluded[face.get3DDataValue()];
		}
	}

}
