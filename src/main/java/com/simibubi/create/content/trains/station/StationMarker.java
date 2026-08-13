package com.simibubi.create.content.trains.station;

import java.util.Objects;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllMapDecorationTypes;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

public class StationMarker {
	public static final Codec<StationMarker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		BlockPos.CODEC.fieldOf("source").forGetter(StationMarker::getSource),
		BlockPos.CODEC.fieldOf("target").forGetter(StationMarker::getTarget),
		Codec.STRING.optionalFieldOf("name", "").forGetter(marker -> marker.getName().getString())
	).apply(instance, (source, target, name) -> new StationMarker(source, target, Component.literal(name))));

	private final BlockPos source;
	private final BlockPos target;
	private final Component name;
	private final String id;

	public StationMarker(BlockPos source, BlockPos target, Component name) {
		this.source = source;
		this.target = target;
		this.name = name;
		id = "create:station-" + target.getX() + "," + target.getY() + "," + target.getZ();
	}

	public static StationMarker load(CompoundTag tag, HolderLookup.Provider registries) {
		BlockPos source = NBTHelper.readBlockPos(tag, "source");
		BlockPos target = NBTHelper.readBlockPos(tag, "target");
		Component name = tag.contains("name") ? Component.literal(tag.getStringOr("name", "")) : CommonComponents.EMPTY;

		return new StationMarker(source, target, name);
	}

	public static StationMarker fromWorld(BlockGetter level, BlockPos pos) {
		Optional<StationBlockEntity> stationOption = AllBlockEntityTypes.TRACK_STATION.get(level, pos);

		if (stationOption.isEmpty() || stationOption.get().getStation() == null)
			return null;

		String name = stationOption.get()
			.getStation().name;
		return new StationMarker(pos, BlockEntityBehaviour.get(stationOption.get(), TrackTargetingBehaviour.TYPE)
			.getPositionForMapMarker(), Component.literal(name));
	}

	public CompoundTag save(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.put("source", writeBlockPos(source));
		tag.put("target", writeBlockPos(target));
		tag.putString("name", name.getString());

		return tag;
	}

	private static CompoundTag writeBlockPos(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
		return tag;
	}

	public BlockPos getSource() {
		return source;
	}

	public BlockPos getTarget() {
		return target;
	}

	public Component getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StationMarker that = (StationMarker) o;

		if (!target.equals(that.target)) return false;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(target, name);
	}

	public static MapDecoration createStationDecoration(byte x, byte y, Optional<Component> name) {
		return new MapDecoration(AllMapDecorationTypes.STATION_MAP_DECORATION, x, y, (byte) 0, name);
	}
}
