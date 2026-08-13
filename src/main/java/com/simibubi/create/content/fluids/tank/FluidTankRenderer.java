package com.simibubi.create.content.fluids.tank;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class FluidTankRenderer extends SafeBlockEntityRenderer<FluidTankBlockEntity> {
	private List<BlockStateModelPart> boilerGaugeModel;
	private List<BlockStateModelPart> boilerGaugeDialModel;

	public FluidTankRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(FluidTankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		if (!be.isController())
			return;
		if (!be.window) {
			if (be.boiler.isActive())
				renderAsBoiler(be, partialTicks, ms, buffer, light);
			return;
		}

		LerpedFloat fluidLevel = be.getFluidLevel();
		if (fluidLevel == null)
			return;

		float capHeight = 1 / 4f;
		float tankHullWidth = 1 / 16f + 1 / 128f;
		float minPuddleHeight = 1 / 16f;
		float totalHeight = be.height - 2 * capHeight - minPuddleHeight;

		float level = fluidLevel.getValue(partialTicks);
		if (level <= 0 && be.tankInventory.getFluidAmount() > 0)
			level = be.getFillState();
		if (level < 1 / (512f * totalHeight))
			return;
		float clampedLevel = Mth.clamp(level * totalHeight, 0, totalHeight);

		FluidTank tank = be.tankInventory;
		FluidStack fluidStack = tank.getFluid();
		if (fluidStack.isEmpty())
			return;

		boolean top = fluidStack.getFluid()
			.getFluidType()
			.isLighterThanAir();

		float xMin = tankHullWidth;
		float xMax = xMin + be.width - 2 * tankHullWidth;
		float yMin = totalHeight + capHeight + minPuddleHeight - clampedLevel;
		float yMax = yMin + clampedLevel;

		if (top) {
			yMin += totalHeight - clampedLevel;
			yMax += totalHeight - clampedLevel;
		}

		float zMin = tankHullWidth;
		float zMax = zMin + be.width - 2 * tankHullWidth;

		ms.pushPose();
		ms.translate(0, clampedLevel - totalHeight, 0);
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, xMin, yMin, zMin, xMax, yMax, zMax, buffer,
			ms, light, false, true);
		ms.popPose();
	}

	protected void renderAsBoiler(FluidTankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light) {
	}

	private void renderAsBoiler(FluidTankBlockEntity be, float partialTicks, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		float dialPivotY = 6f / 16;
		float dialPivotZ = 8f / 16;
		float progress = be.boiler.gauge.getValue(partialTicks);

		List<BlockStateModelPart> gauge = getBoilerGaugeModel();
		List<BlockStateModelPart> dial = getBoilerGaugeDialModel();
		if (gauge.isEmpty() || dial.isEmpty())
			return;

		ms.pushPose();
		ms.translate(be.width / 2f, 0.5, be.width / 2f);
		for (Direction d : Iterate.horizontalDirections) {
			if (be.boiler.occludedDirections[d.get2DDataValue()])
				continue;
			float yRot = -d.toYRot() - 90;

			ms.pushPose();
			ms.mulPose(Axis.YP.rotationDegrees(yRot));
			ms.translate(-.5f, -.5f, -.5f);
			ms.translate(be.width / 2f - 6 / 16f, 0, 0);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), gauge, BlockModelRenderState.EMPTY_TINTS,
				light, OverlayTexture.NO_OVERLAY, 0);
			ms.translate(0, dialPivotY, dialPivotZ);
			ms.mulPose(Axis.XP.rotationDegrees(-145 * progress + 90));
			ms.translate(0, -dialPivotY, -dialPivotZ);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), dial, BlockModelRenderState.EMPTY_TINTS,
				light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		ms.popPose();
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new FluidTankRenderState();
	}

	@Override
	public void extractRenderState(FluidTankBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof FluidTankRenderState tankState) {
			tankState.controller = be.isController();
			tankState.window = be.window;
			tankState.boilerActive = be.boiler.isActive();
			tankState.width = be.width;
			tankState.height = be.height;
			tankState.partialTicks = partialTicks;
			LerpedFloat fluidLevel = be.getFluidLevel();
			tankState.level = fluidLevel == null ? 0 : fluidLevel.getValue(partialTicks);
			tankState.fluid = be.tankInventory.getFluid().copy();
			tankState.blockEntity = be;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof FluidTankRenderState tankState))
			return;
		if (!tankState.controller)
			return;
		if (!tankState.window) {
			FluidTankBlockEntity be = tankState.blockEntity;
			if (tankState.boilerActive && be != null && !isInvalid(be))
				renderAsBoiler(be, tankState.partialTicks, ms, collector, state.lightCoords);
			return;
		}

		submitFluid(tankState, ms, collector, state.lightCoords);
	}

	private static void submitFluid(FluidTankRenderState state, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		float capHeight = 1 / 4f;
		float tankHullWidth = 1 / 16f + 1 / 128f;
		float minPuddleHeight = 1 / 16f;
		float totalHeight = state.height - 2 * capHeight - minPuddleHeight;

		float level = state.level;
		if (level < 1 / (512f * totalHeight))
			return;
		float clampedLevel = Mth.clamp(level * totalHeight, 0, totalHeight);

		FluidStack fluidStack = state.fluid;
		if (fluidStack.isEmpty())
			return;

		boolean top = fluidStack.getFluid()
			.getFluidType()
			.isLighterThanAir();

		float xMin = tankHullWidth;
		float xMax = xMin + state.width - 2 * tankHullWidth;
		float yMin = totalHeight + capHeight + minPuddleHeight - clampedLevel;
		float yMax = yMin + clampedLevel;

		if (top) {
			yMin += totalHeight - clampedLevel;
			yMax += totalHeight - clampedLevel;
		}

		float zMin = tankHullWidth;
		float zMax = zMin + state.width - 2 * tankHullWidth;

		ms.pushPose();
		ms.translate(0, clampedLevel - totalHeight, 0);
		FluidRenderHelper.submitFluidBox(collector, (TypedInstance<Fluid>) fluidStack, xMin, yMin, zMin, xMax, yMax,
			zMax, ms, light, false, true);
		ms.popPose();
	}

	public boolean shouldRenderOffScreen(FluidTankBlockEntity be) {
		return be.isController();
	}

	private List<BlockStateModelPart> getBoilerGaugeModel() {
		if (boilerGaugeModel != null)
			return boilerGaugeModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.BOILER_GAUGE);
		return boilerGaugeModel = model == null ? List.of() : List.of(model);
	}

	private List<BlockStateModelPart> getBoilerGaugeDialModel() {
		if (boilerGaugeDialModel != null)
			return boilerGaugeDialModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.BOILER_GAUGE_DIAL);
		return boilerGaugeDialModel = model == null ? List.of() : List.of(model);
	}

	private static class FluidTankRenderState extends BlockEntityRenderState {
		private boolean controller;
		private boolean window;
		private boolean boilerActive;
		private int width;
		private int height;
		private float level;
		private FluidStack fluid = FluidStack.EMPTY;
		private FluidTankBlockEntity blockEntity;
		private float partialTicks;
	}

}
