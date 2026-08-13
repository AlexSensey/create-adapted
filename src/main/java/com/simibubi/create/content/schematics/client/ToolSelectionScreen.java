package com.simibubi.create.content.schematics.client;

import java.util.List;
import java.util.function.Consumer;

import com.simibubi.create.AllKeys;
import com.simibubi.create.content.schematics.client.tools.ToolType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public class ToolSelectionScreen {

	private static final Component SCROLL_TO_CYCLE = CreateLang.translateDirect("gui.toolmenu.cycle");
	private static final String HOLD_TO_FOCUS = "gui.toolmenu.focusKey";

	protected final List<ToolType> tools;
	protected final Consumer<ToolType> callback;
	public boolean focused;
	private float yOffset;
	protected int selection;
	private final int menuWidth;

	public ToolSelectionScreen(List<ToolType> tools, Consumer<ToolType> callback) {
		this.tools = tools;
		this.callback = callback;
		menuWidth = Math.max(tools.size() * 50 + 30, 220);
		if (!tools.isEmpty())
			callback.accept(tools.getFirst());
	}

	public void setSelectedElement(ToolType tool) {
		if (tools.contains(tool))
			selection = tools.indexOf(tool);
	}

	public void cycle(int direction) {
		if (tools.isEmpty())
			return;
		selection = Math.floorMod(selection + (direction < 0 ? 1 : -1), tools.size());
	}

	public void update() {
		if (focused)
			yOffset += (10 - yOffset) * .1f;
		else
			yOffset *= .9f;
	}

	public void renderPassive(GuiGraphicsExtractor graphics, float partialTicks) {
		if (tools.isEmpty())
			return;
		Minecraft minecraft = Minecraft.getInstance();
		int x = (graphics.guiWidth() - menuWidth) / 2 + 15;
		int y = graphics.guiHeight() - 30 - 75 - Math.round(yOffset);
		AllGuiTextures background = AllGuiTextures.HUD_BACKGROUND;
		int backgroundAlpha = focused ? 0xE0000000 : 0x80000000;

		graphics.blit(RenderPipelines.GUI_TEXTURED, background.getLocation(), x - 15, y,
			background.getStartX(), background.getStartY(), menuWidth, 30,
			background.getWidth(), background.getHeight(), 256, 256, backgroundAlpha);

		float tooltipAlpha = yOffset / 10;
		if (tooltipAlpha > .25f) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, background.getLocation(), x - 15, y + 33,
				background.getStartX(), background.getStartY(), menuWidth, 52,
				background.getWidth(), background.getHeight(), 256, 256,
				((int) (tooltipAlpha * 0xC0) << 24) | 0xFFFFFF);
			List<Component> description = tools.get(selection)
				.getDescription();
			int alpha = (int) (tooltipAlpha * 0xFF) << 24;
			if (!description.isEmpty())
				graphics.text(minecraft.font, description.get(0), x - 10, y + 38, alpha | 0xEEEEEE, false);
			if (description.size() > 1)
				graphics.text(minecraft.font, description.get(1), x - 10, y + 50, alpha | 0xCCDDFF, false);
			if (description.size() > 2)
				graphics.text(minecraft.font, description.get(2), x - 10, y + 60, alpha | 0xCCDDFF, false);
			if (description.size() > 3)
				graphics.text(minecraft.font, description.get(3), x - 10, y + 72, alpha | 0xCCCCDD, false);
		}

		if (tools.size() > 1) {
			Component hint = focused ? SCROLL_TO_CYCLE
				: CreateLang.translateDirect(HOLD_TO_FOCUS, AllKeys.TOOL_MENU.getBoundKey());
			graphics.centeredText(minecraft.font, hint, graphics.guiWidth() / 2, y - 10, 0xFFCCDDFF);
		} else {
			x += 65;
		}

		for (int i = 0; i < tools.size(); i++) {
			boolean selected = i == selection;
			int iconY = y + (selected ? 1 : 11);
			int alpha = selected || focused ? 0xFF : 0x33;
			tools.get(i)
				.getIcon()
				.render(graphics, x + i * 50 + 16, iconY + 1, alpha << 24);
			tools.get(i)
				.getIcon()
				.render(graphics, x + i * 50 + 16, iconY, (alpha << 24) | 0xFFFFFF);
			if (selected)
				graphics.centeredText(minecraft.font, tools.get(i)
					.getDisplayName(), x + i * 50 + 24, y + 28, 0xFFCCDDFF);
		}
	}

	public void onClose() {
		focused = false;
		if (!tools.isEmpty())
			callback.accept(tools.get(selection));
	}
}
