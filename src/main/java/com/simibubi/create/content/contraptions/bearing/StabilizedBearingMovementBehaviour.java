package com.simibubi.create.content.contraptions.bearing;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;


public class StabilizedBearingMovementBehaviour implements MovementBehaviour {
	private List<BlockStateModelPart> bearingTopModel;

	@Override
	public ItemStack canBeDisabledVia(MovementContext context) {
		return null;
	}

	@Override
	public boolean disableBlockEntityRendering() {
		return true;
	}

	@Override
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
									ContraptionMatrices matrices, MultiBufferSource buffer) {
		List<BlockStateModelPart> top = getBearingTopModel();
		if (top.isEmpty())
			return;

		Direction facing = context.state.getValue(BlockStateProperties.FACING);
		float renderPartialTicks = AnimationTickHolder.getPartialTicks();

		Quaternionf orientation = getBlockStateOrientation(facing);
		float angle = getCounterRotationAngle(context, facing, renderPartialTicks) * facing.getAxisDirection()
			.getStep();

		Quaternionf rotation = Axis.of(facing.step())
			.rotationDegrees(angle);
		rotation.mul(orientation);

		PoseStack ms = matrices.getModel();
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation);
		ms.translate(-.5, -.5, -.5);
		renderModel(top, ms, buffer.getBuffer(RenderTypes.cutoutMovingBlock()),
			LightCoordsUtil.getLightCoords(renderWorld, context.localPos));
		ms.popPose();
	}

	public void submitInContraption(MovementContext context, PoseStack ms, SubmitNodeCollector collector, int light) {
		List<BlockStateModelPart> top = getBearingTopModel();
		if (top.isEmpty())
			return;

		Direction facing = context.state.getValue(BlockStateProperties.FACING);
		float renderPartialTicks = AnimationTickHolder.getPartialTicks();
		Quaternionf orientation = getBlockStateOrientation(facing);
		float angle = getCounterRotationAngle(context, facing, renderPartialTicks) * facing.getAxisDirection()
			.getStep();
		Quaternionf rotation = Axis.of(facing.step())
			.rotationDegrees(angle);
		rotation.mul(orientation);

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), top, BlockModelRenderState.EMPTY_TINTS,
			light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	@Nullable
	@Override
	public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld,
									MovementContext movementContext) {
		return new StabilizedBearingVisual(visualizationContext, simulationWorld, movementContext);
	}

	static float getCounterRotationAngle(MovementContext context, Direction facing, float renderPartialTicks) {
		if (!context.contraption.canBeStabilized(facing, context.localPos))
			return 0;

		float offset = 0;
		Direction.Axis axis = facing.getAxis();
		AbstractContraptionEntity entity = context.contraption.entity;

		if (entity instanceof ControlledContraptionEntity controlledCE) {
			if (context.contraption.canBeStabilized(facing, context.localPos))
				offset = -controlledCE.getAngle(renderPartialTicks);

		} else if (entity instanceof OrientedContraptionEntity orientedCE) {
			if (axis.isVertical())
				offset = -orientedCE.getViewYRot(renderPartialTicks);
			else {
				if (orientedCE.isInitialOrientationPresent() && orientedCE.getInitialOrientation()
					.getAxis() == axis)
					offset = -orientedCE.getViewXRot(renderPartialTicks);
			}
		}
		return offset;
	}

	private static Quaternionf getBlockStateOrientation(Direction facing) {
		Quaternionf orientation = facing.getAxis()
			.isHorizontal() ? Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite()))
				: new Quaternionf();

		orientation.mul(Axis.XP.rotationDegrees(-90 - AngleHelper.verticalAngle(facing)));
		return orientation;
	}

	private List<BlockStateModelPart> getBearingTopModel() {
		if (bearingTopModel != null)
			return bearingTopModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.BEARING_TOP);
		return bearingTopModel = model == null ? List.of() : List.of(model);
	}

	private static void renderModel(List<BlockStateModelPart> parts, PoseStack ms, VertexConsumer consumer, int light) {
		QuadInstance quadInstance = new QuadInstance();
		quadInstance.setColor(0xFFFFFFFF);
		quadInstance.setLightCoords(light);
		quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

		for (BlockStateModelPart part : parts) {
			renderQuads(part.getQuads(null), ms, consumer, quadInstance);
			for (Direction side : Direction.values())
				renderQuads(part.getQuads(side), ms, consumer, quadInstance);
		}
	}

	private static void renderQuads(List<BakedQuad> quads, PoseStack ms, VertexConsumer consumer,
		QuadInstance quadInstance) {
		for (BakedQuad quad : quads)
			consumer.putBakedQuad(ms.last(), quad, quadInstance);
	}

}
