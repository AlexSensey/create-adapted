package com.simibubi.create.content.logistics.packagerLink;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.Create;

import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class LogisticsNetwork {

	public UUID id;
	public RequestPromiseQueue panelPromises;

	public Set<GlobalPos> totalLinks;
	public Set<GlobalPos> loadedLinks;

	public UUID owner;
	public boolean locked;

	public LogisticsNetwork(UUID networkId) {
		id = networkId;
		panelPromises = new RequestPromiseQueue(Create.LOGISTICS::markDirty);
		totalLinks = new HashSet<>();
		loadedLinks = new HashSet<>();
		owner = null;
		locked = false;
	}

	public CompoundTag write(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Id", id.toString());
		tag.put("Promises", panelPromises.write(registries));

		tag.put("Links", NBTHelper.writeCompoundList(totalLinks, p -> {
			CompoundTag nbt = new CompoundTag();
			nbt.put("Pos", writeBlockPos(p.pos()));
			return nbt;
		}));

		if (owner != null)
			tag.putString("Owner", owner.toString());

		tag.putBoolean("Locked", locked);
		return tag;
	}

	public static LogisticsNetwork read(CompoundTag tag, HolderLookup.Provider registries) {
		LogisticsNetwork network = new LogisticsNetwork(readUuid(tag, "Id", UUID.randomUUID()));
		network.panelPromises = RequestPromiseQueue.read(tag.getCompoundOrEmpty("Promises"), registries, Create.LOGISTICS::markDirty);

		NBTHelper.iterateCompoundList(tag.getListOrEmpty("Links"), nbt -> {
			network.totalLinks.add(GlobalPos.of(nbt.contains("Dim")
				? ResourceKey.create(Registries.DIMENSION, Identifier.parse(nbt.getStringOr("Dim", "minecraft:overworld")))
				: Level.OVERWORLD, readBlockPos(nbt.getCompoundOrEmpty("Pos"))));
		});

		network.owner = tag.contains("Owner") ? readUuid(tag, "Owner", null) : null;
		network.locked = tag.getBooleanOr("Locked", false);

		return network;
	}

	private static UUID readUuid(CompoundTag tag, String key, UUID fallback) {
		try {
			return UUID.fromString(tag.getStringOr(key, ""));
		} catch (IllegalArgumentException e) {
			return fallback;
		}
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

}
