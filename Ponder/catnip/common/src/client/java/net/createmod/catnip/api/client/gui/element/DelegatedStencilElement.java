package net.createmod.catnip.api.client.gui.element;

import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class DelegatedStencilElement extends AbstractRenderElement implements StencilElement {
	protected static final FadableScreenElement EMPTY_RENDERER = (graphics, width, height, alpha) -> {};
	protected static final FadableScreenElement DEFAULT_ELEMENT = (graphics, width, height, alpha) ->
		UIRenderHelper.angledGradient(graphics, 0, -3, 5, height + 4, width + 6, new Color(0xff_10dd10).scaleAlpha(alpha), new Color(0xff_1010dd).scaleAlpha(alpha));

	protected FadableScreenElement stencil;
	protected FadableScreenElement element;

	public DelegatedStencilElement() {
		stencil = EMPTY_RENDERER;
		element = DEFAULT_ELEMENT;
	}

	public DelegatedStencilElement(FadableScreenElement stencil, FadableScreenElement element) {
		this.stencil = stencil;
		this.element = element;
	}

	public <T extends DelegatedStencilElement> T withStencilRenderer(FadableScreenElement renderer) {
		stencil = renderer;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends DelegatedStencilElement> T withElementRenderer(FadableScreenElement renderer) {
		element = renderer;
		//noinspection unchecked
		return (T) this;
	}

	@Override
	public void renderStencil(GuiGraphicsExtractor graphics) {
		stencil.render(graphics, width, height, 1);
	}

	@Override
	public void renderElement(GuiGraphicsExtractor graphics) {
		element.render(graphics, width, height, alpha);
	}
}
