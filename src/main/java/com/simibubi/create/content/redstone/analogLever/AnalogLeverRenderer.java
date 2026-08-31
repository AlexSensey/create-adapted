package com.simibubi.create.content.redstone.analogLever;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.theme.Color;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;

public class AnalogLeverRenderer extends SafeBlockEntityRenderer<AnalogLeverBlockEntity> {
	private BlockStateModelPart handle;
	private BlockStateModelPart indicator;

	public AnalogLeverRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(AnalogLeverBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new AnalogLeverRenderState();
	}

	@Override
	public void extractRenderState(AnalogLeverBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof AnalogLeverRenderState leverState) {
			leverState.blockState = be.getBlockState();
			leverState.value = be.clientState.getValue(partialTicks);
			leverState.visualized = CreateVisualizationManager.supportsVisualization(be.getLevel());
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof AnalogLeverRenderState leverState) || leverState.blockState == null
			|| leverState.visualized)
			return;

		if (handle == null)
			handle = Minecraft.getInstance().getModelManager()
				.getStandaloneModel(CreateStandaloneModels.ANALOG_LEVER_HANDLE);
		if (indicator == null)
			indicator = tintAll(Minecraft.getInstance().getModelManager()
				.getStandaloneModel(CreateStandaloneModels.ANALOG_LEVER_INDICATOR));
		if (handle == null || indicator == null)
			return;

		BlockState blockState = leverState.blockState;
		AttachFace face = blockState.getValue(AnalogLeverBlock.FACE);
		float xRotation = face == AttachFace.FLOOR ? 0 : face == AttachFace.WALL ? 90 : 180;
		float yRotation = AngleHelper.horizontalAngle(blockState.getValue(AnalogLeverBlock.FACING));
		float angle = leverState.value / 15f * 90f;

		ms.pushPose();
		orient(ms, xRotation, yRotation);
		ms.translate(.5f, 1 / 16f, .5f);
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(angle));
		ms.translate(-.5f, -1 / 16f, -.5f);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(handle),
			BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();

		int color = 0xFF000000 | Color.mixColors(0x2C0300, 0xCD0000, leverState.value / 15f);
		ms.pushPose();
		orient(ms, xRotation, yRotation);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(indicator), new int[] { color },
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static void orient(PoseStack ms, float xRotation, float yRotation) {
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRotation));
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRotation));
		ms.translate(-.5f, -.5f, -.5f);
	}

	private static BlockStateModelPart tintAll(BlockStateModelPart part) {
		return part == null ? null : new TintedBlockStateModelPart(part);
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
				quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
				tintedMaterial, quad.bakedNormals(), quad.bakedColors());
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

	private static class AnalogLeverRenderState extends BlockEntityRenderState {
		private BlockState blockState;
		private float value;
		private boolean visualized;
	}

}
