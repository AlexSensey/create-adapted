package dev.engine_room.flywheel.impl.compat;

/** Iris' 26.2 API is not available yet; keep the backend on the vanilla pipeline. */
public final class IrisCompat {
	public static final boolean ACTIVE = false;

	private IrisCompat() {
	}

	public static boolean isShaderPackInUse() {
		return false;
	}

	public static boolean isRenderingShadowPass() {
		return false;
	}
}
