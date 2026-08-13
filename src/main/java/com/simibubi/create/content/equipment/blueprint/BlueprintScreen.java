package com.simibubi.create.content.equipment.blueprint;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket.Option;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlueprintScreen extends AbstractSimiContainerScreen<BlueprintMenu> {

	protected AllGuiTextures background;
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton resetButton;
	private IconButton confirmButton;

	public BlueprintScreen(BlueprintMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		background = AllGuiTextures.BLUEPRINT;
	}

	@Override
	protected void init() {
		setWindowSize(background.getWidth(), background.getHeight() + 4 + PLAYER_INVENTORY.getHeight());
		setWindowOffset(1, 0);
		super.init();

		int x = leftPos;
		int y = topPos;

		resetButton = new IconButton(x + background.getWidth() - 62, y + background.getHeight() - 24,
			AllIcons.I_TRASH);
		resetButton.withCallback(() -> {
			menu.clearContents();
			contentsCleared();
			menu.sendClearPacket();
		});
		confirmButton = new IconButton(x + background.getWidth() - 33, y + background.getHeight() - 24,
			AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> minecraft.player.closeContainer());

		addRenderableWidget(resetButton);
		addRenderableWidget(confirmButton);
		extraAreas = ImmutableList.of(
			new Rect2i(x + background.getWidth(), y + background.getHeight() - 36, 56, 44));
	}

	@Override
	protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
		int invX = getLeftOfCentered(PLAYER_INVENTORY.getWidth());
		int invY = topPos + background.getHeight() + 4;
		renderPlayerInventory(graphics, invX, invY);
		background.render(graphics, leftPos, topPos);
	}

	@Override
	protected void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
		graphics.text(font, title, leftPos + 15, topPos + 4, 0xFFFFFF, false);
		GuiGameElement.of(AllItems.CRAFTING_BLUEPRINT.asStack())
			.<GuiGameElement.GuiRenderBuilder>at(leftPos + background.getWidth() + 20,
				topPos + background.getHeight() - 32, 0)
			.rotate(45, -45, 22.5f)
			.scale(40)
			.submit(graphics);

		if (!menu.getCarried().isEmpty() || hoveredSlot == null
			|| hoveredSlot.container == menu.playerInventory)
			return;

		int slot = hoveredSlot.getSlotIndex();
		if (slot < 0 || slot > 10)
			return;

		List<Component> tooltip = new LinkedList<>();
		if (hoveredSlot.hasItem())
			tooltip.addAll(getTooltipFromContainerItem(hoveredSlot.getItem()));
		addSlotDescription(tooltip, slot, !hoveredSlot.hasItem());
		graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
	}

	private void addSlotDescription(List<Component> tooltip, int slot, boolean emptySlot) {
		if (slot < 9) {
			tooltip.add(CreateLang.translateDirect("crafting_blueprint.crafting_slot")
				.withStyle(ChatFormatting.GOLD));
			tooltip.add(CreateLang.translateDirect("crafting_blueprint.filter_items_viable")
				.withStyle(ChatFormatting.GRAY));
			return;
		}

		if (slot == 9) {
			tooltip.add(CreateLang.translateDirect("crafting_blueprint.display_slot")
				.withStyle(ChatFormatting.GOLD));
			if (!emptySlot)
				tooltip.add(CreateLang
					.translateDirect("crafting_blueprint."
						+ (menu.contentHolder.inferredIcon ? "inferred" : "manually_assigned"))
					.withStyle(ChatFormatting.GRAY));
			return;
		}

		tooltip.add(CreateLang.translateDirect("crafting_blueprint.secondary_display_slot")
			.withStyle(ChatFormatting.GOLD));
		if (emptySlot)
			tooltip.add(CreateLang.translateDirect("crafting_blueprint.optional")
				.withStyle(ChatFormatting.GRAY));
	}

	@Override
	protected void containerTick() {
		if (!menu.contentHolder.isEntityAlive()) {
			menu.player.closeContainer();
			return;
		}
		super.containerTick();
	}

	protected void contentsCleared() {
	}

	protected void sendOptionUpdate(Option option) {
		// Blueprint has no filter options; retained for compatibility with older subclasses.
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}
