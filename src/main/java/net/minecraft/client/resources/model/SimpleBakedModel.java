package net.minecraft.client.resources.model;

import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;

public class SimpleBakedModel implements BakedModel {
	private final List<BakedQuad> unculledFaces;
	private final Map<Direction, List<BakedQuad>> culledFaces;
	private final boolean ambientOcclusion;
	private final boolean usesBlockLight;
	private final boolean gui3d;
	private final TextureAtlasSprite particle;
	private final ItemTransforms transforms;
	private final ItemOverrides overrides;

	public SimpleBakedModel(List<BakedQuad> unculledFaces, Map<Direction, List<BakedQuad>> culledFaces,
		boolean ambientOcclusion, boolean usesBlockLight, boolean gui3d, TextureAtlasSprite particle,
		ItemTransforms transforms, ItemOverrides overrides) {
		this.unculledFaces = unculledFaces;
		this.culledFaces = culledFaces;
		this.ambientOcclusion = ambientOcclusion;
		this.usesBlockLight = usesBlockLight;
		this.gui3d = gui3d;
		this.particle = particle;
		this.transforms = transforms;
		this.overrides = overrides;
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data,
		net.minecraft.client.renderer.rendertype.RenderType renderType) {
		if (side == null)
			return unculledFaces;
		return culledFaces.getOrDefault(side, List.of());
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		return particle;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return ambientOcclusion;
	}

	@Override
	public boolean usesBlockLight() {
		return usesBlockLight;
	}

	@Override
	public boolean isGui3d() {
		return gui3d;
	}

	@Override
	public ItemTransforms getTransforms() {
		return transforms;
	}

	@Override
	public ItemOverrides getOverrides() {
		return overrides;
	}
}
