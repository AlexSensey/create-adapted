package net.neoforged.fml.loading;

import net.neoforged.api.distmarker.Dist;

public final class FMLEnvironment {
	public static Dist dist = getDist();

	private FMLEnvironment() {
	}

	public static Dist getDist() {
		return FMLLoader.getCurrent().getDist();
	}

	public static boolean isProduction() {
		return FMLLoader.getCurrent().isProduction();
	}
}
