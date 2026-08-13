package com.simibubi.create.foundation.blockEntity.behaviour.scrollValue;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.content.logistics.depot.EjectorBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.gui.AllIcons;

import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ScrollValueLabelRenderer {
	private static final long FADE_MS = 2000;
	private static final long FADE_DELAY_MS = 1200;
	private static final Map<BlockPos, OverlayState> LAST_OVERLAYS = new HashMap<>();
	private static final OverlayConfig MOTOR_OVERLAY =
		new OverlayConfig(6 / 16f, 6 / 16f, 2 / 16f, 1 / 16f, .5f, .5f, .09f, .044f, .009f);
	private static final OverlayConfig SPEED_CONTROLLER_OVERLAY =
		new OverlayConfig(6 / 16f, 6 / 16f, 2 / 16f, 1 / 16f, .5f, .5f, .09f, .044f, .009f);
	private static final OverlayConfig EJECTOR_OVERLAY =
		new OverlayConfig(6 / 16f, 6 / 16f, 2 / 16f, 1 / 16f, .5f, .5f, .09f, .044f, .009f);
	private static final OverlayConfig VALVE_HANDLE_OVERLAY =
		new OverlayConfig(6 / 16f, 6 / 16f, 2 / 16f, 1 / 16f, .5f, .5f, .105f, .05f, .01f);
	private static final OverlayConfig BRASS_DIODE_OVERLAY =
		new OverlayConfig(1f, 1f, .24f, .08f, .5f, .5f, .18f, .09f, .018f);

	public static void submitMotorValueBox(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !(mc.hitResult instanceof BlockHitResult hit))
			return;
		if (!(mc.level.getBlockEntity(hit.getBlockPos()) instanceof CreativeMotorBlockEntity be))
			return;

		BlockPos pos = be.getBlockPos();
		ms.pushPose();
		ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
		submitMotor(be, null, ms, collector, null);
		ms.popPose();
	}

	public static void submitBrassDiode(ScrollValueBehaviour behaviour, BlockEntityRenderState renderState,
		PoseStack ms, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		if (behaviour == null || !behaviour.isActive())
			return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !(mc.hitResult instanceof BlockHitResult blockHit))
			return;
		BlockPos pos = behaviour.getPos();
		if (!blockHit.getBlockPos().equals(pos))
			return;

		LevelAccessor level = behaviour.getWorld();
		BlockState blockState = level.getBlockState(pos);
		ValueBoxTransform slot = behaviour.slotPositioning;
		if (!slot.shouldRender(level, pos, blockState))
			return;

		boolean active = behaviour.testHit(blockHit.getLocation());
		OverlayState overlay = getBehaviourOverlay(behaviour, active);
		if (overlay == null)
			return;
		Vec3 offset = slot.getLocalOffset(level, pos, blockState);
		if (offset == null)
			return;

		ms.pushPose();
		Vec3 normal = Vec3.atLowerCornerOf(Direction.UP.getUnitVec3i());
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d,
			offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateToFace(ms, Direction.UP);
		Direction facing = blockState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
		float rotation = switch (facing) {
			case NORTH -> 0;
			case SOUTH -> 180;
			case WEST -> 90;
			case EAST -> 270;
			default -> 0;
		};
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation + 180));
		ms.scale(.5f, .5f, .5f);
		int digitAlpha = overlay.fade > 0 ? 255 : 0;
		String ticks = behaviour.formatValue();
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> renderOverlay(pose, consumer, ticks, digitAlpha, overlay.active,
				BRASS_DIODE_OVERLAY));
		ms.popPose();
	}

	public static void submitMotor(BlockPos pos, Direction motorFacing, int value, BlockEntityRenderState state, PoseStack ms,
		SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		OverlayState overlay =
			getOverlay(pos, face -> isMotorFaceAllowed(face, motorFacing), face -> motorFaceShift(face, motorFacing), MOTOR_OVERLAY);
		if (overlay == null)
			return;
		FaceShift shift = motorFaceShift(overlay.face, motorFacing);
		if (overlay.face == Direction.UP)
			shift = new FaceShift(shift.x - 1 / 16f, shift.y - 1 / 16f);
		submitOnFace(overlay, motorFaceOffset(overlay.face), shift.y, shift.x, Integer.toString(Math.abs(value)), state, ms,
			collector, MOTOR_OVERLAY, motorFacing);
	}

	public static void submitMotor(CreativeMotorBlockEntity be, BlockEntityRenderState renderState, PoseStack ms,
		SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (mc.level == null || !(hitResult instanceof BlockHitResult blockHit))
			return;

		BlockPos pos = be.getBlockPos();
		ScrollValueBehaviour behaviour = be.generatedSpeed;
		ValueBoxTransform slot = behaviour.slotPositioning;
		BlockState blockState = be.getBlockState();
		Direction activeFace = null;
		boolean active = false;

		if (blockHit.getBlockPos()
			.equals(pos)) {
			Direction hitFace = blockHit.getDirection();
			if (slot instanceof ValueBoxTransform.Sided sided)
				sided.fromSide(hitFace);
			active = slot.shouldRender(behaviour.getWorld(), pos, blockState);
			if (active)
				activeFace = hitFace;
		}

		OverlayState overlay = getBehaviourOverlay(behaviour, activeFace, active);
		if (overlay == null)
			return;

		if (slot instanceof ValueBoxTransform.Sided sided)
			sided.fromSide(overlay.face);
		if (!slot.shouldRender(behaviour.getWorld(), pos, blockState))
			return;

		Vec3 offset = slot.getLocalOffset(behaviour.getWorld(), pos, blockState);
		if (offset == null)
			return;

		Direction face = overlay.face;
		Vec3 normal = Vec3.atLowerCornerOf(face.getUnitVec3i());
		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d, offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateToFace(ms, face);
		if (face == Direction.UP)
			rotateTopMotorLabel(ms, blockState.getValue(com.simibubi.create.content.kinetics.motor.CreativeMotorBlock.FACING));
		int digitAlpha = overlay.fade > 0 ? 255 : 0;
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, vertexConsumer) -> renderOverlay(pose, vertexConsumer,
				Integer.toString(Math.abs(be.generatedSpeed.getValue())), digitAlpha, overlay.active, MOTOR_OVERLAY));
		ms.popPose();
	}

	public static void submitSpeedController(BlockPos pos, Axis controllerAxis, int value, BlockEntityRenderState state,
		PoseStack ms, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		OverlayState overlay = getOverlay(pos, face -> !face.getAxis()
			.isVertical() && face.getAxis() != controllerAxis, face -> new FaceShift(0, .1875f), SPEED_CONTROLLER_OVERLAY);
		if (overlay == null)
			return;

		submitOnFace(overlay, .501f, .1875f, 0, Integer.toString(Math.abs(value)), state, ms, collector,
			SPEED_CONTROLLER_OVERLAY, null);
	}

	public static void submitSpeedController(SpeedControllerBlockEntity be, BlockEntityRenderState renderState, PoseStack ms,
		SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (mc.level == null || !(hitResult instanceof BlockHitResult blockHit))
			return;

		BlockPos pos = be.getBlockPos();
		ScrollValueBehaviour behaviour = be.targetSpeed;
		ValueBoxTransform slot = behaviour.slotPositioning;
		BlockState blockState = be.getBlockState();
		Direction activeFace = null;
		boolean active = false;

		if (blockHit.getBlockPos()
			.equals(pos)) {
			Direction hitFace = blockHit.getDirection();
			if (slot instanceof ValueBoxTransform.Sided sided)
				sided.fromSide(hitFace);
			active = slot.shouldRender(behaviour.getWorld(), pos, blockState) && behaviour.testHit(blockHit.getLocation());
			if (active)
				activeFace = hitFace;
		}

		OverlayState overlay = getBehaviourOverlay(behaviour, activeFace, active);
		if (overlay == null)
			return;

		if (slot instanceof ValueBoxTransform.Sided sided)
			sided.fromSide(overlay.face);
		if (!slot.shouldRender(behaviour.getWorld(), pos, blockState))
			return;

		Vec3 offset = slot.getLocalOffset(behaviour.getWorld(), pos, blockState);
		if (offset == null)
			return;

		Direction face = overlay.face;
		Vec3 normal = Vec3.atLowerCornerOf(face.getUnitVec3i());
		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d, offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateToFace(ms, face);
		int digitAlpha = overlay.fade > 0 ? 255 : 0;
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, vertexConsumer) -> renderOverlay(pose, vertexConsumer,
				Integer.toString(Math.abs(be.targetSpeed.getValue())), digitAlpha, overlay.active,
				SPEED_CONTROLLER_OVERLAY));
		ms.popPose();
	}

	public static void submitValveHandle(ValveHandleBlockEntity be, BlockEntityRenderState renderState, PoseStack ms,
		SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		if (be.angleInput == null)
			return;

		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (mc.level == null || !(hitResult instanceof BlockHitResult blockHit))
			return;

		BlockPos pos = be.getBlockPos();
		ScrollValueBehaviour behaviour = be.angleInput;
		ValueBoxTransform slot = behaviour.slotPositioning;
		BlockState blockState = be.getBlockState();
		Direction activeFace = null;
		boolean active = false;

		if (blockHit.getBlockPos()
			.equals(pos)) {
			Direction hitFace = blockHit.getDirection();
			if (slot instanceof ValueBoxTransform.Sided sided)
				sided.fromSide(hitFace);
			active = slot.shouldRender(behaviour.getWorld(), pos, blockState) && behaviour.testHit(blockHit.getLocation());
			if (active)
				activeFace = hitFace;
		}

		OverlayState overlay = getBehaviourOverlay(behaviour, activeFace, active);
		if (overlay == null)
			return;

		if (slot instanceof ValueBoxTransform.Sided sided)
			sided.fromSide(overlay.face);
		if (!slot.shouldRender(behaviour.getWorld(), pos, blockState))
			return;

		Vec3 offset = slot.getLocalOffset(behaviour.getWorld(), pos, blockState);
		if (offset == null)
			return;

		Direction face = overlay.face;
		Vec3 normal = Vec3.atLowerCornerOf(face.getUnitVec3i());
		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d, offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateToFace(ms, face);
		int digitAlpha = overlay.fade > 0 ? 255 : 0;
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, vertexConsumer) -> renderDigits(pose, vertexConsumer,
				Integer.toString(Math.abs(behaviour.getValue())), digitAlpha, VALVE_HANDLE_OVERLAY));
		ms.popPose();
	}

	public static void submitEjector(BlockPos pos, Direction facing, boolean topActive, String value,
		BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		OverlayState overlay = getOverlay(pos, face -> isEjectorFaceAllowed(face, facing, topActive),
			face -> ejectorHitFaceShift(face, facing), EJECTOR_OVERLAY);
		if (overlay == null && topActive)
			overlay = getTopFaceOverlay(pos, facing);
		if (overlay == null)
			return;

		float faceOffset = overlay.face == Direction.UP ? .189f : .501f;
		FaceShift shift = ejectorFaceShift(overlay.face, facing);
		submitOnFace(overlay, faceOffset, shift.y, shift.x, value, state, ms, collector, EJECTOR_OVERLAY,
			overlay.face == Direction.UP ? facing : null);
	}

	public static void submitEjector(ScrollValueBehaviour behaviour, BlockEntityRenderState renderState, PoseStack ms,
		SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (mc.level == null || !(hitResult instanceof BlockHitResult blockHit))
			return;

		BlockPos pos = behaviour.getPos();
		if (!blockHit.getBlockPos()
			.equals(pos))
			return;

		LevelAccessor level = behaviour.getWorld();
		BlockState blockState = level.getBlockState(pos);
		ValueBoxTransform slot = behaviour.slotPositioning;
		if (slot instanceof ValueBoxTransform.Sided sided)
			sided.fromSide(blockHit.getDirection());
		if (!slot.shouldRender(level, pos, blockState))
			return;

		boolean active = behaviour.testHit(blockHit.getLocation());
		OverlayState overlay = getBehaviourOverlay(behaviour, active);
		if (overlay == null)
			return;

		Vec3 offset = slot.getLocalOffset(level, pos, blockState);
		if (offset == null)
			return;

		ms.pushPose();
		Direction hitFace = blockHit.getDirection();
		Vec3 normal = Vec3.atLowerCornerOf(hitFace.getUnitVec3i());
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d, offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateEjectorSlot(ms, blockState, hitFace);
		int digitAlpha = overlay.fade > 0 ? 255 : 0;
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, vertexConsumer) -> renderOverlay(pose, vertexConsumer, behaviour.formatValue(), digitAlpha,
				overlay.active, EJECTOR_OVERLAY));
		ms.popPose();
	}

	public static void submitWallAttachedScrollOption(ScrollOptionBehaviour<?> behaviour, BlockEntityRenderState renderState,
		PoseStack ms, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		if (behaviour == null || !behaviour.isActive())
			return;

		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (mc.level == null || !(hitResult instanceof BlockHitResult blockHit))
			return;

		BlockPos pos = behaviour.getPos();
		if (!blockHit.getBlockPos()
			.equals(pos))
			return;

		LevelAccessor level = behaviour.getWorld();
		BlockState blockState = level.getBlockState(pos);
		ValueBoxTransform slot = behaviour.slotPositioning;
		Direction hitFace = blockHit.getDirection();
		if (slot instanceof ValueBoxTransform.Sided sided)
			sided.fromSide(hitFace);
		if (!slot.shouldRender(level, pos, blockState))
			return;
		if (!behaviour.testHit(blockHit.getLocation()))
			return;

		Vec3 offset = slot.getLocalOffset(level, pos, blockState);
		if (offset == null)
			return;

		AllIcons icon = behaviour.getIconForSelected()
			.getIcon();
		Vec3 normal = Vec3.atLowerCornerOf(hitFace.getUnitVec3i());
		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d,
			offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateToFace(ms, hitFace);
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));
		ms.scale(-1, 1, 1);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, vertexConsumer) -> renderCorners(pose, vertexConsumer, EJECTOR_OVERLAY));

		ms.scale(.25f, .25f, .25f);
		ms.translate(-.5f, -.5f, 1 / 256f);
		collector.submitCustomGeometry(ms, RenderTypes.textSeeThrough(AllIcons.ICON_ATLAS),
			(pose, vertexConsumer) -> icon.renderDoubleSided(pose, vertexConsumer, 0xDDDDDD));
		ms.popPose();
	}

	private static void rotateEjectorSlot(PoseStack ms, BlockState state, Direction side) {
		rotateToFace(ms, side);
		if (!side.getAxis()
			.isVertical())
			return;
		if (!state.hasProperty(EjectorBlock.HORIZONTAL_FACING))
			return;

		Direction facing = state.getValue(EjectorBlock.HORIZONTAL_FACING);
		float angle = switch (facing) {
			case NORTH -> 0;
			case SOUTH -> 180;
			case WEST -> 90;
			case EAST -> 270;
			default -> 0;
		};
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
	}

	private static OverlayState getBehaviourOverlay(ScrollValueBehaviour behaviour, boolean active) {
		BlockPos pos = behaviour.getPos();
		long now = System.currentTimeMillis();
		if (active) {
			OverlayState state = new OverlayState(Direction.UP, now, true, 1);
			LAST_OVERLAYS.put(pos.immutable(), state);
			return state;
		}

		OverlayState previous = LAST_OVERLAYS.get(pos);
		if (previous == null)
			return null;

		long elapsed = now - previous.lastSeen;
		if (elapsed > FADE_MS) {
			LAST_OVERLAYS.remove(pos);
			return null;
		}

		float fade = elapsed <= FADE_DELAY_MS ? 1 : 1 - (elapsed - FADE_DELAY_MS) / (float) (FADE_MS - FADE_DELAY_MS);
		return new OverlayState(Direction.UP, previous.lastSeen, false, fade);
	}

	private static OverlayState getBehaviourOverlay(ScrollValueBehaviour behaviour, Direction activeFace, boolean active) {
		BlockPos pos = behaviour.getPos();
		long now = System.currentTimeMillis();
		if (active && activeFace != null) {
			OverlayState state = new OverlayState(activeFace, now, true, 1);
			LAST_OVERLAYS.put(pos.immutable(), state);
			return state;
		}

		OverlayState previous = LAST_OVERLAYS.get(pos);
		if (previous == null)
			return null;

		long elapsed = now - previous.lastSeen;
		if (elapsed > FADE_MS) {
			LAST_OVERLAYS.remove(pos);
			return null;
		}

		float fade = elapsed <= FADE_DELAY_MS ? 1 : 1 - (elapsed - FADE_DELAY_MS) / (float) (FADE_MS - FADE_DELAY_MS);
		return new OverlayState(previous.face, previous.lastSeen, false, fade);
	}

	private static OverlayState getTopFaceOverlay(BlockPos pos, Direction facing) {
		long now = System.currentTimeMillis();
		HitResult hitResult = Minecraft.getInstance().hitResult;
		FaceShift shift = ejectorHitFaceShift(Direction.UP, facing);
		if (hitResult instanceof BlockHitResult blockHit && blockHit.getBlockPos()
			.equals(pos) && blockHit.getDirection() == Direction.UP
			&& hitsValueSquare(pos, blockHit, Direction.UP, shift.x, shift.y, EJECTOR_OVERLAY)) {
			OverlayState state = new OverlayState(Direction.UP, now, true, 1);
			LAST_OVERLAYS.put(pos.immutable(), state);
			return state;
		}
		return null;
	}

	private static OverlayState getOverlay(BlockPos pos, java.util.function.Predicate<Direction> faceFilter,
		java.util.function.Function<Direction, FaceShift> faceShift, OverlayConfig config) {
		long now = System.currentTimeMillis();
		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (hitResult instanceof BlockHitResult blockHit && blockHit.getBlockPos()
			.equals(pos)) {
			Direction face = blockHit.getDirection();
			FaceShift shift = faceShift.apply(face);
			if (faceFilter.test(face) && hitsValueSquare(pos, blockHit, face, shift.x, shift.y, config)) {
				OverlayState state = new OverlayState(face, now, true, 1);
				LAST_OVERLAYS.put(pos.immutable(), state);
				return state;
			}
		}

		OverlayState previous = LAST_OVERLAYS.get(pos);
		if (previous == null)
			return null;

		long elapsed = now - previous.lastSeen;
		if (elapsed > FADE_MS) {
			LAST_OVERLAYS.remove(pos);
			return null;
		}

		float fade = elapsed <= FADE_DELAY_MS ? 1 : 1 - (elapsed - FADE_DELAY_MS) / (float) (FADE_MS - FADE_DELAY_MS);
		return new OverlayState(previous.face, previous.lastSeen, false, fade);
	}

	private static boolean hitsValueSquare(BlockPos pos, BlockHitResult hit, Direction face, float xOffset, float yOffset,
		OverlayConfig config) {
		Vec3 local = hit.getLocation()
			.subtract(pos.getX(), pos.getY(), pos.getZ());
		float u;
		float v;
		switch (face) {
			case NORTH -> {
				u = (float) (.5 - local.x);
				v = (float) (local.y - .5);
			}
			case SOUTH -> {
				u = (float) (local.x - .5);
				v = (float) (local.y - .5);
			}
			case WEST -> {
				u = (float) (local.z - .5);
				v = (float) (local.y - .5);
			}
			case EAST -> {
				u = (float) (.5 - local.z);
				v = (float) (local.y - .5);
			}
			case UP -> {
				u = (float) (local.x - .5);
				v = (float) (.5 - local.z);
			}
			case DOWN -> {
				u = (float) (local.x - .5);
				v = (float) (local.z - .5);
			}
			default -> {
				u = 0;
				v = 0;
			}
		}
		return Math.abs(u - xOffset) <= config.hitWidth / 2 && Math.abs(v - yOffset) <= config.hitHeight / 2;
	}

	private static boolean isMotorFaceAllowed(Direction face, Direction motorFacing) {
		if (face == Direction.UP)
			return true;
		if (face.getAxis()
			.isVertical())
			return false;
		if (motorFacing.getAxis()
			.isVertical())
			return true;
		return face.getAxis() != motorFacing.getAxis();
	}

	private static boolean isEjectorFaceAllowed(Direction face, Direction facing, boolean topActive) {
		if (face == Direction.UP)
			return topActive;
		if (face.getAxis()
			.isVertical())
			return false;
		return face.getAxis() == facing.getAxis();
	}

	private static FaceShift ejectorFaceShift(Direction face, Direction facing) {
		if (face != Direction.UP)
			return new FaceShift(0, -2 / 16f);

		Vec3 topSlot = new Vec3(.5, 10.5 / 16f, .5)
			.add(VecHelper.rotate(VecHelper.voxelSpace(0, 0, -5), AngleHelper.horizontalAngle(facing), Axis.Y));
		float x = (float) (topSlot.x - .5);
		float y = (float) (.5 - topSlot.z);
		return new FaceShift(y, -x);
	}

	private static FaceShift ejectorHitFaceShift(Direction face, Direction facing) {
		if (face != Direction.UP)
			return ejectorFaceShift(face, facing);
		return new FaceShift(-5 / 16f, 0);
	}

	private static float motorFaceOffset(Direction face) {
		return face == Direction.UP ? .314f : .314f;
	}

	private static FaceShift motorFaceShift(Direction face, Direction motorFacing) {
		double x = -motorFacing.getStepX() / 16d;
		double y = -motorFacing.getStepY() / 16d;
		double z = -motorFacing.getStepZ() / 16d;

		return switch (face) {
			case NORTH -> new FaceShift((float) -x, (float) y);
			case SOUTH -> new FaceShift((float) x, (float) y);
			case WEST -> new FaceShift((float) z, (float) y);
			case EAST -> new FaceShift((float) -z, (float) y);
			case UP -> new FaceShift((float) x, (float) -z);
			case DOWN -> new FaceShift((float) x, (float) z);
		};
	}

	private static void submitOnFace(OverlayState overlay, float faceOffset, float yOffset, float xOffset, String text,
		BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector, OverlayConfig config, Direction motorFacing) {
		ms.pushPose();
		Direction face = overlay.face;
		Vec3 normal = Vec3.atLowerCornerOf(face.getUnitVec3i());
		Vec3 labelPos = new Vec3(.5, .5, .5).add(normal.scale(faceOffset));
		ms.translate(labelPos.x, labelPos.y, labelPos.z);
		rotateToFace(ms, face);
		if (face == Direction.UP && motorFacing != null)
			rotateTopMotorLabel(ms, motorFacing);
		ms.translate(xOffset, yOffset, 0);
		int digitAlpha = overlay.fade > 0 ? 255 : 0;
		boolean active = overlay.active;
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, vertexConsumer) -> renderOverlay(pose, vertexConsumer, text, digitAlpha, active, config));
		ms.popPose();
	}

	private static void rotateTopMotorLabel(PoseStack ms, Direction motorFacing) {
		if (motorFacing.getAxis()
			.isVertical())
			return;
		float angle = switch (motorFacing) {
			case NORTH -> 0;
			case SOUTH -> 180;
			case WEST -> 90;
			case EAST -> 270;
			default -> 0;
		};
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
	}

	private static void rotateToFace(PoseStack ms, Direction face) {
		switch (face) {
			case SOUTH -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0));
			case NORTH -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
			case EAST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
			case WEST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270));
			case UP -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(270));
			case DOWN -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
		}
	}

	private static void renderOverlay(Pose pose, VertexConsumer consumer, String text, int digitAlpha, boolean active,
		OverlayConfig config) {
		if (active)
			renderCorners(pose, consumer, config);
		if (config == BRASS_DIODE_OVERLAY)
			renderBrassDiodeValue(pose, consumer, text, digitAlpha);
		else
			renderDigits(pose, consumer, text, digitAlpha, config);
	}

	private static void renderBrassDiodeValue(Pose pose, VertexConsumer consumer, String text, int alpha) {
		if (text.isEmpty())
			return;
		char last = text.charAt(text.length() - 1);
		boolean hasUnit = last == 't' || last == 's' || last == 'm';
		String number = hasUnit ? text.substring(0, text.length() - 1) : text;
		if (number.isEmpty())
			return;

		float cell = switch (number.length()) {
			case 1 -> .041f;
			case 2 -> .036f;
			default -> .029f;
		};
		float gap = cell * .55f;
		float digitWidth = 5 * cell;
		float numberWidth = number.length() * digitWidth + Math.max(0, number.length() - 1) * gap;
		String[] unitGlyph = hasUnit ? durationUnitGlyph(last) : new String[0];
		float unitCell = cell * .95f;
		float unitGap = hasUnit ? cell * .65f : 0;
		float unitWidth = hasUnit ? unitGlyph[0].length() * unitCell : 0;
		float totalWidth = numberWidth + unitGap + unitWidth;
		float startX = -totalWidth / 2;
		float digitBottom = -3.5f * cell;
		int color = (alpha << 24) | 0x351515;

		for (int i = 0; i < number.length(); i++) {
			String[] rows = durationDigitGlyph(number.charAt(i));
			renderPixelGlyph(pose, consumer, rows, startX + i * (digitWidth + gap), digitBottom, cell, color);
		}
		if (hasUnit) {
			float unitX = startX + numberWidth + unitGap;
			float unitBottom = digitBottom;
			renderPixelGlyph(pose, consumer, unitGlyph, unitX, unitBottom, unitCell, color);
		}
	}

	private static void renderPixelGlyph(Pose pose, VertexConsumer consumer, String[] rows, float x, float bottom,
		float cell, int color) {
		for (int row = 0; row < rows.length; row++) {
			String bits = rows[row];
			for (int column = 0; column < bits.length(); column++) {
				if (bits.charAt(column) != '1')
					continue;
				float px = x + column * cell;
				float py = bottom + (rows.length - 1 - row) * cell;
				quad(pose, consumer, px, py, px + cell, py + cell, 1 / 512f, color);
			}
		}
	}

	private static String[] durationDigitGlyph(char c) {
		return switch (c) {
			case '0' -> new String[] { "01110", "10001", "10011", "10101", "11001", "10001", "01110" };
			case '1' -> new String[] { "00100", "01100", "00100", "00100", "00100", "00100", "01110" };
			case '2' -> new String[] { "01110", "10001", "00001", "00010", "00100", "01000", "11111" };
			case '3' -> new String[] { "11110", "00001", "00010", "00110", "00001", "10001", "01110" };
			case '4' -> new String[] { "00010", "00110", "01010", "10010", "11111", "00010", "00010" };
			case '5' -> new String[] { "11111", "10000", "11110", "00001", "00001", "10001", "01110" };
			case '6' -> new String[] { "00110", "01000", "10000", "11110", "10001", "10001", "01110" };
			case '7' -> new String[] { "11111", "00001", "00010", "00100", "01000", "01000", "01000" };
			case '8' -> new String[] { "01110", "10001", "10001", "01110", "10001", "10001", "01110" };
			case '9' -> new String[] { "01110", "10001", "10001", "01111", "00001", "00010", "01100" };
			default -> new String[] { "00000", "00000", "00000", "00000", "00000", "00000", "00000" };
		};
	}

	private static String[] durationUnitGlyph(char c) {
		return switch (c) {
			case 't' -> new String[] { "010", "111", "010", "010", "011" };
			case 's' -> new String[] { "111", "100", "111", "001", "111" };
			case 'm' -> new String[] { "10001", "11011", "10101", "10101", "10101" };
			default -> new String[] { "000", "000", "000", "000", "000" };
		};
	}

	private static void renderCorners(Pose pose, VertexConsumer consumer, OverlayConfig config) {
		float w = config.frameWidth;
		float h = config.frameHeight;
		float x0 = -w / 2;
		float x1 = w / 2;
		float y0 = -h / 2;
		float y1 = h / 2;
		float l = config.cornerLength;
		float t = config.cornerThickness;
		int color = 0xFFFFFFFF;

		quad(pose, consumer, x0, y1 - t, x0 + l, y1, color);
		quad(pose, consumer, x0, y1 - l, x0 + t, y1, color);
		quad(pose, consumer, x1 - l, y1 - t, x1, y1, color);
		quad(pose, consumer, x1 - t, y1 - l, x1, y1, color);
		quad(pose, consumer, x0, y0, x0 + l, y0 + t, color);
		quad(pose, consumer, x0, y0, x0 + t, y0 + l, color);
		quad(pose, consumer, x1 - l, y0, x1, y0 + t, color);
		quad(pose, consumer, x1 - t, y0, x1, y0 + l, color);
	}

	private static void renderDigits(Pose pose, VertexConsumer consumer, String text, int alpha, OverlayConfig config) {
		float scale = text.length() <= 1 ? 1.24f : text.length() == 2 ? 1.16f : 1;
		float digitHeight = config.digitHeight * scale;
		float digitWidth = config.digitWidth * scale;
		float gap = config.digitGap * scale;
		float totalWidth = text.length() * digitWidth + Math.max(0, text.length() - 1) * gap;
		float x = -totalWidth / 2;
		int white = (alpha << 24) | 0xFFFFFF;

		for (int i = 0; i < text.length(); i++) {
			int digit = text.charAt(i) - '0';
			if (digit >= 0 && digit <= 9) {
				float y = -digitHeight / 2;
				renderDigit(pose, consumer, digit, x, y, digitWidth, digitHeight, 0, white);
			} else if (text.charAt(i) == '*') {
				float y = -digitHeight / 2;
				renderAnyMarker(pose, consumer, x, y, digitWidth, digitHeight, white);
			} else if (text.charAt(i) == 's' || text.charAt(i) == 't' || text.charAt(i) == 'm') {
				float y = -digitHeight / 2;
				renderUnit(pose, consumer, text.charAt(i), x, y, digitWidth, digitHeight, white);
			}
			x += digitWidth + gap;
		}
	}

	private static void renderUnit(Pose pose, VertexConsumer consumer, char unit, float x, float y, float w, float h,
		int color) {
		float t = Math.min(w, h) * .22f;
		float right = x + w;
		float top = y + h;
		float mid = y + h / 2;
		switch (unit) {
			case 's' -> renderDigit(pose, consumer, 5, x, y, w, h, 0, color);
			case 't' -> {
				quad(pose, consumer, x, top - t, right, top, color);
				quad(pose, consumer, x + w / 2 - t / 2, y, x + w / 2 + t / 2, top, color);
			}
			case 'm' -> {
				quad(pose, consumer, x, y, x + t, top, color);
				quad(pose, consumer, right - t, y, right, top, color);
				quad(pose, consumer, x + t, mid, x + w / 2, top, color);
				quad(pose, consumer, x + w / 2, top, right - t, mid, color);
			}
			default -> {
			}
		}
	}

	private static void renderAnyMarker(Pose pose, VertexConsumer consumer, float x, float y, float w, float h, int color) {
		float cell = Math.min(w, h);
		float size = cell * .28f;
		float gap = cell * .36f;
		float cx = x + w / 2;
		float cy = y + h / 2;
		pip(pose, consumer, cx, cy, size, color);
		pip(pose, consumer, cx - gap, cy - gap, size, color);
		pip(pose, consumer, cx + gap, cy - gap, size, color);
		pip(pose, consumer, cx - gap, cy + gap, size, color);
		pip(pose, consumer, cx + gap, cy + gap, size, color);
	}

	private static void pip(Pose pose, VertexConsumer consumer, float cx, float cy, float size, int color) {
		quad(pose, consumer, cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, 0, color);
	}

	private static void renderDigit(Pose pose, VertexConsumer consumer, int digit, float x, float y, float w, float h, float z,
		int color) {
		if (digit == 1) {
			float t = Math.min(w, h) * .28f;
			float midX = x + w / 2;
			float halfT = t / 2;
			float midY = y + h / 2;
			quad(pose, consumer, midX - halfT, midY + halfT, midX + halfT, y + h - t, z, color);
			quad(pose, consumer, midX - halfT, y + t, midX + halfT, midY - halfT, z, color);
			return;
		}

		boolean[] segments = switch (digit) {
			case 0 -> new boolean[] { true, true, true, true, true, true, false };
			case 2 -> new boolean[] { true, true, false, true, true, false, true };
			case 3 -> new boolean[] { true, true, true, true, false, false, true };
			case 4 -> new boolean[] { false, true, true, false, false, true, true };
			case 5 -> new boolean[] { true, false, true, true, false, true, true };
			case 6 -> new boolean[] { true, false, true, true, true, true, true };
			case 7 -> new boolean[] { true, true, true, false, false, false, false };
			case 8 -> new boolean[] { true, true, true, true, true, true, true };
			case 9 -> new boolean[] { true, true, true, true, false, true, true };
			default -> new boolean[7];
		};

		float t = Math.min(w, h) * .28f;
		float halfT = t / 2;
		float midY = y + h / 2;
		float right = x + w;
		float top = y + h;

		if (segments[0])
			quad(pose, consumer, x + t, top - t, right - t, top, z, color);
		if (segments[1])
			quad(pose, consumer, right - t, midY + halfT, right, top - t, z, color);
		if (segments[2])
			quad(pose, consumer, right - t, y + t, right, midY - halfT, z, color);
		if (segments[3])
			quad(pose, consumer, x + t, y, right - t, y + t, z, color);
		if (segments[4])
			quad(pose, consumer, x, y + t, x + t, midY - halfT, z, color);
		if (segments[5])
			quad(pose, consumer, x, midY + halfT, x + t, top - t, z, color);
		if (segments[6])
			quad(pose, consumer, x + t, midY - halfT, right - t, midY + halfT, z, color);
	}

	private static void quad(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1, int color) {
		quad(pose, consumer, x0, y0, x1, y1, 0, color);
	}

	private static void quad(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1, float z, int color) {
		consumer.addVertex(pose, x0, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x0, y0, z).setColor(color);
	}

	private record OverlayState(Direction face, long lastSeen, boolean active, float fade) {
	}

	private record FaceShift(float x, float y) {
	}

	private record OverlayConfig(float frameWidth, float frameHeight, float cornerLength, float cornerThickness,
		float hitWidth, float hitHeight, float digitHeight, float digitWidth, float digitGap) {
	}

	private ScrollValueLabelRenderer() {
	}

}
