package com.simibubi.create.content.trains.station;

import com.simibubi.create.foundation.mixin.accessor.FontAccessor;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

public class NoShadowFontWrapper extends Font {

	private final Font wrapped;

	public NoShadowFontWrapper(Font wrapped) {
		super(((FontAccessor) wrapped).create$getProvider());
		this.wrapped = wrapped;
	}

	@Override
	public PreparedText prepareText(String text, float x, float y, int color, boolean dropShadow, int backgroundColor) {
		return wrapped.prepareText(text, x, y, color, false, backgroundColor);
	}

	@Override
	public PreparedText prepareText(FormattedCharSequence text, float x, float y, int color, boolean dropShadow,
		boolean flag, int backgroundColor) {
		return wrapped.prepareText(text, x, y, color, false, flag, backgroundColor);
	}
}
