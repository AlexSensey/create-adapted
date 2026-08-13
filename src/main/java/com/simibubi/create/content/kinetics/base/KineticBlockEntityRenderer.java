package com.simibubi.create.content.kinetics.base;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class KineticBlockEntityRenderer<T extends KineticBlockEntity> extends SafeBlockEntityRenderer<T> {

	public KineticBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new KineticRenderState();
	}

	@Override
	public void extractRenderState(T be, BlockEntityRenderState state, float partialTicks, Vec3 cameraPos,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof KineticRenderState kineticState) {
			kineticState.blockEntity = be;
			kineticState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof KineticBlockEntity be))
			return;
		if (isInvalid((T) be))
			return;
		// The subclass renderer may still submit non-instanced details after this
		// method returns, but the base rotating part is already owned by Flywheel.
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockState renderedState = getRenderedBlockState((T) be);
		List<BlockStateModelPart> parts = getRotatingModelParts((T) be, renderedState);
		if (parts.isEmpty())
			return;

		ms.pushPose();
		transformRotatingModel((T) be, ms, kineticState.partialTicks);
		collector.submitBlockModel(ms, getRotatingRenderType(parts), parts, BlockModelRenderState.EMPTY_TINTS, state.lightCoords,
			OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	@Override
	protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		if (isInvalid(be))
			return;
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockState renderedState = getRenderedBlockState(be);
		VertexConsumer vertexConsumer = buffer.getBuffer(RenderTypes.cutoutMovingBlock());

		ms.pushPose();
		transformRotatingModel(be, ms, partialTicks);
		CachedBuffers.block(renderedState)
			.reset()
			.light(light)
			.renderInto(ms, vertexConsumer);
		ms.popPose();
	}

	protected BlockState getRenderedBlockState(T be) {
		return be.getBlockState();
	}

	protected List<BlockStateModelPart> getRotatingModelParts(T be, BlockState renderedState) {
		BlockStateModel model = Minecraft.getInstance()
			.getModelManager()
			.getBlockStateModelSet()
			.get(renderedState);

		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(renderedState.getSeed(be.getBlockPos())), parts);
		return parts;
	}

	protected RenderType getRotatingRenderType(List<BlockStateModelPart> parts) {
		for (BlockStateModelPart part : parts)
			if ((part.materialFlags() & BakedQuad.FLAG_TRANSLUCENT) != 0)
				return RenderTypes.translucentMovingBlock();
		return RenderTypes.cutoutMovingBlock();
	}

	protected void transformRotatingModel(T be, PoseStack ms, float partialTicks) {
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(getRotationAxisOf(be), getAngleForBe(be, be.getBlockPos(), getRotationAxisOf(be), partialTicks)));
		ms.translate(-.5, -.5, -.5);
	}

	public static void renderRotatingKineticBlock(KineticBlockEntity be, BlockState renderedState, PoseStack ms,
		VertexConsumer buffer, int light) {
		renderRotatingBuffer(be, CachedBuffers.block(renderedState), ms, buffer, light);
	}

	public static void renderRotatingBuffer(KineticBlockEntity be, SuperByteBuffer superBuffer, PoseStack ms,
		VertexConsumer buffer, int light) {
		Axis axis = getRotationAxisOf(be);
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(axis, getAngleForBe(be, be.getBlockPos(), axis)));
		ms.translate(-.5, -.5, -.5);
		superBuffer.reset()
			.light(light)
			.renderInto(ms, buffer);
		ms.popPose();
	}

	public static float getAngleForBe(KineticBlockEntity be, final BlockPos pos, Axis axis) {
		return getAngleForBe(be, pos, axis, AnimationTickHolder.getPartialTicks());
	}

	public static float getAngleForBe(KineticBlockEntity be, final BlockPos pos, Axis axis, float partialTicks) {
		float time = getRenderTime(be, partialTicks);
		float offset = getRotationOffsetForPosition(be, pos, axis);
		float speed = be.getSpeed();
		if (speed == 0 && be.getTheoreticalSpeed() == 0 && be.getGeneratedSpeed() != 0)
			speed = be.getGeneratedSpeed();
		return ((time * speed * 3f / 10 + offset) % 360) / 180 * (float) Math.PI;
	}

	/** All connected kinetic renderers use the same smooth client render clock. */
	public static float getRenderTime(KineticBlockEntity be, float partialTicks) {
		return AnimationTickHolder.getRenderTime();
	}

	public static SuperByteBuffer standardKineticRotationTransform(SuperByteBuffer buffer, KineticBlockEntity be,
		int light) {
		final BlockPos pos = be.getBlockPos();
		Axis axis = getRotationAxisOf(be);
		return kineticRotationTransform(buffer, be, axis, getAngleForBe(be, pos, axis), light);
	}

	public static SuperByteBuffer kineticRotationTransform(SuperByteBuffer buffer, KineticBlockEntity be, Axis axis,
		float angle, int light) {
		buffer.light(light);
		return buffer;
	}

	public static float getRotationOffsetForPosition(KineticBlockEntity be, final BlockPos pos, final Axis axis) {
		return KineticBlockEntityVisual.rotationOffset(be.getBlockState(), axis, pos) + be.getRotationAngleOffset(axis);
	}

	public static BlockState shaft(Axis axis) {
		return AllBlocks.SHAFT.getDefaultState()
			.setValue(BlockStateProperties.AXIS, axis);
	}

	public static Axis getRotationAxisOf(KineticBlockEntity be) {
		return ((IRotate) be.getBlockState()
			.getBlock()).getRotationAxis(be.getBlockState());
	}

	protected static org.joml.Quaternionf rotation(Axis axis, float angle) {
		return switch (axis) {
			case X -> com.mojang.math.Axis.XP.rotation(angle);
			case Y -> com.mojang.math.Axis.YP.rotation(angle);
			case Z -> com.mojang.math.Axis.ZP.rotation(angle);
		};
	}

	protected static class KineticRenderState extends BlockEntityRenderState {
		public KineticBlockEntity blockEntity;
		public float partialTicks;
	}
}
