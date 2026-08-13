package com.simibubi.create.content.trains;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;

import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class RailwaySavedData extends SavedData {

	private Map<UUID, TrackGraph> trackNetworks = new HashMap<>();
	private Map<UUID, SignalEdgeGroup> signalEdgeGroups = new HashMap<>();
	private Map<UUID, Train> trains = new HashMap<>();

	private static final Identifier ID = Create.asResource("create_tracks");
	private static final Codec<RailwaySavedData> CODEC = Codec.of(RailwaySavedData::encode, RailwaySavedData::decode);
	private static final SavedDataType<RailwaySavedData> TYPE =
		new SavedDataType<>(ID, RailwaySavedData::new, CODEC, DataFixTypes.LEVEL);

	private static <T> DataResult<T> encode(RailwaySavedData data, DynamicOps<T> ops, T prefix) {
		CompoundTag tag = data.save(new CompoundTag(), registryOps(ops));
		return CompoundTag.CODEC.encode(tag, ops, prefix);
	}

	private static <T> DataResult<Pair<RailwaySavedData, T>> decode(DynamicOps<T> ops, T input) {
		return CompoundTag.CODEC.decode(ops, input)
			.map(pair -> pair.mapFirst(tag -> RailwaySavedData.load(tag, registryOps(ops))));
	}

	private static RegistryOps<Tag> registryOps(DynamicOps<?> ops) {
		if (ops instanceof RegistryOps<?> registryOps)
			return registryOps.withParent(NbtOps.INSTANCE);
		Create.LOGGER.warn("RailwaySavedData codec is running without RegistryOps; mounted train cargo cannot be serialized with registry-aware codecs.");
		return null;
	}

	public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
		return save(nbt, registries == null ? null : registries.createSerializationContext(NbtOps.INSTANCE));
	}

	public CompoundTag save(CompoundTag nbt, RegistryOps<Tag> registryOps) {
		GlobalRailwayManager railways = Create.RAILWAYS;
//		Create.LOGGER.info("Saving Railway Information...");
		DimensionPalette dimensions = new DimensionPalette();
		nbt.put("RailGraphs", NBTHelper.writeCompoundList(railways.trackNetworks.values(), tg -> tg.write(null, dimensions)));
		nbt.put("SignalBlocks", NBTHelper.writeCompoundList(railways.signalEdgeGroups.values(), seg -> {
			if (seg.fallbackGroup && !railways.trackNetworks.containsKey(seg.id))
				return null;
			return seg.write();
		}));
		nbt.put("Trains", NBTHelper.writeCompoundList(railways.trains.values(), t -> t.write(dimensions, registryOps)));
		dimensions.write(nbt);
		return nbt;
	}

	private static RailwaySavedData load(CompoundTag nbt, HolderLookup.Provider registries) {
		return load(nbt, registries == null ? null : registries.createSerializationContext(NbtOps.INSTANCE));
	}

	private static RailwaySavedData load(CompoundTag nbt, RegistryOps<Tag> registryOps) {
		RailwaySavedData sd = new RailwaySavedData();
		sd.trackNetworks = new HashMap<>();
		sd.signalEdgeGroups = new HashMap<>();
		sd.trains = new HashMap<>();
//		Create.LOGGER.info("Loading Railway Information...");

		DimensionPalette dimensions = DimensionPalette.read(nbt);
		NBTHelper.iterateCompoundList(nbt.getListOrEmpty("RailGraphs"), c -> {
			TrackGraph graph = TrackGraph.read(c, null, dimensions);
			sd.trackNetworks.put(graph.id, graph);
		});
		NBTHelper.iterateCompoundList(nbt.getListOrEmpty("SignalBlocks"), c -> {
			SignalEdgeGroup group = SignalEdgeGroup.read(c);
			sd.signalEdgeGroups.put(group.id, group);
		});
		NBTHelper.iterateCompoundList(nbt.getListOrEmpty("Trains"), c -> {
			Train train = Train.read(c, registryOps, sd.trackNetworks, dimensions);
			sd.trains.put(train.id, train);
		});

		for (TrackGraph graph : sd.trackNetworks.values()) {
			for (SignalBoundary signal : graph.getPoints(EdgePointType.SIGNAL)) {
				UUID groupId = signal.groups.getFirst();
				UUID otherGroupId = signal.groups.getSecond();
				if (groupId == null || otherGroupId == null)
					continue;
				SignalEdgeGroup group = sd.signalEdgeGroups.get(groupId);
				SignalEdgeGroup otherGroup = sd.signalEdgeGroups.get(otherGroupId);
				if (group == null || otherGroup == null)
					continue;
				group.putAdjacent(otherGroupId);
				otherGroup.putAdjacent(groupId);
			}
		}

		return sd;
	}

	public Map<UUID, TrackGraph> getTrackNetworks() {
		return trackNetworks;
	}

	public Map<UUID, Train> getTrains() {
		return trains;
	}

	public Map<UUID, SignalEdgeGroup> getSignalBlocks() {
		return signalEdgeGroups;
	}

	private RailwaySavedData() {}

	public static RailwaySavedData load(MinecraftServer server) {
		return server.overworld()
			.getDataStorage()
			.computeIfAbsent(TYPE);
	}

}
