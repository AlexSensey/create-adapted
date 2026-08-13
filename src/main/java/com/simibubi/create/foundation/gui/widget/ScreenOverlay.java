package com.simibubi.create.foundation.gui.widget;

/**
 * A set of widgets that are offset on the Z axis, allowing them to render above/below other "layers".
 */
public class ScreenOverlay extends CompositeWidget {
	public final int zOffset;

	public ScreenOverlay(int zOffset) {
		this.zOffset = zOffset;
	}

}
