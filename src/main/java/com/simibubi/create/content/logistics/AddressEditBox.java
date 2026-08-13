package com.simibubi.create.content.logistics;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.simibubi.create.content.trains.schedule.DestinationSuggestions;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class AddressEditBox extends EditBox {

	private final DestinationSuggestions destinationSuggestions;
	private Consumer<String> mainResponder;
	private String prevValue = "=)";

	public AddressEditBox(Screen screen, Font pFont, int pX, int pY, int pWidth, int pHeight, boolean anchorToBottom) {
		this(screen, pFont, pX, pY, pWidth, pHeight, anchorToBottom, null);
	}

	public AddressEditBox(Screen screen, Font pFont, int pX, int pY, int pWidth, int pHeight, boolean anchorToBottom,
		String localAddress) {
		super(pFont, pX, pY, pWidth, pHeight, Component.empty());
		destinationSuggestions = AddressEditBoxHelper.createSuggestions(screen, this, anchorToBottom, localAddress);
		destinationSuggestions.setAllowSuggestions(true);
		destinationSuggestions.updateCommandInfo();
		mainResponder = t -> {
			if (!t.equals(prevValue))
				destinationSuggestions.updateCommandInfo();
			prevValue = t;
		};
		setResponder(mainResponder);
		setBordered(false);
		setFocused(false);
		setMaxLength(25);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (destinationSuggestions.keyPressed(event))
			return true;
		if (isFocused() && event.key() == GLFW.GLFW_KEY_ENTER) {
			setFocused(false);
			moveCursorToEnd(false);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (destinationSuggestions.mouseScrolled(Mth.clamp(scrollY, -1.0D, 1.0D)))
			return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && isMouseOver(event.x(), event.y())) {
			setValue("");
			return true;
		}

		boolean wasFocused = isFocused();
		if (super.mouseClicked(event, doubleClick)) {
			if (!wasFocused) {
				setHighlightPos(0);
				setCursorPosition(getValue().length());
			}
			return true;
		}
		return destinationSuggestions.mouseClicked(event, doubleClick);
	}

	@Override
	public void setValue(String text) {
		setHighlightPos(0);
		super.setValue(text);
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.nextStratum();
		destinationSuggestions.render(guiGraphics, mouseX, mouseY);
	}

	@Override
	public void setResponder(Consumer<String> pResponder) {
		super.setResponder(pResponder == mainResponder ? mainResponder : mainResponder.andThen(pResponder));
	}

	public void tick() {
		if (!isFocused())
			destinationSuggestions.hide();
		if (isFocused() && destinationSuggestions.suggestions == null)
			destinationSuggestions.updateCommandInfo();
		destinationSuggestions.tick();
	}
}
