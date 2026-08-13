package com.simibubi.create.content.logistics.filter;

import org.lwjgl.glfw.GLFW;

import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket.Option;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.widget.IconButton;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PackageFilterScreen extends AbstractFilterScreen<PackageFilterMenu> {

	private AddressEditBox addressBox;
	private boolean deferFocus;

	public PackageFilterScreen(PackageFilterMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, AllGuiTextures.PACKAGE_FILTER);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (deferFocus) {
			deferFocus = false;
			setFocused(addressBox);
		}
		if (addressBox != null)
			addressBox.tick();
	}

	@Override
	protected void init() {
		setWindowOffset(-11, 7);
		super.init();

		int x = leftPos;
		int y = topPos;
		addressBox = new AddressEditBox(this, new NoShadowFontWrapper(font), x + 44, y + 28, 129, 9, false);
		addressBox.setTextColor(0xFFFFFFFF);
		addressBox.setTextShadow(false);
		addressBox.setValue(menu.address);
		addressBox.setResponder(this::onAddressEdited);
		addRenderableWidget(addressBox);
		setFocused(addressBox);
	}

	@Override
	protected void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		GuiGameElement.of(PackageStyles.getDefaultBox())
			.at(leftPos + 16, topPos + 23, -100)
			.submit(graphics);
		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
	}

	public void onAddressEdited(String address) {
		menu.address = address;
		CompoundTag tag = new CompoundTag();
		tag.putString("Address", address);
		ClientNetworkHelper.INSTANCE.sendToServer(new FilterScreenPacket(Option.UPDATE_ADDRESS, tag));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (addressBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
			return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)
			setFocused(null);
		return super.keyPressed(event);
	}

	@Override
	protected void contentsCleared() {
		addressBox.setValue("");
		deferFocus = true;
	}

	@Override
	protected boolean isButtonEnabled(IconButton button) {
		return false;
	}

	@Override
	protected int getTitleColor() {
		return 0x3D3C48;
	}
}
