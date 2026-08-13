package com.simibubi.create.content.fluids.pipes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection.Flow;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.fluid.FluidRenderer;

import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public class TransparentStraightPipeRenderer extends SafeBlockEntityRenderer<StraightPipeBlockEntity> {

	public TransparentStraightPipeRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(StraightPipeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		renderStreams(be, partialTicks, ms, buffer, null, light);
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new TransparentPipeRenderState();
	}

	@Override
	public void extractRenderState(StraightPipeBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof TransparentPipeRenderState pipeState) {
			pipeState.blockEntity = be;
			pipeState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof TransparentPipeRenderState pipeState))
			return;
		StraightPipeBlockEntity be = pipeState.blockEntity;
		if (be == null || isInvalid(be))
			return;
		renderStreams(be, pipeState.partialTicks, ms, null, collector, state.lightCoords);
	}

	private void renderStreams(StraightPipeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		SubmitNodeCollector collector, int light) {
		FluidTransportBehaviour pipe = be.getBehaviour(FluidTransportBehaviour.TYPE);
		if (pipe == null)
			return;

		for (Direction side : Iterate.directions) {

			Flow flow = pipe.getFlow(side);
			if (flow == null)
				continue;
			FluidStack fluidStack = flow.fluid;
			if (fluidStack.isEmpty())
				continue;
			LerpedFloat progress = flow.progress;
			if (progress == null)
				continue;

			float value = progress.getValue(partialTicks);
			boolean inbound = flow.inbound;
			if (value == 1) {
				if (inbound) {
					Flow opposite = pipe.getFlow(side.getOpposite());
					if (opposite == null)
						value -= 1e-6f;
				} else {
					FluidTransportBehaviour adjacent = BlockEntityBehaviour.get(be.getLevel(), be.getBlockPos()
						.relative(side), FluidTransportBehaviour.TYPE);
					if (adjacent == null)
						value -= 1e-6f;
					else {
						Flow other = adjacent.getFlow(side.getOpposite());
						if (other == null || !other.inbound && !other.complete)
							value -= 1e-6f;
					}
				}
			}

			if (collector != null)
				FluidRenderer.submitFluidStream(collector, fluidStack, side, 3 / 16f, value, inbound, ms, light);
			else
				FluidRenderer.renderFluidStream(fluidStack, side, 3 / 16f, value, inbound, buffer, ms, light);
		}

	}

	private static class TransparentPipeRenderState extends BlockEntityRenderState {
		private StraightPipeBlockEntity blockEntity;
		private float partialTicks;
	}

}
