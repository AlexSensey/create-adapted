package com.simibubi.create.content.contraptions.actors.harvester;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionActorRotation;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class HarvesterRenderer extends SafeBlockEntityRenderer<HarvesterBlockEntity> {
	private static final Vec3 PIVOT = new Vec3(0, 6 / 16f, 9 / 16f);
	private List<BlockStateModelPart> bladeModel;

	public HarvesterRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(HarvesterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new HarvesterRenderState();
	}

	@Override
	public void extractRenderState(HarvesterBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof HarvesterRenderState harvesterState) {
			harvesterState.blockEntity = be;
			harvesterState.blockState = be.getBlockState();
			harvesterState.partialTicks = partialTicks;
			harvesterState.speed = be.getAnimatedSpeed();
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof HarvesterRenderState harvesterState))
			return;
		HarvesterBlockEntity be = harvesterState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		List<BlockStateModelPart> blade = getBladeModel();
		if (blade.isEmpty())
			return;

		ms.pushPose();
		transform(harvesterState.blockState, harvesterState.speed, harvesterState.partialTicks, ms);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), blade, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffers) {
		SuperByteBuffer block = CachedBuffers.block(context.state);
		int light = LightCoordsUtil.getLightCoords(renderWorld, context.localPos);
		if (!block.isEmpty())
			block.light(light)
				.renderInto(matrices.getModel(), buffers.getBuffer(RenderTypes.cutoutMovingBlock()));

	}

	public static void submitInContraption(MovementContext context, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		BlockStateModelPart blade = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.HARVESTER_BLADE);
		if (blade == null)
			return;

		Direction facing = context.state.getValue(HarvesterBlock.FACING);
		float speed = !VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite())
			? context.getAnimationSpeed()
			: 0;
		if (context.contraption.stalled)
			speed = 0;

		ms.pushPose();
		transform(context.state, ContraptionActorRotation.getAngle(context, speed), ms);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(blade), BlockModelRenderState.EMPTY_TINTS,
			light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	public static void transform(Level world, Direction facing, SuperByteBuffer superBuffer, float speed, Vec3 pivot) {
		// Legacy Flywheel helper retained for source compatibility; rendering now uses submit().
	}

	private List<BlockStateModelPart> getBladeModel() {
		if (bladeModel != null)
			return bladeModel;
		BlockStateModelPart blade = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.HARVESTER_BLADE);
		return bladeModel = blade == null ? List.of() : List.of(blade);
	}

	private static void transform(BlockState blockState, float speed, float partialTicks, PoseStack ms) {
		transform(blockState, getBladeAngle(speed, partialTicks), ms);
	}

	private static void transform(BlockState blockState, float angle, PoseStack ms) {
		Direction facing = blockState.getValue(HarvesterBlock.FACING);
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.translate(-.5, -.5, -.5);
		ms.translate(PIVOT.x, PIVOT.y, PIVOT.z);
		ms.mulPose(Axis.XN.rotation(angle));
		ms.translate(-PIVOT.x, -PIVOT.y, -PIVOT.z);
	}

	private static float getBladeAngle(float speed, float partialTicks) {
		if (speed == 0)
			return 0;
		float time = AnimationTickHolder.getRenderTime();
		return ((time * speed / 20f) % 360) / 180 * (float) Math.PI;
	}

	private static class HarvesterRenderState extends BlockEntityRenderState {
		private HarvesterBlockEntity blockEntity;
		private BlockState blockState;
		private float partialTicks;
		private float speed;
	}

}
