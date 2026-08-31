package com.simibubi.create.foundation.render;

import java.util.function.Function;
import java.util.function.BiFunction;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public class RenderTypes {
	private static final Function<Identifier, RenderType> CHAIN = Util.memoize(location -> RenderType.create(
		"chain_conveyor_chain",
		RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT_CULL)
			.withTexture("Sampler0", location, () -> RenderSystem.getSamplerCache()
				.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.NEAREST, true))
			.useLightmap()
			.useOverlay()
			.createRenderSetup()));
	private static final Function<Identifier, RenderType> BELT = Util.memoize(location -> RenderType.create(
		"create_belt",
		RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT)
			.withTexture("Sampler0", location, () -> RenderSystem.getSamplerCache()
				.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.NEAREST, true))
			.useLightmap()
			.useOverlay()
			.createRenderSetup()));
	private static final Function<Identifier, RenderType> SCROLLING_CUTOUT = Util.memoize(location -> RenderType.create(
		"create_scrolling_cutout",
		RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT)
			.withTexture("Sampler0", location, () -> RenderSystem.getSamplerCache()
				.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.NEAREST, true))
			.useLightmap()
			.useOverlay()
			.createRenderSetup()));
	private static final Function<Identifier, RenderType> GLUE_OVERLAY = Util.memoize(location -> RenderType.create(
		"create_glue_overlay",
		RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
			.withTexture("Sampler0", location, () -> RenderSystem.getSamplerCache()
				.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.NEAREST, true))
			.useLightmap()
			.useOverlay()
			.createRenderSetup()));
	private static final RenderType ENTITY_SOLID_BLOCK_MIPPED = blockAtlasType("create:entity_solid_block_mipped",
		RenderPipelines.ENTITY_SOLID, true);
	private static final RenderType ENTITY_CUTOUT_BLOCK_MIPPED = blockAtlasType("create:entity_cutout_block_mipped",
		RenderPipelines.ENTITY_CUTOUT, true);
	private static final RenderType ENTITY_TRANSLUCENT_BLOCK_MIPPED = blockAtlasType(
		"create:entity_translucent_block_mipped", RenderPipelines.ENTITY_TRANSLUCENT_CULL, true);
	private static final RenderPipeline ADDITIVE_PIPELINE = createAdditivePipeline();
	private static final RenderType ADDITIVE = additiveBlockAtlasType("create:additive", ADDITIVE_PIPELINE);
	private static final RenderType ITEM_GLOWING_SOLID = blockAtlasType("create:item_glowing_solid",
		RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE, false);
	private static final RenderType ITEM_GLOWING_TRANSLUCENT = blockAtlasType("create:item_glowing_translucent",
		RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE, false);

	private static RenderType blockAtlasType(String name, com.mojang.blaze3d.pipeline.RenderPipeline pipeline,
		boolean mipmap) {
		return RenderType.create(name, RenderSetup.builder(pipeline)
			.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, () -> RenderSystem.getSamplerCache()
				.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR,
					mipmap ? FilterMode.LINEAR : FilterMode.NEAREST, mipmap))
			.useLightmap()
			.useOverlay()
			.createRenderSetup());
	}

	private static RenderType additiveBlockAtlasType(String name, RenderPipeline pipeline) {
		// The legacy RenderType used by Create 1.21.1 had both of its final
		// flags enabled: it affected crumbling and, critically, sorted its
		// translucent quads before upload.  Omitting sortOnUpload in the 26.2
		// port made nested additive models depend on face submission order, so
		// an outer glass face could hide the bulb core from one direction.
		return RenderType.create(name, RenderSetup.builder(pipeline)
			.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS, () -> RenderSystem.getSamplerCache()
				.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR,
					FilterMode.NEAREST, false))
			.useLightmap()
			.useOverlay()
			.affectsCrumbling()
			.sortOnUpload()
			.createRenderSetup());
	}

	private static RenderPipeline createAdditivePipeline() {
		// Match Create 1.21.1's additive block pass.  Keeping this separate from
		// the entity-translucent pipeline prevents translucent light shells from
		// inheriting the colour of the block behind them.
		return RenderPipelines.SOLID_BLOCK.toBuilder()
			.withLocation(Identifier.fromNamespaceAndPath("create", "pipeline/entity_additive_emissive"))
			// Legacy ADDITIVE_TRANSPARENCY was SRC_ALPHA, ONE. The 26.2
			// BlendFunction.ADDITIVE constant is ONE, ONE and washes out the
			// translucent bulb texture and its coloured filament.
			.withColorTargetState(new ColorTargetState(
				new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE)))
			.withCull(false)
			.build();
	}

	public static RenderType entitySolidBlockMipped() {
		return ENTITY_SOLID_BLOCK_MIPPED;
	}

	public static RenderType entityCutoutBlockMipped() {
		return ENTITY_CUTOUT_BLOCK_MIPPED;
	}

	public static RenderType entityTranslucentBlockMipped() {
		return ENTITY_TRANSLUCENT_BLOCK_MIPPED;
	}

	public static RenderType additive() {
		return ADDITIVE;
	}

	public static final BiFunction<Identifier, Boolean, RenderType> TRAIN_MAP = Util.memoize(RenderTypes::getTrainMap);

	private static RenderType getTrainMap(Identifier location, boolean linearFiltering) {
		FilterMode filterMode = linearFiltering ? FilterMode.LINEAR : FilterMode.NEAREST;
		return RenderType.create(
			"create_train_map",
			RenderSetup.builder(RenderPipelines.GUI_TEXTURED)
				.withTexture("Sampler0", location, () -> RenderSystem.getSamplerCache()
					.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filterMode, filterMode, false))
				.useLightmap()
				.createRenderSetup());
	}

	public static RenderType itemGlowingSolid() {
		return ITEM_GLOWING_SOLID;
	}

	public static RenderType itemGlowingTranslucent() {
		return ITEM_GLOWING_TRANSLUCENT;
	}

	public static RenderType chain(Identifier location) {
		return CHAIN.apply(location);
	}

	public static RenderType belt(Identifier location) {
		return BELT.apply(location);
	}

	public static RenderType scrollingCutout(Identifier location) {
		return SCROLLING_CUTOUT.apply(location);
	}

	public static RenderType glueOverlay(Identifier location) {
		return GLUE_OVERLAY.apply(location);
	}
}
