package net.createmod.ponder.impl.client.gui.element;

import net.createmod.catnip.api.client.gui.TextureSheetSegment;
import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.client.gui.element.DelegatedStencilElement;
import net.createmod.catnip.api.client.gui.element.ScreenElement;
import net.createmod.catnip.api.client.render.ColoredRenderable;
import net.createmod.catnip.api.theme.Color;
import net.createmod.ponder.api.Ponder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public enum PonderGuiTextures implements TextureSheetSegment, ScreenElement, ColoredRenderable {
	//logo
	LOGO("logo", 0, 0, 32, 32, 32, 32);

	public final Identifier id;
	private final int width;
	private final int height;
	private final int startX;
	private final int startY;
	private final int sheetWidth;
	private final int sheetHeight;

	PonderGuiTextures(String id, int iconColumn, int iconRow) {
		this(id, iconColumn * 16, iconRow * 16, 16, 16);
	}

	PonderGuiTextures(String id, int startX, int startY, int width, int height) {
		this(Ponder.MOD_ID, id, startX, startY, width, height, 256, 256);
	}

	PonderGuiTextures(String id, int startX, int startY, int width, int height, int sheetWidth, int sheetHeight) {
		this(Ponder.MOD_ID, id, startX, startY, width, height, sheetWidth, sheetHeight);
	}

	PonderGuiTextures(String namespace, String id, int startX, int startY, int width, int height, int sheetWidth, int sheetHeight) {
		this.id = Identifier.fromNamespaceAndPath(namespace, "textures/gui/" + id + ".png");
		this.width = width;
		this.height = height;
		this.startX = startX;
		this.startY = startY;
		this.sheetWidth = sheetWidth;
		this.sheetHeight = sheetHeight;
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, getId(), x, y, 0, startX, startY, width, height, sheetWidth, sheetHeight);
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, int x, int y, Color c) {
		UIRenderHelper.drawColoredTexture(graphics, bind(), c, x, y, startX, startY, width, height);
	}

	@Override
	public Identifier getId() {
		return id;
	}

	@Override
	public int getStartX() {
		return startX;
	}

	@Override
	public int getStartY() {
		return startY;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	public DelegatedStencilElement asStencil() {
		return new DelegatedStencilElement().withStencilRenderer((ms, w, h, alpha) -> this.render(ms, 0, 0)).withBounds(16, 16);
	}
}
