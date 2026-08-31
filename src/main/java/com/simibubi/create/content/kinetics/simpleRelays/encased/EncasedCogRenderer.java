package com.simibubi.create.content.kinetics.simpleRelays.encased;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.CreateVisualizationManager;

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
import net.minecraft.core.Direction.Axis;

public class EncasedCogRenderer extends KineticBlockEntityRenderer<SimpleKineticBlockEntity> {

	private final boolean large;
	private List<BlockStateModelPart> cogModel;
	private List<BlockStateModelPart> shaftHalfModel;

	public static EncasedCogRenderer small(BlockEntityRendererProvider.Context context) {
		return new EncasedCogRenderer(context, false);
	}

	public static EncasedCogRenderer large(BlockEntityRendererProvider.Context context) {
		return new EncasedCogRenderer(context, true);
	}

	public EncasedCogRenderer(BlockEntityRendererProvider.Context context, boolean large) {
		super(context);
		this.large = large;
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof SimpleKineticBlockEntity be) || isInvalid(be))
			return;
		if (CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;
		if (!(be.getBlockState().getBlock() instanceof IRotate rotatingBlock))
			return;

		Axis axis = getRotationAxisOf(be);
		float cogAngle = getAngleForBe(be, be.getBlockPos(), axis, kineticState.partialTicks);
		submitVerticalPart(getCogModel(), axis, cogAngle, state, ms, collector);

		float shaftAngle = large
			? BracketedKineticBlockEntityRenderer.getAngleForLargeCogShaft(be, axis, kineticState.partialTicks)
			: cogAngle;
		for (Direction direction : Direction.values()) {
			if (direction.getAxis() != axis)
				continue;
			if (!rotatingBlock.hasShaftTowards(be.getLevel(), be.getBlockPos(), be.getBlockState(), direction))
				continue;
			submitShaftHalf(direction, shaftAngle, state, ms, collector);
		}
	}

	private static void submitVerticalPart(List<BlockStateModelPart> model, Axis axis, float angle,
		BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector) {
		if (model.isEmpty())
			return;
		ms.pushPose();
		ms.translate(.5, .5, .5);
		orientVerticalModelToAxis(ms, axis);
		ms.mulPose(com.mojang.math.Axis.YP.rotation(angle));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), model, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private void submitShaftHalf(Direction direction, float angle, BlockEntityRenderState state, PoseStack ms,
		SubmitNodeCollector collector) {
		List<BlockStateModelPart> model = getShaftHalfModel();
		if (model.isEmpty())
			return;
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(direction.getAxis(), angle));
		rotateHalfShaftTo(ms, direction);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), model, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private List<BlockStateModelPart> getCogModel() {
		if (cogModel != null)
			return cogModel;
		BlockStateModelPart model = Minecraft.getInstance().getModelManager().getStandaloneModel(large
			? CreateStandaloneModels.SHAFTLESS_LARGE_COGWHEEL
			: CreateStandaloneModels.SHAFTLESS_COGWHEEL);
		return cogModel = model == null ? List.of() : List.of(model);
	}

	private List<BlockStateModelPart> getShaftHalfModel() {
		if (shaftHalfModel != null)
			return shaftHalfModel;
		BlockStateModelPart model = Minecraft.getInstance().getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SHAFT_HALF);
		return shaftHalfModel = model == null ? List.of() : List.of(model);
	}

	private static void orientVerticalModelToAxis(PoseStack ms, Axis axis) {
		switch (axis) {
			case X -> {
				ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
				ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
			}
			case Z -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
			case Y -> {
			}
		}
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
