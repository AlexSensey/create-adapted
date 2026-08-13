package dev.engine_room.flywheel.impl.event;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Captures matrices which Minecraft 26.2 does not expose in CameraRenderState. */
public final class LevelRenderMatrices {
	private static final Matrix4f PROJECTION = new Matrix4f();
	private static boolean hasProjection;

	private LevelRenderMatrices() {
	}

	public static void captureProjection(Matrix4fc projection) {
		PROJECTION.set(projection);
		hasProjection = true;
	}

	public static Matrix4fc projection(Matrix4fc fallback) {
		return hasProjection ? PROJECTION : fallback;
	}
}
