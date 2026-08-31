package net.createmod.catnip.api.client.render;

import java.util.function.BiFunction;
import java.util.function.Function;

import net.createmod.catnip.api.Catnip;
import net.createmod.catnip.api.client.gui.texture.CatnipSpecialTextures;
import net.createmod.catnip.impl.client.mixin.RenderTypeAccessor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public abstract class PonderRenderTypes {
	private static final RenderType GUI = RenderTypeAccessor.catnip$create(
		createLayerName("gui"),
		RenderSetup.builder(RenderPipelines.GUI)
			.createRenderSetup()
	);

	private static final RenderType OUTLINE_SOLID = RenderTypeAccessor.catnip$create(
		createLayerName("outline_solid"),
		RenderSetup.builder(RenderPipelines.ENTITY_SOLID)
			.withTexture("Sampler0", CatnipSpecialTextures.BLANK.getId())
			.useLightmap()
			.useOverlay()
			.createRenderSetup()
	);

	private static final BiFunction<Identifier, Boolean, RenderType> OUTLINE_TRANSLUCENT = Util.memoize((texture, cull) ->
		RenderTypeAccessor.catnip$create(
			createLayerName("outline_translucent" + (cull ? "_cull" : "")),
			RenderSetup.builder(cull ? RenderPipelines.ENTITY_TRANSLUCENT_CULL : RenderPipelines.ENTITY_TRANSLUCENT)
				.withTexture("Sampler0", texture)
				.sortOnUpload()
				.useLightmap()
				.useOverlay()
				.setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
				.createRenderSetup()
		)
	);

	// Use the block-translucent shader from the old ghost path, with depth writes
	// disabled by CatnipRenderPipelines so later water remains visible through it.
	private static final RenderType GHOST_BLOCK = RenderTypeAccessor.catnip$create(
		createLayerName("ghost_block"),
		RenderSetup.builder(CatnipRenderPipelines.GHOST_BLOCK)
			.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
			.sortOnUpload()
			.useLightmap()
			.createRenderSetup()
	);
	private static final Function<SingleQuadParticle.Layer, RenderType> PARTICLE = Util.memoize(layer ->
		RenderTypeAccessor.catnip$create(
			createLayerName("particle_" + (layer.translucent() ? "translucent" : "opaque")),
			RenderSetup.builder(layer.pipeline())
				.withTexture("Sampler0", layer.textureAtlasLocation())
				.useLightmap()
				.createRenderSetup()
		)
	);

	public static RenderType gui() {
		return GUI;
	}

	public static RenderType outlineSolid() {
		return OUTLINE_SOLID;
	}

	public static RenderType ghostBlock() {
		return GHOST_BLOCK;
	}

	public static RenderType outlineTranslucent(Identifier texture, boolean cull) {
		return OUTLINE_TRANSLUCENT.apply(texture, cull);
	}

	public static RenderType particle(SingleQuadParticle.Layer layer) {
		return PARTICLE.apply(layer);
	}

	private static String createLayerName(String name) {
		return Catnip.ID + ":" + name;
	}
}
