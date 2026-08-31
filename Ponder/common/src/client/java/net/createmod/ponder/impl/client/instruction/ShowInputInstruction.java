package net.createmod.ponder.impl.client.instruction;

import net.createmod.ponder.api.client.scene.PonderScene;
import net.createmod.ponder.impl.client.element.InputWindowElement;

public class ShowInputInstruction extends FadeInOutInstruction {
	private final InputWindowElement element;

	public ShowInputInstruction(InputWindowElement element, int ticks) {
		super(ticks);
		this.element = element;
	}

	@Override
	protected void show(PonderScene scene) {
		scene.addElement(element);
		element.setVisible(true);
	}

	@Override
	protected void hide(PonderScene scene) {
		element.setVisible(false);
	}

	@Override
	protected void applyFade(PonderScene scene, float fade) {
		element.setFade(fade);
	}
}
