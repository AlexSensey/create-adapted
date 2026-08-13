package com.simibubi.create.content.logistics.stockTicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.menu.GhostItemSubmitPacket;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.SlotItemHandler;

public class StockKeeperCategoryScreen extends AbstractSimiContainerScreen<StockKeeperCategoryMenu> {

	private static final int CARD_HEIGHT = 20;
	private static final int CARD_WIDTH = 160;
	private static final int SLICES = 4;

	private final List<ItemStack> schedule;
	private final LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton confirmButton;
	private IconButton editorConfirm;
	private EditBox editorEditBox;
	private ItemStack editingItem;
	private int editingIndex;
	private int panelHeight;

	private final Component clickToEdit = CreateLang.translateDirect("gui.schedule.lmb_edit")
		.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);

	public StockKeeperCategoryScreen(StockKeeperCategoryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		schedule = new ArrayList<>(menu.contentHolder.categories);
		menu.slotsActive = false;
	}

	@Override
	protected void init() {
		AllGuiTextures body = AllGuiTextures.STOCK_KEEPER_CATEGORY;
		panelHeight = body.getHeight() * SLICES + AllGuiTextures.STOCK_KEEPER_CATEGORY_HEADER.getHeight()
			+ AllGuiTextures.STOCK_KEEPER_CATEGORY_FOOTER.getHeight();
		setWindowSize(body.getWidth(), panelHeight);
		super.init();
		clearWidgets();

		confirmButton = new IconButton(leftPos + body.getWidth() - 25, topPos + panelHeight - 25, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> minecraft.player.closeContainer());
		addRenderableWidget(confirmButton);

		if (editingItem != null) {
			editingItem = null;
			menu.slotsActive = false;
		}

		extraAreas = ImmutableList.of(new Rect2i(leftPos + body.getWidth(), topPos + panelHeight - 40, 48, 40));
	}

	private void startEditing(int index) {
		confirmButton.visible = false;
		editorConfirm = new IconButton(leftPos + 167, topPos + 59, AllIcons.I_CONFIRM);
		editorConfirm.withCallback(this::stopEditing);
		menu.slotsActive = true;

		editorEditBox = new EditBox(font, leftPos + 47, topPos + 28, 124, 10, Component.empty());
		editorEditBox.setTextColor(0xffeeeeee);
		editorEditBox.setBordered(false);
		editorEditBox.setMaxLength(28);

		editingIndex = index;
		editingItem = index == -1 ? ItemStack.EMPTY : schedule.get(index);
		editorEditBox.setValue(editingItem.isEmpty()
			? CreateLang.translate("gui.stock_ticker.new_category").component().getString()
			: editingItem.getHoverName().getString());
		menu.proxyInventory.setStackInSlot(0, editingItem);
		ClientNetworkHelper.INSTANCE.sendToServer(new GhostItemSubmitPacket(editingItem, 0));

		addRenderableWidget(editorConfirm);
		addRenderableWidget(editorEditBox);
		setFocused(editorEditBox);
	}

	private void stopEditing() {
		if (editingItem == null)
			return;

		ItemStack stack = menu.proxyInventory.getStackInSlot(0).copy();
		if (stack.isEmpty()) {
			if (editingIndex != -1)
				schedule.remove(editingIndex);
		} else {
			String value = editorEditBox.getValue();
			stack.set(DataComponents.CUSTOM_NAME, value.isBlank() ? null : Component.literal(value));
			if (editingIndex == -1)
				schedule.add(stack);
			else
				schedule.set(editingIndex, stack);
		}

		ClientNetworkHelper.INSTANCE.sendToServer(new GhostItemSubmitPacket(ItemStack.EMPTY, 0));
		playUiSound(SoundEvents.UI_BUTTON_CLICK.value(), 1, 1);
		editingItem = null;
		editorConfirm = null;
		editorEditBox = null;
		menu.slotsActive = false;
		init();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		scroll.tickChaser();
		if (editorEditBox == null)
			return;
		if (!menu.proxyInventory.getStackInSlot(0).has(DataComponents.CUSTOM_NAME))
			return;
		String placeholder = CreateLang.translate("gui.stock_ticker.new_category").component().getString();
		if (editorEditBox.getValue().equals(placeholder))
			editorEditBox.setValue(menu.proxyInventory.getStackInSlot(0).getHoverName().getString());
	}

	private void renderCategories(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		var pose = graphics.pose();
		int yOffset = 25;
		float scrollOffset = -scroll.getValue(partialTicks);

		graphics.enableScissor(leftPos + 3, topPos + 16, leftPos + 187,
			topPos + 19 + AllGuiTextures.STOCK_KEEPER_CATEGORY.getHeight() * SLICES);

		pose.pushMatrix();
		pose.translate(0, scrollOffset);
		for (int i = 0; i < schedule.size(); i++) {
			renderCategory(graphics, i, schedule.get(i), yOffset);
			yOffset += CARD_HEIGHT;
		}
		AllGuiTextures.STOCK_KEEPER_CATEGORY_NEW.render(graphics, leftPos + 7, topPos + yOffset);
		pose.popMatrix();
		graphics.disableScissor();
	}

	private void renderCategory(GuiGraphicsExtractor graphics, int index, ItemStack entry, int yOffset) {
		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(leftPos + 7, topPos + yOffset);

		AllGuiTextures.STOCK_KEEPER_CATEGORY_ENTRY.render(graphics, 0, 0);
		if (index > 0)
			AllGuiTextures.STOCK_KEEPER_CATEGORY_UP.render(graphics, CARD_WIDTH + 12, 2);
		if (index < schedule.size() - 1)
			AllGuiTextures.STOCK_KEEPER_CATEGORY_DOWN.render(graphics, CARD_WIDTH + 12, 11);

		graphics.item(entry, 14, 1);
		Component name = entry.isEmpty()
			? CreateLang.translate("gui.stock_ticker.empty_category_name_placeholder").component()
			: Component.literal(shortName(entry.getHoverName().getString()));
		graphics.text(font, name, 35, 5, 0x656565, false);
		pose.popMatrix();
	}

	private static String shortName(String name) {
		return name.length() <= 20 ? name : name.substring(0, 20).stripTrailing() + "...";
	}

	private boolean action(GuiGraphicsExtractor graphics, double mouseX, double mouseY, int click) {
		if (editingItem != null)
			return false;

		int x = (int) mouseX - leftPos - 20;
		int y = (int) mouseY - topPos - 24 + (int) scroll.getValue(0);
		if (x < 0 || x >= 196 || y < 0)
			return false;

		for (int i = 0; i < schedule.size(); i++) {
			if (y >= CARD_HEIGHT) {
				y -= CARD_HEIGHT;
				continue;
			}

			ItemStack entry = schedule.get(i);
			if (x > 0 && x <= 140 && y > 0 && y <= 16) {
				tooltip(graphics, List.of(entry.isEmpty()
					? CreateLang.translate("gui.stock_ticker.empty_category_name_placeholder").component()
					: entry.getHoverName(), clickToEdit), mouseX, mouseY);
				if (click == 0)
					startEditing(i);
				return true;
			}

			if (x > 140 && x <= 156 && y > 0 && y <= 16) {
				tooltip(graphics, List.of(CreateLang.translate("gui.stock_ticker.delete_category").component()), mouseX, mouseY);
				if (click == 0) {
					if (!entry.isEmpty())
						ClientNetworkHelper.INSTANCE.sendToServer(
							new StockKeeperCategoryRefundPacket(menu.contentHolder.getBlockPos(), entry));
					schedule.remove(i);
					init();
				}
				return true;
			}

			if (x > 158 && x < 170 && y > 1 && y <= 10 && i > 0) {
				tooltip(graphics, List.of(CreateLang.translateDirect("gui.schedule.move_up")), mouseX, mouseY);
				if (click == 0) {
					schedule.remove(i);
					schedule.add(isShiftDown() ? 0 : i - 1, entry);
					init();
				}
				return true;
			}

			if (x > 158 && x < 170 && y > 10 && y <= 20 && i < schedule.size() - 1) {
				tooltip(graphics, List.of(CreateLang.translateDirect("gui.schedule.move_down")), mouseX, mouseY);
				if (click == 0) {
					schedule.remove(i);
					schedule.add(isShiftDown() ? schedule.size() : i + 1, entry);
					init();
				}
				return true;
			}
			return false;
		}

		if (x > 0 && x <= 16 && y > 0 && y <= 16) {
			tooltip(graphics, List.of(CreateLang.translate("gui.stock_ticker.new_category").component()), mouseX, mouseY);
			if (click == 0) {
				playUiSound(SoundEvents.UI_BUTTON_CLICK.value(), 1, 1);
				startEditing(-1);
			}
			return true;
		}
		return false;
	}

	private void tooltip(GuiGraphicsExtractor graphics, List<Component> tooltip, double mouseX, double mouseY) {
		if (graphics != null)
			graphics.setComponentTooltipForNextFrame(font, tooltip, (int) mouseX, (int) mouseY);
	}

	private boolean isShiftDown() {
		return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
			|| InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (action(null, event.x(), event.y(), event.button())) {
			playUiSound(SoundEvents.UI_BUTTON_CLICK.value(), 1, 1);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (editingItem == null)
			return super.keyPressed(event);

		int key = event.key();
		InputConstants.Key inputKey = InputConstants.getKey(event);
		boolean close = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER
			|| key == GLFW.GLFW_KEY_KP_ENTER
			|| minecraft.options.keyInventory.isActiveAndMatches(inputKey);
		if (close) {
			stopEditing();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (editingItem != null)
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

		float max = 40 - (3 + AllGuiTextures.STOCK_KEEPER_CATEGORY.getHeight() * SLICES);
		max += schedule.size() * CARD_HEIGHT + 24;
		float target = scroll.getChaseTarget();
		if (max > 0)
			scroll.chase(Mth.clamp(target - (float) scrollY * 12, 0, max), 0.7f, Chaser.EXP);
		else
			scroll.chase(0, 0.7f, Chaser.EXP);
		return true;
	}

	@Override
	protected void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
		GuiGameElement.of(AllBlocks.STOCK_TICKER.asStack())
			.<GuiGameElement.GuiRenderBuilder>at(leftPos + AllGuiTextures.STOCK_KEEPER_CATEGORY.getWidth() + 12,
				topPos + panelHeight - 39, -190)
			.scale(3)
			.submit(graphics);
		action(graphics, mouseX, mouseY, -1);

		if (editingItem != null && hoveredSlot instanceof SlotItemHandler && hoveredSlot.getItem().isEmpty())
			graphics.setComponentTooltipForNextFrame(font,
				List.of(CreateLang.translate("gui.stock_ticker.category_filter").component(),
					CreateLang.translate("gui.stock_ticker.category_filter_tip").style(ChatFormatting.GRAY).component()),
				mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
		int y = topPos;
		AllGuiTextures.STOCK_KEEPER_CATEGORY_HEADER.render(graphics, leftPos, y);
		y += AllGuiTextures.STOCK_KEEPER_CATEGORY_HEADER.getHeight();
		for (int i = 0; i < SLICES; i++) {
			AllGuiTextures.STOCK_KEEPER_CATEGORY.render(graphics, leftPos, y);
			y += AllGuiTextures.STOCK_KEEPER_CATEGORY.getHeight();
		}
		AllGuiTextures.STOCK_KEEPER_CATEGORY_FOOTER.render(graphics, leftPos, y);
		AllGuiTextures.STOCK_KEEPER_CATEGORY_SAYS.render(graphics, leftPos + imageWidth - 6, y + 7);

		Component blockName = menu.contentHolder.getBlockState().getBlock().getName();
		int center = leftPos + AllGuiTextures.STOCK_KEEPER_CATEGORY.getWidth() / 2;
		graphics.text(font, blockName, center - font.width(blockName) / 2, topPos + 4, 0x3D3C48, false);

		if (editingItem == null) {
			renderCategories(graphics, mouseX, mouseY, partialTick);
			return;
		}

		graphics.fillGradient(0, 0, width, height, -1072689136, -804253680);
		y = topPos - 5;
		AllGuiTextures.STOCK_KEEPER_CATEGORY_HEADER.render(graphics, leftPos, y);
		y += AllGuiTextures.STOCK_KEEPER_CATEGORY_HEADER.getHeight();
		AllGuiTextures.STOCK_KEEPER_CATEGORY_EDIT.render(graphics, leftPos, y);
		y += AllGuiTextures.STOCK_KEEPER_CATEGORY_EDIT.getHeight();
		AllGuiTextures.STOCK_KEEPER_CATEGORY_FOOTER.render(graphics, leftPos, y);
		renderPlayerInventory(graphics, leftPos + 10, topPos + 88);

		Component editorTitle = CreateLang.translate("gui.stock_ticker.category_editor").component();
		graphics.text(font, editorTitle, center - font.width(editorTitle) / 2, topPos - 1, 0x3D3C48, false);
	}

	@Override
	public void removed() {
		super.removed();
		ClientNetworkHelper.INSTANCE.sendToServer(
			new StockKeeperCategoryEditPacket(menu.contentHolder.getBlockPos(), schedule));
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}
