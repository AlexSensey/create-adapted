package com.simibubi.create.content.kinetics.belt;

/** Shared animation phase: two stripe repeats per block, moving at RPM / 480 blocks per tick. */
public final class BeltScroll {

	private BeltScroll() {}

	public static float offset(float speed, float renderTime, float texturePeriod) {
		double cycles = (double) speed * renderTime / 240;
		return (float) ((cycles - Math.floor(cycles)) * texturePeriod);
	}
}
