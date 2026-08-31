package com.simibubi.create.content.kinetics.gearbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class GearboxRenderer extends KineticBlockEntityRenderer<GearboxBlockEntity> {
	private List<BlockStateModelPart> shaftHalfModel;

	public GearboxRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new GearboxRenderState();
	}

	@Override
	public void extractRenderState(GearboxBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof GearboxRenderState gearboxState) {
			gearboxState.blockEntity = be;
			gearboxState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof GearboxRenderState gearboxState))
			return;
		GearboxBlockEntity be = gearboxState.blockEntity;
		if (be == null)
			return;
		if (isInvalid(be))
			return;
		if (CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;

		Axis gearboxAxis = be.getBlockState()
			.getValue(GearboxBlock.AXIS);
		for (Direction direction : Direction.values()) {
			if (direction.getAxis() == gearboxAxis)
				continue;
			submitShaftHalf(be, direction, gearboxState.partialTicks, state, ms, collector);
		}
	}

	private void submitShaftHalf(GearboxBlockEntity be, Direction direction, float partialTicks, BlockEntityRenderState state,
		PoseStack ms, SubmitNodeCollector collector) {
		List<BlockStateModelPart> shaftHalf = getShaftHalfModel();
		if (shaftHalf.isEmpty())
			return;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(direction.getAxis(), getGearboxAngle(be, direction, partialTicks)));
		rotateHalfShaftTo(ms, direction);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), shaftHalf, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private List<BlockStateModelPart> getShaftHalfModel() {
		if (shaftHalfModel != null)
			return shaftHalfModel;
		BlockStateModelPart shaftHalf = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SHAFT_HALF);
		return shaftHalfModel = shaftHalf == null ? List.of() : List.of(shaftHalf);
	}

	private static float getGearboxAngle(GearboxBlockEntity be, Direction direction, float partialTicks) {
		Axis shaftAxis = direction.getAxis();
		float time = getRenderTime(be, partialTicks);
		float offset = getRotationOffsetForPosition(be, be.getBlockPos(), shaftAxis);
		float speed = be.getSpeed();
		if (speed == 0 && be.getTheoreticalSpeed() == 0 && be.getGeneratedSpeed() != 0)
			speed = be.getGeneratedSpeed();
		float angle = time * speed * 3f / 10;

		if (speed != 0 && be.hasSource()) {
			BlockPos source = be.source.subtract(be.getBlockPos());
			Direction sourceFacing = Direction.getNearest(source, null);
			angle *= GearboxBlockEntity.getRotationModifier(sourceFacing, direction);
		}

		return ((angle + offset) % 360) / 180 * (float) Math.PI;
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

	@Override
	protected void renderSafe(GearboxBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	private static class GearboxRenderState extends BlockEntityRenderState {
		private GearboxBlockEntity blockEntity;
		private float partialTicks;
	}
}
