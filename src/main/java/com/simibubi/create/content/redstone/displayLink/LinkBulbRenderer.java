package com.simibubi.create.content.redstone.displayLink;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class LinkBulbRenderer extends SafeBlockEntityRenderer<LinkWithBulbBlockEntity> {

	private BlockStateModelPart tube;
	private BlockStateModelPart glow;

	public LinkBulbRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new LinkBulbRenderState();
	}

	@Override
	public void extractRenderState(LinkWithBulbBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof LinkBulbRenderState bulbState) {
			bulbState.blockEntity = be;
			bulbState.partialTicks = partialTicks;
			bulbState.glow = be.getGlow(partialTicks);
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof LinkBulbRenderState bulbState) || bulbState.blockEntity == null
			|| bulbState.glow < .125f)
			return;

		LinkWithBulbBlockEntity be = bulbState.blockEntity;
		BlockState blockState = be.getBlockState();
		Direction face = be.getBulbFacing(blockState);
		float brightness = (float) (1 - 2 * Math.pow(bulbState.glow - .75f, 2));
		brightness = Mth.clamp(brightness, -1, 1);
		if (brightness <= 0)
			return;

		if (tube == null)
			tube = Minecraft.getInstance().getModelManager()
				.getStandaloneModel(CreateStandaloneModels.DISPLAY_LINK_TUBE);
		if (glow == null)
			glow = Minecraft.getInstance().getModelManager()
				.getStandaloneModel(CreateStandaloneModels.DISPLAY_LINK_GLOW);
		if (tube == null || glow == null)
			return;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(face) + 180));
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-AngleHelper.verticalAngle(face) - 90));
		ms.translate(-.5, -.5, -.5);
		Vec3 offset = be.getBulbOffset(blockState);
		ms.translate(offset.x, offset.y, offset.z);

		int fullBright = LightCoordsUtil.pack(15, 15);
		collector.submitBlockModel(ms, RenderTypes.translucentMovingBlock(), List.of(tube),
			BlockModelRenderState.EMPTY_TINTS, fullBright, OverlayTexture.NO_OVERLAY, 0);
		collector.submitBlockModel(ms, RenderTypes.translucentMovingBlock(), List.of(glow),
			BlockModelRenderState.EMPTY_TINTS, fullBright, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	@Override
	protected void renderSafe(LinkWithBulbBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 renderer API.
	}

	private static class LinkBulbRenderState extends BlockEntityRenderState {
		private LinkWithBulbBlockEntity blockEntity;
		private float partialTicks;
		private float glow;
	}
}
