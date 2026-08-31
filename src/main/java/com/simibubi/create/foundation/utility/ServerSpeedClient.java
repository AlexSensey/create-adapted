package com.simibubi.create.foundation.utility;

import net.minecraft.client.Minecraft;

public final class ServerSpeedClient {
	private ServerSpeedClient() {}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.hasSingleplayerServer() && minecraft.isPaused())
			return;
		ServerSpeedProvider.tickClientState();
	}
}
