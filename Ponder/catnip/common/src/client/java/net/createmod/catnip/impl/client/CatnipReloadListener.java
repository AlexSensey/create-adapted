package net.createmod.catnip.impl.client;

import net.createmod.catnip.api.Catnip;
import net.createmod.catnip.api.client.lang.LangNumberFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public enum CatnipReloadListener implements ResourceManagerReloadListener {
	INSTANCE;

	public static final Identifier ID = Catnip.id("reload_listener");

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		LangNumberFormat.numberFormat.update();
	}
}
