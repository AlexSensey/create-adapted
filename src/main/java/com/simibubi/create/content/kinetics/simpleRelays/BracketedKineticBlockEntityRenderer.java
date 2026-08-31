package com.simibubi.create.content.kinetics.simpleRelays;

import java.util.List;
import java.util.ArrayList;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class BracketedKineticBlockEntityRenderer extends KineticBlockEntityRenderer<BracketedKineticBlockEntity> {
	private List<BlockStateModelPart> shaftlessLargeCogModel;
	private List<BlockStateModelPart> cogwheelShaftModel;

	public BracketedKineticBlockEntityRenderer(Context context) {
		super(context);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof BracketedKineticBlockEntity be))
			return;
		if (CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;
		if (!AllBlocks.LARGE_COGWHEEL.has(be.getBlockState())) {
			super.submit(state, ms, collector, cameraRenderState);
		} else if (!isInvalid(be)) {
			Axis axis = getRotationAxisOf(be);
			submitRotatingPart(getShaftlessLargeCogModel(), axis,
				getAngleForBe(be, be.getBlockPos(), axis, kineticState.partialTicks), state, ms, collector);
			submitRotatingPart(getCogwheelShaftModel(), axis,
				getAngleForLargeCogShaft(be, axis, kineticState.partialTicks), state, ms, collector);
		}

		submitBracket(be, state, ms, collector);
	}

	private static void submitBracket(BracketedKineticBlockEntity be, BlockEntityRenderState renderState,
		PoseStack ms, SubmitNodeCollector collector) {
		BracketedBlockEntityBehaviour behaviour =
			BlockEntityBehaviour.get(be, BracketedBlockEntityBehaviour.TYPE);
		if (behaviour == null)
			return;
		BlockState bracketState = behaviour.getBracket();
		if (bracketState == null)
			return;

		BlockStateModel model = Minecraft.getInstance()
			.getModelManager()
			.getBlockStateModelSet()
			.get(bracketState);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(bracketState.getSeed(be.getBlockPos())), parts);
		if (parts.isEmpty())
			return;
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), parts,
			BlockModelRenderState.EMPTY_TINTS, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
	}

	private static void submitRotatingPart(List<BlockStateModelPart> model, Axis axis, float angle,
		BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector) {
		if (model.isEmpty())
			return;
		ms.pushPose();
		ms.translate(.5, .5, .5);
		orientVerticalModelToAxis(ms, axis);
		ms.mulPose(com.mojang.math.Axis.YP.rotation(angle));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), model,
			BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
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

	private List<BlockStateModelPart> getShaftlessLargeCogModel() {
		if (shaftlessLargeCogModel != null)
			return shaftlessLargeCogModel;
		BlockStateModelPart model = Minecraft.getInstance().getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SHAFTLESS_LARGE_COGWHEEL);
		return shaftlessLargeCogModel = model == null ? List.of() : List.of(model);
	}

	private List<BlockStateModelPart> getCogwheelShaftModel() {
		if (cogwheelShaftModel != null)
			return cogwheelShaftModel;
		BlockStateModelPart model = Minecraft.getInstance().getModelManager()
			.getStandaloneModel(CreateStandaloneModels.COGWHEEL_SHAFT);
		return cogwheelShaftModel = model == null ? List.of() : List.of(model);
	}

	public static float getAngleForLargeCogShaft(SimpleKineticBlockEntity be, Axis axis) {
		return getAngleForLargeCogShaft(be, axis, AnimationTickHolder.getPartialTicks());
	}

	public static float getAngleForLargeCogShaft(SimpleKineticBlockEntity be, Axis axis, float partialTicks) {
		BlockPos pos = be.getBlockPos();
		float time = getRenderTime(be, partialTicks);
		float speed = be.getSpeed();
		if (speed == 0 && be.getTheoreticalSpeed() == 0 && be.getGeneratedSpeed() != 0)
			speed = be.getGeneratedSpeed();
		return ((time * speed * 3f / 10 + getShaftAngleOffset(axis, pos)) % 360) / 180 * (float) Math.PI;
	}

	public static float getShaftAngleOffset(Axis axis, BlockPos pos) {
		return KineticBlockEntityVisual.shouldOffset(axis, pos) ? 22.5f : 0;
	}
}
