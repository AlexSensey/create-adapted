package com.simibubi.create.foundation.gui;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;

public class RemovedGuiUtils {
	@NotNull
	private static ItemStack cachedTooltipStack = ItemStack.EMPTY;

	public static void preItemToolTip(@NotNull ItemStack stack) {
		cachedTooltipStack = stack;
	}

	public static void postItemToolTip() {
		cachedTooltipStack = ItemStack.EMPTY;
	}

	public static void drawHoveringText(GuiGraphics graphics, List<? extends FormattedText> textLines, int mouseX,
		int mouseY, int screenWidth, int screenHeight, int maxTextWidth, Font font) {
	}

	public static void drawHoveringText(GuiGraphics graphics, List<? extends FormattedText> textLines, int mouseX,
		int mouseY, int screenWidth, int screenHeight, int maxTextWidth, int backgroundColor, int borderColorStart,
		int borderColorEnd, Font font) {
	}

	public static void drawHoveringText(@NotNull final ItemStack stack, GuiGraphics graphics,
		List<? extends FormattedText> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight,
		int maxTextWidth, Font font) {
	}

	public static void drawHoveringText(@NotNull final ItemStack stack, GuiGraphics graphics,
		List<? extends FormattedText> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight,
		int maxTextWidth, int backgroundColor, int borderColorStart, int borderColorEnd, Font font) {
	}
}
