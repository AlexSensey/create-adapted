package net.createmod.catnip.api.platform;

import net.createmod.catnip.api.platform.services.PlatformHelper;

public enum Loader {
	FABRIC, NEOFORGE;

	public boolean isFabric() {
		return this == FABRIC;
	}

	public boolean isNeoForge() {
		return this == NEOFORGE;
	}

	public boolean isCurrent() {
		return this == PlatformHelper.INSTANCE.getLoader();
	}
}
