package com.simibubi.create.foundation.ponder;

/**
 * Common-side check for Ponder's client-only virtual level.
 *
 * Referencing PonderLevel directly from common code links its client renderer
 * interfaces on a dedicated server. Compare class names instead so the client
 * class is never resolved server-side.
 */
public final class PonderLevelCompat {
	private static final String PONDER_LEVEL_CLASS = "net.createmod.ponder.api.client.level.PonderLevel";

	private PonderLevelCompat() {}

	public static boolean isPonderLevel(Object level) {
		if (level == null)
			return false;
		for (Class<?> type = level.getClass(); type != null; type = type.getSuperclass())
			if (PONDER_LEVEL_CLASS.equals(type.getName()))
				return true;
		return false;
	}
}
