package com.simibubi.create.content.kinetics.deployer;

import static com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;
import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import com.simibubi.create.foundation.render.FlatGuiItemRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DeployerRenderer extends SafeBlockEntityRenderer<DeployerBlockEntity> {

	private BlockStateModelPart poleModel;
	private List<BlockStateModelPart> punchingHandModel;
	private List<BlockStateModelPart> holdingHandModel;
	private List<BlockStateModelPart> pointingHandModel;

	public DeployerRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(DeployerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new DeployerRenderState();
	}

	@Override
	public void extractRenderState(DeployerBlockEntity be, BlockEntityRenderState state, float partialTicks,
		net.minecraft.world.phys.Vec3 cameraPos,
		net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof DeployerRenderState deployerState) {
			deployerState.blockEntity = be;
			deployerState.blockState = be.getBlockState();
			deployerState.deployerState = be.state;
			deployerState.mode = be.mode;
			deployerState.heldItem = be.heldItem.copy();
			deployerState.timer = be.timer;
			deployerState.timerSpeed = be.getTimerSpeed();
			deployerState.reach = be.reach;
			deployerState.fistBump = be.fistBump;
			deployerState.handPose = be.getHandPose();
			deployerState.speed = be.getSpeed();
			deployerState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof DeployerRenderState deployerState))
			return;
		DeployerBlockEntity be = deployerState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		BlockState blockState = deployerState.blockState;
		if (!blockState.hasProperty(FACING))
			return;

		renderFilterItem(be, ms, collector, state.lightCoords);

		float partialTicks = deployerState.partialTicks;
		float handDistance = getHandDistance(deployerState, partialTicks);
		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			renderHeldItem(be, deployerState, handDistance, partialTicks, ms, collector, state.lightCoords);
			return;
		}
		Vec3 offset = getHandOffset(blockState, handDistance);
		BlockStateModelPart pole = getPoleModel();
		BlockStateModelPart hand = getHandModel(deployerState.handPose);
		if (hand == null && deployerState.handPose == AllPartialModels.DEPLOYER_HAND_HOLDING)
			hand = getHandModel(AllPartialModels.DEPLOYER_HAND_POINTING);

		if (pole != null) {
			ms.pushPose();
			ms.translate(offset.x, offset.y, offset.z);
			applyComponentTransform(ms, blockState, true);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(pole),
				BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		if (hand != null) {
			ms.pushPose();
			ms.translate(offset.x, offset.y, offset.z);
			applyComponentTransform(ms, blockState, false);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(hand),
				BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		// Held items are independent render content. A temporarily unavailable
		// standalone pole/hand model must not make the Deployer inventory invisible.
		renderHeldItem(be, deployerState, handDistance, partialTicks, ms, collector, state.lightCoords);
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer) {}

	public static void submitInContraption(MovementContext context, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		BlockState blockState = context.state;
		if (!blockState.hasProperty(FACING))
			return;

		String modeName = context.blockEntityData.getStringOr("Mode", DeployerBlockEntity.Mode.USE.name());
		DeployerBlockEntity.Mode mode;
		try {
			mode = DeployerBlockEntity.Mode.valueOf(modeName);
		} catch (IllegalArgumentException ignored) {
			mode = DeployerBlockEntity.Mode.USE;
		}

		BlockStateModelPart pole = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.DEPLOYER_POLE);
		BlockStateModelPart hand = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(mode == DeployerBlockEntity.Mode.PUNCH
				? CreateStandaloneModels.DEPLOYER_HAND_PUNCHING
				: CreateStandaloneModels.DEPLOYER_HAND_POINTING);
		if (pole == null || hand == null)
			return;

		double factor;
		if (context.contraption.stalled || context.position == null || context.data.contains("StationaryTimer")) {
			factor = Mth.sin(AnimationTickHolder.getRenderTime() * .5f) * .25f + .25f;
		} else {
			Vec3 center = Vec3.atCenterOf(BlockPos.containing(context.position));
			double distance = context.position.distanceTo(center);
			double nextDistance = context.position.add(context.motion)
				.distanceTo(center);
			factor = .5f - Mth.clamp(Mth.lerp(AnimationTickHolder.getPartialTicks(), distance, nextDistance), 0, 1);
		}

		if (context.disabled)
			factor = 0;
		Vec3 offset = getHandOffset(blockState, (float) factor);

		ms.pushPose();
		ms.translate(offset.x, offset.y, offset.z);
		applyComponentTransform(ms, blockState, true);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(pole),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();

		ms.pushPose();
		ms.translate(offset.x, offset.y, offset.z);
		applyComponentTransform(ms, blockState, false);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(hand),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	static PartialModel getHandPose(DeployerBlockEntity.Mode mode) {
		return mode == DeployerBlockEntity.Mode.PUNCH ? AllPartialModels.DEPLOYER_HAND_PUNCHING
			: AllPartialModels.DEPLOYER_HAND_POINTING;
	}

	private static net.neoforged.neoforge.client.model.standalone.StandaloneModelKey<BlockStateModelPart> handModel(
		PartialModel pose) {
		if (pose == AllPartialModels.DEPLOYER_HAND_PUNCHING)
			return CreateStandaloneModels.DEPLOYER_HAND_PUNCHING;
		if (pose == AllPartialModels.DEPLOYER_HAND_HOLDING)
			return CreateStandaloneModels.DEPLOYER_HAND_HOLDING;
		return CreateStandaloneModels.DEPLOYER_HAND_POINTING;
	}

	private BlockStateModelPart getHandModel(PartialModel handPose) {
		net.neoforged.neoforge.client.model.standalone.StandaloneModelKey<BlockStateModelPart> handKey = handModel(handPose);
		if (handKey == CreateStandaloneModels.DEPLOYER_HAND_PUNCHING)
			return first(punchingHandModel = loadHandModel(handKey, punchingHandModel));
		if (handKey == CreateStandaloneModels.DEPLOYER_HAND_HOLDING)
			return first(holdingHandModel = loadHandModel(handKey, holdingHandModel));
		return first(pointingHandModel = loadHandModel(handKey, pointingHandModel));
	}

	private BlockStateModelPart getPoleModel() {
		if (poleModel != null)
			return poleModel;
		return poleModel = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.DEPLOYER_POLE);
	}

	private static BlockStateModelPart first(List<BlockStateModelPart> parts) {
		return parts == null || parts.isEmpty() ? null : parts.getFirst();
	}

	private List<BlockStateModelPart> loadHandModel(
		net.neoforged.neoforge.client.model.standalone.StandaloneModelKey<BlockStateModelPart> handKey,
		List<BlockStateModelPart> cached) {
		if (cached != null)
			return cached;
		BlockStateModelPart hand = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(handKey);
		if (hand == null)
			return null;
		return List.of(hand);
	}

	private static Vec3 getHandOffset(BlockState blockState, float distance) {
		return Vec3.atLowerCornerOf(blockState.getValue(FACING)
			.getUnitVec3i())
			.scale(distance);
	}

	private static float getHandDistance(DeployerRenderState state, float partialTicks) {
		float progress = 0;
		if (state.deployerState == DeployerBlockEntity.State.EXPANDING) {
			progress = 1 - (state.timer - partialTicks * state.timerSpeed) / 1000f;
			if (state.fistBump)
				progress *= progress;
		}
		if (state.deployerState == DeployerBlockEntity.State.RETRACTING)
			progress = (state.timer - partialTicks * state.timerSpeed) / 1000f;

		float handLength = state.handPose == AllPartialModels.DEPLOYER_HAND_POINTING ? 0
			: state.handPose == AllPartialModels.DEPLOYER_HAND_HOLDING ? 4 / 16f : 3 / 16f;
		return Math.min(Mth.clamp(progress, 0, 1) * (state.reach + handLength), 21 / 16f);
	}

	private static void applyComponentTransform(PoseStack ms, BlockState deployerState, boolean axisDirectionMatters) {
		Direction facing = deployerState.getValue(FACING);
		float yRot = AngleHelper.horizontalAngle(facing);
		float xRot = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
		float zRot =
			axisDirectionMatters && (deployerState.getValue(AXIS_ALONG_FIRST_COORDINATE) ^ facing.getAxis() == Direction.Axis.Z)
				? 90
				: 0;

		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(yRot));
		ms.mulPose(Axis.XP.rotationDegrees(xRot));
		ms.mulPose(Axis.ZP.rotationDegrees(zRot));
		ms.translate(-.5, -.5, -.5);
	}

	private static void renderHeldItem(DeployerBlockEntity be, DeployerRenderState state, float handDistance,
		float partialTicks, PoseStack ms, SubmitNodeCollector collector, int light) {
		ItemStack heldItem = state.heldItem;
		if (heldItem.isEmpty())
			return;

		BlockState deployerState = state.blockState;
		Vec3 offset = getHandOffset(deployerState, handDistance)
			.add(.5, .5, .5);
		Direction facing = deployerState.getValue(FACING);
		boolean punching = state.mode == DeployerBlockEntity.Mode.PUNCH;
		boolean blockItem = heldItem.getItem() instanceof BlockItem;
		boolean displayMode = facing == Direction.UP && state.speed == 0 && !punching;
		ItemDisplayContext transform = ItemDisplayContext.FIXED;

		ms.pushPose();
		ms.translate(offset.x, offset.y, offset.z);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing) + 180));

		if (!displayMode) {
			ms.mulPose(Axis.XP.rotationDegrees(facing == Direction.UP ? 90 : facing == Direction.DOWN ? 270 : 0));
			ms.translate(0, 0, -11 / 16f);
		}

		if (punching)
			ms.translate(0, 1 / 8f, -1 / 16f);

		if (displayMode) {
			float scale = blockItem ? 1.25f : 1;
			ms.translate(0, blockItem ? 9 / 16f : 11 / 16f, 0);
			ms.scale(scale, scale, scale);
			ms.mulPose(Axis.YP.rotationDegrees(be.getLevel()
				.getGameTime() + partialTicks));
			transform = ItemDisplayContext.GROUND;
		} else {
			float scale = punching ? .75f : blockItem ? .75f - 1 / 64f : .5f;
			ms.scale(scale, scale, scale);
			transform = punching ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.FIXED;
		}

		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, heldItem, transform, null, null, 0);
		itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static void renderFilterItem(DeployerBlockEntity be, PoseStack ms, SubmitNodeCollector collector,
										 int light) {
		ItemStack filter = be.filtering.getFilter();
		FilteringBehaviour filtering = be.getBehaviour(FilteringBehaviour.TYPE);
		if (filtering == null)
			return;

		BlockState state = be.getBlockState();
		for (Direction side : Direction.values()) {
			DeployerFilterSlot slot = (DeployerFilterSlot) new DeployerFilterSlot().fromSide(side);
			if (!slot.shouldRender(be.getLevel(), be.getBlockPos(), state))
				continue;

			Vec3 offset = slot.getLocalOffset(be.getLevel(), be.getBlockPos(), state);
			Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
			boolean active = shouldRenderFilterOverlay(be.getBlockPos(), offset, side);
			if (active)
				renderFilterOverlay(ms, collector, offset, normal, slot, be, state, !filter.isEmpty());

			if (filter.isEmpty())
				continue;

			ms.pushPose();
			ms.translate(offset.x + normal.x / 32d, offset.y + normal.y / 32d, offset.z + normal.z / 32d);
			rotateFilterSlot(ms, state, side);
			ms.scale(.5f, .5f, .5f);
			renderFilterItemStack(filter, ms, collector, light);
			ms.popPose();
		}
	}

	private static boolean shouldRenderFilterOverlay(BlockPos pos, Vec3 offset, Direction side) {
		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit) || !blockHit.getBlockPos()
			.equals(pos))
			return false;
		if (blockHit.getDirection() != side)
			return false;

		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(pos));
		double halfSize = 3 / 16d;
		return switch (side.getAxis()) {
			case X -> Math.abs(localHit.y - offset.y) <= halfSize && Math.abs(localHit.z - offset.z) <= halfSize;
			case Y -> Math.abs(localHit.x - offset.x) <= halfSize && Math.abs(localHit.z - offset.z) <= halfSize;
			case Z -> Math.abs(localHit.x - offset.x) <= halfSize && Math.abs(localHit.y - offset.y) <= halfSize;
		};
	}

	private static void renderFilterOverlay(PoseStack ms, SubmitNodeCollector collector, Vec3 offset, Vec3 normal,
		DeployerFilterSlot slot, DeployerBlockEntity be, BlockState state, boolean hasFilter) {
		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d, offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateFilterSlot(ms, state, slot.getSide());
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			if (hasFilter)
				renderFilterCorners(pose, consumer);
			else
				renderFilterDots(pose, consumer);
		});
		ms.popPose();
	}

	private static void rotateFilterSlot(PoseStack ms, BlockState state, Direction side) {
		rotateToFace(ms, side);
		if (!side.getAxis()
			.isVertical())
			return;

		Direction facing = state.getValue(DeployerBlock.FACING);
		if (!facing.getAxis()
			.isHorizontal())
			return;

		float angle = switch (facing) {
			case NORTH -> 0;
			case SOUTH -> 180;
			case WEST -> 90;
			case EAST -> 270;
			default -> 0;
		};
		ms.mulPose(Axis.ZP.rotationDegrees(angle));
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

	private static void renderFilterItemStack(ItemStack filter, PoseStack ms, SubmitNodeCollector collector, int light) {
		renderFilterItemStackPass(filter, ms, collector, light);

		ms.pushPose();
		ms.mulPose(Axis.YP.rotationDegrees(180));
		ms.translate(0, 0, 1 / 128f);
		renderFilterItemStackPass(filter, ms, collector, light);
		ms.popPose();
	}

	private static void renderFilterItemStackPass(ItemStack filter, PoseStack ms, SubmitNodeCollector collector,
												  int light) {
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .5f);
	}

	private static class DeployerRenderState extends BlockEntityRenderState {
		private DeployerBlockEntity blockEntity;
		private BlockState blockState;
		private DeployerBlockEntity.State deployerState = DeployerBlockEntity.State.WAITING;
		private DeployerBlockEntity.Mode mode = DeployerBlockEntity.Mode.USE;
		private ItemStack heldItem = ItemStack.EMPTY;
		private int timer;
		private int timerSpeed;
		private float reach;
		private boolean fistBump;
		private PartialModel handPose = AllPartialModels.DEPLOYER_HAND_POINTING;
		private float speed;
		private float partialTicks;
	}
}
