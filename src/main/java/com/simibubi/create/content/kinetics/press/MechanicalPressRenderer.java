package com.simibubi.create.content.kinetics.press;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import com.simibubi.create.foundation.render.CreateVisualizationManager;

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
import net.minecraft.world.level.block.state.BlockState;

public class MechanicalPressRenderer extends KineticBlockEntityRenderer<MechanicalPressBlockEntity> {

	private List<BlockStateModelPart> headModel;

	public MechanicalPressRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	protected void renderSafe(MechanicalPressBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);

		if (!(state instanceof PressRenderState pressState))
			return;
		MechanicalPressBlockEntity be = pressState.blockEntity;
		if (be == null || isInvalid(be))
			return;
		if (CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockState blockState = be.getBlockState();
		PressingBehaviour pressingBehaviour = be.getPressingBehaviour();
		float renderedHeadOffset =
			pressingBehaviour.getRenderedHeadOffset(pressState.partialTicks) * pressingBehaviour.mode.headOffset;
		List<BlockStateModelPart> head = getHeadModel();
		if (head.isEmpty())
			return;

		ms.pushPose();
		ms.translate(.5, .5 - renderedHeadOffset, .5);
		rotateToFacing(ms, blockState.getValue(HORIZONTAL_FACING));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), head, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private List<BlockStateModelPart> getHeadModel() {
		if (headModel != null)
			return headModel;
		BlockStateModelPart head = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.MECHANICAL_PRESS_HEAD);
		return headModel = head == null ? List.of() : List.of(head);
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new PressRenderState();
	}

	@Override
	public void extractRenderState(MechanicalPressBlockEntity be, BlockEntityRenderState state, float partialTicks,
		net.minecraft.world.phys.Vec3 cameraPos,
		net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
		if (state instanceof PressRenderState pressState) {
			pressState.blockEntity = be;
			pressState.partialTicks = partialTicks;
		}
	}

	private static void rotateToFacing(PoseStack ms, Direction facing) {
		switch (facing) {
			case SOUTH -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
			case WEST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
			case EAST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
			default -> {
			}
		}
	}

	@Override
	protected BlockState getRenderedBlockState(MechanicalPressBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	private static class PressRenderState extends KineticRenderState {
		private MechanicalPressBlockEntity blockEntity;
		private float partialTicks;
	}
}
