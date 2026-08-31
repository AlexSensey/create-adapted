package net.createmod.ponder.impl.client.element;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.ponder.api.client.element.AnimatedOverlayElement;

public abstract class AnimatedOverlayElementBase extends PonderElementBase implements AnimatedOverlayElement {

	protected LerpedFloat fade;

	public AnimatedOverlayElementBase() {
		fade = LerpedFloat.linear()
			.startWithValue(0);
	}

	@Override
	public void setFade(float fade) {
		this.fade.setValue(fade);
	}

	@Override
	public float getFade(float partialTicks) {
		return this.fade.getValue(partialTicks);
	}

}
