package net.createmod.catnip.api.client.gui.element;

import org.joml.Matrix3x2fStack;

import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class TextStencilElement extends DelegatedStencilElement {
	protected Font font;
	protected MutableComponent component = Component.empty();
	protected boolean centerVertically = false;
	protected boolean centerHorizontally = false;

	public TextStencilElement(Font font) {
		super();
		this.font = font;
		height = 10;
	}

	public TextStencilElement(Font font, String text) {
		this(font);
		component = Component.literal(text);
	}

	public TextStencilElement(Font font, MutableComponent component) {
		this(font);
		this.component = component;
	}

	public TextStencilElement withText(String text) {
		component = Component.literal(text);
		return this;
	}

	public TextStencilElement withText(MutableComponent component) {
		this.component = component;
		return this;
	}

	public TextStencilElement centered(boolean vertical, boolean horizontal) {
		this.centerVertically = vertical;
		this.centerHorizontally = horizontal;
		return this;
	}

	@Override
	public void renderStencil(GuiGraphicsExtractor graphics) {

		float x = 0, y = 0;
		if (centerHorizontally)
			x = width / 2f - font.width(component) / 2f;

		if (centerVertically)
			y = height / 2f - (font.lineHeight - 1) / 2f;

		graphics.text(font, component, Math.round(x), Math.round(y), Color.BLACK.getRGB(), false);
		//graphics.flush(); TODO - Is there an replacement?
	}

	@Override
	public void renderElement(GuiGraphicsExtractor graphics) {
		float x = 0, y = 0;
		if (centerHorizontally)
			x = width / 2f - font.width(component) / 2f;

		if (centerVertically)
			y = height / 2f - (font.lineHeight - 1) / 2f;

		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();
		poseStack.translate(x, y);
		element.render(graphics, font.width(component), font.lineHeight + 2, alpha);
		poseStack.popMatrix();
	}

	public MutableComponent getComponent() {
		return component;
	}
}
