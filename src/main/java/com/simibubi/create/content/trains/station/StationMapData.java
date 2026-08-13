package com.simibubi.create.content.trains.station;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public interface StationMapData {

	boolean toggleStation(LevelAccessor level, BlockPos pos, StationBlockEntity stationBlockEntity);

	void addStationMarker(StationMarker marker);

	Collection<StationMarker> getStationMarkers();

}
