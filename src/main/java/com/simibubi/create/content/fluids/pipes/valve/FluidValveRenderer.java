package com.simibubi.create.content.fluids.pipes.valve;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.math.AngleHelper;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class FluidValveRenderer extends KineticBlockEntityRenderer<FluidValveBlockEntity> {
	private List<BlockStateModelPart> pointerModel;

	public FluidValveRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof FluidValveBlockEntity be))
			return;
		if (isInvalid(be))
			return;
		if (CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;

		List<BlockStateModelPart> pointer = getPointerModel();
		if (pointer.isEmpty())
			return;

		BlockState blockState = be.getBlockState();
		if (!(blockState.getBlock() instanceof FluidValveBlock))
			return;

		Direction facing = blockState.getValue(FluidValveBlock.FACING);
		float pointerRotation = Mth.lerp(be.pointer.getValue(kineticState.partialTicks), 0, -90);
		Axis pipeAxis = FluidValveBlock.getPipeAxis(blockState);
		Axis shaftAxis = getRotationAxisOf(be);

		int pointerRotationOffset = 0;
		if (pipeAxis.isHorizontal() && shaftAxis == Axis.X || pipeAxis.isVertical())
			pointerRotationOffset = 90;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90));
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(pointerRotationOffset + pointerRotation));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), pointer, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private List<BlockStateModelPart> getPointerModel() {
		if (pointerModel != null)
			return pointerModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.FLUID_VALVE_POINTER);
		return pointerModel = model == null ? List.of() : List.of(model);
	}

	@Override
	protected BlockState getRenderedBlockState(FluidValveBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

}
