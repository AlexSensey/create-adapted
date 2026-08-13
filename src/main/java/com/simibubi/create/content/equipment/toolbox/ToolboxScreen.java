package com.simibubi.create.content.equipment.toolbox;

import java.util.Collections;
import java.util.List;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ToolboxScreen extends AbstractSimiContainerScreen<ToolboxMenu> {

	protected static final AllGuiTextures BG = AllGuiTextures.TOOLBOX;
	protected static final AllGuiTextures PLAYER = AllGuiTextures.PLAYER_INVENTORY;
	private static final int WINDOW_WIDTH = 30 + 188;
	private static final int WINDOW_HEIGHT = 171 + 108 - 24;

	private IconButton confirmButton;
	private IconButton disposeButton;
	private Slot hoveredToolboxSlot;

	public ToolboxScreen(ToolboxMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
	}

	@Override
	protected void init() {
		setWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		setWindowOffset(-35, 0);
		super.init();
		clearWidgets();

		confirmButton =
			new IconButton(leftPos + 30 + BG.getWidth() - 33, topPos + BG.getHeight() - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> minecraft.player.closeContainer());
		addRenderableWidget(confirmButton);

		disposeButton = new IconButton(leftPos + 30 + 81, topPos + 69, AllIcons.I_TOOLBOX);
		disposeButton.withCallback(() -> {
			ClientNetworkHelper.INSTANCE.sendToServer(new ToolboxDisposeAllPacket(menu.contentHolder.getBlockPos()));
		});
		disposeButton.setToolTip(CreateLang.translateDirect("toolbox.depositBox"));
		addRenderableWidget(disposeButton);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		// Each visible slot represents a whole four-stack compartment. Suppress the
		// vanilla slot pass, which would display only the first stack (at most 64).
		menu.renderPass = true;
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		menu.renderPass = false;
		renderTitle(graphics);
	}

	@Override
	protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
		int x = leftPos + WINDOW_WIDTH - BG.getWidth();
		int y = topPos;

		BG.render(graphics, x, y);

		int invX = leftPos;
		int invY = topPos + WINDOW_HEIGHT - PLAYER.getHeight();
		renderPlayerInventory(graphics, invX, invY);

		hoveredToolboxSlot = null;
		for (int compartment = 0; compartment < 8; compartment++) {
			int baseIndex = compartment * ToolboxInventory.STACKS_PER_COMPARTMENT;
			Slot slot = menu.slots.get(baseIndex);
			ItemStack displayed = slot.getItem();
			if (displayed.isEmpty())
				displayed = menu.getFilter(compartment);

			int slotX = leftPos + slot.x;
			int slotY = topPos + slot.y;
			if (!displayed.isEmpty()) {
				graphics.item(displayed, slotX, slotY);
				graphics.itemDecorations(font, displayed, slotX, slotY,
					String.valueOf(menu.totalCountInCompartment(compartment)));
			}

			if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
				hoveredToolboxSlot = slot;
				graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80ffffff);
			}
		}
	}

	@Override
	protected void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		if (hoveredToolboxSlot != null)
			hoveredSlot = hoveredToolboxSlot;
		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
	}

	private void renderTitle(GuiGraphicsExtractor graphics) {
		int x = leftPos + WINDOW_WIDTH - BG.getWidth();
		int y = topPos;
		Component screenTitle = title.getString()
			.isBlank() ? menu.contentHolder.getDisplayName() : title;
		graphics.text(font, screenTitle, x + 15, y + 4, 0xff592424, false);
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return Collections.emptyList();
	}

	@Override
	public void removed() {
		menu.contentHolder.closeAnimatedContainerLocally();
		super.removed();
	}

}
