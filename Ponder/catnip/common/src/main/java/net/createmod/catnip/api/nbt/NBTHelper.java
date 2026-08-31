package net.createmod.catnip.api.nbt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Unit;

// TODO - Everything here needs to be rethought with how codecs exist now and should be used everywhere they can
@Deprecated(forRemoval = true)
public class NBTHelper {
	public static void putMarker(CompoundTag nbt, String marker) {
		nbt.store(marker, Unit.CODEC, Unit.INSTANCE);
	}

	// Backwards compatible with 1.20
	public static BlockPos readBlockPos(CompoundTag nbt, String key) {
		Optional<BlockPos> pos = nbt.read(key, BlockPos.CODEC);
		if (pos.isPresent())
			return pos.get();
		CompoundTag oldTag = nbt.getCompoundOrEmpty(key);
		return new BlockPos(
			oldTag.getIntOr("X", 0),
			oldTag.getIntOr("Y", 0),
			oldTag.getIntOr("Z", 0)
		);
	}

	public static <T> ListTag writeCompoundList(Iterable<T> list, Function<T, CompoundTag> serializer) {
		ListTag listNBT = new ListTag();
		list.forEach(t -> {
			CompoundTag apply = serializer.apply(t);
			if (apply == null)
				return;
			listNBT.add(apply);
		});
		return listNBT;
	}

	public static <T> List<T> readCompoundList(ListTag listNBT, Function<CompoundTag, T> deserializer) {
		List<T> list = new ArrayList<>(listNBT.size());
		listNBT.forEach(inbt -> list.add(deserializer.apply((CompoundTag) inbt)));
		return list;
	}

	public static void iterateCompoundList(ListTag listNBT, Consumer<CompoundTag> consumer) {
		listNBT.forEach(inbt -> consumer.accept((CompoundTag) inbt));
	}

	public static Tag getINBT(CompoundTag nbt, String id) {
		Tag inbt = nbt.get(id);
		if (inbt != null)
			return inbt;
		return new CompoundTag();
	}

	public static CompoundTag intToCompound(int i) {
		CompoundTag compoundTag = new CompoundTag();
		compoundTag.putInt("V", i);
		return compoundTag;
	}

	public static int intFromCompound(CompoundTag compoundTag) {
		return compoundTag.getIntOr("V", 0);
	}
}
