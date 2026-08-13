package com.simibubi.create.content.equipment.zapper;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ZapperScreen extends AbstractSimiScreen {

	protected final Component patternSection = CreateLang.translateDirect("gui.terrainzapper.patternSection");
	protected final AllGuiTextures background;
	protected final ItemStack zapper;
	protected final InteractionHand hand;
	protected final List<IconButton> patternButtons = new ArrayList<>(6);
	protected PlacementPatterns currentPattern;
	protected Component screenTitle = CommonComponents.EMPTY;
	protected int fontColor = AllGuiTextures.FONT_COLOR;
	protected int left;
	protected int top;

	protected ZapperScreen(AllGuiTextures background, ItemStack zapper, InteractionHand hand) {
		super(zapper.getHoverName());
		this.background = background;
		this.zapper = zapper;
		this.hand = hand;
		currentPattern = zapper.getOrDefault(AllDataComponents.PLACEMENT_PATTERN, PlacementPatterns.Solid);
	}

	protected void layout() {
		left = (width - background.getWidth()) / 2 - 10;
		top = (height - background.getHeight()) / 2;
	}

	@Override
	protected void init() {
		layout();
		super.init();
		IconButton confirm = new IconButton(left + background.getWidth() - 33,
			top + background.getHeight() - 24, AllIcons.I_CONFIRM);
		confirm.withCallback(this::onClose);
		addRenderableWidget(confirm);

		patternButtons.clear();
		PlacementPatterns[] patterns = PlacementPatterns.values();
		for (int id = 0; id < Math.min(6, patterns.length); id++) {
			PlacementPatterns pattern = patterns[id];
			IconButton button = new IconButton(left + background.getWidth() - 76 + id % 3 * 18,
				top + 21 + id / 3 * 18, pattern.icon);
			button.withCallback(() -> {
				patternButtons.forEach(b -> b.green = false);
				button.green = true;
				currentPattern = pattern;
			});
			button.setToolTip(CreateLang.translateDirect("gui.terrainzapper.pattern." + pattern.translationKey));
			patternButtons.add(button);
			addRenderableWidget(button);
		}
		if (currentPattern.ordinal() < patternButtons.size())
			patternButtons.get(currentPattern.ordinal()).green = true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		layout();
		background.render(graphics, left, top);
		drawOnBackground(graphics, left, top);
		BlockState selected = zapper.getOrDefault(AllDataComponents.SHAPER_BLOCK_USED,
			Blocks.AIR.defaultBlockState());
		ItemStack selectedStack = new ItemStack(selected.getBlock());
		if (!selectedStack.isEmpty())
			GuiGameElement.of(selectedStack).scale(1.25)
				.at(left + 22, top + 29, 120)
				.submit(graphics);
		GuiGameElement.of(zapper).scale(4)
			// GUI items are anchored at their top-left in 26.2; the legacy renderer was
			// centred here, so compensate instead of placing most of it off-screen.
			.at(left + background.getWidth() - 48, top + background.getHeight() - 54, 100)
			.submit(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	protected void drawOnBackground(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.text(font, screenTitle, x + (background.getWidth() - font.width(screenTitle)) / 2,
			y + 4, 0xff54214f, false);
		graphics.text(font, patternSection, x + background.getWidth() - 77, y + 11,
			0xff767676, false);
	}

	@Override
	public void removed() {
		ConfigureZapperPacket packet = getConfigurationPacket();
		packet.configureZapper(zapper);
		ClientNetworkHelper.INSTANCE.sendToServer(packet);
		super.removed();
	}

	protected abstract ConfigureZapperPacket getConfigurationPacket();
}
