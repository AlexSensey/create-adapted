package com.simibubi.create.content.kinetics.fan;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
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
import net.minecraft.util.Mth;

import java.util.List;

public class EncasedFanRenderer extends KineticBlockEntityRenderer<EncasedFanBlockEntity> {

	private List<BlockStateModelPart> shaftHalfModel;
	private List<BlockStateModelPart> fanInnerModel;

	public EncasedFanRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof EncasedFanBlockEntity be))
			return;
		if (isInvalid(be))
			return;
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		Direction direction = be.getBlockState()
			.getValue(FACING);
		Direction shaftSide = direction.getOpposite();

		List<BlockStateModelPart> shaftHalf = getShaftHalfModel();
		List<BlockStateModelPart> fanInner = getFanInnerModel();
		if (shaftHalf == null || fanInner == null)
			return;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(shaftSide.getAxis(),
			getAngleForBe(be, be.getBlockPos(), shaftSide.getAxis(), kineticState.partialTicks)));
		rotateToFacing(ms, shaftSide);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), shaftHalf,
			BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(direction.getAxis(), getFanAngle(be, kineticState.partialTicks)));
		rotateToFacing(ms, shaftSide);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), fanInner,
			BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	@Override
	protected void renderSafe(EncasedFanBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	private List<BlockStateModelPart> getShaftHalfModel() {
		if (shaftHalfModel != null)
			return shaftHalfModel;
		BlockStateModelPart shaftHalf = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SHAFT_HALF);
		if (shaftHalf == null)
			return null;
		return shaftHalfModel = List.of(shaftHalf);
	}

	private List<BlockStateModelPart> getFanInnerModel() {
		if (fanInnerModel != null)
			return fanInnerModel;
		BlockStateModelPart fanInner = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.ENCASED_FAN_INNER);
		if (fanInner == null)
			return null;
		return fanInnerModel = List.of(fanInner);
	}

	private static float getFanAngle(KineticBlockEntity be, float partialTicks) {
		float time = getRenderTime(be, partialTicks);
		float speed = be.getSpeed() * 5;
		if (speed > 0)
			speed = Mth.clamp(speed, 80, 64 * 20);
		if (speed < 0)
			speed = Mth.clamp(speed, -64 * 20, -80);
		return ((time * speed * 3 / 10f) % 360) / 180f * (float) Math.PI;
	}

	private static void rotateToFacing(PoseStack ms, Direction direction) {
		switch (direction) {
			case NORTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
			case EAST -> ms.mulPose(Axis.YP.rotationDegrees(90));
			case WEST -> ms.mulPose(Axis.YP.rotationDegrees(-90));
			case UP -> ms.mulPose(Axis.XP.rotationDegrees(-90));
			case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(90));
			case SOUTH -> {
			}
		}
	}
}
