package com.simibubi.create.content.kinetics.crank;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueLabelRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

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
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Quaternionf;

public class HandCrankRenderer extends KineticBlockEntityRenderer<HandCrankBlockEntity> {
	private List<BlockStateModelPart> handleModel;
	private List<BlockStateModelPart> valveHandleModel;
	private final Map<DyeColor, List<BlockStateModelPart>> dyedValveHandleModels = new EnumMap<>(DyeColor.class);

	public HandCrankRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof HandCrankBlockEntity be))
			return;
		if (isInvalid(be))
			return;

		if (be instanceof ValveHandleBlockEntity valveHandle) {
			if (!VisualizationManager.supportsVisualization(be.getLevel()))
				submitHandle(state, kineticState, ms, collector, valveHandle);
			ScrollValueLabelRenderer.submitValveHandle(valveHandle, state, ms, collector, cameraRenderState);
			return;
		}

		if (!VisualizationManager.supportsVisualization(be.getLevel()))
			submitHandle(state, kineticState, ms, collector, be);
	}

	private void submitHandle(BlockEntityRenderState state, KineticRenderState kineticState, PoseStack ms,
		SubmitNodeCollector collector, HandCrankBlockEntity be) {
		List<BlockStateModelPart> handle = getHandleModel(be);
		if (handle.isEmpty())
			return;

		Direction facing = be.getBlockState()
			.getValue(HandCrankBlock.FACING);
		float angle = be.getIndependentAngle(kineticState.partialTicks) / 180f * (float) Math.PI;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(facing.getAxis(), angle));
		if (be.getBlockState()
			.getBlock() instanceof ValveHandleBlock)
			ms.mulPose(new Quaternionf().rotateTo(0, 1, 0, facing.getStepX(), facing.getStepY(), facing.getStepZ()));
		else
			rotateToFacing(ms, facing.getOpposite());
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), handle, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	@Override
	protected void renderSafe(HandCrankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	private List<BlockStateModelPart> getHandleModel(HandCrankBlockEntity be) {
		if (be.getBlockState()
			.getBlock() instanceof ValveHandleBlock valveHandleBlock)
			return getValveHandleModel(valveHandleBlock.color);
		if (handleModel != null)
			return handleModel;
		BlockStateModelPart handle = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.HAND_CRANK_HANDLE);
		return handleModel = handle == null ? List.of() : List.of(handle);
	}

	private List<BlockStateModelPart> getValveHandleModel(DyeColor color) {
		if (color == null) {
			if (valveHandleModel != null)
				return valveHandleModel;
			return valveHandleModel = getModel(CreateStandaloneModels.VALVE_HANDLE);
		}
		return dyedValveHandleModels.computeIfAbsent(color,
			c -> getModel(CreateStandaloneModels.DYED_VALVE_HANDLES.get(c)));
	}

	private static List<BlockStateModelPart> getModel(StandaloneModelKey<BlockStateModelPart> key) {
		if (key == null)
			return List.of();
		BlockStateModelPart handle = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		return handle == null ? List.of() : List.of(handle);
	}

	private static void rotateToFacing(PoseStack ms, Direction direction) {
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
