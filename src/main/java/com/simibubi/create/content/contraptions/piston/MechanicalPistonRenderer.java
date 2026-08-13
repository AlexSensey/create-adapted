package com.simibubi.create.content.contraptions.piston;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.gui.AllIcons;

import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MechanicalPistonRenderer extends KineticBlockEntityRenderer<MechanicalPistonBlockEntity> {

	public MechanicalPistonRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof MechanicalPistonBlockEntity be))
			return;
		renderMovementModeOverlay(be, ms, collector);
	}

	@Override
	protected BlockState getRenderedBlockState(MechanicalPistonBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	private static void renderMovementModeOverlay(MechanicalPistonBlockEntity be, PoseStack ms,
		SubmitNodeCollector collector) {
		if (be.movementMode == null || !be.movementMode.isActive())
			return;

		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit))
			return;

		BlockPos pos = be.getBlockPos();
		if (!blockHit.getBlockPos()
			.equals(pos))
			return;

		Direction hitSide = blockHit.getDirection();
		BlockState state = be.getBlockState();
		Level level = be.getLevel();
		ValueBoxTransform slot = be.movementMode.getSlotPositioning();
		Vec3 offset = getActiveSlotOffset(level, pos, state, slot, blockHit, hitSide);
		if (offset == null)
			return;

		AllIcons icon = be.movementMode.get()
			.getIcon();
		renderMovementModeOverlayOnSide(state, offset, hitSide, icon, ms, collector);
	}

	private static Vec3 getActiveSlotOffset(Level level, BlockPos pos, BlockState state, ValueBoxTransform slot,
		BlockHitResult blockHit, Direction side) {
		if (slot instanceof ValueBoxTransform.Sided sided)
			sided.fromSide(side);
		if (!slot.shouldRender(level, pos, state))
			return null;
		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(pos));
		if (!slot.testHit(level, pos, state, localHit))
			return null;
		return slot.getLocalOffset(level, pos, state);
	}

	private static void renderMovementModeOverlayOnSide(BlockState state, Vec3 offset, Direction side, AllIcons icon,
		PoseStack ms, SubmitNodeCollector collector) {
		if (offset == null)
			return;

		Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d,
			offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateOverlay(ms, state, side);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), MechanicalPistonRenderer::renderModeFrame);

		ms.scale(.25f, .25f, .25f);
		ms.translate(-.5f, -.5f, 1 / 256f);
		collector.submitCustomGeometry(ms, RenderTypes.textSeeThrough(AllIcons.ICON_ATLAS),
			(pose, consumer) -> icon.renderDoubleSided(pose, consumer, 0xDDDDDD));
		ms.popPose();
	}

	private static void rotateOverlay(PoseStack ms, BlockState state, Direction face) {
		switch (face) {
		case SOUTH -> {
		}
		case NORTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
		case EAST -> ms.mulPose(Axis.YP.rotationDegrees(90));
		case WEST -> ms.mulPose(Axis.YP.rotationDegrees(270));
		case UP -> ms.mulPose(Axis.XP.rotationDegrees(270));
		case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(90));
		}

		if (!face.getAxis()
			.isVertical())
			return;
		Direction facing = state.getValue(BlockStateProperties.FACING);
		if (facing.getAxis()
			.isVertical())
			return;
		ms.mulPose(Axis.ZP.rotationDegrees(AngleHelper.horizontalAngle(facing) + 180));
	}

	private static void renderModeFrame(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		renderThreePixelCorner(pose, consumer, 5, 5, 1, 1, color);
		renderThreePixelCorner(pose, consumer, 10, 5, -1, 1, color);
		renderThreePixelCorner(pose, consumer, 5, 10, 1, -1, color);
		renderThreePixelCorner(pose, consumer, 10, 10, -1, -1, color);
	}

	private static void renderThreePixelCorner(Pose pose, VertexConsumer consumer, int x, int y, int xStep, int yStep,
		int color) {
		flatPixelXY(pose, consumer, x, y, color);
		flatPixelXY(pose, consumer, x + xStep, y, color);
		flatPixelXY(pose, consumer, x, y + yStep, color);
	}

	private static void flatPixelXY(Pose pose, VertexConsumer consumer, int x, int y, int color) {
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, x * pixel - .5f, y * pixel - .5f, (x + 1) * pixel - .5f,
			(y + 1) * pixel - .5f, 0, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1,
		float z, int color) {
		consumer.addVertex(pose, x0, y0, z)
			.setColor(color);
		consumer.addVertex(pose, x1, y0, z)
			.setColor(color);
		consumer.addVertex(pose, x1, y1, z)
			.setColor(color);
		consumer.addVertex(pose, x0, y1, z)
			.setColor(color);
	}

}
