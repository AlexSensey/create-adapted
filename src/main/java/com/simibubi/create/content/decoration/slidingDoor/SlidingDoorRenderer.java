package com.simibubi.create.content.decoration.slidingDoor;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class SlidingDoorRenderer extends SafeBlockEntityRenderer<SlidingDoorBlockEntity> {

	public SlidingDoorRenderer(Context context) {}

	@Override
	protected void renderSafe(SlidingDoorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer API.
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new SlidingDoorRenderState();
	}

	@Override
	public void extractRenderState(SlidingDoorBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof SlidingDoorRenderState doorState) {
			doorState.blockEntity = be;
			doorState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof SlidingDoorRenderState doorState) || doorState.blockEntity == null)
			return;

		SlidingDoorBlockEntity be = doorState.blockEntity;
		BlockState blockState = be.getBlockState();
		if (!(blockState.getBlock() instanceof SlidingDoorBlock door) || !be.shouldRenderSpecial(blockState))
			return;

		Direction facing = blockState.getValue(DoorBlock.FACING);
		Direction movementDirection = facing.getClockWise();
		if (blockState.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT)
			movementDirection = movementDirection.getOpposite();

		float value = be.animation.getValue(doorState.partialTicks);
		float value2 = Mth.clamp(value * 10, 0, 1);
		float bridgeOffset = be.isVisible(blockState) ? 1 / 256f : 0;

		if (door.isFoldingDoor()) {
			submitFoldingDoor(blockState, facing, value, value2, bridgeOffset, ms, collector, state.lightCoords);
			return;
		}

		Vec3 offset = Vec3.atLowerCornerOf(movementDirection.getUnitVec3i())
			.scale(value * value * 13 / 16f)
			.add(Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(value2 / 32f + bridgeOffset));

		for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
			BlockState renderedState = blockState.setValue(DoorBlock.OPEN, false).setValue(DoorBlock.HALF, half);
			ms.pushPose();
			ms.translate(offset.x, offset.y + (half == DoubleBlockHalf.UPPER ? 1 - 1 / 512f : 0), offset.z);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), blockParts(renderedState),
				BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}
	}

	private static void submitFoldingDoor(BlockState state, Direction facing, float value, float value2,
		float bridgeOffset, PoseStack ms, SubmitNodeCollector collector, int light) {
		Couple<StandaloneModelKey<BlockStateModelPart>> partials =
			CreateStandaloneModels.FOLDING_DOORS.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
		if (partials == null)
			return;

		boolean flip = state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT;
		for (boolean left : Iterate.trueAndFalse) {
			BlockStateModelPart part = Minecraft.getInstance().getModelManager()
				.getStandaloneModel(partials.get(left ^ flip));
			float f = flip ? -1 : 1;

			ms.pushPose();
			ms.translate(0, -1 / 512f, 0);
			Vec3 facingOffset = Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(value2 / 32f + bridgeOffset);
			ms.translate(facingOffset.x, facingOffset.y, facingOffset.z);
			rotateCentered(ms, Axis.YP, AngleHelper.horizontalAngle(facing.getClockWise()));
			if (flip)
				ms.translate(0, 0, 1);
			ms.mulPose(Axis.YP.rotationDegrees(91 * f * value * value));
			if (!left) {
				ms.translate(0, 0, f / 2f);
				ms.mulPose(Axis.YP.rotationDegrees(-181 * f * value * value));
			}
			if (flip)
				ms.translate(0, 0, -1 / 2f);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
				BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}
	}

	private static List<BlockStateModelPart> blockParts(BlockState state) {
		BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(state.getSeed(BlockPos.ZERO)), parts);
		return parts;
	}

	private static void rotateCentered(PoseStack ms, com.mojang.math.Axis axis, float degrees) {
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(axis.rotationDegrees(degrees));
		ms.translate(-.5f, -.5f, -.5f);
	}

	private static class SlidingDoorRenderState extends BlockEntityRenderState {
		private SlidingDoorBlockEntity blockEntity;
		private float partialTicks;
	}

}
