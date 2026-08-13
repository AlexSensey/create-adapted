package com.simibubi.create.content.logistics.packagerLink;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.simibubi.create.Create;

import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class LogisticsNetworkSavedData extends SavedData {

	private Map<UUID, LogisticsNetwork> logisticsNetworks = new HashMap<>();

	private static final Identifier ID = Create.asResource("create_logistics");
	private static final Codec<LogisticsNetworkSavedData> CODEC = CompoundTag.CODEC.xmap(
		tag -> LogisticsNetworkSavedData.load(tag, null),
		data -> data.save(new CompoundTag(), null));
	private static final SavedDataType<LogisticsNetworkSavedData> TYPE =
		new SavedDataType<>(ID, LogisticsNetworkSavedData::new, CODEC, DataFixTypes.LEVEL);

	public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
		GlobalLogisticsManager logistics = Create.LOGISTICS;
		nbt.put("LogisticsNetworks",
			NBTHelper.writeCompoundList(logistics.logisticsNetworks.values(), network -> network.write(registries)));
		return nbt;
	}

	private static LogisticsNetworkSavedData load(CompoundTag nbt, HolderLookup.Provider registries) {
		LogisticsNetworkSavedData sd = new LogisticsNetworkSavedData();
		sd.logisticsNetworks = new HashMap<>();
		NBTHelper.iterateCompoundList(nbt.getListOrEmpty("LogisticsNetworks"), c -> {
			LogisticsNetwork network = LogisticsNetwork.read(c, registries);
			sd.logisticsNetworks.put(network.id, network);
		});
		return sd;
	}

	public Map<UUID, LogisticsNetwork> getLogisticsNetworks() {
		return logisticsNetworks;
	}

	private LogisticsNetworkSavedData() {}

	public static LogisticsNetworkSavedData load(MinecraftServer server) {
		return server.overworld()
			.getDataStorage()
			.computeIfAbsent(TYPE);
	}

}
