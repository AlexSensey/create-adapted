package com.simibubi.create.content.processing.basin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.FlatGuiItemRenderer;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.createmod.catnip.api.data.IntAttached;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.TypedInstance;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.fluids.FluidStack;

public class BasinRenderer extends SmartBlockEntityRenderer<BasinBlockEntity> {

	public BasinRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(BasinBlockEntity basin, float partialTicks, PoseStack ms, MultiBufferSource buffer,
	int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new BasinRenderState();
	}

	@Override
	public void extractRenderState(BasinBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof BasinRenderState basinState) {
			basinState.blockEntity = be;
			basinState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof BasinRenderState basinState))
			return;
		BasinBlockEntity basin = basinState.blockEntity;
		if (basin == null || isInvalid(basin))
			return;

		float fluidLevel = renderFluids(basin, basinState.partialTicks, ms, collector, state.lightCoords);
		renderFilter(basin, ms, collector, state.lightCoords);
		renderItems(basin, basinState.partialTicks, fluidLevel, ms, collector, state.lightCoords);
		renderOutputItems(basin, basinState.partialTicks, ms, collector, state.lightCoords);
	}

	private static void renderItems(BasinBlockEntity basin, float partialTicks, float fluidLevel, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		float level = Mth.clamp(fluidLevel - .3f, .125f, .6f);

		ms.pushPose();
		BlockPos pos = basin.getBlockPos();
		ms.translate(.5, .2f, .5);
		ms.mulPose(Axis.YP.rotationDegrees(basin.ingredientRotation.getValue(partialTicks)));

		RandomSource r = RandomSource.create(pos.hashCode());
		Vec3 baseVector = new Vec3(.125, level, 0);

		IItemHandlerModifiable inv = basin.itemCapability;
		if (inv == null)
			inv = new ItemStackHandler();

		int itemCount = 0;
		for (int slot = 0; slot < inv.getSlots(); slot++)
			if (!inv.getStackInSlot(slot)
				.isEmpty())
				itemCount++;

		if (itemCount == 0) {
			ms.popPose();
			return;
		}

		if (itemCount == 1)
			baseVector = new Vec3(0, level, 0);

		float anglePartition = 360f / itemCount;
		for (int slot = 0; slot < inv.getSlots(); slot++) {
			ItemStack stack = inv.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;

			ms.pushPose();

			if (fluidLevel > 0) {
				ms.translate(0,
					(Mth.sin(AnimationTickHolder.getRenderTime() / 12f + anglePartition * itemCount) + 1.5f)
						* 1 / 32f,
					0);
			}

			Vec3 itemPosition = VecHelper.rotate(baseVector, anglePartition * itemCount, Direction.Axis.Y);
			ms.translate(itemPosition.x, itemPosition.y, itemPosition.z);
			ms.mulPose(Axis.YP.rotationDegrees(anglePartition * itemCount + 35));
			ms.mulPose(Axis.XP.rotationDegrees(65));

			for (int i = 0; i <= stack.getCount() / 8; i++) {
				ms.pushPose();
				Vec3 vec = VecHelper.offsetRandomly(Vec3.ZERO, r, 1 / 16f);
				ms.translate(vec.x, vec.y, vec.z);
				renderItem(ms, collector, light, stack, ItemDisplayContext.GROUND);
				ms.popPose();
			}
			ms.popPose();

			itemCount--;
		}
		ms.popPose();
	}

	private static void renderOutputItems(BasinBlockEntity basin, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		BlockState blockState = basin.getBlockState();
		if (!(blockState.getBlock() instanceof BasinBlock))
			return;
		Direction direction = blockState.getValue(BasinBlock.FACING);
		if (direction == Direction.DOWN)
			return;

		Vec3 directionVec = Vec3.atLowerCornerOf(direction.getUnitVec3i());
		Vec3 outVec = VecHelper.getCenterOf(BlockPos.ZERO)
			.add(directionVec.scale(.55)
				.subtract(0, 1 / 2f, 0));

		boolean outToBasin = basin.getLevel()
			.getBlockState(basin.getBlockPos()
				.relative(direction))
			.getBlock() instanceof BasinBlock;

		for (IntAttached<ItemStack> intAttached : basin.visualizedOutputItems) {
			float progress = 1 - (intAttached.getFirst() - partialTicks) / BasinBlockEntity.OUTPUT_ANIMATION_TIME;
			if (!outToBasin && progress > .35f)
				continue;

			ms.pushPose();
			ms.translate(outVec.x, outVec.y, outVec.z);
			ms.translate(0, Math.max(-.55f, -(progress * progress * 2)), 0);
			ms.translate(directionVec.x * progress * .5f, directionVec.y * progress * .5f,
				directionVec.z * progress * .5f);
			ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(direction)));
			ms.mulPose(Axis.XP.rotationDegrees(progress * 180));
			renderItem(ms, collector, light, intAttached.getValue(), ItemDisplayContext.GROUND);
			ms.popPose();
		}
	}

	private static void renderFilter(BasinBlockEntity basin, PoseStack ms, SubmitNodeCollector collector, int light) {
		FilteringBehaviour filtering = basin.getFilter();
		if (filtering == null || !filtering.isActive())
			return;

		ItemStack filter = filtering.getFilter();
		BlockState state = basin.getBlockState();
		for (Direction side : Iterate.horizontalDirections) {
			BasinBlockEntity.BasinValueBox slot = (BasinBlockEntity.BasinValueBox) new BasinBlockEntity.BasinValueBox()
				.fromSide(side);
			if (!slot.shouldRender(basin.getLevel(), basin.getBlockPos(), state))
				continue;

			Vec3 offset = slot.getLocalOffset(basin.getLevel(), basin.getBlockPos(), state);
			if (offset == null)
				continue;

			boolean active = shouldRenderFilterOverlay(basin, slot, side);
			if (!filter.isEmpty()) {
				ms.pushPose();
				applyFilterSlotTransform(ms, basin, state, offset, side, 1 / 128d, true);
				renderFilterItemStack(filter, ms, collector, light);
				ms.popPose();
			}

			if (active)
				renderFilterOverlay(ms, collector, basin, state, slot, offset, side, !filter.isEmpty());
		}
	}

	private static boolean shouldRenderFilterOverlay(BasinBlockEntity basin, BasinBlockEntity.BasinValueBox slot,
		Direction side) {
		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit))
			return false;
		if (!blockHit.getBlockPos()
			.equals(basin.getBlockPos()))
			return false;
		if (blockHit.getDirection() != side)
			return false;

		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(basin.getBlockPos()));
		Vec3 offset = slot.getLocalOffset(basin.getLevel(), basin.getBlockPos(), basin.getBlockState());
		return offset != null && isHitNearFilterSlot(localHit, offset, side);
	}

	private static boolean isHitNearFilterSlot(Vec3 localHit, Vec3 offset, Direction side) {
		double normalDistance;
		double horizontalDistance;
		if (side.getAxis() == Direction.Axis.X) {
			normalDistance = Math.abs(localHit.x - offset.x);
			horizontalDistance = Math.abs(localHit.z - offset.z);
		} else {
			normalDistance = Math.abs(localHit.z - offset.z);
			horizontalDistance = Math.abs(localHit.x - offset.x);
		}
		double verticalDistance = Math.abs(localHit.y - offset.y);
		return normalDistance < 1 / 8d && horizontalDistance < 3 / 16d && verticalDistance < 3 / 16d;
	}

	private static void renderFilterOverlay(PoseStack ms, SubmitNodeCollector collector, BasinBlockEntity basin,
		BlockState state, BasinBlockEntity.BasinValueBox slot, Vec3 offset, Direction side, boolean hasFilter) {
		ms.pushPose();
		applyFilterSlotTransform(ms, basin, state, offset, side, 1 / 512d, false);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			if (hasFilter)
				renderFilterCorners(pose, consumer);
			else
				renderFilterDots(pose, consumer);
		});
		ms.popPose();
	}

	private static void applyFilterSlotTransform(PoseStack ms, BasinBlockEntity basin, BlockState state,
		Vec3 offset, Direction side, double normalOffset, boolean itemScale) {
		Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
		ms.translate(offset.x + normal.x * normalOffset, offset.y + normal.y * normalOffset,
			offset.z + normal.z * normalOffset);
		rotateToFace(ms, side);
		if (itemScale)
			ms.scale(.5f, .5f, .5f);
	}

	private static void rotateToFace(PoseStack ms, Direction face) {
		switch (face) {
			case SOUTH -> {
			}
			case NORTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
			case EAST -> ms.mulPose(Axis.YP.rotationDegrees(90));
			case WEST -> ms.mulPose(Axis.YP.rotationDegrees(270));
			case UP -> ms.mulPose(Axis.XP.rotationDegrees(270));
			case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(90));
		}
	}

	private static void renderFilterItemStack(ItemStack filter, PoseStack ms, SubmitNodeCollector collector, int light) {
		ms.pushPose();
		if (AllBlocks.HAND_CRANK.isIn(filter))
			ms.mulPose(Axis.YP.rotationDegrees(180));
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .5f);
		ms.popPose();
	}

	private static void renderFilterCorners(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		renderThreePixelCorner(pose, consumer, 5, 5, 1, 1, color);
		renderThreePixelCorner(pose, consumer, 10, 5, -1, 1, color);
		renderThreePixelCorner(pose, consumer, 5, 10, 1, -1, color);
		renderThreePixelCorner(pose, consumer, 10, 10, -1, -1, color);
	}

	private static void renderFilterDots(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		flatPixelXY(pose, consumer, 6, 6, color);
		flatPixelXY(pose, consumer, 9, 6, color);
		flatPixelXY(pose, consumer, 6, 9, color);
		flatPixelXY(pose, consumer, 9, 9, color);
	}

	private static void renderThreePixelCorner(Pose pose, VertexConsumer consumer, int x, int y, int xStep, int yStep,
		int color) {
		flatPixelXY(pose, consumer, x, y, color);
		flatPixelXY(pose, consumer, x + xStep, y, color);
		flatPixelXY(pose, consumer, x, y + yStep, color);
	}

	private static void flatPixelXY(Pose pose, VertexConsumer consumer, int x, int y, int color) {
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, x * pixel - .5f, y * pixel - .5f, (x + 1) * pixel - .5f,
			(y + 1) * pixel - .5f, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1,
		int color) {
		consumer.addVertex(pose, x0, y0, 0).setColor(color);
		consumer.addVertex(pose, x1, y0, 0).setColor(color);
		consumer.addVertex(pose, x1, y1, 0).setColor(color);
		consumer.addVertex(pose, x0, y1, 0).setColor(color);
		consumer.addVertex(pose, x0, y1, 0).setColor(color);
		consumer.addVertex(pose, x1, y1, 0).setColor(color);
		consumer.addVertex(pose, x1, y0, 0).setColor(color);
		consumer.addVertex(pose, x0, y0, 0).setColor(color);
	}

	private static void renderItem(PoseStack ms, SubmitNodeCollector collector, int light, ItemStack stack,
		ItemDisplayContext context) {
		if (stack.isEmpty())
			return;
		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, stack, context, null, null, 0);
		itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
	}

	private static float renderFluids(BasinBlockEntity basin, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		SmartFluidTankBehaviour inputFluids = basin.getBehaviour(SmartFluidTankBehaviour.INPUT);
		SmartFluidTankBehaviour outputFluids = basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT);
		SmartFluidTankBehaviour[] tanks = { inputFluids, outputFluids };
		float totalUnits = basin.getTotalFluidUnits(partialTicks);
		if (totalUnits < 1)
			return 0;

		float fluidLevel = Mth.clamp(totalUnits / 2000, 0, 1);
		fluidLevel = 1 - ((1 - fluidLevel) * (1 - fluidLevel));

		float xMin = 2 / 16f;
		float xMax = 2 / 16f;
		final float yMin = 2 / 16f;
		final float yMax = yMin + 12 / 16f * fluidLevel;
		final float zMin = 2 / 16f;
		final float zMax = 14 / 16f;

		for (SmartFluidTankBehaviour behaviour : tanks) {
			if (behaviour == null)
				continue;
			for (TankSegment tankSegment : behaviour.getTanks()) {
				FluidStack renderedFluid = tankSegment.getRenderedFluid();
				if (renderedFluid.isEmpty())
					continue;
				float units = tankSegment.getTotalUnits(partialTicks);
				if (units < 1)
					continue;

				float partial = Mth.clamp(units / totalUnits, 0, 1);
				xMax += partial * 12 / 16f;
				FluidRenderHelper.submitFluidBox(collector, (TypedInstance<Fluid>) renderedFluid, xMin, yMin, zMin, xMax,
					yMax, zMax, ms, light, false, false);
				xMin = xMax;
			}
		}

		return yMax;
	}

	@Override
	public int getViewDistance() {
		return 16;
	}

	private static class BasinRenderState extends BlockEntityRenderState {
		private BasinBlockEntity blockEntity;
		private float partialTicks;
	}
}
