package com.simibubi.create.foundation.gui.widget;

import java.util.List;

import net.createmod.catnip.api.client.gui.widget.AbstractSimiWidget;
import net.minecraft.network.chat.Component;

public class TooltipArea extends AbstractSimiWidget {

	public TooltipArea(int x, int y, int width, int height) {
		super(x, y, width, height);
	}

	public TooltipArea withTooltip(List<Component> tooltip) {
		this.toolTip = tooltip;
		return this;
	}

}
