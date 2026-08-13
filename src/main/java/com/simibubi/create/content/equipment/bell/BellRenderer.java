package com.simibubi.create.content.equipment.bell;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class BellRenderer<BE extends AbstractBellBlockEntity> extends SafeBlockEntityRenderer<BE> {

	public BellRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new BellRenderState();
	}

	@Override
	public void extractRenderState(BE be, BlockEntityRenderState state, float partialTicks, Vec3 cameraPos,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (!(state instanceof BellRenderState bellState))
			return;
		bellState.blockState = be.getBlockState();
		bellState.model = be instanceof HauntedBellBlockEntity ? CreateStandaloneModels.HAUNTED_BELL
			: CreateStandaloneModels.PECULIAR_BELL;
		bellState.isRinging = be.isRinging;
		bellState.ringingTime = be.ringingTicks + partialTicks;
		bellState.ringDirection = be.ringDirection;
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof BellRenderState bellState) || bellState.blockState == null || bellState.model == null)
			return;
		BlockStateModelPart bell = Minecraft.getInstance().getModelManager().getStandaloneModel(bellState.model);
		if (bell == null)
			return;

		Direction facing = bellState.blockState.getValue(BellBlock.FACING);
		BellAttachType attachment = bellState.blockState.getValue(BellBlock.ATTACHMENT);
		float yRotation = AngleHelper.horizontalAngle(facing);
		if (attachment == BellAttachType.SINGLE_WALL || attachment == BellAttachType.DOUBLE_WALL)
			yRotation += 90;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		if (bellState.isRinging && bellState.ringDirection != null)
			rotateAround(ms, bellState.ringDirection.getCounterClockWise(), getSwingAngle(bellState.ringingTime));
		ms.mulPose(Axis.YP.rotationDegrees(yRotation));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(bell),
			BlockModelRenderState.EMPTY_TINTS, state.lightCoords, 0, 0);
		ms.popPose();
	}

	private static void rotateAround(PoseStack ms, Direction axis, float angle) {
		float signedAngle = axis.getAxisDirection() == Direction.AxisDirection.POSITIVE ? angle : -angle;
		switch (axis.getAxis()) {
		case X -> ms.mulPose(Axis.XP.rotation(signedAngle));
		case Y -> ms.mulPose(Axis.YP.rotation(signedAngle));
		case Z -> ms.mulPose(Axis.ZP.rotation(signedAngle));
		}
	}

	@Override
	protected void renderSafe(BE be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		// TODO 26.2: restore bell renderer.
	}

	public static float getSwingAngle(float time) {
		float t = time / 1.5f;
		return 1.2f * Mth.sin(t / (float) Math.PI) / (2.5f + t / 3.0f);
	}

}

class BellRenderState extends BlockEntityRenderState {
	BlockState blockState;
	StandaloneModelKey<BlockStateModelPart> model;
	boolean isRinging;
	float ringingTime;
	Direction ringDirection;
}
