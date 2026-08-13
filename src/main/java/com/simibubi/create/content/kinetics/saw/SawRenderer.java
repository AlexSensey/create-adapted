package com.simibubi.create.content.kinetics.saw;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import com.simibubi.create.foundation.render.FlatGuiItemRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.StitchedSprite;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SawRenderer extends KineticBlockEntityRenderer<SawBlockEntity> {
	private static final StitchedSprite STONECUTTER_SAW =
		new StitchedSprite(Identifier.withDefaultNamespace("block/stonecutter_saw"));

	private List<BlockStateModelPart> horizontalActiveModel;
	private List<BlockStateModelPart> horizontalInactiveModel;
	private List<BlockStateModelPart> horizontalReversedModel;
	private List<BlockStateModelPart> verticalActiveModel;
	private List<BlockStateModelPart> verticalInactiveModel;
	private List<BlockStateModelPart> verticalReversedModel;
	private List<BlockStateModelPart> shaftHalfModel;
	private final Map<Direction.Axis, List<BlockStateModelPart>> shaftModels = new EnumMap<>(Direction.Axis.class);

	public SawRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(SawBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {
		FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof SawBlockEntity be))
			return;
		if (isInvalid(be))
			return;

		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(SawBlock.FACING);
		boolean vertical = facing.getAxis()
			.isVertical();
		if (!VisualizationManager.supportsVisualization(be.getLevel()))
			renderShaft(be, vertical, kineticState.partialTicks, state, ms, collector);

		List<BlockStateModelPart> blade = getBladeModel(vertical, be.getSpeed());
		if (!blade.isEmpty()) {
			ms.pushPose();
			ms.translate(.5, .5, .5);
			rotateToState(ms, blockState);
			ms.translate(-.5, -.5, -.5);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), blade, BlockModelRenderState.EMPTY_TINTS,
				state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		renderItems(be, kineticState.partialTicks, ms, collector, state.lightCoords);
		renderFilter(be, ms, collector, state.lightCoords);
	}

	private void renderShaft(SawBlockEntity be, boolean vertical, float partialTicks, BlockEntityRenderState state,
		PoseStack ms, SubmitNodeCollector collector) {
		if (vertical) {
			List<BlockStateModelPart> shaft = getShaftModel(getRotationAxisOf(be));
			if (shaft.isEmpty())
				return;

			ms.pushPose();
			transformRotatingModel(be, ms, partialTicks);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), shaft, BlockModelRenderState.EMPTY_TINTS,
				state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
			return;
		}

		List<BlockStateModelPart> shaftHalf = getShaftHalfModel();
		if (shaftHalf.isEmpty())
			return;

		Direction shaftDirection = be.getBlockState()
			.getValue(SawBlock.FACING)
			.getOpposite();
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(shaftDirection.getAxis(), getAngleForBe(be, be.getBlockPos(), shaftDirection.getAxis(),
			partialTicks)));
		rotateHalfShaftTo(ms, shaftDirection);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), shaftHalf, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private List<BlockStateModelPart> getShaftModel(Direction.Axis axis) {
		List<BlockStateModelPart> cached = shaftModels.get(axis);
		if (cached != null)
			return cached;

		BlockState renderedState = shaft(axis);
		BlockStateModel model = Minecraft.getInstance()
			.getModelManager()
			.getBlockStateModelSet()
			.get(renderedState);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(0), parts);
		List<BlockStateModelPart> result = List.copyOf(parts);
		shaftModels.put(axis, result);
		return result;
	}

	private List<BlockStateModelPart> getShaftHalfModel() {
		if (shaftHalfModel != null)
			return shaftHalfModel;
		BlockStateModelPart shaftHalf = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SHAFT_HALF);
		return shaftHalfModel = shaftHalf == null ? List.of() : List.of(shaftHalf);
	}

	private List<BlockStateModelPart> getBladeModel(boolean vertical, float speed) {
		if (vertical) {
			if (speed == 0)
				return getVerticalInactiveModel();
			return speed > 0 ? getVerticalActiveModel() : getVerticalReversedModel();
		}
		if (speed == 0)
			return getHorizontalInactiveModel();
		return speed > 0 ? getHorizontalActiveModel() : getHorizontalReversedModel();
	}

	private List<BlockStateModelPart> getHorizontalActiveModel() {
		if (horizontalActiveModel != null)
			return horizontalActiveModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SAW_BLADE_HORIZONTAL_ACTIVE);
		return horizontalActiveModel = model == null ? List.of() : List.of(model);
	}

	private List<BlockStateModelPart> getHorizontalInactiveModel() {
		if (horizontalInactiveModel != null)
			return horizontalInactiveModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SAW_BLADE_HORIZONTAL_INACTIVE);
		return horizontalInactiveModel = model == null ? List.of() : List.of(model);
	}

	private List<BlockStateModelPart> getHorizontalReversedModel() {
		if (horizontalReversedModel != null)
			return horizontalReversedModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SAW_BLADE_HORIZONTAL_REVERSED);
		return horizontalReversedModel = model == null ? List.of() : List.of(model);
	}

	private List<BlockStateModelPart> getVerticalActiveModel() {
		if (verticalActiveModel != null)
			return verticalActiveModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SAW_BLADE_VERTICAL_ACTIVE);
		return verticalActiveModel = model == null ? List.of() : List.of(model);
	}

	private List<BlockStateModelPart> getVerticalInactiveModel() {
		if (verticalInactiveModel != null)
			return verticalInactiveModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SAW_BLADE_VERTICAL_INACTIVE);
		return verticalInactiveModel = model == null ? List.of() : List.of(model);
	}

	private List<BlockStateModelPart> getVerticalReversedModel() {
		if (verticalReversedModel != null)
			return verticalReversedModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SAW_BLADE_VERTICAL_REVERSED);
		return verticalReversedModel = model == null ? List.of() : List.of(model);
	}

	private static void rotateToState(PoseStack ms, BlockState state) {
		Direction facing = state.getValue(SawBlock.FACING);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.mulPose(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(facing)));

		if (!SawBlock.isHorizontal(state) && state.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE))
			ms.mulPose(Axis.ZP.rotationDegrees(90));
	}

	private static void rotateHalfShaftTo(PoseStack ms, Direction direction) {
		switch (direction) {
			case NORTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
			case EAST -> ms.mulPose(Axis.YP.rotationDegrees(90));
			case WEST -> ms.mulPose(Axis.YP.rotationDegrees(-90));
			case UP -> ms.mulPose(Axis.XP.rotationDegrees(-90));
			case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(90));
			case SOUTH -> {
			}
		}
	}

	private static void renderItems(SawBlockEntity be, float partialTicks, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		if (be.getBlockState()
			.getValue(SawBlock.FACING) != Direction.UP)
			return;
		if (be.inventory.isEmpty())
			return;

		boolean alongZ = !be.getBlockState()
			.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE);

		float duration = be.inventory.recipeDuration;
		boolean moving = duration != 0;
		float offset = moving ? be.inventory.remainingTime / duration : 0;
		float processingSpeed = Mth.clamp(Math.abs(be.getSpeed()) / 32, 1, 128);
		if (moving) {
			offset = Mth.clamp(offset + ((-partialTicks + .5f) * processingSpeed) / duration, .125f, 1f);
			if (!be.inventory.appliedRecipe)
				offset += 1;
			offset /= 2;
		}

		if (be.getSpeed() == 0)
			offset = .5f;
		if (be.getSpeed() < 0 ^ alongZ)
			offset = 1 - offset;

		int outputs = 0;
		for (int i = 1; i < be.inventory.getSlots(); i++)
			if (!be.inventory.getStackInSlot(i)
				.isEmpty())
				outputs++;

		ms.pushPose();
		if (alongZ)
			ms.mulPose(Axis.YP.rotationDegrees(90));
		ms.translate(outputs <= 1 ? .5 : .25, 0, offset);
		ms.translate(alongZ ? -1 : 0, 0, 0);

		int renderedI = 0;
		for (int i = 0; i < be.inventory.getSlots(); i++) {
			ItemStack stack = be.inventory.getStackInSlot(i);
			if (stack.isEmpty())
				continue;

			ms.pushPose();
			ms.translate(0, 13 / 16f, 0);

			if (i > 0 && outputs > 1) {
				ms.translate((.5 / (outputs - 1)) * renderedI, 0, 0);
				ms.translate(0, 1 / 128f * i, 0);
			}

			boolean box = PackageItem.isPackage(stack);
			if (box) {
				ms.translate(0, 4 / 16f, 0);
				ms.scale(1.5f, 1.5f, 1.5f);
			} else {
				ms.scale(.5f, .5f, .5f);
				ms.mulPose(Axis.XP.rotationDegrees(90));
			}

			ItemStackRenderState itemState = new ItemStackRenderState();
			Minecraft.getInstance()
				.getItemModelResolver()
				.updateForTopItem(itemState, stack, ItemDisplayContext.FIXED, null, null, 0);
			itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
			renderedI++;

			ms.popPose();
		}

		ms.popPose();
	}

	private static void renderFilter(SawBlockEntity be, PoseStack ms, SubmitNodeCollector collector, int light) {
		FilteringBehaviour filtering = be.getBehaviour(FilteringBehaviour.TYPE);
		if (filtering == null)
			return;

		BlockState state = be.getBlockState();
		SawFilterSlot slot = new SawFilterSlot();
		if (!slot.shouldRender(be.getLevel(), be.getBlockPos(), state))
			return;

		Vec3 offset = slot.getLocalOffset(be.getLevel(), be.getBlockPos(), state);
		if (offset == null)
			return;

		ItemStack filter = filtering.getFilter();
		boolean active = shouldRenderFilterOverlay(be.getBlockPos(), offset);
		if (active)
			renderFilterOverlay(ms, collector, offset, slot, be, state, !filter.isEmpty());

		if (filter.isEmpty())
			return;

		ms.pushPose();
		ms.translate(offset.x, offset.y, offset.z);
		slot.rotate(be.getLevel(), be.getBlockPos(), state, ms);
		renderFilterItemStack(filter, ms, collector, light);
		ms.popPose();
	}

	private static void renderFilterItemStack(ItemStack filter, PoseStack ms, SubmitNodeCollector collector, int light) {
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .5f);
	}

	private static boolean shouldRenderFilterOverlay(BlockPos pos, Vec3 offset) {
		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit) || !blockHit.getBlockPos()
			.equals(pos))
			return false;

		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(pos));
		double halfSize = 3 / 16d;
		return Math.abs(localHit.x - offset.x) <= halfSize && Math.abs(localHit.z - offset.z) <= halfSize;
	}

	private static void renderFilterOverlay(PoseStack ms, SubmitNodeCollector collector, Vec3 offset, SawFilterSlot slot,
		SawBlockEntity be, BlockState state, boolean hasFilter) {
		ms.pushPose();
		ms.translate(offset.x, offset.y + 1 / 32f + 1 / 512f, offset.z);
		slot.rotate(be.getLevel(), be.getBlockPos(), state, ms);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> {
				if (hasFilter)
					renderFilterCornersXY(pose, consumer);
				else
					renderFilterDotsXY(pose, consumer);
			});
		ms.popPose();
	}

	private static void renderFilterCorners(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		int offsetFromEdge = 3;

		flatPixelXZ(pose, consumer, 5, 8 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 6, 8 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 9, 8 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 10, 8 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 5, 9 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 10, 9 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 5, 12 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 10, 12 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 5, 13 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 6, 13 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 9, 13 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 10, 13 - offsetFromEdge, color);
	}

	private static void renderFilterDots(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		int offsetFromEdge = 4;

		flatPixelXZ(pose, consumer, 6, 10 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 9, 10 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 6, 13 - offsetFromEdge, color);
		flatPixelXZ(pose, consumer, 9, 13 - offsetFromEdge, color);
	}

	private static void renderFilterCornersXY(Pose pose, VertexConsumer consumer) {
		renderThreePixelCornerXY(pose, consumer, 5, 5, 1, 1);
		renderThreePixelCornerXY(pose, consumer, 10, 5, -1, 1);
		renderThreePixelCornerXY(pose, consumer, 5, 10, 1, -1);
		renderThreePixelCornerXY(pose, consumer, 10, 10, -1, -1);
	}

	private static void renderFilterDotsXY(Pose pose, VertexConsumer consumer) {
		flatPixelXY(pose, consumer, 6, 6);
		flatPixelXY(pose, consumer, 9, 6);
		flatPixelXY(pose, consumer, 6, 9);
		flatPixelXY(pose, consumer, 9, 9);
	}

	private static void renderThreePixelCornerXY(Pose pose, VertexConsumer consumer, int x, int y,
		int xStep, int yStep) {
		flatPixelXY(pose, consumer, x, y);
		flatPixelXY(pose, consumer, x + xStep, y);
		flatPixelXY(pose, consumer, x, y + yStep);
	}

	private static void flatPixelXY(Pose pose, VertexConsumer consumer, int x, int y) {
		float pixel = 1 / 16f;
		float x0 = x * pixel - .5f;
		float y0 = y * pixel - .5f;
		float x1 = (x + 1) * pixel - .5f;
		float y1 = (y + 1) * pixel - .5f;
		consumer.addVertex(pose, x0, y0, 0).setColor(0xFFFFFFFF);
		consumer.addVertex(pose, x1, y0, 0).setColor(0xFFFFFFFF);
		consumer.addVertex(pose, x1, y1, 0).setColor(0xFFFFFFFF);
		consumer.addVertex(pose, x0, y1, 0).setColor(0xFFFFFFFF);
		consumer.addVertex(pose, x0, y1, 0).setColor(0xFFFFFFFF);
		consumer.addVertex(pose, x1, y1, 0).setColor(0xFFFFFFFF);
		consumer.addVertex(pose, x1, y0, 0).setColor(0xFFFFFFFF);
		consumer.addVertex(pose, x0, y0, 0).setColor(0xFFFFFFFF);
	}

	private static void renderThreePixelCornerByInsets(Pose pose, VertexConsumer consumer, int xInset, int zInset,
		boolean fromRight, boolean fromBack, int color) {
		int x = pixelFromInsets(xInset, fromRight);
		int z = pixelFromInsets(zInset, fromBack);
		int xStep = fromRight ? -1 : 1;
		int zStep = fromBack ? -1 : 1;
		flatPixelXZ(pose, consumer, x, z, color);
		flatPixelXZ(pose, consumer, x + xStep, z, color);
		flatPixelXZ(pose, consumer, x, z + zStep, color);
	}

	private static void flatPixelByInsetsXZ(Pose pose, VertexConsumer consumer, int xInset, int zInset, int color) {
		flatPixelXZ(pose, consumer, pixelFromInsets(xInset, false), pixelFromInsets(zInset, false), color);
	}

	private static int pixelFromInsets(int inset, boolean fromFarEdge) {
		return fromFarEdge ? 16 - inset - 1 : inset;
	}

	private static void flatPixelXZ(Pose pose, VertexConsumer consumer, int x, int z, int color) {
		float pixel = 1 / 16f;
		flatQuadXZ(pose, consumer, x * pixel - .5f, z * pixel - .5f, (x + 1) * pixel - .5f,
			(z + 1) * pixel - .5f, color);
	}

	private static void flatQuadXZ(Pose pose, VertexConsumer consumer, float x0, float z0, float x1, float z1,
		int color) {
		consumer.addVertex(pose, x0, 0, z0).setColor(color);
		consumer.addVertex(pose, x1, 0, z0).setColor(color);
		consumer.addVertex(pose, x1, 0, z1).setColor(color);
		consumer.addVertex(pose, x0, 0, z1).setColor(color);
		consumer.addVertex(pose, x0, 0, z1).setColor(color);
		consumer.addVertex(pose, x1, 0, z1).setColor(color);
		consumer.addVertex(pose, x1, 0, z0).setColor(color);
		consumer.addVertex(pose, x0, 0, z0).setColor(color);
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		SuperByteBuffer block = CachedBuffers.block(context.state);
		if (!block.isEmpty())
			block.light(LightCoordsUtil.getLightCoords(renderWorld, context.localPos))
				.renderInto(matrices.getModel(), buffer.getBuffer(RenderTypes.cutoutMovingBlock()));

		renderContraptionBlade(context, matrices, buffer, LightCoordsUtil.getLightCoords(renderWorld, context.localPos));
	}

	public static void submitInContraption(MovementContext context, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		BlockState state = context.state;
		boolean vertical = !SawBlock.isHorizontal(state);
		Direction facing = state.getValue(SawBlock.FACING);
		Vec3 facingVec = Vec3.atLowerCornerOf(facing.getUnitVec3i());
		facingVec = context.rotation.apply(facingVec);
		Direction closestToFacing = Direction.getApproximateNearest(facingVec.x, facingVec.y, facingVec.z);
		boolean horizontal = closestToFacing.getAxis().isHorizontal();
		boolean backwards = VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite());
		boolean moving = context.getAnimationSpeed() != 0;
		boolean shouldAnimate = context.contraption.stalled && horizontal
			|| !context.contraption.stalled && !backwards && moving;
		BlockStateModelPart model = getContraptionBladeModel(vertical, shouldAnimate);
		if (model == null)
			return;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		rotateToState(ms, state);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(model), BlockModelRenderState.EMPTY_TINTS,
			light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static BlockStateModelPart getContraptionBladeModel(boolean vertical, boolean active) {
		var models = Minecraft.getInstance()
			.getModelManager();
		if (vertical) {
			return models.getStandaloneModel(active ? CreateStandaloneModels.SAW_BLADE_VERTICAL_ACTIVE
				: CreateStandaloneModels.SAW_BLADE_VERTICAL_INACTIVE);
		}
		return models.getStandaloneModel(active ? CreateStandaloneModels.SAW_BLADE_HORIZONTAL_ACTIVE
			: CreateStandaloneModels.SAW_BLADE_HORIZONTAL_INACTIVE);
	}

	private static void renderContraptionBlade(MovementContext context, ContraptionMatrices matrices,
		MultiBufferSource buffer, int light) {
		TextureAtlasSprite sprite = STONECUTTER_SAW.get();
		if (sprite == null)
			return;

		BlockState state = context.state;
		boolean horizontal = SawBlock.isHorizontal(state);

		PoseStack ms = matrices.getModel();
		ms.pushPose();
		ms.translate(.5, .5, .5);
		rotateToState(ms, state);
		ms.translate(-.5, -.5, -.5);

		VertexConsumer consumer = buffer.getBuffer(RenderTypes.cutoutMovingBlock());
		if (horizontal)
			renderBladeQuad(consumer, ms.last(), 1 / 16f, 8 / 16f, 11 / 16f, 15 / 16f, 8.062f / 16f, 18 / 16f,
				sprite, light);
		else
			renderBladeQuad(consumer, ms.last(), 0, 8 / 16f, 11 / 16f, 1, 8.001f / 16f, 19 / 16f, sprite, light);
		ms.popPose();
	}

	private static void renderBladeQuad(VertexConsumer consumer, PoseStack.Pose pose, float minX, float minY,
		float minZ, float maxX, float maxY, float maxZ, TextureAtlasSprite sprite, int light) {
		float u0 = sprite.getU(0);
		float u1 = sprite.getU(16);
		float v0 = sprite.getV(8);
		float v1 = sprite.getV(16);

		putBladeVertex(consumer, pose, minX, maxY, maxZ, u0, v1, Direction.UP, light);
		putBladeVertex(consumer, pose, maxX, maxY, maxZ, u1, v1, Direction.UP, light);
		putBladeVertex(consumer, pose, maxX, maxY, minZ, u1, v0, Direction.UP, light);
		putBladeVertex(consumer, pose, minX, maxY, minZ, u0, v0, Direction.UP, light);

		putBladeVertex(consumer, pose, minX, minY, minZ, u1, v0, Direction.DOWN, light);
		putBladeVertex(consumer, pose, maxX, minY, minZ, u0, v0, Direction.DOWN, light);
		putBladeVertex(consumer, pose, maxX, minY, maxZ, u0, v1, Direction.DOWN, light);
		putBladeVertex(consumer, pose, minX, minY, maxZ, u1, v1, Direction.DOWN, light);
	}

	private static void putBladeVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u,
		float v, Direction normal, int light) {
		Vec3i normalVec = normal.getUnitVec3i();
		consumer.addVertex(pose.pose(), x, y, z)
			.setColor(255, 255, 255, 255)
			.setUv(u, v)
			.setLight(light)
			.setNormal(pose.copy(), normalVec.getX(), normalVec.getY(), normalVec.getZ());
	}
}
