package com.simibubi.create.content.equipment.toolbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

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
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ToolboxRenderer extends SmartBlockEntityRenderer<ToolboxBlockEntity> {
	private final Map<DyeColor, BlockStateModelPart> lidModels = new EnumMap<>(DyeColor.class);
	private BlockStateModelPart drawerModel;

	public ToolboxRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ToolboxBlockEntity blockEntity, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new ToolboxRenderState();
	}

	@Override
	public void extractRenderState(ToolboxBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof ToolboxRenderState toolboxState) {
			toolboxState.blockEntity = be;
			toolboxState.blockState = be.getBlockState();
			toolboxState.color = be.getColor();
			toolboxState.lidAngle = be.lid.getValue(partialTicks);
			toolboxState.drawerOffset = be.drawers.getValue(partialTicks);
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);
		if (!(state instanceof ToolboxRenderState toolboxState))
			return;
		ToolboxBlockEntity be = toolboxState.blockEntity;
		if (be == null || isInvalid(be))
			return;
		if (!toolboxState.blockState.hasProperty(ToolboxBlock.FACING))
			return;

		BlockStateModelPart lid = getLidModel(toolboxState.color);
		BlockStateModelPart drawer = getDrawerModel();
		if (lid == null || drawer == null)
			return;

		Direction facing = toolboxState.blockState.getValue(ToolboxBlock.FACING)
			.getOpposite();

		ms.pushPose();
		rotateToFacing(ms, facing);
		ms.translate(0, 6 / 16f, 12 / 16f);
		ms.mulPose(Axis.XP.rotationDegrees(135 * toolboxState.lidAngle));
		ms.translate(0, -6 / 16f, -12 / 16f);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(lid),
			BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();

		for (int offset = 0; offset <= 1; offset++) {
			ms.pushPose();
			rotateToFacing(ms, facing);
			ms.translate(0, offset * 1 / 8f, -toolboxState.drawerOffset * .175f * (2 - offset));
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(drawer),
				BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}
	}

	private BlockStateModelPart getLidModel(DyeColor color) {
		BlockStateModelPart cached = lidModels.get(color);
		if (cached != null)
			return cached;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.TOOLBOX_LIDS.get(color));
		if (model != null)
			lidModels.put(color, model);
		return model;
	}

	private BlockStateModelPart getDrawerModel() {
		if (drawerModel != null)
			return drawerModel;
		return drawerModel = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.TOOLBOX_DRAWER);
	}

	private static void rotateToFacing(PoseStack ms, Direction facing) {
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
		ms.translate(-.5, -.5, -.5);
	}

	private static class ToolboxRenderState extends BlockEntityRenderState {
		ToolboxBlockEntity blockEntity;
		BlockState blockState;
		DyeColor color;
		float lidAngle;
		float drawerOffset;
	}
}
