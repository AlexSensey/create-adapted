package net.createmod.catnip.api.data;

import net.createmod.catnip.api.platform.ServiceHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/// Holds a pair of registries for [PreparableReloadListener]s. One for client assets, and one for server data.
public interface ReloadListenerRegistries {
	ReloadListenerRegistries INSTANCE = ServiceHelper.load(ReloadListenerRegistries.class);

	Registry assets();
	Registry data();

	/// A registry of [PreparableReloadListener]s.
	interface Registry {
		void register(Identifier id, PreparableReloadListener listener);

		void addOrdering(Identifier before, Identifier after);
	}
}
