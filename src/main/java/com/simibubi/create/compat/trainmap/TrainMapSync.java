package com.simibubi.create.compat.trainmap;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.common.cache.Cache;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.signal.SignalBlock.SignalType;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.utility.TickBasedCache;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.api.data.codec.stream.CatnipLargerStreamCodecs;
import net.createmod.catnip.api.data.codec.stream.CatnipStreamCodecBuilders;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.phys.Vec3;
import net.createmod.catnip.api.data.Pair;
import net.createmod.catnip.platform.CatnipServices;

public class TrainMapSync {

	public static final int lightPacketInterval = 5;
	public static final int fullPacketInterval = 10;
	private static int ticks;

	public enum TrainState {
		RUNNING, RUNNING_MANUALLY, DERAILED, SCHEDULE_INTERRUPTED, CONDUCTOR_MISSING, NAVIGATION_FAILED;

		public static final StreamCodec<ByteBuf, TrainState> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(TrainState.class);
	}

	public enum SignalState {
		NOT_WAITING, WAITING_FOR_REDSTONE, BLOCK_SIGNAL, CHAIN_SIGNAL;

		public static final StreamCodec<ByteBuf, SignalState> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(SignalState.class);
	}

	public static class TrainMapSyncEntry {
		public static final StreamCodec<FriendlyByteBuf, TrainMapSyncEntry> STREAM_CODEC = CatnipLargerStreamCodecs.composite(
			CatnipStreamCodecBuilders.array(ByteBufCodecs.FLOAT, Float.class), packet -> packet.positions,
			CatnipStreamCodecBuilders.list(CatnipStreamCodecBuilders.nullable(ResourceKey.streamCodec(Registries.DIMENSION))), packet -> packet.dimensions,
			TrainState.STREAM_CODEC, packet -> packet.state,
			SignalState.STREAM_CODEC, packet -> packet.signalState,
			ByteBufCodecs.BOOL, packet -> packet.fueled,
			ByteBufCodecs.BOOL, packet -> packet.backwards,
			ByteBufCodecs.VAR_INT, packet -> packet.targetStationDistance,
			ByteBufCodecs.STRING_UTF8, packet -> packet.ownerName,
			ByteBufCodecs.STRING_UTF8, packet -> packet.targetStationName,
			CatnipStreamCodecBuilders.nullable(UUIDUtil.STREAM_CODEC), packet -> packet.waitingForTrain,
			TrainMapSyncEntry::new
		);

		public Float[] positions;
		public List<ResourceKey<Level>> dimensions;
		public TrainState state = TrainState.RUNNING;
		public SignalState signalState = SignalState.NOT_WAITING;
		public boolean fueled;
		public boolean backwards;
		public int targetStationDistance;
		public String ownerName = "";
		public String targetStationName = "";
		public UUID waitingForTrain;
		public Float[] prevPositions;
		public List<ResourceKey<Level>> prevDims;

		public TrainMapSyncEntry() {}

		public TrainMapSyncEntry(Float[] positions, List<ResourceKey<Level>> dimensions, TrainState state,
								SignalState signalState, boolean fueled, boolean backwards, int targetStationDistance,
								String ownerName, String targetStationName, UUID waitingForTrain) {
			this.positions = positions;
			this.dimensions = dimensions;
			this.state = state;
			this.signalState = signalState;
			this.fueled = fueled;
			this.backwards = backwards;
			this.targetStationDistance = targetStationDistance;
			this.ownerName = ownerName;
			this.targetStationName = targetStationName;
			this.waitingForTrain = waitingForTrain;
		}

		public void updateFrom(TrainMapSyncEntry other, boolean light) {
			prevPositions = positions;
			prevDims = dimensions;
			positions = other.positions;
			dimensions = other.dimensions;
			state = other.state;
			signalState = other.signalState;
			fueled = other.fueled;
			backwards = other.backwards;
			targetStationDistance = other.targetStationDistance;
			if (prevDims != null && prevPositions != null)
				for (int i = 0; i < Math.min(prevDims.size(), dimensions.size()); i++)
					if (prevDims.get(i) != dimensions.get(i))
						System.arraycopy(positions, i * 6, prevPositions, i * 6, 6);
			if (!light) {
				ownerName = other.ownerName;
				targetStationName = other.targetStationName;
				waitingForTrain = other.waitingForTrain;
			}
		}

		public Vec3 getPosition(int carriageIndex, boolean firstBogey, double time) {
			int start = carriageIndex * 6 + (firstBogey ? 0 : 3);
			if (positions == null || positions.length <= start + 2)
				return Vec3.ZERO;
			Vec3 current = new Vec3(positions[start], positions[start + 1], positions[start + 2]);
			if (prevPositions == null || prevPositions.length <= start + 2)
				return current;
			return new Vec3(prevPositions[start], prevPositions[start + 1], prevPositions[start + 2])
				.lerp(current, time);
		}
	}

	public static Cache<UUID, WeakReference<ServerPlayer>> requestingPlayers = new TickBasedCache<>(20, false);

	public static void requestReceived(ServerPlayer sender) {
		boolean sendImmediately = requestingPlayers.getIfPresent(sender.getUUID()) == null;
		requestingPlayers.put(sender.getUUID(), new WeakReference<>(sender));
		if (sendImmediately)
			send(sender.level().getServer(), false);
	}

	public static void serverTick(ServerTickEvent event) {
		ticks++;
		if (ticks % fullPacketInterval == 0)
			send(event.getServer(), false);
		else if (ticks % lightPacketInterval == 0)
			send(event.getServer(), true);
	}

	private static void send(MinecraftServer server, boolean light) {
		if (requestingPlayers.size() == 0)
			return;
		TrainMapSyncPacket packet = new TrainMapSyncPacket(light);
		for (Train train : Create.RAILWAYS.trains.values())
			packet.add(train.id, createEntry(server, train));
		for (WeakReference<ServerPlayer> reference : requestingPlayers.asMap().values()) {
			ServerPlayer player = reference.get();
			if (player != null)
				CatnipServices.NETWORK.sendToClient(player, packet);
		}
	}

	private static TrainMapSyncEntry createEntry(MinecraftServer server, Train train) {
		TrainMapSyncEntry entry = new TrainMapSyncEntry();
		boolean stopped = Math.abs(train.speed) < .05;
		entry.positions = new Float[train.carriages.size() * 6];
		entry.dimensions = new ArrayList<>();
		Arrays.fill(entry.positions, 0f);

		for (int i = 0; i < train.carriages.size(); i++) {
			Carriage carriage = train.carriages.get(i);
			Vec3 leadingPos;
			Vec3 trailingPos;
			if (train.graph == null) {
				Pair<ResourceKey<Level>, DimensionalCarriageEntity> dimensional = carriage.anyAvailableDimensionalCarriage();
				if (dimensional == null || carriage.presentInMultipleDimensions()) {
					entry.dimensions.add(null);
					continue;
				}
				leadingPos = dimensional.getSecond().rotationAnchors.getFirst();
				trailingPos = dimensional.getSecond().rotationAnchors.getSecond();
				if (leadingPos == null || trailingPos == null) {
					entry.dimensions.add(null);
					continue;
				}
				entry.dimensions.add(dimensional.getFirst());
			} else {
				TravellingPoint leading = carriage.getLeadingPoint();
				TravellingPoint trailing = carriage.getTrailingPoint();
				if (leading == null || trailing == null || leading.edge == null || trailing.edge == null) {
					entry.dimensions.add(null);
					continue;
				}
				ResourceKey<Level> leadingDim = leading.node1 == null || leading.edge.isInterDimensional() ? null
					: leading.node1.getLocation().getDimension();
				ResourceKey<Level> trailingDim = trailing.node1 == null || trailing.edge.isInterDimensional() ? null
					: trailing.node1.getLocation().getDimension();
				entry.dimensions.add(leadingDim == null || leadingDim != trailingDim ? null : leadingDim);
				leadingPos = leading.getPosition(train.graph);
				trailingPos = trailing.getPosition(train.graph);
			}
			entry.positions[i * 6] = (float) leadingPos.x();
			entry.positions[i * 6 + 1] = (float) leadingPos.y();
			entry.positions[i * 6 + 2] = (float) leadingPos.z();
			entry.positions[i * 6 + 3] = (float) trailingPos.x();
			entry.positions[i * 6 + 4] = (float) trailingPos.y();
			entry.positions[i * 6 + 5] = (float) trailingPos.z();
		}

		entry.backwards = train.currentlyBackwards;
		if (train.owner != null) {
			ServerPlayer owner = server.getPlayerList().getPlayer(train.owner);
			if (owner != null)
				entry.ownerName = owner.getName().getString();
		}
		if (train.derailed) {
			entry.state = TrainState.DERAILED;
			return entry;
		}

		ScheduleRuntime runtime = train.runtime;
		if (runtime.getSchedule() != null && stopped) {
			if (runtime.paused) {
				entry.state = TrainState.SCHEDULE_INTERRUPTED;
				return entry;
			}
			if (train.status.conductor) {
				entry.state = TrainState.CONDUCTOR_MISSING;
				return entry;
			}
			if (train.status.navigation) {
				entry.state = TrainState.NAVIGATION_FAILED;
				return entry;
			}
		}
		if ((runtime.getSchedule() == null || runtime.paused) && train.speed != 0)
			entry.state = TrainState.RUNNING_MANUALLY;

		GlobalStation station = train.getCurrentStation();
		if (station != null) {
			entry.targetStationName = station.name;
		} else if (train.navigation.destination != null && !runtime.paused) {
			entry.targetStationName = train.navigation.destination.name;
			entry.targetStationDistance = Math.max(0, Mth.floor(train.navigation.distanceToDestination));
		}

		if (stopped && train.navigation.waitingForSignal != null) {
			UUID signalId = train.navigation.waitingForSignal.getFirst();
			boolean side = train.navigation.waitingForSignal.getSecond();
			SignalBoundary signal = train.graph.getPoint(EdgePointType.SIGNAL, signalId);
			if (signal != null) {
				entry.signalState = signal.types.get(side) == SignalType.CROSS_SIGNAL ? SignalState.CHAIN_SIGNAL
					: SignalState.BLOCK_SIGNAL;
				if (signal.isForcedRed(side))
					entry.signalState = SignalState.WAITING_FOR_REDSTONE;
				else {
					SignalEdgeGroup group = Create.RAILWAYS.signalEdgeGroups.get(signal.groups.get(side));
					if (group != null)
						for (Train other : group.trains)
							if (other != train) {
								entry.waitingForTrain = other.id;
								break;
							}
				}
			}
		}
		entry.fueled = train.fuelTicks > 0 && !stopped;
		return entry;
	}

}
