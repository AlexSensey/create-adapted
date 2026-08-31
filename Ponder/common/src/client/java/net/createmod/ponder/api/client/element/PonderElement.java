package net.createmod.ponder.api.client.element;

import net.createmod.ponder.api.client.scene.PonderScene;

public interface PonderElement {
	default void whileSkipping(PonderScene scene) {
	}

	default void tick(PonderScene scene) {
	}

	default void reset(PonderScene scene) {
	}

	boolean isVisible();

	void setVisible(boolean visible);
}
