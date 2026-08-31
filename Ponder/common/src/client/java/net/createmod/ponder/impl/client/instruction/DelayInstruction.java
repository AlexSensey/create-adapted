package net.createmod.ponder.impl.client.instruction;

public class DelayInstruction extends TickingInstruction {
	public DelayInstruction(int ticks) {
		super(true, ticks);
	}
}
