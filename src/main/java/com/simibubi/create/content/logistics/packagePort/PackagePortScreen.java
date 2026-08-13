package com.simibubi.create.content.logistics.packagePort;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class PackagePortScreen extends AbstractSimiContainerScreen<PackagePortMenu> {

	private final boolean frogMode;
	private final AllGuiTextures background;
	private final ItemStack icon;

	private EditBox addressBox;
	private IconButton confirmButton;
	private IconButton dontAcceptPackages;
	private IconButton acceptPackages;

	private List<Rect2i> extraAreas = Collections.emptyList();

	public PackagePortScreen(PackagePortMenu container, Inventory inv, Component title) {
		super(container, inv, title);
		background = AllGuiTextures.FROGPORT_BG;
		frogMode = container.contentHolder instanceof FrogportBlockEntity;
		icon = new ItemStack(container.contentHolder.getBlockState()
			.getBlock()
			.asItem());
	}

	@Override
	protected void init() {
		int fullHeight = background.getHeight() + AllGuiTextures.PLAYER_INVENTORY.getHeight();
		setWindowSize(background.getWidth(), fullHeight);
		super.init();
		clearWidgets();

		int x = getGuiLeft();
		int y = getGuiTop();

		Consumer<String> onTextChanged = s -> addressBox.setX(nameBoxX(s, addressBox));
		addressBox = new EditBox(font, x + 23, y - 11, background.getWidth() - 20, 10, Component.empty());
		addressBox.setBordered(false);
		addressBox.setMaxLength(25);
		addressBox.setTextColor(0xFF3D3C48);
		addressBox.setTextShadow(false);
		addressBox.setValue(menu.contentHolder.addressFilter);
		addressBox.setFocused(false);
		addressBox.setResponder(onTextChanged);
		addressBox.setX(nameBoxX(addressBox.getValue(), addressBox));
		addRenderableWidget(addressBox);

		confirmButton =
			new IconButton(x + background.getWidth() - 33, y + background.getHeight() - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> minecraft.player.closeContainer());
		addRenderableWidget(confirmButton);

		acceptPackages = new IconButton(x + 37, y + background.getHeight() - 24, AllIcons.I_SEND_AND_RECEIVE);
		acceptPackages.withCallback(() -> {
			acceptPackages.green = true;
			dontAcceptPackages.green = false;
		});
		acceptPackages.green = menu.contentHolder.acceptsPackages;
		acceptPackages.setToolTip(CreateLang.translateDirect("gui.package_port.send_and_receive"));
		addRenderableWidget(acceptPackages);

		dontAcceptPackages = new IconButton(x + 55, y + background.getHeight() - 24, AllIcons.I_SEND_ONLY);
		dontAcceptPackages.withCallback(() -> {
			acceptPackages.green = false;
			dontAcceptPackages.green = true;
		});
		dontAcceptPackages.green = !menu.contentHolder.acceptsPackages;
		dontAcceptPackages.setToolTip(CreateLang.translateDirect("gui.package_port.send_only"));
		addRenderableWidget(dontAcceptPackages);

		containerTick();
		extraAreas = ImmutableList.of(
			new Rect2i(x + imageWidth, y, background.getWidth() - imageWidth, fullHeight),
			new Rect2i(x + background.getWidth(), y + background.getHeight() - 50, 70, 60));
	}

	private int nameBoxX(String text, EditBox nameBox) {
		return getGuiLeft() + background.getWidth() / 2 - (Math.min(font.width(text), nameBox.getWidth()) + 10) / 2;
	}

	@Override
	protected void containerTick() {
		if (acceptPackages != null) {
			acceptPackages.visible = menu.contentHolder.target != null;
			dontAcceptPackages.visible = menu.contentHolder.target != null;
		}
		super.containerTick();
	}

	@Override
	protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
		int x = getGuiLeft();
		int y = getGuiTop();

		AllGuiTextures header = frogMode ? AllGuiTextures.FROGPORT_HEADER : AllGuiTextures.POSTBOX_HEADER;
		header.render(graphics, x, y - header.getHeight());
		background.render(graphics, x, y);

		String text = addressBox.getValue();
		if (!addressBox.isFocused()) {
			if (text.isEmpty()) {
				text = icon.getHoverName()
					.getString();
				graphics.text(font, text, nameBoxX(text, addressBox), y - 11, 0xFF3D3C48, false);
			}
			AllGuiTextures.FROGPORT_EDIT_NAME.render(graphics,
				nameBoxX(text, addressBox) + font.width(text) + 5, y - 14);
		}

		GuiGameElement.of(icon)
			.scale(4)
			.at(x + background.getWidth() + 6, y + background.getHeight() - 56, -200)
			.submit(graphics);

		int invX = leftPos + 30;
		int invY = topPos + background.getHeight() + 8;
		renderPlayerInventory(graphics, invX, invY);

		if (menu.contentHolder.target == null)
			return;

		int slotX = x + 13;
		int slotY = y + 58;
		AllGuiTextures.FROGPORT_SLOT.render(graphics, slotX, slotY);
		graphics.item(menu.contentHolder.target.getIcon(), slotX + 1, slotY + 1);

		if (addressBox.isHovered()) {
			graphics.setComponentTooltipForNextFrame(font,
				List.of(CreateLang.translate("gui.package_port.catch_packages")
						.component()
						.withStyle(style -> style.withColor(AbstractSimiWidget.HEADER_RGB.getRGB())),
					CreateLang.translate("gui.package_port.catch_packages_empty")
						.style(ChatFormatting.GRAY)
						.component(),
					CreateLang.translate("gui.package_port.catch_packages_wildcard")
						.style(ChatFormatting.GRAY)
						.component()),
				mouseX, mouseY);
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		boolean hitEnter = getFocused() instanceof EditBox && (event.key() == 257 || event.key() == 335);
		if (hitEnter && addressBox.isFocused()) {
			addressBox.setFocused(false);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		ClientNetworkHelper.INSTANCE.sendToServer(new PackagePortConfigurationPacket(menu.contentHolder.getBlockPos(),
			addressBox.getValue(), acceptPackages.green));
		super.removed();
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}
