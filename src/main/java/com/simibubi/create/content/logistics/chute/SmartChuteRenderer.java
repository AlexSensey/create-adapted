package com.simibubi.create.content.logistics.chute;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

public class SmartChuteRenderer extends SmartBlockEntityRenderer<SmartChuteBlockEntity> {

	public SmartChuteRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new SmartChuteRenderState();
	}

	@Override
	public void extractRenderState(SmartChuteBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof SmartChuteRenderState chuteState) {
			chuteState.blockEntity = be;
			chuteState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof SmartChuteRenderState chuteState))
			return;
		SmartChuteBlockEntity be = chuteState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		submitBehaviours(be, chuteState.partialTicks, ms, collector, state.lightCoords);
		if (be.item.isEmpty())
			return;
		if (be.itemPosition.getValue(chuteState.partialTicks) > 0)
			return;

		ChuteRenderer.submitItem(be, chuteState.partialTicks, ms, collector, state.lightCoords, OverlayTexture.NO_OVERLAY);
	}

	@Override
	protected void renderSafe(SmartChuteBlockEntity blockEntity, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay);
		if (blockEntity.item.isEmpty())
			return;
		if (blockEntity.itemPosition.getValue(partialTicks) > 0)
			return;
	}

	private static class SmartChuteRenderState extends BlockEntityRenderState {
		private SmartChuteBlockEntity blockEntity;
		private float partialTicks;
	}

}
