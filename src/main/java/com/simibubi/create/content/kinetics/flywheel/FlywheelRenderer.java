package com.simibubi.create.content.kinetics.flywheel;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

public class FlywheelRenderer extends KineticBlockEntityRenderer<FlywheelBlockEntity> {

	public FlywheelRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(FlywheelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);

		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof FlywheelBlockEntity be))
			return;
		if (isInvalid(be))
			return;
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		List<BlockStateModelPart> parts = getRotatingModelParts(be, be.getBlockState());
		if (parts.isEmpty())
			return;

		float speed = be.visualSpeed.getValue(kineticState.partialTicks) * 3 / 10f;
		float angle = be.angle + speed * kineticState.partialTicks;
		Axis axis = getRotationAxisOf(be);

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(axis, angle / 180 * (float) Math.PI));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, getRotatingRenderType(parts), parts, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	@Override
	protected BlockState getRenderedBlockState(FlywheelBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}
}
