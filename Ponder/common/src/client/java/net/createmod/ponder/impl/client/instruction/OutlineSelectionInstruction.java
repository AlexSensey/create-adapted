package net.createmod.ponder.impl.client.instruction;

import net.createmod.ponder.api.client.PonderPalette;
import net.createmod.ponder.api.client.scene.PonderScene;
import net.createmod.ponder.api.client.scene.Selection;

public class OutlineSelectionInstruction extends TickingInstruction {
	private final PonderPalette color;
	private final Object slot;
	private final Selection selection;

	public OutlineSelectionInstruction(PonderPalette color, Object slot, Selection selection, int ticks) {
		super(false, ticks);
		this.color = color;
		this.slot = slot;
		this.selection = selection;
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		selection.makeOutline(scene.getOutliner(), slot)
			.lineWidth(1 / 16f)
			.colored(color.getColor());
	}
}
