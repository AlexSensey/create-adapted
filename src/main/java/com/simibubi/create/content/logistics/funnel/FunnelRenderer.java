package com.simibubi.create.content.logistics.funnel;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.logistics.FlapStuffs;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.FlatGuiItemRenderer;

import net.createmod.catnip.api.math.AngleHelper;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FunnelRenderer extends SmartBlockEntityRenderer<FunnelBlockEntity> {

	private static final int MARK_BACKGROUND = 0xFF555555;
	private static final int MARK_FOREGROUND = 0xFFFFFFFF;

	private List<BlockStateModelPart> funnelFlapModel;
	private List<BlockStateModelPart> beltFunnelFlapModel;

	public FunnelRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(FunnelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new FunnelRenderState();
	}

	@Override
	public void extractRenderState(FunnelBlockEntity be, BlockEntityRenderState state, float partialTicks,
		net.minecraft.world.phys.Vec3 cameraPos,
		net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof FunnelRenderState funnelState) {
			funnelState.blockEntity = be;
			funnelState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof FunnelRenderState funnelState))
			return;
		FunnelBlockEntity be = funnelState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		BlockState blockState = be.getBlockState();
		renderFilter(be, blockState, ms, collector, state.lightCoords);

		if (!be.hasFlap())
			return;

		Direction facing = AbstractFunnelBlock.getFunnelFacing(blockState);
		if (facing == null || !facing.getAxis()
			.isHorizontal())
			return;

		List<BlockStateModelPart> flap = getFlapModel(blockState.getBlock() instanceof BeltFunnelBlock);
		if (flap.isEmpty())
			return;

		float flapness = be.flap.getValue(funnelState.partialTicks);
		float zOffset = -be.getFlapOffset();
		float horizontalAngle = AngleHelper.horizontalAngle(facing.getOpposite());

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(horizontalAngle));
		ms.translate(-.5, -.5, -.5);
		ms.translate(FlapStuffs.X_OFFSET, 0, zOffset);

		for (int segment = 0; segment < FlapStuffs.FLAP_COUNT; segment++) {
			ms.pushPose();
			ms.translate(FlapStuffs.FUNNEL_PIVOT.x, FlapStuffs.FUNNEL_PIVOT.y, FlapStuffs.FUNNEL_PIVOT.z);
			ms.mulPose(Axis.XP.rotationDegrees(FlapStuffs.flapAngle(flapness, segment)));
			ms.translate(-FlapStuffs.FUNNEL_PIVOT.x, -FlapStuffs.FUNNEL_PIVOT.y, -FlapStuffs.FUNNEL_PIVOT.z);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), flap,
				BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
			ms.translate(FlapStuffs.SEGMENT_STEP, 0, 0);
		}
		ms.popPose();
	}

	private static void renderFilter(FunnelBlockEntity be, BlockState state, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		FilteringBehaviour filtering = be.getBehaviour(FilteringBehaviour.TYPE);
		if (filtering == null || !filtering.isActive())
			return;

		for (Direction side : Direction.values()) {
			FunnelFilterSlotPositioning slot = (FunnelFilterSlotPositioning) new FunnelFilterSlotPositioning()
				.fromSide(side);
			if (!slot.shouldRender(be.getLevel(), be.getBlockPos(), state))
				continue;

			ItemStack filter = filtering.getFilter(side);
			Vec3 offset = slot.getLocalOffset(be.getLevel(), be.getBlockPos(), state);
			if (offset == null)
				continue;
			Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
			boolean hasFilter = !filter.isEmpty();
			boolean active = shouldRenderFilterOverlay(be, state, slot, side);
			String amountLabel = getExtractingAmountLabel(filtering, filter, state);
			boolean markerVisible = filtering.isCountVisible() || isExtendedBeltFunnel(state);
			if (!hasFilter && active)
				renderFilterOverlay(ms, collector, offset, normal, slot, be, state, false, amountLabel, markerVisible);

			if (hasFilter) {
				ms.pushPose();
				applyFilterSlotTransform(ms, be, state, slot, side, offset, normal, 1 / 32d, true);
				if (isExtendedBeltFunnel(state))
					ms.mulPose(Axis.ZP.rotationDegrees(180));
				renderFilterItemStack(filter, ms, collector, light);
				ms.popPose();
				if (active)
					renderFilterOverlay(ms, collector, offset, normal, slot, be, state, true, amountLabel, markerVisible);
			}

		}
	}

	private static boolean isExtracting(FunnelBlockEntity be, BlockState state) {
		if (state.getBlock() instanceof BeltFunnelBlock) {
			BeltFunnelBlock.Shape shape = state.getValue(BeltFunnelBlock.SHAPE);
			if (shape == BeltFunnelBlock.Shape.PUSHING)
				return true;
			if (shape == BeltFunnelBlock.Shape.PULLING)
				return false;

			BlockState beltState = be.getLevel()
				.getBlockState(be.getBlockPos()
					.below());
			if (beltState.getBlock() instanceof BeltBlock && BeltBlock.canTransportObjects(beltState))
				return beltState.getValue(BeltBlock.HORIZONTAL_FACING) == state.getValue(BeltFunnelBlock.HORIZONTAL_FACING);
		}

		FunnelBlockEntity.Mode mode = be.determineCurrentMode();
		return mode == FunnelBlockEntity.Mode.EXTRACT || mode == FunnelBlockEntity.Mode.PUSHING_TO_BELT;
	}

	private static String getExtractingAmountLabel(FilteringBehaviour filtering, ItemStack filter, BlockState state) {
		if (!filtering.isCountVisible() && !isExtendedBeltFunnel(state))
			return null;
		if (!filtering.upTo)
			return String.valueOf(Math.max(1, filtering.getAmount()));
		int maxStackSize = isExtendedBeltFunnel(state) ? 64 : filtering.getMaxStackSize(filter);
		int amount = filtering.getAmount();
		return amount >= maxStackSize ? null : String.valueOf(Math.max(1, amount));
	}

	private static boolean isExtendedBeltFunnel(BlockState state) {
		return state.getBlock() instanceof BeltFunnelBlock
			&& state.getValue(BeltFunnelBlock.SHAPE) == BeltFunnelBlock.Shape.EXTENDED;
	}

	private static boolean shouldRenderFilterOverlay(FunnelBlockEntity be, BlockState state,
		FunnelFilterSlotPositioning slot, Direction side) {
		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit))
			return false;
		if (blockHit.getDirection() != side)
			return false;
		if (!isFilterHitBlock(be, state, blockHit))
			return false;

		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
		return slot.testHit(be.getLevel(), be.getBlockPos(), state, localHit);
	}

	private static boolean isFilterHitBlock(FunnelBlockEntity be, BlockState state, BlockHitResult blockHit) {
		if (blockHit.getBlockPos()
			.equals(be.getBlockPos()))
			return true;
		if (!(state.getBlock() instanceof BeltFunnelBlock) || state.getValue(BeltFunnelBlock.SHAPE) != BeltFunnelBlock.Shape.EXTENDED)
			return false;
		Direction facing = FunnelBlock.getFunnelFacing(state);
		return facing != null && blockHit.getBlockPos()
			.equals(be.getBlockPos()
				.relative(facing));
	}

	private static void renderFilterOverlay(PoseStack ms, SubmitNodeCollector collector, Vec3 offset, Vec3 normal,
		FunnelFilterSlotPositioning slot, FunnelBlockEntity be, BlockState state, boolean hasFilter,
		String amountLabel, boolean markerVisible) {
		ms.pushPose();
		applyFilterSlotTransform(ms, be, state, slot, slot.getSide(), offset, normal, 1 / 32d + 1 / 512d, false);
		if (isTiltedFunnel(state))
			ms.translate(0, -.2f / 16f, 0);
		float markerZSign = isExtendedBeltFunnel(state) ? -1 : 1;
		boolean mirrorText = isExtendedBeltFunnel(state);
		boolean reverseText = isExtendedBeltFunnel(state);
		float textRotation = isExtendedBeltFunnel(state) ? 180 : 0;
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			if (hasFilter)
				renderFilterCorners(pose, consumer);
			else
				renderFilterDots(pose, consumer);
			if (!markerVisible)
				return;
			if (amountLabel != null)
				renderAmountMark(pose, consumer, markCenterX(hasFilter), markCenterY(hasFilter, state), amountLabel, markerZSign,
					mirrorText, reverseText, textRotation);
			else
				renderFivePipMark(pose, consumer, markCenterX(hasFilter), markCenterY(hasFilter, state), markerZSign);
		});
		ms.popPose();
	}

	private static boolean isTiltedFunnel(BlockState state) {
		Direction facing = FunnelBlock.getFunnelFacing(state);
		return state.getBlock() instanceof FunnelBlock && facing != null && facing.getAxis()
			.isHorizontal();
	}

	private static void applyFilterSlotTransform(PoseStack ms, FunnelBlockEntity be, BlockState state,
		FunnelFilterSlotPositioning slot, Direction side, Vec3 offset, Vec3 normal, double normalOffset,
		boolean itemScale) {
		ms.translate(offset.x + normal.x * normalOffset, offset.y + normal.y * normalOffset,
			offset.z + normal.z * normalOffset);
		rotateFilterSlot(ms, state, side);
		if (itemScale)
			ms.scale(.5f, .5f, .5f);
	}

	private static void rotateFilterSlot(PoseStack ms, BlockState state, Direction side) {
		Direction facing = FunnelBlock.getFunnelFacing(state);
		if (facing == null) {
			rotateToFace(ms, side);
			return;
		}

		if (facing.getAxis()
			.isVertical()) {
			rotateToFace(ms, side);
			return;
		}

		boolean isBeltFunnel = state.getBlock() instanceof BeltFunnelBlock;
		if (isBeltFunnel && state.getValue(BeltFunnelBlock.SHAPE) != BeltFunnelBlock.Shape.EXTENDED) {
			rotateToFace(ms, side);
			BeltFunnelBlock.Shape shape = state.getValue(BeltFunnelBlock.SHAPE);
			if (shape == BeltFunnelBlock.Shape.PULLING || shape == BeltFunnelBlock.Shape.PUSHING)
				ms.mulPose(Axis.XP.rotationDegrees(22.5f));
			return;
		}

		if (state.getBlock() instanceof FunnelBlock) {
			rotateToFace(ms, side);
			ms.mulPose(Axis.XP.rotationDegrees(22.5f));
			return;
		}

		rotateToFace(ms, facing);
		ms.mulPose(Axis.XP.rotationDegrees(facing == Direction.DOWN ? -90 : 90));
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

	private static float markCenterX(boolean hasFilter) {
		return hasFilter ? 9.25f : 8;
	}

	private static float markCenterY(boolean hasFilter, BlockState state) {
		if (hasFilter && isExtendedBeltFunnel(state))
			return 9.25f;
		return hasFilter ? 6.75f : 8;
	}

	private static void renderFivePipMark(Pose pose, VertexConsumer consumer, float centerX, float centerY,
		float zSign) {
		float spacing = .24f;
		renderPip(pose, consumer, centerX - spacing, centerY - spacing, zSign);
		renderPip(pose, consumer, centerX + spacing, centerY - spacing, zSign);
		renderPip(pose, consumer, centerX, centerY, zSign);
		renderPip(pose, consumer, centerX - spacing, centerY + spacing, zSign);
		renderPip(pose, consumer, centerX + spacing, centerY + spacing, zSign);
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

	private static void flatSubPixelXY(Pose pose, VertexConsumer consumer, float x, float y, int color) {
		flatSubPixelXY(pose, consumer, x, y, .28f, 1 / 64f, color);
	}

	private static void renderPip(Pose pose, VertexConsumer consumer, float x, float y, float zSign) {
		flatSubPixelXY(pose, consumer, x, y, .46f, zSign / 128f, MARK_BACKGROUND);
		flatSubPixelXY(pose, consumer, x, y, .28f, zSign / 64f, MARK_FOREGROUND);
	}

	private static void renderAmountMark(Pose pose, VertexConsumer consumer, float centerX, float centerY, String text,
		float zSign, boolean mirrorX, boolean reverseText, float rotation) {
		float cell = .16f;
		float gap = cell;
		float totalWidth = text.length() * 3 * cell + Math.max(0, text.length() - 1) * gap;
		float startX = centerX - totalWidth / 2 + cell / 2;
		float startY = centerY - 5 * cell / 2 + cell / 2;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(reverseText ? text.length() - 1 - i : i);
			if (c < '0' || c > '9')
				continue;
			float digitStartX = startX + i * (3 * cell + gap);
			renderDigit(pose, consumer, c, digitStartX, startY, cell, zSign, mirrorX, digitStartX + cell, centerX,
				centerY, rotation);
		}
	}

	private static void renderDigit(Pose pose, VertexConsumer consumer, char digit, float startX, float startY,
		float cell, float zSign, boolean mirrorX, float mirrorCenterX, float rotationCenterX, float rotationCenterY,
		float rotation) {
		String[] rows = digitRows(digit);
		for (int row = 0; row < rows.length; row++) {
			String bits = rows[row];
			for (int column = 0; column < bits.length(); column++) {
				if (bits.charAt(column) != '1')
					continue;
				float x = startX + column * cell;
				float y = startY + (rows.length - 1 - row) * cell;
				if (mirrorX)
					x = mirrorCenterX - (x - mirrorCenterX);
				float drawX = rotateX(x, y, rotationCenterX, rotationCenterY, rotation);
				float drawY = rotateY(x, y, rotationCenterX, rotationCenterY, rotation);
				flatSubPixelXY(pose, consumer, drawX, drawY, cell * 1.45f, zSign / 128f, MARK_BACKGROUND);
				flatSubPixelXY(pose, consumer, drawX, drawY, cell, zSign / 64f, MARK_FOREGROUND);
			}
		}
	}

	private static float rotateX(float x, float y, float centerX, float centerY, float rotation) {
		if (rotation == 0)
			return x;
		double radians = Math.toRadians(rotation);
		float dx = x - centerX;
		float dy = y - centerY;
		return centerX + (float) (dx * Math.cos(radians) - dy * Math.sin(radians));
	}

	private static float rotateY(float x, float y, float centerX, float centerY, float rotation) {
		if (rotation == 0)
			return y;
		double radians = Math.toRadians(rotation);
		float dx = x - centerX;
		float dy = y - centerY;
		return centerY + (float) (dx * Math.sin(radians) + dy * Math.cos(radians));
	}

	private static String[] digitRows(char digit) {
		return switch (digit) {
			case '0' -> new String[] {"111", "101", "101", "101", "111"};
			case '1' -> new String[] {"010", "110", "010", "010", "111"};
			case '2' -> new String[] {"111", "001", "111", "100", "111"};
			case '3' -> new String[] {"111", "001", "111", "001", "111"};
			case '4' -> new String[] {"101", "101", "111", "001", "001"};
			case '5' -> new String[] {"111", "100", "111", "001", "111"};
			case '6' -> new String[] {"111", "100", "111", "101", "111"};
			case '7' -> new String[] {"111", "001", "001", "010", "010"};
			case '8' -> new String[] {"111", "101", "111", "101", "111"};
			case '9' -> new String[] {"111", "101", "111", "001", "111"};
			default -> new String[] {"000", "000", "000", "000", "000"};
		};
	}

	private static void flatSubPixelXY(Pose pose, VertexConsumer consumer, float x, float y, float sizeScale,
		float z, int color) {
		float pixel = 1 / 16f;
		float size = pixel * sizeScale;
		float centerX = x * pixel - .5f;
		float centerY = y * pixel - .5f;
		flatQuadXY(pose, consumer, centerX - size / 2, centerY - size / 2, centerX + size / 2,
			centerY + size / 2, z, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1,
		int color) {
		flatQuadXY(pose, consumer, x0, y0, x1, y1, 0, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1, float z,
		int color) {
		consumer.addVertex(pose, x0, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x0, y0, z).setColor(color);
	}

	private static void renderFilterItemStack(ItemStack filter, PoseStack ms, SubmitNodeCollector collector, int light) {
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .5f);
	}

	private List<BlockStateModelPart> getFlapModel(boolean beltFunnel) {
		if (beltFunnel) {
			if (beltFunnelFlapModel != null)
				return beltFunnelFlapModel;
			BlockStateModelPart flap = Minecraft.getInstance()
				.getModelManager()
				.getStandaloneModel(CreateStandaloneModels.BELT_FUNNEL_FLAP);
			return beltFunnelFlapModel = flap == null ? List.of() : List.of(flap);
		}

		if (funnelFlapModel != null)
			return funnelFlapModel;
		BlockStateModelPart flap = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.FUNNEL_FLAP);
		return funnelFlapModel = flap == null ? List.of() : List.of(flap);
	}

	private static class FunnelRenderState extends BlockEntityRenderState {
		FunnelBlockEntity blockEntity;
		float partialTicks;
	}
}
