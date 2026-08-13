package com.simibubi.create.content.trains.bogey;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class StandardBogeyRenderer implements BogeyRenderer {
	private List<BlockStateModelPart> shaftZ;

	private static final float MODEL_SCALE_EPSILON = 1 - 1 / 512f;

	@Override
	public void render(CompoundTag bogeyData, float wheelAngle, float partialTick, PoseStack poseStack,
		MultiBufferSource bufferSource, int light, int overlay, boolean inContraption) {
		VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.cutoutMovingBlock());
		List<BlockStateModelPart> shaft = getShaftZ();
		for (int i = 0; i < 2; i++) {
			PoseStack local = new PoseStack();
			local.translate(-.5f, .25f, i * -1f);
			rotateCentered(local, Axis.ZP, wheelAngle);
			renderParts(poseStack, local, buffer, shaft, light);
		}
	}

	@Override
	public void submit(CompoundTag bogeyData, float wheelAngle, float partialTick, PoseStack poseStack,
		SubmitNodeCollector collector, int light, int overlay, boolean inContraption) {
		List<BlockStateModelPart> shaft = getShaftZ();
		for (int i = 0; i < 2; i++) {
			PoseStack local = new PoseStack();
			local.translate(-.5f, .25f, i * -1f);
			rotateCentered(local, Axis.ZP, wheelAngle);
			submitParts(poseStack, local, collector, shaft, light);
		}
	}

	public static class Small extends StandardBogeyRenderer {
		private BlockStateModelPart frame;
		private BlockStateModelPart wheels;

		@Override
		public void render(CompoundTag bogeyData, float wheelAngle, float partialTick, PoseStack poseStack,
			MultiBufferSource bufferSource, int light, int overlay, boolean inContraption) {
			super.render(bogeyData, wheelAngle, partialTick, poseStack, bufferSource, light, overlay, inContraption);

			VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.cutoutMovingBlock());
			PoseStack frameTransform = new PoseStack();
			frameTransform.scale(MODEL_SCALE_EPSILON, MODEL_SCALE_EPSILON, MODEL_SCALE_EPSILON);
			renderPart(poseStack, frameTransform, buffer, getFrame(), light);

			for (int side : new int[] { 1, -1 }) {
				PoseStack wheelTransform = new PoseStack();
				wheelTransform.translate(0, 12 / 16f, side);
				wheelTransform.mulPose(Axis.XP.rotationDegrees(wheelAngle));
				renderPart(poseStack, wheelTransform, buffer, getWheels(), light);
			}
		}

		@Override
		public void submit(CompoundTag bogeyData, float wheelAngle, float partialTick, PoseStack poseStack,
			SubmitNodeCollector collector, int light, int overlay, boolean inContraption) {
			super.submit(bogeyData, wheelAngle, partialTick, poseStack, collector, light, overlay, inContraption);

			PoseStack frameTransform = new PoseStack();
			frameTransform.scale(MODEL_SCALE_EPSILON, MODEL_SCALE_EPSILON, MODEL_SCALE_EPSILON);
			submitPart(poseStack, frameTransform, collector, getFrame(), light);

			for (int side : new int[] { 1, -1 }) {
				PoseStack wheelTransform = new PoseStack();
				wheelTransform.translate(0, 12 / 16f, side);
				wheelTransform.mulPose(Axis.XP.rotationDegrees(wheelAngle));
				submitPart(poseStack, wheelTransform, collector, getWheels(), light);
			}
		}

		private BlockStateModelPart getFrame() {
			return frame = loadPart(frame, CreateStandaloneModels.BOGEY_FRAME);
		}

		private BlockStateModelPart getWheels() {
			return wheels = loadPart(wheels, CreateStandaloneModels.SMALL_BOGEY_WHEELS);
		}
	}

	public static class Large extends StandardBogeyRenderer {
		public static final float BELT_RADIUS_PX = 5f;
		public static final float BELT_RADIUS_IN_UV_SPACE = BELT_RADIUS_PX / 16f;

		private List<BlockStateModelPart> shaftX;
		private BlockStateModelPart drive;
		private BlockStateModelPart belt;
		private BlockStateModelPart piston;
		private BlockStateModelPart wheels;
		private BlockStateModelPart pin;

		@Override
		public void render(CompoundTag bogeyData, float wheelAngle, float partialTick, PoseStack poseStack,
			MultiBufferSource bufferSource, int light, int overlay, boolean inContraption) {
			super.render(bogeyData, wheelAngle, partialTick, poseStack, bufferSource, light, overlay, inContraption);

			VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.cutoutMovingBlock());
			for (int i = 0; i < 2; i++) {
				PoseStack shaftTransform = new PoseStack();
				shaftTransform.translate(-.5f, .25f, .5f + i * -2f);
				rotateCentered(shaftTransform, Axis.XP, wheelAngle);
				renderParts(poseStack, shaftTransform, buffer, getShaftX(), light);
			}

			PoseStack driveTransform = new PoseStack();
			driveTransform.scale(MODEL_SCALE_EPSILON, MODEL_SCALE_EPSILON, MODEL_SCALE_EPSILON);
			renderPart(poseStack, driveTransform, buffer, getDrive(), light);
			renderPart(poseStack, driveTransform, buffer, getBelt(), light);

			PoseStack pistonTransform = new PoseStack();
			pistonTransform.translate(0, 0, 1 / 4f * Math.sin(Math.toRadians(wheelAngle)));
			renderPart(poseStack, pistonTransform, buffer, getPiston(), light);

			PoseStack wheelsTransform = new PoseStack();
			wheelsTransform.translate(0, 1, 0);
			wheelsTransform.mulPose(Axis.XP.rotationDegrees(wheelAngle));
			renderPart(poseStack, wheelsTransform, buffer, getWheels(), light);

			PoseStack pinTransform = new PoseStack();
			pinTransform.translate(0, 1, 0);
			pinTransform.mulPose(Axis.XP.rotationDegrees(wheelAngle));
			pinTransform.translate(0, 1 / 4f, 0);
			pinTransform.mulPose(Axis.XP.rotationDegrees(-wheelAngle));
			renderPart(poseStack, pinTransform, buffer, getPin(), light);
		}

		@Override
		public void submit(CompoundTag bogeyData, float wheelAngle, float partialTick, PoseStack poseStack,
			SubmitNodeCollector collector, int light, int overlay, boolean inContraption) {
			super.submit(bogeyData, wheelAngle, partialTick, poseStack, collector, light, overlay, inContraption);

			for (int i = 0; i < 2; i++) {
				PoseStack shaftTransform = new PoseStack();
				shaftTransform.translate(-.5f, .25f, .5f + i * -2f);
				rotateCentered(shaftTransform, Axis.XP, wheelAngle);
				submitParts(poseStack, shaftTransform, collector, getShaftX(), light);
			}

			PoseStack driveTransform = new PoseStack();
			driveTransform.scale(MODEL_SCALE_EPSILON, MODEL_SCALE_EPSILON, MODEL_SCALE_EPSILON);
			submitPart(poseStack, driveTransform, collector, getDrive(), light);
			submitPart(poseStack, driveTransform, collector, getBelt(), light);

			PoseStack pistonTransform = new PoseStack();
			pistonTransform.translate(0, 0, 1 / 4f * Math.sin(Math.toRadians(wheelAngle)));
			submitPart(poseStack, pistonTransform, collector, getPiston(), light);

			PoseStack wheelsTransform = new PoseStack();
			wheelsTransform.translate(0, 1, 0);
			wheelsTransform.mulPose(Axis.XP.rotationDegrees(wheelAngle));
			submitPart(poseStack, wheelsTransform, collector, getWheels(), light);

			PoseStack pinTransform = new PoseStack();
			pinTransform.translate(0, 1, 0);
			pinTransform.mulPose(Axis.XP.rotationDegrees(wheelAngle));
			pinTransform.translate(0, 1 / 4f, 0);
			pinTransform.mulPose(Axis.XP.rotationDegrees(-wheelAngle));
			submitPart(poseStack, pinTransform, collector, getPin(), light);
		}

		private List<BlockStateModelPart> getShaftX() {
			if (shaftX != null)
				return shaftX;
			return shaftX = loadBlockParts(AllBlocks.SHAFT.getDefaultState()
				.setValue(ShaftBlock.AXIS, Direction.Axis.X));
		}

		private BlockStateModelPart getDrive() {
			return drive = loadPart(drive, CreateStandaloneModels.BOGEY_DRIVE);
		}

		private BlockStateModelPart getBelt() {
			return belt = loadPart(belt, CreateStandaloneModels.BOGEY_DRIVE_BELT);
		}

		private BlockStateModelPart getPiston() {
			return piston = loadPart(piston, CreateStandaloneModels.BOGEY_PISTON);
		}

		private BlockStateModelPart getWheels() {
			return wheels = loadPart(wheels, CreateStandaloneModels.LARGE_BOGEY_WHEELS);
		}

		private BlockStateModelPart getPin() {
			return pin = loadPart(pin, CreateStandaloneModels.BOGEY_PIN);
		}
	}

	private List<BlockStateModelPart> getShaftZ() {
		if (shaftZ != null)
			return shaftZ;
		return shaftZ = loadBlockParts(AllBlocks.SHAFT.getDefaultState()
			.setValue(ShaftBlock.AXIS, Direction.Axis.Z));
	}

	private static BlockStateModelPart loadPart(BlockStateModelPart cached,
		StandaloneModelKey<BlockStateModelPart> key) {
		if (cached != null)
			return cached;
		return Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
	}

	private static List<BlockStateModelPart> loadBlockParts(BlockState state) {
		BlockStateModel model = Minecraft.getInstance()
			.getModelManager()
			.getBlockStateModelSet()
			.get(state);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(state.getSeed(BlockPos.ZERO)), parts);
		return parts;
	}

	private static void rotateCentered(PoseStack transform, com.mojang.math.Axis axis, float degrees) {
		transform.translate(.5f, .5f, .5f);
		transform.mulPose(axis.rotationDegrees(degrees));
		transform.translate(-.5f, -.5f, -.5f);
	}

	private static void submitPart(PoseStack root, PoseStack local, SubmitNodeCollector collector,
		BlockStateModelPart part, int light) {
		if (part == null)
			return;
		submitParts(root, local, collector, List.of(part), light);
	}

	private static void submitParts(PoseStack root, PoseStack local, SubmitNodeCollector collector,
		List<BlockStateModelPart> parts, int light) {
		if (parts == null || parts.isEmpty())
			return;
		PoseStack pose = compose(root, local);
		collector.submitBlockModel(pose, RenderTypes.cutoutMovingBlock(), parts, BlockModelRenderState.EMPTY_TINTS,
			light, OverlayTexture.NO_OVERLAY, 0);
	}

	private static void renderPart(PoseStack root, PoseStack local, VertexConsumer consumer, BlockStateModelPart part,
		int light) {
		if (part == null)
			return;
		renderParts(root, local, consumer, List.of(part), light);
	}

	private static void renderParts(PoseStack root, PoseStack local, VertexConsumer consumer,
		List<BlockStateModelPart> parts, int light) {
		if (parts == null || parts.isEmpty())
			return;
		PoseStack pose = compose(root, local);
		QuadInstance quadInstance = new QuadInstance();
		quadInstance.setColor(0xFFFFFFFF);
		quadInstance.setLightCoords(light);
		quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

		for (BlockStateModelPart part : parts) {
			renderQuads(part.getQuads(null), pose, consumer, quadInstance);
			for (Direction side : Direction.values())
				renderQuads(part.getQuads(side), pose, consumer, quadInstance);
		}
	}

	private static void renderQuads(List<BakedQuad> quads, PoseStack pose, VertexConsumer consumer,
		QuadInstance quadInstance) {
		for (BakedQuad quad : quads)
			consumer.putBakedQuad(pose.last(), quad, quadInstance);
	}

	private static PoseStack compose(PoseStack root, PoseStack local) {
		PoseStack pose = new PoseStack();
		pose.last()
			.pose()
			.set(root.last()
				.pose())
			.mul(local.last()
				.pose());
		pose.last()
			.normal()
			.set(root.last()
				.normal())
			.mul(local.last()
				.normal());
		return pose;
	}
}
