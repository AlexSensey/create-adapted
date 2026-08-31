package com.simibubi.create.content.kinetics.clock;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.clock.CuckooClockBlockEntity.Animation;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.math.AngleHelper;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class CuckooClockRenderer extends KineticBlockEntityRenderer<CuckooClockBlockEntity> {

	public CuckooClockRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(CuckooClockBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof CuckooClockBlockEntity be))
			return;
		if (isInvalid(be))
			return;

		Direction direction = be.getBlockState()
			.getValue(CuckooClockBlock.HORIZONTAL_FACING);
		float partialTicks = kineticState.partialTicks;

		if (!CreateVisualizationManager.supportsVisualization(be.getLevel()))
			submitShaftHalf(be, direction, partialTicks, state, ms, collector);

		float yRot = AngleHelper.horizontalAngle(direction.getCounterClockWise());
		submitHand(CreateStandaloneModels.CUCKOO_HOUR_HAND, be.hourHand.getValue(partialTicks), yRot, state, ms,
			collector);
		submitHand(CreateStandaloneModels.CUCKOO_MINUTE_HAND, be.minuteHand.getValue(partialTicks), yRot, state, ms,
			collector);

		float angle = getDoorAngle(be, partialTicks);
		submitDoor(CreateStandaloneModels.CUCKOO_LEFT_DOOR, angle, true, yRot, state, ms, collector);
		submitDoor(CreateStandaloneModels.CUCKOO_RIGHT_DOOR, angle, false, yRot, state, ms, collector);

		if (be.animationType != Animation.NONE) {
			float offset = -(angle / 135) * 1 / 2f + 10 / 16f;
			StandaloneModelKey<BlockStateModelPart> figure = be.animationType == Animation.PIG
				? CreateStandaloneModels.CUCKOO_PIG
				: CreateStandaloneModels.CUCKOO_CREEPER;
			ms.pushPose();
			rotateCentered(ms, yRot);
			ms.translate(offset, 0, 0);
			submitPart(figure, ms, collector, state.lightCoords);
			ms.popPose();
		}
	}

	private static float getDoorAngle(CuckooClockBlockEntity be, float partialTicks) {
		float angle = 0;

		if (be.animationType != null) {
			float value = be.animationProgress.getValue(partialTicks);
			int step = be.animationType == Animation.SURPRISE ? 3 : 15;
			for (int phase = 30; phase <= 60; phase += step) {
				float local = value - phase;
				if (local < -step / 3)
					continue;
				if (local < 0)
					angle = Mth.lerp((value - (phase - 5)) / 5, 0, 135);
				else if (local < step / 3)
					angle = 135;
				else if (local < 2 * step / 3)
					angle = Mth.lerp((value - (phase + 5)) / 5, 135, 0);
			}
		}

		return angle;
	}

	private void submitShaftHalf(CuckooClockBlockEntity be, Direction direction, float partialTicks,
		BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector) {
		Direction shaftSide = direction.getOpposite();

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(shaftSide.getAxis(),
			getAngleForBe(be, be.getBlockPos(), shaftSide.getAxis(), partialTicks)));
		rotateHalfShaftTo(ms, shaftSide);
		ms.translate(-.5, -.5, -.5);
		submitPart(CreateStandaloneModels.SHAFT_HALF, ms, collector, state.lightCoords);
		ms.popPose();
	}

	private static void submitHand(StandaloneModelKey<BlockStateModelPart> key, float angle, float yRot,
		BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector) {
		float pivotX = 2 / 16f;
		float pivotY = 6 / 16f;
		float pivotZ = 8 / 16f;

		ms.pushPose();
		rotateCentered(ms, yRot);
		ms.translate(pivotX, pivotY, pivotZ);
		ms.mulPose(Axis.XP.rotationDegrees(angle));
		ms.translate(-pivotX, -pivotY, -pivotZ);
		submitPart(key, ms, collector, state.lightCoords);
		ms.popPose();
	}

	private static void submitDoor(StandaloneModelKey<BlockStateModelPart> key, float angle, boolean left, float yRot,
		BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector) {
		float pivotX = 2 / 16f;
		float pivotY = 0;
		float pivotZ = (left ? 6 : 10) / 16f;

		ms.pushPose();
		rotateCentered(ms, yRot);
		ms.translate(pivotX, pivotY, pivotZ);
		ms.mulPose(Axis.YP.rotationDegrees(angle * (left ? -1 : 1)));
		ms.translate(-pivotX, -pivotY, -pivotZ);
		submitPart(key, ms, collector, state.lightCoords);
		ms.popPose();
	}

	private static void rotateCentered(PoseStack ms, float yRot) {
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(yRot));
		ms.translate(-.5, -.5, -.5);
	}

	private static void rotateHalfShaftTo(PoseStack ms, Direction direction) {
		switch (direction) {
			case NORTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
			case EAST -> ms.mulPose(Axis.YP.rotationDegrees(90));
			case WEST -> ms.mulPose(Axis.YP.rotationDegrees(-90));
			case UP -> ms.mulPose(Axis.XP.rotationDegrees(-90));
			case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(90));
			case SOUTH -> {
			}
		}
	}

	private static void submitPart(StandaloneModelKey<BlockStateModelPart> key, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		if (part == null)
			return;
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}
}
