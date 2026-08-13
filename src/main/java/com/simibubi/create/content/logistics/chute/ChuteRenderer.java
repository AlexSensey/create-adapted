package com.simibubi.create.content.logistics.chute;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.chute.ChuteBlock.Shape;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ChuteRenderer extends SafeBlockEntityRenderer<ChuteBlockEntity> {
	public ChuteRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new ChuteRenderState();
	}

	@Override
	public void extractRenderState(ChuteBlockEntity be, BlockEntityRenderState state, float partialTicks, Vec3 cameraPos,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof ChuteRenderState chuteState) {
			chuteState.blockEntity = be;
			chuteState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof ChuteRenderState chuteState))
			return;
		ChuteBlockEntity be = chuteState.blockEntity;
		if (be == null || isInvalid(be) || be.item.isEmpty())
			return;
		BlockState blockState = be.getBlockState();
		if (blockState.getValue(ChuteBlock.FACING) != Direction.DOWN)
			return;
		if (blockState.getValue(ChuteBlock.SHAPE) != Shape.WINDOW
			&& (be.bottomPullDistance == 0 || be.itemPosition.getValue(chuteState.partialTicks) > .5f))
			return;

		renderItem(be, chuteState.partialTicks, ms, collector, state.lightCoords, OverlayTexture.NO_OVERLAY);
	}

	@Override
	protected void renderSafe(ChuteBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {}

	public static void submitItem(ChuteBlockEntity be, float partialTicks, PoseStack ms, SubmitNodeCollector collector,
		int light, int overlay) {
		renderItem(be, partialTicks, ms, collector, light, overlay);
	}

	private static void renderItem(ChuteBlockEntity be, float partialTicks, PoseStack ms, SubmitNodeCollector collector,
		int light, int overlay) {
		ms.pushPose();
		ms.translate(.5f, .5f, .5f);
		float itemPosition = be.itemPosition.getValue(partialTicks);
		ms.translate(0, -.5 + itemPosition, 0);
		if (PackageItem.isPackage(be.item)) {
			ms.scale(1.5f, 1.5f, 1.5f);
		} else {
			ms.scale(.5f, .5f, .5f);
			float angle = itemPosition * 180;
			ms.mulPose(Axis.XP.rotationDegrees(angle));
			ms.mulPose(Axis.YP.rotationDegrees(angle));
		}

		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, be.item, ItemDisplayContext.FIXED, null, null, 0);
		itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static class ChuteRenderState extends BlockEntityRenderState {
		private ChuteBlockEntity blockEntity;
		private float partialTicks;
	}
}
