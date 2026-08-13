package com.simibubi.create.content.fluids.drain;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.TypedInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;

public class ItemDrainRenderer extends SmartBlockEntityRenderer<ItemDrainBlockEntity> {

	public ItemDrainRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ItemDrainBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new ItemDrainRenderState();
	}

	@Override
	public void extractRenderState(ItemDrainBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof ItemDrainRenderState drainState) {
			drainState.blockEntity = be;
			drainState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof ItemDrainRenderState drainState))
			return;
		ItemDrainBlockEntity be = drainState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		submitBehaviours(be, drainState.partialTicks, ms, collector, state.lightCoords);
		submitFluid(be, drainState.partialTicks, ms, collector, state.lightCoords);
		submitItem(be, drainState.partialTicks, ms, collector, state.lightCoords);
	}

	private static void submitItem(ItemDrainBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		TransportedItemStack transported = be.heldItem;
		if (transported == null)
			return;

		Vec3 itemPosition = VecHelper.getCenterOf(be.getBlockPos());

		Direction insertedFrom = transported.insertedFrom;
		if (!insertedFrom.getAxis()
			.isHorizontal())
			return;

		ms.pushPose();
		ms.translate(.5f, 15 / 16f, .5f);
		float offset = Mth.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
		float sideOffset = Mth.lerp(partialTicks, transported.prevSideOffset, transported.sideOffset);

		Vec3 offsetVec = Vec3.atLowerCornerOf(insertedFrom.getOpposite()
			.getUnitVec3i())
			.scale(.5f - offset);
		ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);
		boolean alongX = insertedFrom.getClockWise()
			.getAxis() == Direction.Axis.X;
		if (!alongX)
			sideOffset *= -1;
		ms.translate(alongX ? sideOffset : 0, 0, alongX ? 0 : sideOffset);

		ItemStack itemStack = transported.stack;
		boolean renderUpright = BeltHelper.isItemUpright(itemStack);

		if (renderUpright)
			ms.translate(0, 3 / 32d, 0);

		int positive = insertedFrom.getAxisDirection()
			.getStep();
		float verticalAngle = positive * offset * 360;
		if (insertedFrom.getAxis() != Direction.Axis.X)
			ms.mulPose(Axis.XP.rotationDegrees(verticalAngle));
		if (insertedFrom.getAxis() != Direction.Axis.Z)
			ms.mulPose(Axis.ZP.rotationDegrees(-verticalAngle));

		if (renderUpright) {
			Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.mainCamera()
				.position();
			Vec3 vectorForOffset = itemPosition.add(offsetVec);
			Vec3 diff = vectorForOffset.subtract(cameraPosition);

			if (insertedFrom.getAxis() != Direction.Axis.X)
				diff = VecHelper.rotate(diff, verticalAngle, Direction.Axis.X);
			if (insertedFrom.getAxis() != Direction.Axis.Z)
				diff = VecHelper.rotate(diff, -verticalAngle, Direction.Axis.Z);

			float yRot = (float) Mth.atan2(diff.z, -diff.x);
			ms.mulPose(Axis.YP.rotation((float) (yRot - Math.PI / 2)));
			ms.translate(0, 0, -1 / 16f);
		}

		submitItemStack(ms, collector, light, itemStack, renderUpright);
		ms.popPose();
	}

	private static void submitItemStack(PoseStack ms, SubmitNodeCollector collector, int light, ItemStack itemStack,
		boolean renderUpright) {
		if (itemStack.isEmpty())
			return;

		Random random = new Random(0);
		int count = Mth.log2(itemStack.getCount()) / 2;
		boolean blockItem = itemStack.getItem() instanceof net.minecraft.world.item.BlockItem;

		for (int i = 0; i <= count; i++) {
			ms.pushPose();
			if (blockItem)
				ms.translate(random.nextFloat() * .0625f * i, 0, random.nextFloat() * .0625f * i);
			ms.scale(.5f, .5f, .5f);
			if (!blockItem && !renderUpright)
				ms.mulPose(Axis.XP.rotationDegrees(90));

			ItemStackRenderState itemState = new ItemStackRenderState();
			Minecraft.getInstance()
				.getItemModelResolver()
				.updateForTopItem(itemState, itemStack, ItemDisplayContext.FIXED, null, null, 0);
			itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();

			if (!renderUpright) {
				if (!blockItem)
					ms.mulPose(Axis.YP.rotationDegrees(10));
				ms.translate(0, blockItem ? 1 / 64d : 1 / 16d, 0);
			} else
				ms.translate(0, 0, -1 / 16f);
		}
	}

	private static void submitFluid(ItemDrainBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		SmartFluidTankBehaviour tank = be.internalTank;
		if (tank == null)
			return;

		TankSegment primaryTank = tank.getPrimaryTank();
		FluidStack fluidStack = primaryTank.getRenderedFluid();
		float level = primaryTank.getFluidLevel()
			.getValue(partialTicks);

		if (!fluidStack.isEmpty() && level != 0) {
			float yMin = 5f / 16f;
			float min = 2f / 16f;
			float max = min + (12 / 16f);
			float yOffset = (7 / 16f) * level;
			ms.pushPose();
			ms.translate(0, yOffset, 0);
			FluidRenderHelper.submitFluidBox(collector, (TypedInstance<Fluid>) fluidStack, min, yMin - yOffset, min,
				max, yMin, max, ms, light, false, false);
			ms.popPose();
		}

		ItemStack heldItemStack = be.getHeldItemStack();
		if (heldItemStack.isEmpty())
			return;
		FluidStack fluidStack2 = GenericItemEmptying.emptyItem(be.getLevel(), heldItemStack, true)
			.getFirst();
		if (fluidStack2.isEmpty()) {
			if (fluidStack.isEmpty())
				return;
			fluidStack2 = fluidStack;
		}

		int processingTicks = be.processingTicks;
		float processingPT = processingTicks - partialTicks;
		float processingProgress = 1 - (processingPT - 5) / 10;
		processingProgress = Mth.clamp(processingProgress, 0, 1);

		if (processingTicks != -1) {
			float radius = (float) (Math.pow(((2 * processingProgress) - 1), 2) - 1);
			AABB bb = new AABB(0.5, 1.0, 0.5, 0.5, 0.25, 0.5).inflate(radius / 32f);
			FluidRenderHelper.submitFluidBox(collector, (TypedInstance<Fluid>) fluidStack2, (float) bb.minX,
				(float) bb.minY, (float) bb.minZ, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ, ms, light, true,
				false);
		}
	}

	private static class ItemDrainRenderState extends BlockEntityRenderState {
		private ItemDrainBlockEntity blockEntity;
		private float partialTicks;
	}

}
