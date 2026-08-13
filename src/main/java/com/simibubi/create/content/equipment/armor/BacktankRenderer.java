package com.simibubi.create.content.equipment.armor;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class BacktankRenderer extends KineticBlockEntityRenderer<BacktankBlockEntity> {

	public BacktankRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(BacktankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer API.
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState)
			|| !(kineticState.blockEntity instanceof BacktankBlockEntity be) || isInvalid(be))
			return;

		BlockState blockState = be.getBlockState();
		BlockStateModelPart shaft = model(getShaftModelKey(blockState));
		BlockStateModelPart cogs = model(getCogsModelKey(blockState));

		if (shaft != null) {
			ms.pushPose();
			transformRotatingModel(be, ms, kineticState.partialTicks);
			submitPart(state, ms, collector, shaft);
			ms.popPose();
		}

		if (cogs != null) {
			Direction facing = blockState.getValue(BacktankBlock.HORIZONTAL_FACING);
			float angle = Mth.DEG_TO_RAD
				* (be.getSpeed() / 4f * AnimationTickHolder.getRenderTime() % 360);

			ms.pushPose();
			rotateCentered(ms, com.mojang.math.Axis.YP,
				180 + AngleHelper.horizontalAngle(facing));
			ms.translate(0, 6.5f / 16, 11f / 16);
			ms.mulPose(com.mojang.math.Axis.XP.rotation(angle));
			ms.translate(0, -6.5f / 16, -11f / 16);
			submitPart(state, ms, collector, cogs);
			ms.popPose();
		}
	}

	protected SuperByteBuffer getRotatedModel(BacktankBlockEntity be, BlockState state) {
		return null;
	}

	public static PartialModel getCogsModel(BlockState state) {
		return AllBlocks.NETHERITE_BACKTANK.has(state) ? AllPartialModels.NETHERITE_BACKTANK_COGS
			: AllPartialModels.COPPER_BACKTANK_COGS;
	}

	public static PartialModel getShaftModel(BlockState state) {
		return AllBlocks.NETHERITE_BACKTANK.has(state) ? AllPartialModels.NETHERITE_BACKTANK_SHAFT
			: AllPartialModels.COPPER_BACKTANK_SHAFT;
	}

	public static StandaloneModelKey<BlockStateModelPart> getCogsModelKey(BlockState state) {
		return AllBlocks.NETHERITE_BACKTANK.has(state) ? CreateStandaloneModels.NETHERITE_BACKTANK_COGS
			: CreateStandaloneModels.COPPER_BACKTANK_COGS;
	}

	public static StandaloneModelKey<BlockStateModelPart> getShaftModelKey(BlockState state) {
		return AllBlocks.NETHERITE_BACKTANK.has(state) ? CreateStandaloneModels.NETHERITE_BACKTANK_SHAFT
			: CreateStandaloneModels.COPPER_BACKTANK_SHAFT;
	}

	private static BlockStateModelPart model(StandaloneModelKey<BlockStateModelPart> key) {
		return Minecraft.getInstance().getModelManager().getStandaloneModel(key);
	}

	private static void submitPart(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		BlockStateModelPart part) {
		collector.submitBlockModel(ms, RenderTypes.solidMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
	}

	private static void rotateCentered(PoseStack ms, com.mojang.math.Axis axis, float degrees) {
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(axis.rotationDegrees(degrees));
		ms.translate(-.5f, -.5f, -.5f);
	}
}
