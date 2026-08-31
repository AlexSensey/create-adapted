package com.simibubi.create.content.kinetics.speedController;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueLabelRenderer;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class SpeedControllerRenderer extends KineticBlockEntityRenderer<SpeedControllerBlockEntity> {
	private List<BlockStateModelPart> bracketModel;

	public SpeedControllerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected BlockState getRenderedBlockState(SpeedControllerBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof SpeedControllerBlockEntity be))
			return;
		if (!CreateVisualizationManager.supportsVisualization(be.getLevel()))
			submitBracket(be, state, ms, collector);
		ScrollValueLabelRenderer.submitSpeedController(be, state, ms, collector, cameraRenderState);
	}

	private void submitBracket(SpeedControllerBlockEntity be, BlockEntityRenderState state, PoseStack ms,
		SubmitNodeCollector collector) {
		if (!be.hasBracket)
			return;
		List<BlockStateModelPart> bracket = getBracketModel();
		if (bracket.isEmpty())
			return;

		boolean alongX = be.getBlockState()
			.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) == Axis.X;
		ms.pushPose();
		ms.translate(0, 1, 0);
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(alongX ? 180 : 90));
		ms.translate(-.5, -.5, -.5);
		BlockPos lightPos = be.getBlockPos().above();
		int bracketLight = LightCoordsUtil.pack(
			be.getLevel().getBrightness(LightLayer.BLOCK, lightPos),
			be.getLevel().getBrightness(LightLayer.SKY, lightPos));
		collector.submitBlockModel(ms, RenderTypes.solidMovingBlock(), bracket,
			BlockModelRenderState.EMPTY_TINTS, bracketLight, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private List<BlockStateModelPart> getBracketModel() {
		if (bracketModel != null)
			return bracketModel;
		BlockStateModelPart bracket = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SPEED_CONTROLLER_BRACKET);
		return bracketModel = bracket == null ? List.of() : List.of(bracket);
	}
}
