package net.createmod.ponder.impl.client.instruction;

import net.createmod.ponder.api.client.instruction.PonderInstruction;
import net.createmod.ponder.api.client.scene.PonderScene;

public class MarkAsFinishedInstruction extends PonderInstruction {
	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public void tick(PonderScene scene) {
		scene.setFinished(true);
	}

	@Override
	public void onScheduled(PonderScene scene) {
		scene.stopCounting();
	}
}
