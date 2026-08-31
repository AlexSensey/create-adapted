package net.createmod.catnip.api.platform;

import net.createmod.catnip.api.platform.services.PlatformHelper;

public enum Env {
	CLIENT, SERVER;

	public boolean isClient() {
		return this == CLIENT;
	}

	public boolean isServer() {
		return this == SERVER;
	}

	public boolean isCurrent() {
		return this == PlatformHelper.INSTANCE.getEnv();
	}
}
