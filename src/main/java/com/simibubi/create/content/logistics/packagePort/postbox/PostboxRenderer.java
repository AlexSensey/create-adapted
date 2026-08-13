package com.simibubi.create.content.logistics.packagePort.postbox;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PostboxRenderer extends SmartBlockEntityRenderer<PostboxBlockEntity> {

	public PostboxRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(PostboxBlockEntity blockEntity, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new PostboxRenderState();
	}

	@Override
	public void extractRenderState(PostboxBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof PostboxRenderState postboxState) {
			postboxState.blockEntity = be;
			postboxState.blockState = be.getBlockState();
			postboxState.flagValue = be.flag.getValue(partialTicks);
			postboxState.flagTarget = be.flag.getChaseTarget();
			postboxState.flagSettled = be.flag.settled();
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof PostboxRenderState postboxState) || postboxState.blockState == null)
			return;
		if (postboxState.blockEntity != null)
			submitBehaviours(postboxState.blockEntity, 0, ms, collector, state.lightCoords);
		BlockStateModelPart flag = Minecraft.getInstance().getModelManager()
			.getStandaloneModel(CreateStandaloneModels.POSTBOX_FLAG);
		if (flag == null)
			return;

		float rotation = 180 - postboxState.blockState.getValue(PostboxBlock.FACING).toYRot();
		float progress = flagProgress(postboxState.flagValue, postboxState.flagTarget, postboxState.flagSettled);
		ms.pushPose();
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));
		ms.translate(-.5f, -.5f, -.5f);
		ms.translate(0, 10 / 16f, 2 / 16f);
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-progress * 90));
		ms.translate(0, -10 / 16f, -2 / 16f);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(flag),
			BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
		if (postboxState.blockEntity.addressFilter != null && !postboxState.blockEntity.addressFilter.isBlank())
			submitNameplateOnHover(postboxState.blockEntity,
				Component.literal(postboxState.blockEntity.addressFilter), 1, ms, collector,
				cameraRenderState, state.lightCoords);
	}

	private static float flagProgress(float value, float target, boolean settled) {
		float progress = (float) Math.pow(Math.min(value * 5, 1), 2);
		if (target > 0 && !settled && progress == 1) {
			float wiggleProgress = (value - .2f) / .8f;
			progress += (float) ((Math.sin(wiggleProgress * (2 * Mth.PI) * 4) / 8f)
				/ Math.max(1, 8f * wiggleProgress));
		}
		return progress;
	}

	private static class PostboxRenderState extends BlockEntityRenderState {
		private PostboxBlockEntity blockEntity;
		private BlockState blockState;
		private float flagValue;
		private float flagTarget;
		private boolean flagSettled;
	}
}
