package com.simibubi.create.content.trains.bogey;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BogeyBlockEntityRenderer<T extends AbstractBogeyBlockEntity> extends SafeBlockEntityRenderer<T> {
	public BogeyBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {
		BlockState blockState = be.getBlockState();
		if (!(blockState.getBlock() instanceof AbstractBogeyBlock<?> bogey)) {
			return;
		}

		float angle = be.getVirtualAngle(partialTicks);
		ms.pushPose();
		ms.translate(.5f, .5f, .5f);
		if (blockState.getValue(AbstractBogeyBlock.AXIS) == Direction.Axis.X)
			ms.mulPose(Axis.YP.rotationDegrees(90));
		be.getStyle().render(bogey.getSize(), partialTicks, ms, buffer, light, overlay, angle, be.getBogeyData(), false);
		ms.popPose();
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new BogeyRenderState();
	}

	@Override
	public void extractRenderState(T be, BlockEntityRenderState state, float partialTicks, Vec3 cameraPos,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof BogeyRenderState bogeyState) {
			bogeyState.blockEntity = be;
			bogeyState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof BogeyRenderState bogeyState))
			return;
		AbstractBogeyBlockEntity be = bogeyState.blockEntity;
		if (be == null || !be.hasLevel())
			return;

		BlockState blockState = be.getBlockState();
		if (!(blockState.getBlock() instanceof AbstractBogeyBlock<?> bogey))
			return;

		float angle = be.getVirtualAngle(bogeyState.partialTicks);
		ms.pushPose();
		ms.translate(.5f, .5f, .5f);
		if (blockState.getValue(AbstractBogeyBlock.AXIS) == Direction.Axis.X)
			ms.mulPose(Axis.YP.rotationDegrees(90));
		be.getStyle().submit(bogey.getSize(), bogeyState.partialTicks, ms, collector, state.lightCoords,
			0, angle, be.getBogeyData(), false);
		ms.popPose();
	}

	private static class BogeyRenderState extends BlockEntityRenderState {
		private AbstractBogeyBlockEntity blockEntity;
		private float partialTicks;
	}
}
