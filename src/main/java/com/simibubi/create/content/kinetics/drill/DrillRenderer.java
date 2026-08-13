package com.simibubi.create.content.kinetics.drill;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionActorRotation;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;

public class DrillRenderer extends KineticBlockEntityRenderer<DrillBlockEntity> {
	private List<BlockStateModelPart> drillHeadModel;

	public DrillRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof DrillBlockEntity be))
			return;
		if (isInvalid(be))
			return;

		List<BlockStateModelPart> head = getDrillHeadModel();
		if (head.isEmpty())
			return;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		rotateToFacing(ms, be.getBlockState());
		ms.mulPose(Axis.ZP.rotation(getAngleForBe(be, be.getBlockPos(), getRotationAxisOf(be), kineticState.partialTicks)));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), head, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private List<BlockStateModelPart> getDrillHeadModel() {
		if (drillHeadModel != null)
			return drillHeadModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.DRILL_HEAD);
		return drillHeadModel = model == null ? List.of() : List.of(model);
	}

	private static void rotateToFacing(PoseStack ms, BlockState state) {
		Direction facing = state.getValue(DrillBlock.FACING);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.mulPose(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(facing)));
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		SuperByteBuffer block = CachedBuffers.block(context.state);
		if (!block.isEmpty())
			block.light(LightCoordsUtil.getLightCoords(renderWorld, context.localPos))
				.renderInto(matrices.getModel(), buffer.getBuffer(RenderTypes.cutoutMovingBlock()));
	}

	public static void submitInContraption(MovementContext context, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.DRILL_HEAD);
		if (model == null)
			return;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		rotateToFacing(ms, context.state);
		ms.mulPose(Axis.ZP.rotation(getContraptionAngle(context)));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(model), BlockModelRenderState.EMPTY_TINTS,
			light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static float getContraptionAngle(MovementContext context) {
		Direction facing = context.state.getValue(DrillBlock.FACING);
		float speed = context.contraption.stalled
			|| !VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite())
				? context.getAnimationSpeed()
				: 0;
		return ContraptionActorRotation.getAngle(context, speed);
	}
}
