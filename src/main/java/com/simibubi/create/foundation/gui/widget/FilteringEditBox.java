package com.simibubi.create.foundation.gui.widget;

import java.util.function.Predicate;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Restores the value validation hook removed from Minecraft's 26.2 EditBox API. */
public class FilteringEditBox extends EditBox {

	private Predicate<String> filter = value -> true;

	public FilteringEditBox(Font font, int x, int y, int width, int height, Component message) {
		super(font, x, y, width, height, message);
	}

	public void setFilter(Predicate<String> filter) {
		this.filter = filter;
	}

	@Override
	public void setValue(String value) {
		if (filter.test(value))
			super.setValue(value);
	}

	@Override
	public void insertText(String input) {
		String previousValue = getValue();
		int previousCursor = getCursorPosition();
		super.insertText(input);
		if (filter.test(getValue()))
			return;
		super.setValue(previousValue);
		setCursorPosition(previousCursor);
	}
}
