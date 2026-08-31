package net.createmod.catnip.api.client.config;

import com.mojang.blaze3d.platform.InputConstants;

import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;

public class HintableTextFieldWidget extends EditBox {
	protected Font font;
	protected String hint = "";

	public HintableTextFieldWidget(Font font, int x, int y, int width, int height) {
		super(font, x, y, width, height, CommonComponents.EMPTY);
		this.font = font;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);

		if (hint.isEmpty())
			return;

		if (!getValue().isEmpty())
			return;

		graphics.text(font, hint, getX() + 5, this.getY() + (this.height - 8) / 2, UIRenderHelper.COLOR_TEXT.getFirst().scaleAlpha(.75f).getRGB());
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!isMouseOver(event.x(), event.y()))
			return false;

		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			setValue("");
			return true;
		} else {
			return super.mouseClicked(event, doubleClick);
		}
	}

	@Override
	public boolean keyPressed(KeyEvent keyevent) {
		if (Minecraft.getInstance().options.keyInventory.matches(keyevent)) {
			return true;
		}

		return super.keyPressed(keyevent);
	}
}
