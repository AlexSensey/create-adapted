package net.createmod.ponder.impl.client.instruction;

import net.createmod.ponder.api.client.instruction.PonderInstruction;
import net.createmod.ponder.api.client.scene.PonderScene;

public class KeyframeInstruction extends PonderInstruction {
	public static final KeyframeInstruction IMMEDIATE = new KeyframeInstruction(false);
	public static final KeyframeInstruction DELAYED = new KeyframeInstruction(true);

	private final boolean delayed;

	private KeyframeInstruction(boolean delayed) {
		this.delayed = delayed;
	}

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public void tick(PonderScene scene) {
	}

	@Override
	public void onScheduled(PonderScene scene) {
		scene.markKeyframe(delayed ? 6 : 0);
	}
}
