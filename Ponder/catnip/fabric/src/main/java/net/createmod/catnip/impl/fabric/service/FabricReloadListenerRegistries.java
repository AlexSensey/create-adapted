package net.createmod.catnip.impl.fabric.service;

import net.createmod.catnip.api.data.ReloadListenerRegistries;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public record FabricReloadListenerRegistries(Registry assets, Registry data) implements ReloadListenerRegistries {
	@SuppressWarnings("unused") // will be invoked by ServiceLoader
	public FabricReloadListenerRegistries() {
		this(new FabricRegistry(PackType.CLIENT_RESOURCES), new FabricRegistry(PackType.SERVER_DATA));
	}

	private record FabricRegistry(ResourceLoader resourceLoader) implements ReloadListenerRegistries.Registry {
		private FabricRegistry(PackType type) {
			this(ResourceLoader.get(type));
		}

		@Override
		public void register(Identifier id, PreparableReloadListener listener) {
			this.resourceLoader.registerReloadListener(id, listener);
		}

		@Override
		public void addOrdering(Identifier before, Identifier after) {
			this.resourceLoader.addListenerOrdering(before, after);
		}
	}
}
