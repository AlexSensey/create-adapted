package net.createmod.catnip.api.client.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;

public class AnimationTickHolder {
	private static int ticks;
	private static int pausedTicks;
	private static float guiPartialTicks;
	private static long lastGuiFrame = Long.MIN_VALUE;
	private static final ThreadLocal<Float> PARTIAL_TICKS_OVERRIDE = ThreadLocal.withInitial(() -> -1f);
	private static final ThreadLocal<Integer> TICKS_OVERRIDE = ThreadLocal.withInitial(() -> -1);

	public static void reset() {
		ticks = 0;
		pausedTicks = 0;
		guiPartialTicks = 0;
		lastGuiFrame = Long.MIN_VALUE;
	}

	public static void tick() {
		if (!Minecraft.getInstance()
			.isPaused()) {
			ticks = (ticks + 1) % 1_728_000; // wrap around every 24 hours so we maintain enough floating point precision
		} else {
			pausedTicks = (pausedTicks + 1) % 1_728_000;
			guiPartialTicks = 0;
			lastGuiFrame = Long.MIN_VALUE;
		}
	}

	public static int getTicks() {
		return getTicks(false);
	}

	public static int getTicks(boolean includePaused) {
		int overridden = TICKS_OVERRIDE.get();
		if (overridden >= 0)
			return overridden;
		return includePaused ? ticks + pausedTicks : ticks;
	}

	public static float getRenderTime() {
		return getTicks(PARTIAL_TICKS_OVERRIDE.get() >= 0) + getPartialTicks();
	}

	/**
	 * @return the fraction between the current tick to the next tick, frozen during game pause [0-1]
	 */
	public static float getPartialTicks() {
		float overridden = PARTIAL_TICKS_OVERRIDE.get();
		if (overridden >= 0)
			return overridden;
		return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	/** Temporarily supplies the virtual partial tick used while a Ponder scene is submitted. */
	public static float pushPartialTicks(float partialTicks) {
		float previous = PARTIAL_TICKS_OVERRIDE.get();
		PARTIAL_TICKS_OVERRIDE.set(partialTicks);
		return previous;
	}

	public static void restorePartialTicks(float previous) {
		PARTIAL_TICKS_OVERRIDE.set(previous);
	}

	/** Temporarily supplies the virtual tick used while a Ponder scene is submitted. */
	public static int pushTicks(int tickValue) {
		int previous = TICKS_OVERRIDE.get();
		TICKS_OVERRIDE.set(tickValue);
		return previous;
	}

	public static void restoreTicks(int previous) {
		TICKS_OVERRIDE.set(previous);
	}

	/// In `Screen.render`, the partialTicks value is actually incorrect.
	///
	/// In other cases, like entity rendering, partialTicks is an accumulated fraction of ticks that have
	/// passed since the last game tick. It should range from 0-1, but may be larger during lag spikes.
	///
	/// `Screen.render` is instead given a simple frame delta, which is not very useful for smooth animations.
	/// The value will pretty much always be the same.
	///
	/// This method provides access to the accumulated delta. This is actually what vanilla
	/// does in [EnchantmentScreen], which needs a smooth animation for the book opening.
	public static float getGuiPartialTicks() {
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isPaused())
			return getPartialTicks();

		long frame = minecraft.getFrameTimeNs();
		if (lastGuiFrame != frame) {
			lastGuiFrame = frame;
			guiPartialTicks = Math.min(1, guiPartialTicks + minecraft.getDeltaTracker().getRealtimeDeltaTicks());
		}
		return guiPartialTicks;
	}
}
