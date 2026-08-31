package com.simibubi.create.foundation.gui;

import java.util.function.Predicate;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Restores the input validation hook removed from Minecraft's EditBox in 26.2.
 */
public class FilteringEditBox extends EditBox {

	private Predicate<String> filter = value -> true;

	public FilteringEditBox(Font font, int x, int y, int width, int height, Component narration) {
		super(font, x, y, width, height, narration);
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
		setHighlightPos(previousCursor);
	}
}
