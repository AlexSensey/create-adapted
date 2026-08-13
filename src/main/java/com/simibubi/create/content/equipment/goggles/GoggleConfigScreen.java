package com.simibubi.create.content.equipment.goggles;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class GoggleConfigScreen extends AbstractSimiScreen {
	private int offsetX;
	private int offsetY;
	private final List<Component> tooltip;

	public GoggleConfigScreen() {
		super(Component.empty());
		Component spacing = Component.literal("    ");
		tooltip = new ArrayList<>();
		tooltip.add(spacing.copy().append(CreateLang.translateDirect("gui.config.overlay1")));
		tooltip.add(spacing.copy().append(CreateLang.translateDirect("gui.config.overlay2")
			.withStyle(ChatFormatting.GRAY)));
		tooltip.add(CommonComponents.EMPTY);
		tooltip.add(spacing.copy().append(CreateLang.translateDirect("gui.config.overlay3")));
		tooltip.add(spacing.copy().append(CreateLang.translateDirect("gui.config.overlay4")));
		tooltip.add(CommonComponents.EMPTY);
		tooltip.add(spacing.copy().append(CreateLang.translateDirect("gui.config.overlay5")
			.withStyle(ChatFormatting.GRAY)));
		tooltip.add(spacing.copy().append(CreateLang.translateDirect("gui.config.overlay6")
			.withStyle(ChatFormatting.GRAY)));
		tooltip.add(CommonComponents.EMPTY);
		tooltip.add(spacing.copy().append(CreateLang.translateDirect("gui.config.overlay7")));
		tooltip.add(spacing.copy().append(CreateLang.translateDirect("gui.config.overlay8")));
	}

	@Override
	protected void init() {
		width = minecraft.getWindow().getGuiScaledWidth();
		height = minecraft.getWindow().getGuiScaledHeight();
		offsetX = AllConfigs.client().overlayOffsetX.get();
		offsetY = AllConfigs.client().overlayOffsetY.get();
	}

	@Override
	public void removed() {
		AllConfigs.client().overlayOffsetX.set(offsetX);
		AllConfigs.client().overlayOffsetY.set(offsetY);
		super.removed();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		updateOffset(event.x(), event.y());
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		updateOffset(event.x(), event.y());
		return true;
	}

	private void updateOffset(double windowX, double windowY) {
		offsetX = (int) (windowX - width / 2d);
		offsetY = (int) (windowY - height / 2d);

		int tooltipWidth = 0;
		for (FormattedText line : tooltip)
			tooltipWidth = Math.max(tooltipWidth, font.width(line));
		int tooltipHeight = 8;
		if (tooltip.size() > 1) {
			tooltipHeight += (tooltip.size() - 1) * 10;
			tooltipHeight += 2;
		}

		offsetX = Mth.clamp(offsetX, -width / 2 - 5, width / 2 - tooltipWidth - 20);
		offsetY = Mth.clamp(offsetY, -height / 2 + 17, height / 2 - tooltipHeight + 5);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		int posX = width / 2 + offsetX;
		int posY = height / 2 + offsetY;
		graphics.setComponentTooltipForNextFrame(font, tooltip, posX, posY);

		ItemStack goggles = AllItems.GOGGLES.asStack();
		GuiGameElement.of(goggles)
			.at(posX + 10, posY - 16, 450)
			.submit(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}
}
