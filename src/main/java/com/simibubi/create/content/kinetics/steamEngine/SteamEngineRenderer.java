package com.simibubi.create.content.kinetics.steamEngine;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueLabelRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.math.AngleHelper;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class SteamEngineRenderer extends SafeBlockEntityRenderer<SteamEngineBlockEntity> {

	public SteamEngineRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(SteamEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new SteamEngineRenderState();
	}

	@Override
	public void extractRenderState(SteamEngineBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof SteamEngineRenderState engineState) {
			engineState.blockEntity = be;
			engineState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof SteamEngineRenderState engineState))
			return;
		SteamEngineBlockEntity be = engineState.blockEntity;
		if (be == null)
			return;

		ScrollValueLabelRenderer.submitWallAttachedScrollOption(be.movementDirection, state, ms, collector,
			cameraRenderState);

		if (CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;

		Float angle = be.getTargetAngle();
		if (angle == null)
			return;

		BlockState blockState = be.getBlockState();
		Direction facing = SteamEngineBlock.getFacing(blockState);
		Direction.Axis facingAxis = facing.getAxis();
		Direction.Axis axis = Direction.Axis.Y;

		PoweredShaftBlockEntity shaft = be.getShaft();
		if (shaft != null)
			axis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);

		boolean roll90 = facingAxis.isHorizontal() && axis == Direction.Axis.Y
			|| facingAxis.isVertical() && axis == Direction.Axis.Z;
		float piston = (6 / 16f) * Mth.sin(angle)
			- Mth.sqrt(Mth.square(14 / 16f) - Mth.square(6 / 16f) * Mth.square(Mth.cos(angle)));
		float distance = Mth.sqrt(Mth.square(piston - 6 / 16f * Mth.sin(angle)));
		float angle2 = (float) Math.acos(distance / (14 / 16f)) * (Mth.cos(angle) >= 0 ? 1f : -1f);

		ms.pushPose();
		transform(ms, facing, roll90);
		ms.translate(0, piston + 20 / 16f, 0);
		submitPart(CreateStandaloneModels.ENGINE_PISTON, ms, collector, state.lightCoords);
		ms.popPose();

		ms.pushPose();
		transform(ms, facing, roll90);
		ms.translate(.5, .5, .5);
		ms.translate(0, 1, 0);
		ms.translate(-.5, -.5, -.5);
		ms.translate(0, piston + 20 / 16f, 0);
		ms.translate(0, 4 / 16f, 8 / 16f);
		ms.mulPose(Axis.XP.rotation(angle2));
		ms.translate(0, -4 / 16f, -8 / 16f);
		submitPart(CreateStandaloneModels.ENGINE_LINKAGE, ms, collector, state.lightCoords);
		ms.popPose();

		ms.pushPose();
		transform(ms, facing, roll90);
		ms.translate(0, 2, 0);
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.XP.rotation(-(angle + Mth.HALF_PI)));
		ms.translate(-.5, -.5, -.5);
		submitPart(CreateStandaloneModels.ENGINE_CONNECTOR, ms, collector, state.lightCoords);
		ms.popPose();
	}

	private static void transform(PoseStack ms, Direction facing, boolean roll90) {
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.mulPose(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(facing) + 90));
		if (roll90)
			ms.mulPose(Axis.YP.rotationDegrees(-90));
		ms.translate(-.5, -.5, -.5);
	}

	private static void submitPart(StandaloneModelKey<BlockStateModelPart> key, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	private static class SteamEngineRenderState extends BlockEntityRenderState {
		private SteamEngineBlockEntity blockEntity;
		private float partialTicks;
	}
}
