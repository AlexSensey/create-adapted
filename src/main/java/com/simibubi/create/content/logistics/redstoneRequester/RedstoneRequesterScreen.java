package com.simibubi.create.content.logistics.redstoneRequester;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.SlotItemHandler;

public class RedstoneRequesterScreen extends AbstractSimiContainerScreen<RedstoneRequesterMenu> {

	private AddressEditBox addressBox;
	private IconButton confirmButton;
	private List<Rect2i> extraAreas = Collections.emptyList();
	private final List<Integer> amounts = new ArrayList<>();

	private IconButton dontAllowPartial;
	private IconButton allowPartial;

	public RedstoneRequesterScreen(RedstoneRequesterMenu container, Inventory inv, Component title) {
		super(container, inv, title);

		for (int i = 0; i < 9; i++)
			amounts.add(1);

		List<BigItemStack> stacks = menu.contentHolder.encodedRequest.stacks();
		for (int i = 0; i < stacks.size() && i < amounts.size(); i++)
			amounts.set(i, Math.max(1, stacks.get(i).count));
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (addressBox != null)
			addressBox.tick();
		for (int i = 0; i < amounts.size(); i++)
			if (menu.ghostInventory.getStackInSlot(i)
				.isEmpty())
				amounts.set(i, 1);
	}

	@Override
	protected void init() {
		int bgHeight = AllGuiTextures.REDSTONE_REQUESTER.getHeight();
		int bgWidth = AllGuiTextures.REDSTONE_REQUESTER.getWidth();
		setWindowSize(bgWidth, bgHeight + AllGuiTextures.PLAYER_INVENTORY.getHeight());
		setWindowOffset(-45, 0);
		super.init();
		clearWidgets();
		int x = getGuiLeft();
		int y = getGuiTop();

		if (addressBox == null) {
			addressBox = new AddressEditBox(this, new NoShadowFontWrapper(font), x + 55, y + 68, 110, 10, false);
			addressBox.setValue(menu.contentHolder.encodedTargetAdress);
			addressBox.setTextColor(0xFF555555);
			addressBox.setTextShadow(false);
		}
		addRenderableWidget(addressBox);

		confirmButton = new IconButton(x + bgWidth - 30, y + bgHeight - 25, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> minecraft.player.closeContainer());
		addRenderableWidget(confirmButton);

		allowPartial = new IconButton(x + 12, y + bgHeight - 25, AllIcons.I_PARTIAL_REQUESTS);
		allowPartial.withCallback(() -> {
			allowPartial.green = true;
			dontAllowPartial.green = false;
		});
		allowPartial.green = menu.contentHolder.allowPartialRequests;
		allowPartial.setToolTip(CreateLang.translateDirect("gui.redstone_requester.allow_partial"));
		addRenderableWidget(allowPartial);

		dontAllowPartial = new IconButton(x + 30, y + bgHeight - 25, AllIcons.I_FULL_REQUESTS);
		dontAllowPartial.withCallback(() -> {
			allowPartial.green = false;
			dontAllowPartial.green = true;
		});
		dontAllowPartial.green = !menu.contentHolder.allowPartialRequests;
		dontAllowPartial.setToolTip(CreateLang.translateDirect("gui.redstone_requester.dont_allow_partial"));
		addRenderableWidget(dontAllowPartial);

		extraAreas = List.of(new Rect2i(x + bgWidth, y + bgHeight - 50, 70, 60));
	}

	@Override
	protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
		int x = getGuiLeft();
		int y = getGuiTop();
		AllGuiTextures.REDSTONE_REQUESTER.render(graphics, x + 3, y);
		renderPlayerInventory(graphics, x - 3, y + 124);

		ItemStack stack = AllBlocks.REDSTONE_REQUESTER.asStack();
		Component screenTitle = stack.getHoverName();
		graphics.text(font, screenTitle, x + 117 - font.width(screenTitle) / 2, y + 4, 0x3D3C48, false);

		GuiGameElement.of(stack)
			.scale(3)
			.at(x + 245, y + 80, -200)
			.submit(graphics);
	}

	@Override
	protected void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
		int x = getGuiLeft();
		int y = getGuiTop();

		for (int i = 0; i < amounts.size(); i++) {
			int inputX = x + 27 + i * 20;
			int inputY = y + 28;
			ItemStack itemStack = menu.ghostInventory.getStackInSlot(i);
			if (!itemStack.isEmpty())
				graphics.itemDecorations(font, itemStack, inputX, inputY, Integer.toString(amounts.get(i)));
		}

		if (addressBox.isHovered() && !addressBox.isFocused()) {
			if (addressBox.getValue()
				.isBlank())
				graphics.setComponentTooltipForNextFrame(font,
					List.of(CreateLang.translate("gui.redstone_requester.requester_address")
							.component()
							.withStyle(style -> style.withColor(ScrollInput.HEADER_RGB.getRGB())),
						CreateLang.translate("gui.redstone_requester.requester_address_tip")
							.style(ChatFormatting.GRAY)
							.component(),
						CreateLang.translate("gui.redstone_requester.requester_address_tip_1")
							.style(ChatFormatting.GRAY)
							.component(),
						CreateLang.translate("gui.schedule.lmb_edit")
							.style(ChatFormatting.DARK_GRAY)
							.style(ChatFormatting.ITALIC)
							.component()),
					mouseX, mouseY);
			else
				graphics.setComponentTooltipForNextFrame(font,
					List.of(CreateLang.translate("gui.redstone_requester.requester_address_given")
							.component()
							.withStyle(style -> style.withColor(ScrollInput.HEADER_RGB.getRGB())),
						CreateLang.text("'" + addressBox.getValue() + "'")
							.style(ChatFormatting.GRAY)
							.component()),
					mouseX, mouseY);
			return;
		}

		if (!(hoveredSlot instanceof SlotItemHandler) || hoveredSlot.getItem().isEmpty())
			return;
		int slotIndex = hoveredSlot.getSlotIndex();
		if (slotIndex < 0 || slotIndex >= amounts.size())
			return;
		ItemStack stack = hoveredSlot.getItem();
		graphics.setComponentTooltipForNextFrame(font,
			List.of(CreateLang.translate("gui.factory_panel.send_item",
					CreateLang.itemName(stack)
						.add(CreateLang.text(" x" + amounts.get(slotIndex))))
					.component()
					.withStyle(style -> style.withColor(ScrollInput.HEADER_RGB.getRGB())),
				CreateLang.translate("gui.factory_panel.scroll_to_change_amount")
					.style(ChatFormatting.DARK_GRAY)
					.style(ChatFormatting.ITALIC)
					.component(),
				CreateLang.translate("gui.scrollInput.shiftScrollsFaster")
					.style(ChatFormatting.DARK_GRAY)
					.style(ChatFormatting.ITALIC)
					.component()),
			mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int x = getGuiLeft();
		int y = getGuiTop();

		if (addressBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
			return true;

		for (int i = 0; i < amounts.size(); i++) {
			int inputX = x + 27 + i * 20;
			int inputY = y + 28;
			if (mouseX >= inputX && mouseX < inputX + 16 && mouseY >= inputY && mouseY < inputY + 16) {
				if (menu.ghostInventory.getStackInSlot(i)
					.isEmpty())
					return true;
				amounts.set(i,
					Mth.clamp((int) (amounts.get(i) + Math.signum(scrollY) * (isShiftDown() ? 10 : 1)), 1, 256));
				return true;
			}
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private boolean isShiftDown() {
		return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
			|| InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

	@Override
	public void removed() {
		if (addressBox != null && allowPartial != null)
			ClientNetworkHelper.INSTANCE.sendToServer(new RedstoneRequesterConfigurationPacket(
				menu.contentHolder.getBlockPos(), addressBox.getValue(), allowPartial.green, amounts));
		super.removed();
	}
}
