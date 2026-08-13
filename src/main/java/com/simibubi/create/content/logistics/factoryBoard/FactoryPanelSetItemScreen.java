package com.simibubi.create.content.logistics.factoryBoard;

import java.util.Collections;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class FactoryPanelSetItemScreen extends AbstractSimiContainerScreen<FactoryPanelSetItemMenu> {

	private IconButton confirmButton;
	private List<Rect2i> extraAreas = Collections.emptyList();

	public FactoryPanelSetItemScreen(FactoryPanelSetItemMenu container, Inventory inv, Component title) {
		super(container, inv, title);
	}

	@Override
	protected void init() {
		int backgroundHeight = AllGuiTextures.FACTORY_GAUGE_SET_ITEM.getHeight();
		int backgroundWidth = AllGuiTextures.FACTORY_GAUGE_SET_ITEM.getWidth();
		setWindowSize(backgroundWidth, backgroundHeight + AllGuiTextures.PLAYER_INVENTORY.getHeight());
		super.init();
		clearWidgets();

		confirmButton = new IconButton(leftPos + backgroundWidth - 40,
			topPos + backgroundHeight - 25, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> minecraft.player.closeContainer());
		addRenderableWidget(confirmButton);

		extraAreas = List.of(new Rect2i(leftPos + backgroundWidth,
			topPos + backgroundHeight - 30, 40, 20));
	}

	@Override
	protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
		int x = leftPos;
		int y = topPos;
		AllGuiTextures.FACTORY_GAUGE_SET_ITEM.render(graphics, x - 5, y);
		renderPlayerInventory(graphics, x + 5, y + 94);

		Component instruction = CreateLang.translate("gui.factory_panel.place_item_to_monitor")
			.component();
		graphics.text(font, instruction, x + imageWidth / 2 - font.width(instruction) / 2 - 5,
			y + 4, 0xFF3D3C48, false);

		ItemStack stack = AllBlocks.FACTORY_GAUGE.asStack();
		GuiGameElement.of(stack)
			.scale(3)
			.at(x + 180, y + 48)
			.submit(graphics);
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}
