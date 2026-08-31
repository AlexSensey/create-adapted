package net.createmod.catnip.api.client.gui;

import java.util.List;

import net.createmod.catnip.impl.client.mixin.ScreenAccessor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class AbstractSimiScreen extends Screen implements CatnipScreenExtensions {
	protected AbstractSimiScreen(Component title) {
		super(title);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnE() {
		return true;
	}

	protected final List<Renderable> getRenderables() {
		return ((ScreenAccessor) this).catnip$getRenderables();
	}
}
