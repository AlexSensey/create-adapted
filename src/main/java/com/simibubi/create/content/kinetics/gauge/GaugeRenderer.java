package com.simibubi.create.content.kinetics.gauge;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock.Type;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.createmod.catnip.api.data.Iterate;
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

public class GaugeRenderer extends ShaftRenderer<GaugeBlockEntity> {

	protected GaugeBlock.Type type;

	public static GaugeRenderer speed(BlockEntityRendererProvider.Context context) {
		return new GaugeRenderer(context, Type.SPEED);
	}

	public static GaugeRenderer stress(BlockEntityRendererProvider.Context context) {
		return new GaugeRenderer(context, Type.STRESS);
	}

	protected GaugeRenderer(BlockEntityRendererProvider.Context context, GaugeBlock.Type type) {
		super(context);
		this.type = type;
	}

	@Override
	protected void renderSafe(GaugeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);

		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof GaugeBlockEntity be))
			return;
		if (isInvalid(be))
			return;
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockState gaugeState = be.getBlockState();
		if (!(gaugeState.getBlock() instanceof GaugeBlock gaugeBlock))
			return;

		StandaloneModelKey<BlockStateModelPart> head = type == Type.SPEED
			? CreateStandaloneModels.GAUGE_HEAD_SPEED
			: CreateStandaloneModels.GAUGE_HEAD_STRESS;
		float dialPivot = 5.75f / 16;
		float progress = Mth.lerp(kineticState.partialTicks, be.prevDialState, be.dialState);

		for (Direction facing : Iterate.directions) {
			if (!gaugeBlock.shouldRenderHeadOnFace(be.getLevel(), be.getBlockPos(), gaugeState, facing))
				continue;

			ms.pushPose();
			rotateTowards(ms, facing);
			ms.translate(0, dialPivot, dialPivot);
			ms.mulPose(Axis.XP.rotationDegrees(-90 * progress));
			ms.translate(0, -dialPivot, -dialPivot);
			submitPart(CreateStandaloneModels.GAUGE_DIAL, ms, collector, state.lightCoords);
			ms.popPose();

			ms.pushPose();
			rotateTowards(ms, facing);
			submitPart(head, ms, collector, state.lightCoords);
			ms.popPose();
		}
	}

	private static void rotateTowards(PoseStack ms, Direction target) {
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(-target.toYRot() - 90));
		ms.translate(-.5, -.5, -.5);
	}

	private static void submitPart(StandaloneModelKey<BlockStateModelPart> key, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		if (part == null)
			return;
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}
}
