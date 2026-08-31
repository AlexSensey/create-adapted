package com.simibubi.create.content.contraptions.bearing;

import java.util.List;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.CreateVisualizationManager;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class StabilizedBearingMovementClient {
	private static List<BlockStateModelPart> bearingTopModel;

	private StabilizedBearingMovementClient() {}

	public static void render(MovementContext context, VirtualRenderWorld world, ContraptionMatrices matrices,
		MultiBufferSource buffer) {
		if (CreateVisualizationManager.supportsVisualization(context.world))
			return;
		List<BlockStateModelPart> top = getBearingTopModel();
		if (top.isEmpty())
			return;
		Direction facing = context.state.getValue(BlockStateProperties.FACING);
		Quaternionf rotation = rotation(context, facing);
		PoseStack ms = matrices.getModel();
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation);
		ms.translate(-.5, -.5, -.5);
		renderModel(top, ms, buffer.getBuffer(RenderTypes.cutoutMovingBlock()),
			LightCoordsUtil.getLightCoords(world, context.localPos));
		ms.popPose();
	}

	public static void submit(MovementContext context, PoseStack ms, SubmitNodeCollector collector, int light) {
		List<BlockStateModelPart> top = getBearingTopModel();
		if (top.isEmpty())
			return;
		Direction facing = context.state.getValue(BlockStateProperties.FACING);
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(context, facing));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), top, BlockModelRenderState.EMPTY_TINTS,
			light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static Quaternionf rotation(MovementContext context, Direction facing) {
		Quaternionf orientation = facing.getAxis().isHorizontal()
			? Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite())) : new Quaternionf();
		orientation.mul(Axis.XP.rotationDegrees(-90 - AngleHelper.verticalAngle(facing)));
		float angle = StabilizedBearingMovementBehaviour.getCounterRotationAngle(context, facing,
			AnimationTickHolder.getPartialTicks()) * facing.getAxisDirection().getStep();
		Quaternionf rotation = Axis.of(facing.step()).rotationDegrees(angle);
		rotation.mul(orientation);
		return rotation;
	}

	private static List<BlockStateModelPart> getBearingTopModel() {
		if (bearingTopModel != null)
			return bearingTopModel;
		BlockStateModelPart model = Minecraft.getInstance().getModelManager()
			.getStandaloneModel(CreateStandaloneModels.BEARING_TOP);
		return bearingTopModel = model == null ? List.of() : List.of(model);
	}

	private static void renderModel(List<BlockStateModelPart> parts, PoseStack ms, VertexConsumer consumer, int light) {
		QuadInstance instance = new QuadInstance();
		instance.setColor(0xFFFFFFFF);
		instance.setLightCoords(light);
		instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
		for (BlockStateModelPart part : parts) {
			renderQuads(part.getQuads(null), ms, consumer, instance);
			for (Direction side : Direction.values())
				renderQuads(part.getQuads(side), ms, consumer, instance);
		}
	}

	private static void renderQuads(List<BakedQuad> quads, PoseStack ms, VertexConsumer consumer,
		QuadInstance instance) {
		for (BakedQuad quad : quads)
			consumer.putBakedQuad(ms.last(), quad, instance);
	}
}
