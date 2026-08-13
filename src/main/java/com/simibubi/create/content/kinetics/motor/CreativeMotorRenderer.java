package com.simibubi.create.content.kinetics.motor;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class CreativeMotorRenderer extends KineticBlockEntityRenderer<CreativeMotorBlockEntity> {

	public CreativeMotorRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected BlockState getRenderedBlockState(CreativeMotorBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof CreativeMotorBlockEntity be))
			return;
		if (isInvalid(be))
			return;

		BlockState renderedState = getRenderedBlockState(be);
		List<BlockStateModelPart> parts = getRotatingModelParts(be, renderedState);
		if (parts.isEmpty())
			return;

		Direction facing = be.getBlockState()
			.getValue(CreativeMotorBlock.FACING);

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(getRotationAxisOf(be), getAngleForBe(be, be.getBlockPos(), getRotationAxisOf(be),
			kineticState.partialTicks)));
		ms.translate(facing.getStepX() * .25, facing.getStepY() * .25, facing.getStepZ() * .25);
		switch (facing.getAxis()) {
			case X -> ms.scale(.5f, 1, 1);
			case Y -> ms.scale(1, .5f, 1);
			case Z -> ms.scale(1, 1, .5f);
		}
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, getRotatingRenderType(parts), parts, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

}
