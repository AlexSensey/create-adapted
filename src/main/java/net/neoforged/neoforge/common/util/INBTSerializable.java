package net.neoforged.neoforge.common.util;

import net.minecraft.core.HolderLookup;

public interface INBTSerializable<T> {
	T serializeNBT(HolderLookup.Provider provider);

	void deserializeNBT(HolderLookup.Provider provider, T nbt);
}
