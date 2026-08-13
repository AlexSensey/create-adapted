package com.simibubi.create.content.contraptions.chassis;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class StickerRenderer extends SafeBlockEntityRenderer<StickerBlockEntity> {

	private BlockStateModelPart headModel;

	public StickerRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(StickerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new StickerRenderState();
	}

	@Override
	public void extractRenderState(StickerBlockEntity be, BlockEntityRenderState state, float partialTicks,
		net.minecraft.world.phys.Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof StickerRenderState stickerState) {
			stickerState.blockEntity = be;
			stickerState.blockState = be.getBlockState();
			stickerState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof StickerRenderState stickerState))
			return;
		StickerBlockEntity be = stickerState.blockEntity;
		if (be == null || isInvalid(be))
			return;
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockState blockState = stickerState.blockState;
		if (!blockState.hasProperty(StickerBlock.FACING))
			return;

		BlockStateModelPart head = getHeadModel();
		if (head == null)
			return;

		float offset = be.piston.getValue(stickerState.partialTicks);
		if (be.getLevel() != Minecraft.getInstance().level && !be.isVirtual())
			offset = blockState.getValue(StickerBlock.EXTENDED) ? 1 : 0;

		Direction facing = blockState.getValue(StickerBlock.FACING);
		ms.pushPose();
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.mulPose(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(facing) + 90));
		ms.translate(-.5f, -.5f, -.5f);
		ms.translate(0, (offset * offset) * 4 / 16f, 0);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(head), BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private BlockStateModelPart getHeadModel() {
		if (headModel != null)
			return headModel;
		return headModel = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.STICKER_HEAD);
	}

	private static class StickerRenderState extends BlockEntityRenderState {
		private StickerBlockEntity blockEntity;
		private BlockState blockState;
		private float partialTicks;
	}
}
