package com.simibubi.create.content.redstone.link.controller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class LecternControllerRenderer extends SafeBlockEntityRenderer<LecternControllerBlockEntity> {

	public LecternControllerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(LecternControllerBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new LecternControllerRenderState();
	}

	@Override
	public void extractRenderState(LecternControllerBlockEntity be, BlockEntityRenderState state, float partialTicks,
		net.minecraft.world.phys.Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof LecternControllerRenderState controllerState) {
			controllerState.blockState = be.getBlockState();
			controllerState.controller = be.getController();
			controllerState.active = be.hasUser();
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof LecternControllerRenderState controllerState))
			return;
		ItemStack controller = controllerState.controller;
		if (controllerState.blockState == null || controller.isEmpty())
			return;

		Direction facing = controllerState.blockState.getValue(LecternControllerBlock.FACING);

		ms.pushPose();
		ms.translate(0.5, 1.45, 0.5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing) - 90));
		ms.translate(0.28, 0, 0);
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-22));

		ItemStackRenderState itemState = new ItemStackRenderState();
		LinkedControllerItemModel.beginLecternRender(controllerState.active);
		try {
			Minecraft.getInstance()
				.getItemModelResolver()
				.updateForTopItem(itemState, controller, ItemDisplayContext.NONE, null, null, 0);
		} finally {
			LinkedControllerItemModel.endLecternRender();
		}
		itemState.submit(ms, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static class LecternControllerRenderState extends BlockEntityRenderState {
		private BlockState blockState;
		private ItemStack controller = ItemStack.EMPTY;
		private boolean active;
	}

}
