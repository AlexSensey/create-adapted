package com.simibubi.create.content.redstone.link.controller;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class LinkedControllerScreen extends AbstractSimiContainerScreen<LinkedControllerMenu> {

	private static final Component SCREEN_TITLE = Component.translatable("item.create.linked_controller");
	private static final int TITLE_COLOR = 0xFF592424;

	protected AllGuiTextures background;
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton resetButton;
	private IconButton confirmButton;

	public LinkedControllerScreen(LinkedControllerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		background = AllGuiTextures.LINKED_CONTROLLER;
	}

	@Override
	protected void init() {
		setWindowSize(background.getWidth(), background.getHeight() + 4 + PLAYER_INVENTORY.getHeight());
		setWindowOffset(1, 0);
		super.init();

		int x = leftPos;
		int y = topPos;

		resetButton = new IconButton(x + background.getWidth() - 62, y + background.getHeight() - 24, AllIcons.I_TRASH);
		resetButton.withCallback(() -> {
			menu.clearContents();
			menu.sendClearPacket();
		});
		confirmButton = new IconButton(x + background.getWidth() - 33, y + background.getHeight() - 24,
			AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> minecraft.player.closeContainer());

		addRenderableWidget(resetButton);
		addRenderableWidget(confirmButton);

		extraAreas = ImmutableList.of(new Rect2i(x + background.getWidth() + 4, y + background.getHeight() - 44, 64,
			56));
	}

	@Override
	protected void containerTick() {
		if (!AllItems.LINKED_CONTROLLER.isIn(menu.player.getMainHandItem())
			|| !ItemStack.matches(menu.player.getMainHandItem(), menu.contentHolder)) {
			menu.player.closeContainer();
			return;
		}

		super.containerTick();
	}

	@Override
	protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
		int invX = getLeftOfCentered(PLAYER_INVENTORY.getWidth());
		int invY = topPos + background.getHeight() + 4;
		renderPlayerInventory(graphics, invX, invY);

		int x = leftPos;
		int y = topPos;

		background.render(graphics, x, y);
	}

	@Override
	protected void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
		renderTitle(graphics);
		GuiGameElement.of(menu.contentHolder).<GuiGameElement
			.GuiRenderBuilder>at(leftPos + background.getWidth() - 4, topPos + background.getHeight() - 56, -200)
			.scale(5)
			.submit(graphics);

		if (!menu.getCarried().isEmpty() || hoveredSlot == null || hoveredSlot.container == menu.playerInventory)
			return;
		int slot = hoveredSlot.getSlotIndex();
		if (slot < 0 || slot >= 12)
			return;

		List<Component> tooltip = new LinkedList<>();
		if (hoveredSlot.hasItem())
			tooltip.addAll(getTooltipFromContainerItem(hoveredSlot.getItem()));
		tooltip.add(CreateLang.translateDirect("linked_controller.frequency_slot_" + (slot % 2 + 1),
				ControlsUtil.getControls().get(slot / 2).getTranslatedKeyMessage().getString())
			.withStyle(ChatFormatting.GOLD));
		graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
	}

	private void renderTitle(GuiGraphicsExtractor graphics) {
		graphics.text(font, SCREEN_TITLE, leftPos + 15, topPos + 4, TITLE_COLOR, false);
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

}
