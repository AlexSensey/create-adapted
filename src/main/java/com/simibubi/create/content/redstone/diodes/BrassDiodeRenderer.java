package com.simibubi.create.content.redstone.diodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueLabelRenderer;

import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.theme.Color;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class BrassDiodeRenderer extends SafeBlockEntityRenderer<BrassDiodeBlockEntity> {
	private BlockStateModelPart indicator;

	public BrassDiodeRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(BrassDiodeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new BrassDiodeRenderState();
	}

	@Override
	public void extractRenderState(BrassDiodeBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof BrassDiodeRenderState diodeState)
			diodeState.progress = be.getProgress();
		if (state instanceof BrassDiodeRenderState diodeState)
			diodeState.maxState = be.maxState;
		if (state instanceof BrassDiodeRenderState diodeState)
			diodeState.visualized = CreateVisualizationManager.supportsVisualization(be.getLevel());
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof BrassDiodeRenderState diodeState))
			return;
		ScrollValueLabelRenderer.submitBrassDiode(diodeState.maxState, state, ms, collector, cameraRenderState);
		if (diodeState.visualized)
			return;
		if (indicator == null) {
			BlockStateModelPart model = Minecraft.getInstance().getModelManager()
				.getStandaloneModel(CreateStandaloneModels.FLEXPEATER_INDICATOR);
			indicator = model == null ? null : new TintedBlockStateModelPart(model);
		}
		if (indicator == null)
			return;

		int color = 0xFF000000 | Color.mixColors(0x2C0300, 0xCD0000, diodeState.progress);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(indicator), new int[] { color },
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
	}

	private static class BrassDiodeRenderState extends BlockEntityRenderState {
		private float progress;
		private ScrollValueBehaviour maxState;
		private boolean visualized;
	}

	private static class TintedBlockStateModelPart implements BlockStateModelPart {
		private final BlockStateModelPart wrapped;
		private final Map<Direction, List<BakedQuad>> tintedQuads = new HashMap<>();
		private List<BakedQuad> tintedNullQuads;

		private TintedBlockStateModelPart(BlockStateModelPart wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public List<BakedQuad> getQuads(Direction direction) {
			if (direction == null) {
				if (tintedNullQuads == null)
					tintedNullQuads = tint(wrapped.getQuads(null));
				return tintedNullQuads;
			}
			return tintedQuads.computeIfAbsent(direction, side -> tint(wrapped.getQuads(side)));
		}

		private static List<BakedQuad> tint(List<BakedQuad> quads) {
			return quads.stream().map(TintedBlockStateModelPart::tint).toList();
		}

		private static BakedQuad tint(BakedQuad quad) {
			BakedQuad.MaterialInfo material = quad.materialInfo();
			BakedQuad.MaterialInfo tintedMaterial = new BakedQuad.MaterialInfo(material.sprite(), material.layer(),
				material.itemRenderType(), 0, material.shade(), material.lightEmission(), material.ambientOcclusion());
			return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
				quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), tintedMaterial,
				quad.bakedNormals(), quad.bakedColors());
		}

		@Override
		public boolean useAmbientOcclusion() {
			return wrapped.useAmbientOcclusion();
		}

		@Override
		public Material.Baked particleMaterial() {
			return wrapped.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return wrapped.materialFlags();
		}
	}

}
