package net.createmod.ponder.impl.client.element;

import net.createmod.ponder.api.client.element.PonderElement;

public abstract class PonderElementBase implements PonderElement {

	boolean visible = true;

	@Override
	public boolean isVisible() {
		return visible;
	}

	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

}
