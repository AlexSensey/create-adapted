package com.simibubi.create.content.fluids.spout;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.api.client.render.FluidRenderHelper;
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
import net.minecraft.core.TypedInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.fluids.FluidStack;

public class SpoutRenderer extends SafeBlockEntityRenderer<SpoutBlockEntity> {
	private List<BlockStateModelPart> top;
	private List<BlockStateModelPart> middle;
	private List<BlockStateModelPart> bottom;

	public SpoutRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(SpoutBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new SpoutRenderState();
	}

	@Override
	public void extractRenderState(SpoutBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof SpoutRenderState spoutState) {
			spoutState.blockEntity = be;
			spoutState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof SpoutRenderState spoutState))
			return;
		SpoutBlockEntity be = spoutState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		SmartFluidTankBehaviour tank = be.tank;
		if (tank == null)
			return;

		TankSegment primaryTank = tank.getPrimaryTank();
		FluidStack fluidStack = primaryTank.getRenderedFluid();
		float level = primaryTank.getFluidLevel()
			.getValue(spoutState.partialTicks);

		if (!fluidStack.isEmpty() && level != 0)
			submitFluid(fluidStack, level, ms, collector, state.lightCoords);

		float squeeze = submitProcessingStream(be, fluidStack, spoutState.partialTicks, ms, collector, state.lightCoords);
		submitBits(squeeze, ms, collector, state.lightCoords);
	}

	private static void submitFluid(FluidStack fluidStack, float level, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		boolean top = fluidStack.getFluid()
			.getFluidType()
			.isLighterThanAir();

		level = Math.max(level, 0.175f);
		float min = 2.5f / 16f;
		float max = min + (11 / 16f);
		float yOffset = (11 / 16f) * level;

		ms.pushPose();
		if (!top)
			ms.translate(0, yOffset, 0);
		else
			ms.translate(0, max - min, 0);

		FluidRenderHelper.submitFluidBox(collector, (TypedInstance<Fluid>) fluidStack, min, min - yOffset, min, max,
			min, max, ms, light, false, true);
		ms.popPose();
	}

	private static float submitProcessingStream(SpoutBlockEntity be, FluidStack fluidStack, float partialTicks,
		PoseStack ms, SubmitNodeCollector collector, int light) {
		int processingTicks = be.processingTicks;
		float processingPT = processingTicks - partialTicks;
		float processingProgress = 1 - (processingPT - 5) / 10;
		processingProgress = Mth.clamp(processingProgress, 0, 1);
		float radius = 0;

		if (!fluidStack.isEmpty() && processingTicks != -1) {
			radius = (float) (Math.pow(((2 * processingProgress) - 1), 2) - 1);
			AABB bb = new AABB(0.5, 0.0, 0.5, 0.5, -1.2, 0.5).inflate(radius / 32f);
			FluidRenderHelper.submitFluidBox(collector, (TypedInstance<Fluid>) fluidStack, (float) bb.minX,
				(float) bb.minY, (float) bb.minZ, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ, ms, light, true,
				true);
		}

		float squeeze = radius;
		if (processingPT < 0)
			squeeze = 0;
		else if (processingPT < 2)
			squeeze = Mth.lerp(processingPT / 2f, 0, -1);
		else if (processingPT < 10)
			squeeze = -1;
		return squeeze;
	}

	private void submitBits(float squeeze, PoseStack ms, SubmitNodeCollector collector, int light) {
		ms.pushPose();
		for (List<BlockStateModelPart> bit : List.of(getTop(), getMiddle(), getBottom())) {
			if (!bit.isEmpty())
				collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), bit, BlockModelRenderState.EMPTY_TINTS,
					light, OverlayTexture.NO_OVERLAY, 0);
			ms.translate(0, -3 * squeeze / 32f, 0);
		}
		ms.popPose();
	}

	private List<BlockStateModelPart> getTop() {
		return top = top == null ? getModel(CreateStandaloneModels.SPOUT_TOP) : top;
	}

	private List<BlockStateModelPart> getMiddle() {
		return middle = middle == null ? getModel(CreateStandaloneModels.SPOUT_MIDDLE) : middle;
	}

	private List<BlockStateModelPart> getBottom() {
		return bottom = bottom == null ? getModel(CreateStandaloneModels.SPOUT_BOTTOM) : bottom;
	}

	private static List<BlockStateModelPart> getModel(StandaloneModelKey<BlockStateModelPart> key) {
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		return model == null ? List.of() : List.of(model);
	}

	private static class SpoutRenderState extends BlockEntityRenderState {
		private SpoutBlockEntity blockEntity;
		private float partialTicks;
	}

}
