package net.createmod.catnip.impl.fabric.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.createmod.catnip.api.platform.Env;
import net.createmod.catnip.api.platform.Loader;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class FabricPlatformHelper implements PlatformHelper {
	@Override
	public Loader getLoader() {
		return Loader.FABRIC;
	}

	@Override
	public Env getEnv() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? Env.CLIENT : Env.SERVER;
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	@Override
	public List<String> getLoadedMods() {
		List<String> modIds = new ArrayList<>();
		for (ModContainer mod : FabricLoader.getInstance().getAllMods())
			modIds.add(mod.getMetadata().getId());
		return modIds;
	}

	@Override
	public String getModDisplayName(String modId) {
		return FabricLoader.getInstance().getModContainer(modId)
				.map(mod -> mod.getMetadata().getName())
				.orElse("h"); // FIXME: config
				// .orElse(ConfigScreen.toHumanReadable(modId));
	}

	@Override
	public void executeOnClientOnly(Supplier<Runnable> toRun) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
			toRun.get().run();
	}

	@Override
	public void executeOnServerOnly(Supplier<Runnable> toRun) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
			toRun.get().run();
	}
}
