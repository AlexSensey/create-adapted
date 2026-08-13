package com.simibubi.create.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class DoubleItemIcon implements IDrawable {

	private Supplier<ItemStack> primarySupplier;
	private Supplier<ItemStack> secondarySupplier;
	private ItemStack primaryStack;
	private ItemStack secondaryStack;

	public DoubleItemIcon(Supplier<ItemStack> primary, Supplier<ItemStack> secondary) {
		this.primarySupplier = primary;
		this.secondarySupplier = secondary;
	}

	@Override
	public int getWidth() {
		return 18;
	}

	@Override
	public int getHeight() {
		return 18;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		if (primaryStack == null) {
			primaryStack = primarySupplier.get();
			secondaryStack = secondarySupplier.get();
		}

		GuiGameElement.of(primaryStack)
			.at(xOffset + 1, yOffset + 1)
			.submit(graphics);
		GuiGameElement.of(secondaryStack)
			.at(xOffset + 10, yOffset + 10)
			.scale(.5f)
			.submit(graphics);
	}

}
