package com.simibubi.create.content.logistics.filter;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

import java.util.Collections;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket.Option;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.item.TooltipHelper;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.lang.FontHelper.Palette;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractFilterScreen<F extends AbstractFilterMenu> extends AbstractSimiContainerScreen<F> {

	protected AllGuiTextures background;
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton resetButton;
	private IconButton confirmButton;

	protected AbstractFilterScreen(F menu, Inventory inv, Component title, AllGuiTextures background) {
		super(menu, inv, title);
		this.background = background;
	}

	@Override
	protected void init() {
		setWindowSize(Math.max(background.getWidth(), PLAYER_INVENTORY.getWidth()),
			background.getHeight() + 4 + PLAYER_INVENTORY.getHeight());
		super.init();

		int x = leftPos;
		int y = topPos;

		resetButton = new IconButton(x + background.getWidth() - 62, y + background.getHeight() - 24, AllIcons.I_TRASH);
		resetButton.withCallback(() -> {
			menu.clearContents();
			contentsCleared();
			menu.sendClearPacket();
		});
		confirmButton =
			new IconButton(x + background.getWidth() - 33, y + background.getHeight() - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> minecraft.player.closeContainer());

		addRenderableWidgets(resetButton, confirmButton);
		extraAreas =
			List.of(new Rect2i(x + background.getWidth(), y + background.getHeight() - 40, 80, 48));
	}

	@Override
	protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
		int invX = getLeftOfCentered(PLAYER_INVENTORY.getWidth());
		int invY = topPos + background.getHeight() + 4;
		renderPlayerInventory(graphics, invX, invY);

		int x = leftPos;
		int y = topPos;
		background.render(graphics, x, y);
		graphics.text(font, title, x + (background.getWidth() - 8) / 2 - font.width(title) / 2, y + 4,
			opaque(getTitleColor()), false);

		GuiGameElement.of(menu.contentHolder)
			.<GuiGameElement.GuiRenderBuilder>at(x + background.getWidth() + 8,
				y + background.getHeight() - 52, -200)
			.scale(4)
			.submit(graphics);
	}

	protected int getTitleColor() {
		return 0x592424;
	}

	@Override
	protected void containerTick() {
		if (!ItemStack.isSameItemSameComponents(menu.player.getMainHandItem(), menu.contentHolder)) {
			menu.player.closeContainer();
			return;
		}

		super.containerTick();
		handleTooltips();
		handleIndicators();
	}

	protected void handleTooltips() {
		List<IconButton> tooltipButtons = getTooltipButtons();
		boolean shiftDown = isShiftDown();

		for (IconButton button : tooltipButtons) {
			if (button.getToolTip()
				.isEmpty())
				continue;
			button.setToolTip(button.getToolTip()
				.get(0));
			button.getToolTip()
				.add(TooltipHelper.holdShift(Palette.YELLOW, shiftDown));
		}

		if (!shiftDown)
			return;
		List<MutableComponent> tooltipDescriptions = getTooltipDescriptions();
		for (int i = 0; i < tooltipButtons.size() && i < tooltipDescriptions.size(); i++)
			fillToolTip(tooltipButtons.get(i), tooltipDescriptions.get(i));
	}

	public void handleIndicators() {
		for (IconButton button : getTooltipButtons())
			button.green = !isButtonEnabled(button);
	}

	protected abstract boolean isButtonEnabled(IconButton button);

	protected List<IconButton> getTooltipButtons() {
		return Collections.emptyList();
	}

	protected List<MutableComponent> getTooltipDescriptions() {
		return Collections.emptyList();
	}

	private void fillToolTip(IconButton button, Component tooltip) {
		if (!button.isHoveredOrFocused())
			return;
		button.getToolTip()
			.addAll(TooltipHelper.cutTextComponent(tooltip, Palette.ALL_GRAY));
	}

	private boolean isShiftDown() {
		return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
			|| InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	private static int opaque(int color) {
		return (color & 0xff000000) == 0 ? color | 0xff000000 : color;
	}

	protected void contentsCleared() {}

	protected void sendOptionUpdate(Option option) {
		ClientNetworkHelper.INSTANCE.sendToServer(new FilterScreenPacket(option));
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}
