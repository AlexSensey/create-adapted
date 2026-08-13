package com.simibubi.create.content.contraptions.render;

import java.util.Map;
import java.util.WeakHashMap;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.util.Mth;

/**
 * Keeps moving-actor rotation continuous when the contraption speed is updated.
 * Multiplying the current speed by the global render time changes the entire
 * phase whenever network synchronisation adjusts that speed, causing visible
 * jumps for the first few seconds after joining a world.
 */
public final class ContraptionActorRotation {
	private static final Map<MovementContext, State> STATES = new WeakHashMap<>();

	private ContraptionActorRotation() {}

	public static float getAngle(MovementContext context, float speed) {
		float renderTime = AnimationTickHolder.getRenderTime();
		State state = STATES.computeIfAbsent(context, $ -> new State(renderTime));
		float delta = renderTime - state.lastRenderTime;
		state.lastRenderTime = renderTime;

		// Do not catch up in one frame after pausing, reloading, or returning
		// to an actor that was outside the render distance.
		if (delta < 0 || delta > 5)
			delta = 0;

		state.angleDegrees = Mth.wrapDegrees(state.angleDegrees + delta * speed / 20f);
		return state.angleDegrees / 180f * (float) Math.PI;
	}

	private static class State {
		float lastRenderTime;
		float angleDegrees;

		State(float renderTime) {
			lastRenderTime = renderTime;
		}
	}
}
