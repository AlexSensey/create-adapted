package net.minecraft.client.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class GuiGraphics extends GuiGraphicsExtractor {
	public GuiGraphics() {
		super(Minecraft.getInstance(), new GuiRenderState(), 0, 0);
	}

	public MultiBufferSource.BufferSource bufferSource() {
		return MultiBufferSource.immediateWithBuffers(java.util.Map.of(), new com.mojang.blaze3d.vertex.ByteBufferBuilder(256));
	}

	public void drawString(Font font, String text, int x, int y, int color) {
		text(font, text, x, y, color);
	}

	public void drawString(Font font, Component text, int x, int y, int color) {
		text(font, text, x, y, color);
	}

	public void drawCenteredString(Font font, Component text, int x, int y, int color) {
		centeredText(font, text, x, y, color);
	}

	public void renderItem(ItemStack stack, int x, int y) {
		item(stack, x, y);
	}

	public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
		itemDecorations(font, stack, x, y);
	}

	public void renderTooltip(Font font, List<Component> tooltip, int x, int y) {
		setComponentTooltipForNextFrame(font, tooltip, x, y);
	}

	public void blit(Identifier texture, int x, int y, int u, int v, int width, int height) {
		blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 256, 256);
	}
}
