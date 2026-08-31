package com.simibubi.create.content.trains.track;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.EdgeData;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphHelper;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


public class TrackTargetingBehaviour<T extends TrackEdgePoint> extends BlockEntityBehaviour {

	public static final BehaviourType<TrackTargetingBehaviour<?>> TYPE = new BehaviourType<>();

	private BlockPos targetTrack;
	private BezierTrackPointLocation targetBezier;
	private AxisDirection targetDirection;
	private UUID id;

	private Vec3 prevDirection;
	private Vec3 rotatedDirection;

	private CompoundTag migrationData;
	private EdgePointType<T> edgePointType;
	private T edgePoint;
	private boolean orthogonal;

	public TrackTargetingBehaviour(SmartBlockEntity be, EdgePointType<T> edgePointType) {
		super(be);
		this.edgePointType = edgePointType;
		targetDirection = AxisDirection.POSITIVE;
		targetTrack = BlockPos.ZERO;
		id = UUID.randomUUID();
		migrationData = null;
		orthogonal = false;
	}

	@Override
	public boolean isSafeNBT() {
		return true;
	}

	@Override
	public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		nbt.putString("Id", id.toString());
		nbt.put("TargetTrack", writeBlockPos(targetTrack));
		nbt.putBoolean("Ortho", orthogonal);
		nbt.putBoolean("TargetDirection", targetDirection == AxisDirection.POSITIVE);
		if (rotatedDirection != null)
			nbt.put("RotatedAxis", VecHelper.writeNBT(rotatedDirection));
		if (prevDirection != null)
			nbt.put("PrevAxis", VecHelper.writeNBT(prevDirection));
		if (migrationData != null && !clientPacket)
			nbt.put("Migrate", migrationData);
		if (targetBezier != null) {
			CompoundTag bezierNbt = new CompoundTag();
			bezierNbt.putInt("Segment", targetBezier.segment());
			bezierNbt.put("Key", writeBlockPos(targetBezier.curveTarget()
				.subtract(getPos())));
			nbt.put("Bezier", bezierNbt);
		}
		super.write(nbt, registries, clientPacket);
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		id = nbt.contains("Id") ? readUuid(nbt, "Id") : UUID.randomUUID();
		targetTrack = readBlockPos(nbt.getCompoundOrEmpty("TargetTrack"));
		targetDirection = nbt.getBooleanOr("TargetDirection", true) ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;
		orthogonal = nbt.getBooleanOr("Ortho", false);
		if (nbt.contains("PrevAxis"))
			prevDirection = VecHelper.readNBT(nbt.getListOrEmpty("PrevAxis"));
		if (nbt.contains("RotatedAxis"))
			rotatedDirection = VecHelper.readNBT(nbt.getListOrEmpty("RotatedAxis"));
		if (nbt.contains("Migrate"))
			migrationData = nbt.getCompoundOrEmpty("Migrate");
		if (clientPacket)
			edgePoint = null;
		if (nbt.contains("Bezier")) {
			CompoundTag bezierNbt = nbt.getCompoundOrEmpty("Bezier");
			BlockPos key = readBlockPos(bezierNbt.getCompoundOrEmpty("Key"));
			targetBezier = new BezierTrackPointLocation(key.offset(getPos()),
				bezierNbt.getIntOr("Segment", 0));
		} else
			targetBezier = null;
		super.read(nbt, registries, clientPacket);
	}

	@Nullable
	public T getEdgePoint() {
		return edgePoint;
	}

	public UUID getId() {
		return id;
	}

	public void invalidateEdgePoint(CompoundTag migrationData) {
		this.migrationData = migrationData;
		edgePoint = null;
		blockEntity.sendData();
	}

	@Override
	public void tick() {
		super.tick();
		if (edgePoint == null)
			edgePoint = createEdgePoint();
	}

	@SuppressWarnings("unchecked")
	public T createEdgePoint() {
		Level level = getWorld();
		boolean isClientSide = level.isClientSide();
		if (migrationData == null || isClientSide)
			for (TrackGraph trackGraph : Create.RAILWAYS.sided(level).trackNetworks.values()) {
				T point = trackGraph.getPoint(edgePointType, id);
				if (point == null)
					continue;
				return point;
			}

		if (isClientSide)
			return null;
		if (!hasValidTrack())
			return null;
		TrackGraphLocation loc = determineGraphLocation();
		if (loc == null)
			return null;

		TrackGraph graph = loc.graph;
		TrackNode node1 = graph.locateNode(loc.edge.getFirst());
		TrackNode node2 = graph.locateNode(loc.edge.getSecond());
		TrackEdge edge = graph.getConnectionsFrom(node1)
			.get(node2);
		if (edge == null)
			return null;

		T point = edgePointType.create();
		boolean front = getTargetDirection() == AxisDirection.POSITIVE;

		prevDirection = edge.getDirectionAt(loc.position)
			.scale(front ? -1 : 1);

		if (rotatedDirection != null) {
			double dot = prevDirection.dot(rotatedDirection);
			if (dot < -.85f) {
				rotatedDirection = null;
				targetDirection = targetDirection.opposite();
				return null;
			}

			rotatedDirection = null;
		}

		double length = edge.getLength();
		CompoundTag data = migrationData;
		migrationData = null;

		{
			orthogonal = targetBezier == null;
			Vec3 direction = edge.getDirection(true);
			int nonZeroComponents = 0;
			for (Axis axis : Iterate.axes)
				nonZeroComponents += direction.get(axis) != 0 ? 1 : 0;
			orthogonal &= nonZeroComponents <= 1;
		}

		EdgeData signalData = edge.getEdgeData();
		if (signalData.hasPoints()) {
			for (EdgePointType<?> otherType : EdgePointType.TYPES.values()) {
				TrackEdgePoint otherPoint = signalData.get(otherType, loc.position);
				if (otherPoint == null)
					continue;
				if (otherType != edgePointType) {
					if (!otherPoint.canCoexistWith(edgePointType, front))
						return null;
					continue;
				}
				if (!otherPoint.canMerge())
					return null;
				otherPoint.blockEntityAdded(blockEntity, front);
				id = otherPoint.getId();
				blockEntity.notifyUpdate();
				return (T) otherPoint;
			}
		}

		if (data != null)
			point.read(data, level.registryAccess(), true, DimensionPalette.read(data));

		point.setId(id);
		boolean reverseEdge = front || point instanceof SingleBlockEntityEdgePoint;
		point.setLocation(reverseEdge ? loc.edge : loc.edge.swap(), reverseEdge ? loc.position : length - loc.position);
		point.blockEntityAdded(blockEntity, front);
		loc.graph.addPoint(edgePointType, point);
		blockEntity.sendData();
		return point;
	}

	@Override
	public void destroy() {
		super.destroy();
		if (getWorld().isClientSide())
			return;

		T point = edgePoint;
		if (point == null)
			for (TrackGraph trackGraph : Create.RAILWAYS.sided(getWorld()).trackNetworks.values()) {
				point = trackGraph.getPoint(edgePointType, id);
				if (point != null)
					break;
			}

		if (point != null)
			point.blockEntityRemoved(getPos(), getTargetDirection() == AxisDirection.POSITIVE);
		edgePoint = null;
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	public boolean isOnCurve() {
		return targetBezier != null;
	}

	public boolean isOrthogonal() {
		return orthogonal;
	}

	public boolean hasValidTrack() {
		return getTrackBlockState().getBlock() instanceof ITrackBlock;
	}

	public ITrackBlock getTrack() {
		return (ITrackBlock) getTrackBlockState().getBlock();
	}

	public BlockState getTrackBlockState() {
		return getWorld().getBlockState(getGlobalPosition());
	}

	public BlockPos getGlobalPosition() {
		return targetTrack.offset(blockEntity.getBlockPos());
	}

	public BlockPos getPositionForMapMarker() {
		BlockPos target = targetTrack.offset(blockEntity.getBlockPos());
		if (targetBezier != null && getWorld().getBlockEntity(target) instanceof TrackBlockEntity tbe) {
			BezierConnection bc = tbe.getConnections()
				.get(targetBezier.curveTarget());
			if (bc == null)
				return target;
			double length = Mth.floor(bc.getLength() * 2);
			int seg = targetBezier.segment() + 1;
			double t = seg / length;
			return BlockPos.containing(bc.getPosition(t));
		}
		return target;
	}

	public AxisDirection getTargetDirection() {
		return targetDirection;
	}

	public BezierTrackPointLocation getTargetBezier() {
		return targetBezier;
	}

	public TrackGraphLocation determineGraphLocation() {
		Level level = getWorld();
		BlockPos pos = getGlobalPosition();
		BlockState state = getTrackBlockState();
		ITrackBlock track = getTrack();
		List<Vec3> trackAxes = track.getTrackAxes(level, pos, state);
		AxisDirection targetDirection = getTargetDirection();

		return targetBezier != null
			? TrackGraphHelper.getBezierGraphLocationAt(level, pos, targetDirection, targetBezier)
			: TrackGraphHelper.getGraphLocationAt(level, pos, targetDirection, trackAxes.get(0));
	}

	public static enum RenderedTrackOverlayType {
		STATION, SIGNAL, DUAL_SIGNAL, OBSERVER;
	}

	private static CompoundTag writeBlockPos(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
		return tag;
	}

	private static BlockPos readBlockPos(CompoundTag tag) {
		return new BlockPos(tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0));
	}

	private static UUID readUuid(CompoundTag tag, String key) {
		try {
			return UUID.fromString(tag.getStringOr(key, ""));
		} catch (IllegalArgumentException e) {
			return UUID.randomUUID();
		}
	}

	public void transform(BlockEntity be, StructureTransform transform) {
		id = UUID.randomUUID();
		targetTrack = transform.applyWithoutOffset(targetTrack);
		if (prevDirection != null)
			rotatedDirection = transform.applyWithoutOffsetUncentered(prevDirection);
		if (targetBezier != null)
			targetBezier = new BezierTrackPointLocation(transform.applyWithoutOffset(targetBezier.curveTarget()
					.subtract(getPos()))
				.offset(getPos()), targetBezier.segment());
		blockEntity.notifyUpdate();
	}

}
