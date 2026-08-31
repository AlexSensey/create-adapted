package com.simibubi.create.content.contraptions.actors.trainControls;

import java.util.List;
import java.util.Collection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class ControlsRenderer {
	private static ControlsMovementBehaviour.LeverAngles updateAngles(MovementContext context) {
		if (!(context.temporaryData instanceof ControlsMovementBehaviour.LeverAngles angles))
			return null;
		AbstractContraptionEntity entity = context.contraption.entity;
		if (!(entity instanceof CarriageContraptionEntity carriage))
			return null;
		StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
		Direction initial = carriage.getInitialOrientation().getCounterClockWise();
		boolean inverted = info != null && info.state().hasProperty(ControlsBlock.FACING)
			&& !info.state().getValue(ControlsBlock.FACING).equals(initial);
		if (ControlsHandler.getContraption() == entity && context.localPos.equals(ControlsHandler.getControlsPos())) {
			Collection<Integer> pressed = ControlsHandler.currentlyPressed;
			angles.equipAnimation.chase(1, .2f, Chaser.EXP);
			angles.steering.chase((pressed.contains(3) ? 1 : 0) + (pressed.contains(2) ? -1 : 0), .2f, Chaser.EXP);
			float direction = carriage.movingBackwards ^ inverted ? -1 : 1;
			angles.speed.chase(Math.min(context.motion.length(), .5f) * direction, .2f, Chaser.EXP);
		} else {
			angles.equipAnimation.chase(0, .2f, Chaser.EXP);
			angles.steering.chase(0, 0, Chaser.EXP);
			angles.speed.chase(0, 0, Chaser.EXP);
		}
		return angles;
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld world,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		ControlsMovementBehaviour.LeverAngles angles = updateAngles(context);
		if (angles == null)
			return;
		float pt = AnimationTickHolder.getPartialTicks();
		render(context, world, matrices, buffer, angles.equipAnimation.getValue(pt), angles.speed.getValue(pt),
			angles.steering.getValue(pt));
	}

	public static void submitInContraption(MovementContext context, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		ControlsMovementBehaviour.LeverAngles angles = updateAngles(context);
		if (angles == null)
			return;
		float pt = AnimationTickHolder.getPartialTicks();
		submitInContraption(context, ms, collector, light, angles.equipAnimation.getValue(pt),
			angles.speed.getValue(pt), angles.steering.getValue(pt));
	}

	public static void render(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices,
		MultiBufferSource buffer, float equipAnimation, float firstLever, float secondLever) {
	}

	public static void submitInContraption(MovementContext context, PoseStack ms, SubmitNodeCollector collector,
		int light, float equipAnimation, float firstLever, float secondLever) {
		BlockState state = context.state;
		Direction facing = state.getValue(ControlsBlock.FACING);
		float hAngle = 180 + AngleHelper.horizontalAngle(facing);

		BlockStateModelPart cover = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.TRAIN_CONTROLS_COVER);
		if (cover != null) {
			ms.pushPose();
			transformCover(ms, hAngle);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(cover),
				BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		BlockStateModelPart lever = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.TRAIN_CONTROLS_LEVER);
		if (lever == null)
			return;

		double yOffset = Mth.lerp(equipAnimation * equipAnimation, -0.15f, 0.05f);
		for (boolean first : Iterate.trueAndFalse) {
			float vAngle = Mth.clamp(first ? firstLever * 70 - 25 : secondLever * 15, -45, 45);
			ms.pushPose();
			transformLever(ms, hAngle, vAngle, yOffset, first);
			collector.submitBlockModel(ms, RenderTypes.solidMovingBlock(), List.of(lever),
				BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}
	}

	private static void transformCover(PoseStack ms, float hAngle) {
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(Axis.YP.rotationDegrees(hAngle));
		ms.translate(-.5f, -.5f, -.5f);
	}

	private static void transformLever(PoseStack ms, float hAngle, float vAngle, double yOffset, boolean first) {
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(Axis.YP.rotationDegrees(hAngle));
		ms.translate(0, 4 / 16f, 4 / 16f);
		ms.mulPose(Axis.XP.rotationDegrees(vAngle - 45));
		ms.translate(0, yOffset, 0);
		ms.mulPose(Axis.XP.rotationDegrees(45));
		ms.translate(-.5f, -.5f, -.5f);
		ms.translate(0, -6 / 16f, -3 / 16f);
		ms.translate(first ? 0 : 6 / 16f, 0, 0);
	}
}
