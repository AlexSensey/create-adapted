package com.simibubi.create.content.kinetics.transmission;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SplitShaftRenderer extends KineticBlockEntityRenderer<SplitShaftBlockEntity> {
	private List<BlockStateModelPart> shaftHalfModel;

	public SplitShaftRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(SplitShaftBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof SplitShaftBlockEntity be))
			return;
		if (isInvalid(be))
			return;
		if (CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockState blockState = be.getBlockState();
		Block block = blockState.getBlock();
		Axis boxAxis = ((IRotate) block).getRotationAxis(blockState);

		for (Direction direction : Direction.values()) {
			if (direction.getAxis() != boxAxis)
				continue;
			submitShaftHalf(be, direction, kineticState.partialTicks, state, ms, collector);
		}
	}

	private void submitShaftHalf(SplitShaftBlockEntity be, Direction direction, float partialTicks,
		BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector) {
		List<BlockStateModelPart> shaftHalf = getShaftHalfModel();
		if (shaftHalf.isEmpty())
			return;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(direction.getAxis(), getSplitShaftAngle(be, direction, partialTicks)));
		rotateHalfShaftTo(ms, direction);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), shaftHalf, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private List<BlockStateModelPart> getShaftHalfModel() {
		if (shaftHalfModel != null)
			return shaftHalfModel;
		BlockStateModelPart shaftHalf = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SHAFT_HALF);
		return shaftHalfModel = shaftHalf == null ? List.of() : List.of(shaftHalf);
	}

	private static float getSplitShaftAngle(SplitShaftBlockEntity be, Direction direction, float partialTicks) {
		Axis shaftAxis = direction.getAxis();
		BlockPos pos = be.getBlockPos();
		float time = getRenderTime(be, partialTicks);
		float offset = getRotationOffsetForPosition(be, pos, shaftAxis);
		float speed = be.getSpeed();
		if (speed == 0 && be.getTheoreticalSpeed() == 0 && be.getGeneratedSpeed() != 0)
			speed = be.getGeneratedSpeed();

		float angle = (time * speed * 3f / 10) % 360;
		angle *= be.getRotationSpeedModifier(direction);
		angle += offset;
		return angle / 180f * (float) Math.PI;
	}

	private static void rotateHalfShaftTo(PoseStack ms, Direction direction) {
		switch (direction) {
			case NORTH -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
			case EAST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
			case WEST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
			case UP -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
			case DOWN -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
			case SOUTH -> {
			}
		}
	}
}
