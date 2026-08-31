package net.createmod.catnip.api.client.config;

import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;

public class ConfigTextField extends HintableTextFieldWidget {
	public ConfigTextField(Font font, int x, int y, int width, int height) {
		super(font, x, y, width, height);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!isMouseOver(event.x(), event.y()))
			setFocused(false);
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		super.onClick(event, doubleClick);
		setFocused(true);
	}

	@Override
	public void setFocused(boolean focus) {
		super.setFocused(focus);

		if (!focus) {
			if (ConfigScreenList.currentText == this)
				ConfigScreenList.currentText = null;

			return;
		}

		if (ConfigScreenList.currentText != null && ConfigScreenList.currentText != this)
			ConfigScreenList.currentText.setFocused(false);

		ConfigScreenList.currentText = this;
	}
}
