package net.createmod.ponder.api.client.scene;

import java.util.function.Consumer;

import net.createmod.ponder.api.client.instruction.PonderInstruction;

public interface DebugInstructions {
	void debugSchematic();

	void addInstructionInstance(PonderInstruction instruction);

	void enqueueCallback(Consumer<PonderScene> callback);
}
