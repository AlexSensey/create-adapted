package com.simibubi.create.content.trains.track;

import java.util.List;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.mutable.MutableObject;

import com.simibubi.create.Create;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphHelper;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;

import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


public class TrackTargetingBlockItem extends BlockItem {

	private final EdgePointType<?> type;

	public static <T extends Block> NonNullBiFunction<? super T, Item.Properties, TrackTargetingBlockItem> ofType(
		EdgePointType<?> type) {
		return (b, p) -> new TrackTargetingBlockItem(b, p, type);
	}

	public TrackTargetingBlockItem(Block block, Properties properties, EdgePointType<?> type) {
		super(block, properties);
		this.type = type;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		BlockState state = level.getBlockState(pos);
		Player player = context.getPlayer();

		if (player == null)
			return InteractionResult.FAIL;

		if (player.isShiftKeyDown() && stack.has(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS)) {
			if (level.isClientSide())
				return InteractionResult.SUCCESS;
			com.simibubi.create.content.trains.TrainMessages.actionBar(player, CreateLang.translateDirect("track_target.clear"));
			stack.remove(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS);
			stack.remove(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_DIRECTION);
			stack.remove(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER);
			AllSoundEvents.CONTROLLER_CLICK.play(level, null, pos, 1, .5f);
			return InteractionResult.SUCCESS;
		}

		if (state.getBlock() instanceof ITrackBlock track) {
			if (level.isClientSide())
				return InteractionResult.SUCCESS;

			Vec3 lookAngle = player.getLookAngle();
			boolean front = track.getNearestTrackAxis(level, pos, state, lookAngle)
				.getSecond() == AxisDirection.POSITIVE;
			EdgePointType<?> type = getType(stack);

			MutableObject<OverlapResult> result = new MutableObject<>(null);
			withGraphLocation(level, pos, front, null, type, (overlap, location) -> result.setValue(overlap));
			if (result.getValue() == OverlapResult.NO_TRACK) {
				TrackPropagator.onRailAdded(level, pos, state);
				withGraphLocation(level, pos, front, null, type, (overlap, location) -> result.setValue(overlap));
			}
			if (result.getValue().feedback != null) {
				com.simibubi.create.content.trains.TrainMessages.actionBar(player, CreateLang.translateDirect(result.getValue().feedback)
					.withStyle(ChatFormatting.RED));
				AllSoundEvents.DENY.play(level, null, pos, .5f, 1);
				return InteractionResult.FAIL;
			}

			stack.set(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS, pos);
			stack.set(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_DIRECTION, front);
			stack.remove(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER);
			com.simibubi.create.content.trains.TrainMessages.actionBar(player, CreateLang.translateDirect("track_target.set"));
			AllSoundEvents.CONTROLLER_CLICK.play(level, null, pos, 1, 1);
			return InteractionResult.SUCCESS;
		}

		if (!stack.has(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS)) {
			com.simibubi.create.content.trains.TrainMessages.actionBar(player, CreateLang.translateDirect("track_target.missing")
				.withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		CompoundTag blockEntityData = new CompoundTag();

		BlockPos selectedPos = stack.get(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS);
		BlockPos placedPos = pos.relative(context.getClickedFace(), state.canBeReplaced() ? 0 : 1);

		boolean selectedDirection = stack.getOrDefault(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_DIRECTION, false);
		boolean bezier = stack.has(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER);
		BezierTrackPointLocation selectedBezier =
			bezier ? stack.get(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER) : null;
		blockEntityData.putBoolean("TargetDirection", selectedDirection);

		if (!selectedPos.closerThan(placedPos,
			bezier ? AllConfigs.server().trains.maxTrackPlacementLength.get() + 16 : 16)) {
			com.simibubi.create.content.trains.TrainMessages.actionBar(player, CreateLang.translateDirect("track_target.too_far")
				.withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		if (!level.isClientSide()) {
			MutableObject<OverlapResult> validation = new MutableObject<>(null);
			MutableObject<TrackGraphLocation> validatedLocation = new MutableObject<>(null);
			withGraphLocation(level, selectedPos, selectedDirection, selectedBezier, getType(stack), (overlap, location) -> {
				validation.setValue(overlap);
				validatedLocation.setValue(location);
			});
			if (validation.getValue() == null || validation.getValue().feedback != null) {
				String feedback = validation.getValue() == null ? "track_target.invalid" : validation.getValue().feedback;
				com.simibubi.create.content.trains.TrainMessages.actionBar(player, CreateLang.translateDirect(feedback)
					.withStyle(ChatFormatting.RED));
				AllSoundEvents.DENY.play(level, null, pos, .5f, 1);
				return InteractionResult.FAIL;
			}
		}

		if (bezier) {
			CompoundTag bezierNbt = new CompoundTag();
			bezierNbt.putInt("Segment", selectedBezier.segment());
			bezierNbt.put("Key", writeBlockPos(selectedBezier.curveTarget()
				.subtract(placedPos)));
			blockEntityData.put("Bezier", bezierNbt);
		}

		blockEntityData.put("TargetTrack", writeBlockPos(selectedPos.subtract(placedPos)));

		stack.set(DataComponents.BLOCK_ENTITY_DATA,
			TypedEntityData.of(((IBE<?>) getBlock()).getBlockEntityType(), blockEntityData));

		InteractionResult useOn = super.useOn(context);
		stack.remove(DataComponents.BLOCK_ENTITY_DATA);
		if (level.isClientSide() || useOn == InteractionResult.FAIL)
			return useOn;

		stack.remove(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS);
		stack.remove(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_DIRECTION);
		stack.remove(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER);

		ItemStack itemInHand = player.getItemInHand(context.getHand());
		if (!itemInHand.isEmpty()) {
			itemInHand.remove(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS);
			itemInHand.remove(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_DIRECTION);
			itemInHand.remove(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER);
		}
		com.simibubi.create.content.trains.TrainMessages.actionBar(player, CreateLang.translateDirect("track_target.success")
			.withStyle(ChatFormatting.GREEN));

		if (type == EdgePointType.SIGNAL)
			AllAdvancements.SIGNAL.awardTo(player);

		return useOn;
	}

	public EdgePointType<?> getType(ItemStack stack) {
		return type;
	}

	public boolean useOnCurve(BezierPointSelection selection, ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		TrackBlockEntity be = selection.blockEntity();
		BezierTrackPointLocation loc = selection.loc();
		boolean front = player.getLookAngle()
			.dot(selection.direction()) < 0;

		ClientNetworkHelper.INSTANCE.sendToServer(new CurvedTrackSelectionPacket(be.getBlockPos(), loc.curveTarget(),
			front, loc.segment(), player.getInventory().getSelectedSlot()));
		return true;
	}

	public static enum OverlapResult {
		VALID,
		OCCUPIED("track_target.occupied"),
		JUNCTION("track_target.no_junctions"),
		NO_TRACK("track_target.invalid");

		public String feedback;

		private OverlapResult() {
		}

		private OverlapResult(String feedback) {
			this.feedback = feedback;
		}
	}

	public static void withGraphLocation(Level level, BlockPos pos, boolean front,
		BezierTrackPointLocation targetBezier, EdgePointType<?> type,
		BiConsumer<OverlapResult, TrackGraphLocation> callback) {
		BlockState state = level.getBlockState(pos);

		if (!(state.getBlock() instanceof ITrackBlock track)) {
			callback.accept(OverlapResult.NO_TRACK, null);
			return;
		}

		List<Vec3> trackAxes = track.getTrackAxes(level, pos, state);
		if (targetBezier == null && trackAxes.size() > 1) {
			callback.accept(OverlapResult.JUNCTION, null);
			return;
		}

		AxisDirection targetDirection = front ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;
		TrackGraphLocation location =
			targetBezier != null ? TrackGraphHelper.getBezierGraphLocationAt(level, pos, targetDirection, targetBezier)
				: TrackGraphHelper.getGraphLocationAt(level, pos, targetDirection, trackAxes.get(0));

		if (location == null) {
			callback.accept(OverlapResult.NO_TRACK, null);
			return;
		}

		Couple<TrackNode> nodes = location.edge.map(location.graph::locateNode);
		TrackEdge edge = location.graph.getConnection(nodes);
		if (edge == null) {
			callback.accept(OverlapResult.NO_TRACK, null);
			return;
		}

		EdgeData edgeData = edge.getEdgeData();
		double edgePosition = location.position;

		for (TrackEdgePoint edgePoint : List.copyOf(edgeData.getPoints())) {
			if (isOrphanedSingleBlockPoint(level, edgePoint)) {
				removeOrphanedPoint(location, edgePoint);
				continue;
			}

			double otherEdgePosition = edgePoint.getLocationOn(edge);
			double distance = Math.abs(edgePosition - otherEdgePosition);
			if (distance > .75)
				continue;
			if (edgePoint.canCoexistWith(type, front) && distance < .25)
				continue;

			callback.accept(OverlapResult.OCCUPIED, location);
			return;
		}

		callback.accept(OverlapResult.VALID, location);
	}

	private static boolean isOrphanedSingleBlockPoint(Level level, TrackEdgePoint edgePoint) {
		if (!(edgePoint instanceof SingleBlockEntityEdgePoint singleBlockPoint))
			return false;
		BlockPos blockEntityPos = singleBlockPoint.getBlockEntityPos();
		if (blockEntityPos == null)
			return false;
		if (!level.dimension().equals(singleBlockPoint.getBlockEntityDimension()))
			return false;
		if (!level.isLoaded(blockEntityPos))
			return false;
		if (!level.getBlockState(blockEntityPos).hasBlockEntity())
			return true;
		BlockEntity blockEntity = level.getBlockEntity(blockEntityPos);
		if (blockEntity == null || blockEntity.isRemoved())
			return true;
		TrackTargetingBehaviour<?> behaviour =
			BlockEntityBehaviour.get(level, blockEntityPos, TrackTargetingBehaviour.TYPE);
		return behaviour == null || !behaviour.getId()
			.equals(edgePoint.getId());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void removeOrphanedPoint(TrackGraphLocation location, TrackEdgePoint edgePoint) {
		location.graph.removePoint((EdgePointType) edgePoint.getType(), edgePoint.getId());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void removeEdgePointsAt(Level level, BlockPos blockEntityPos, EdgePointType<?> type) {
		if (level.isClientSide())
			return;
		for (TrackGraph graph : Create.RAILWAYS.sided(level).trackNetworks.values())
			for (Object object : List.copyOf(graph.getPoints((EdgePointType) type))) {
				TrackEdgePoint edgePoint = (TrackEdgePoint) object;
				if (!(edgePoint instanceof SingleBlockEntityEdgePoint singleBlockPoint))
					continue;
				if (!level.dimension().equals(singleBlockPoint.getBlockEntityDimension()))
					continue;
				if (!blockEntityPos.equals(singleBlockPoint.getBlockEntityPos()))
					continue;
				graph.removePoint((EdgePointType) edgePoint.getType(), edgePoint.getId());
			}
	}

	private static CompoundTag writeBlockPos(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
		return tag;
	}
}
