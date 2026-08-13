package com.simibubi.create.content.redstone.thresholdSwitch;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity.ThresholdType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;

import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.ponder.impl.client.gui.PonderTagScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ThresholdSwitchScreen extends AbstractSimiScreen {
	private static final Identifier REDSTONE_TORCH_ON =
		Identifier.fromNamespaceAndPath("minecraft", "textures/block/redstone_torch.png");
	private static final Identifier REDSTONE_TORCH_OFF =
		Identifier.fromNamespaceAndPath("minecraft", "textures/block/redstone_torch_off.png");

	private ScrollInput offBelow;
	private ScrollInput onAbove;
	private SelectionScrollInput inStacks;

	private IconButton flipSignals;

	private final Component invertSignal = CreateLang.translateDirect("gui.threshold_switch.invert_signal");
	private final ItemStack renderedItem = AllBlocks.THRESHOLD_SWITCH.asStack();
	private final AllGuiTextures background = AllGuiTextures.THRESHOLD_SWITCH;
	private final ThresholdSwitchBlockEntity blockEntity;

	private int lastModification;
	private int left;
	private int top;

	public ThresholdSwitchScreen(ThresholdSwitchBlockEntity be) {
		super(CreateLang.translateDirect("gui.threshold_switch.title"));
		blockEntity = be;
		lastModification = -1;
	}

	@Override
	protected void init() {
		layout();
		super.init();
		clearWidgets();

		int x = left;
		int y = top;

		inStacks = (SelectionScrollInput) new SelectionScrollInput(x + 100, y + 23, 52, 42)
			.forOptions(List.of(CreateLang.translateDirect("schedule.condition.threshold.items"),
				CreateLang.translateDirect("schedule.condition.threshold.stacks")))
			.titled(CreateLang.translateDirect("schedule.condition.threshold.item_measure"))
			.setState(blockEntity.inStacks ? 1 : 0);

		offBelow = new ScrollInput(x + 48, y + 47, 1, 18)
			.withRange(blockEntity.getMinLevel(), blockEntity.getMaxLevel() + 1 - getValueStep())
			.titled(CreateLang.translateDirect("gui.threshold_switch.lower_threshold"))
			.calling(state -> {
				lastModification = 0;
				int valueStep = getValueStep();
				if (onAbove.getState() / valueStep == 0 && state / valueStep == 0)
					return;
				if (onAbove.getState() / valueStep <= state / valueStep) {
					onAbove.setState((state + valueStep) / valueStep * valueStep);
					onAbove.onChanged();
				}
			})
			.withStepFunction(context -> context.shift ? 10 * getValueStep() : getValueStep())
			.setState(blockEntity.offWhenBelow);

		onAbove = new ScrollInput(x + 48, y + 23, 1, 18)
			.withRange(blockEntity.getMinLevel() + getValueStep(), blockEntity.getMaxLevel() + 1)
			.titled(CreateLang.translateDirect("gui.threshold_switch.upper_threshold"))
			.calling(state -> {
				lastModification = 0;
				int valueStep = getValueStep();
				if (offBelow.getState() / valueStep == 0 && state / valueStep == 0)
					return;
				if (offBelow.getState() / valueStep >= state / valueStep) {
					offBelow.setState((state - valueStep) / valueStep * valueStep);
					offBelow.onChanged();
				}
			})
			.withStepFunction(context -> context.shift ? 10 * getValueStep() : getValueStep())
			.setState(blockEntity.onWhenAbove);

		onAbove.onChanged();
		offBelow.onChanged();
		addRenderableWidget(onAbove);
		addRenderableWidget(offBelow);
		addRenderableWidget(inStacks);

		IconButton confirmButton =
			new IconButton(x + background.getWidth() - 33, y + background.getHeight() - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(this::onClose);
		addRenderableWidget(confirmButton);

		flipSignals =
			new IconButton(x + background.getWidth() - 62, y + background.getHeight() - 24, AllIcons.I_FLIP);
		flipSignals.withCallback(() -> send(!blockEntity.isInverted()));
		flipSignals.setToolTip(invertSignal);
		addRenderableWidget(flipSignals);

		updateInputBoxes();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		layout();
		renderWindow(graphics, mouseX, mouseY);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		renderTooltips(graphics, mouseX, mouseY);
	}

	private void renderWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int x = left;
		int y = top;
		background.render(graphics, x, y);
		graphics.text(font, title, x + background.getWidth() / 2 - font.width(title) / 2, y + 4,
			0xFF592424, false);

		ThresholdType type = blockEntity.getTypeOfCurrentTarget();
		boolean forItems = type == ThresholdType.ITEM;
		AllGuiTextures inputBackground = forItems ? AllGuiTextures.THRESHOLD_SWITCH_ITEMCOUNT_INPUTS
			: AllGuiTextures.THRESHOLD_SWITCH_MISC_INPUTS;
		inputBackground.render(graphics, x + 44, y + 21);
		inputBackground.render(graphics, x + 44, y + 45);

		int valueStep = type == ThresholdType.FLUID ? 1000 : 1;
		boolean stacks = inStacks != null && inStacks.getState() == 1;
		if (forItems) {
			Component suffix = CreateLang.translateDirect(stacks ? "schedule.condition.threshold.stacks"
				: "schedule.condition.threshold.items");
			valueStep = stacks ? 64 : 1;
			graphics.text(font, suffix, x + 105, y + 28, 0xFFFFFFFF, true);
			graphics.text(font, suffix, x + 105, y + 52, 0xFFFFFFFF, true);
		}

		String upper = type == ThresholdType.UNSUPPORTED ? ""
			: forItems ? Integer.toString(onAbove.getState() / valueStep)
				: blockEntity.format(onAbove.getState() / valueStep, stacks).getString();
		String lower = type == ThresholdType.UNSUPPORTED ? ""
			: forItems ? Integer.toString(offBelow.getState() / valueStep)
				: blockEntity.format(offBelow.getState() / valueStep, stacks).getString();
		graphics.text(font, Component.literal("\u2265 " + upper), x + 53, y + 28, 0xFFFFFFFF, true);
		graphics.text(font, Component.literal("\u2264 " + lower), x + 53, y + 52, 0xFFFFFFFF, true);

		GuiGameElement.of(renderedItem)
			.scale(5)
			.at(x + background.getWidth() + 6, y + background.getHeight() - 56, -200)
			.submit(graphics);

		int itemX = x + 13;
		int itemY = y + 80;
		ItemStack displayItem = blockEntity.getDisplayItemForScreen();
		graphics.item(displayItem.isEmpty() ? new ItemStack(Items.BARRIER) : displayItem, itemX, itemY);

		int torchX = x + 23;
		int torchY = y + 24;
		boolean highlightTopRow = blockEntity.isInverted() ^ blockEntity.isPowered();
		AllGuiTextures.THRESHOLD_SWITCH_CURRENT_STATE.render(graphics, torchX - 3,
			torchY - 4 + (highlightTopRow ? 0 : 24));

		for (boolean power : new boolean[] { true, false }) {
			boolean lit = blockEntity.isInverted() ^ power;
			int iconY = torchY + (power ? 0 : 24);
			graphics.blit(RenderPipelines.GUI_TEXTURED, lit ? REDSTONE_TORCH_ON : REDSTONE_TORCH_OFF,
				torchX, iconY, 0, 0, 16, 16, 16, 16);
		}
	}

	private void renderTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int itemX = left + 13;
		int itemY = top + 80;
		if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
			List<Component> tooltip = createTargetTooltip();
			graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
			return;
		}

		int torchX = left + 23;
		int torchY = top + 24;
		for (boolean power : new boolean[] { true, false }) {
			int currentY = power ? torchY : torchY + 24;
			if (mouseX >= torchX && mouseX < torchX + 16 && mouseY >= currentY && mouseY < currentY + 16) {
				graphics.setComponentTooltipForNextFrame(font,
					List.of(CreateLang.translate(power ^ blockEntity.isInverted()
							? "gui.threshold_switch.power_on_when" : "gui.threshold_switch.power_off_when")
						.component()
						.withStyle(style -> style.withColor(0x5391E1))), mouseX, mouseY);
				return;
			}
		}
	}

	private List<Component> createTargetTooltip() {
		ArrayList<Component> tooltip = new ArrayList<>();
		ItemStack displayItem = blockEntity.getDisplayItemForScreen();
		ThresholdType type = blockEntity.getTypeOfCurrentTarget();
		if (displayItem.isEmpty()) {
			tooltip.add(CreateLang.translateDirect("gui.threshold_switch.not_attached"));
			tooltip.add(CreateLang.translateDirect("display_link.view_compatible")
				.withStyle(ChatFormatting.DARK_GRAY));
			return tooltip;
		}

		tooltip.add(displayItem.getHoverName());
		if (type == ThresholdType.UNSUPPORTED) {
			tooltip.add(CreateLang.translateDirect("gui.threshold_switch.incompatible")
				.withStyle(ChatFormatting.GRAY));
			tooltip.add(CreateLang.translateDirect("display_link.view_compatible")
				.withStyle(ChatFormatting.DARK_GRAY));
			return tooltip;
		}

		int valueStep = getValueStep();
		boolean stacks = inStacks.getState() == 1;
		tooltip.add(CreateLang.translate("gui.threshold_switch.currently",
				blockEntity.format(blockEntity.currentLevel / valueStep, stacks))
			.style(ChatFormatting.DARK_AQUA)
			.component());
		if (blockEntity.currentMinLevel / valueStep == 0)
			tooltip.add(CreateLang.translate("gui.threshold_switch.range_max",
					blockEntity.format(blockEntity.currentMaxLevel / valueStep, stacks))
				.style(ChatFormatting.GRAY)
				.component());
		else
			tooltip.add(CreateLang.translate("gui.threshold_switch.range", blockEntity.currentMinLevel / valueStep,
					blockEntity.format(blockEntity.currentMaxLevel / valueStep, stacks))
				.style(ChatFormatting.GRAY)
				.component());
		tooltip.add(CreateLang.translateDirect("display_link.view_compatible")
			.withStyle(ChatFormatting.DARK_GRAY));
		return tooltip;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int itemX = left + 13;
		int itemY = top + 80;
		if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
			ScreenOpener.transitionTo(new PonderTagScreen(AllCreatePonderTags.THRESHOLD_SWITCH_TARGETS));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scroll(onAbove, mouseX, mouseY, scrollX, scrollY)
			|| scroll(offBelow, mouseX, mouseY, scrollX, scrollY)
			|| scroll(inStacks, mouseX, mouseY, scrollX, scrollY))
			return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private boolean scroll(ScrollInput input, double mouseX, double mouseY, double scrollX, double scrollY) {
		return input != null && input.visible && input.active && input.isMouseOver(mouseX, mouseY)
			&& input.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void tick() {
		super.tick();
		if (lastModification >= 0)
			lastModification++;
		if (lastModification >= 20) {
			lastModification = -1;
			send(blockEntity.isInverted());
		}
		if (inStacks != null)
			updateInputBoxes();
	}

	private void updateInputBoxes() {
		ThresholdType type = blockEntity.getTypeOfCurrentTarget();
		boolean forItems = type == ThresholdType.ITEM;
		int valueStep = getValueStep();
		inStacks.active = inStacks.visible = forItems;
		onAbove.setWidth(forItems ? 48 : 103);
		offBelow.setWidth(forItems ? 48 : 103);
		onAbove.visible = type != ThresholdType.UNSUPPORTED;
		offBelow.visible = type != ThresholdType.UNSUPPORTED;

		int min = blockEntity.currentMinLevel + valueStep;
		int max = blockEntity.currentMaxLevel;
		if (max < min)
			return;
		onAbove.withRange(min, max + 1);
		int roundedState = Mth.clamp(onAbove.getState() / valueStep * valueStep, min, max);
		if (roundedState != onAbove.getState()) {
			onAbove.setState(roundedState);
			onAbove.onChanged();
		}

		min = blockEntity.currentMinLevel;
		max = blockEntity.currentMaxLevel - valueStep;
		if (max < min)
			return;
		offBelow.withRange(min, max + 1);
		roundedState = Mth.clamp(offBelow.getState() / valueStep * valueStep, min, max);
		if (roundedState != offBelow.getState()) {
			offBelow.setState(roundedState);
			offBelow.onChanged();
		}
	}

	private int getValueStep() {
		boolean stacks = inStacks != null && inStacks.getState() == 1;
		if (blockEntity.getTypeOfCurrentTarget() == ThresholdType.FLUID)
			return 1000;
		return stacks ? 64 : 1;
	}

	@Override
	public void removed() {
		if (offBelow != null && onAbove != null && inStacks != null)
			send(blockEntity.isInverted());
		super.removed();
	}

	private void send(boolean invert) {
		ClientNetworkHelper.INSTANCE.sendToServer(new ConfigureThresholdSwitchPacket(blockEntity.getBlockPos(),
			offBelow.getState(), onAbove.getState(), invert, inStacks.getState() == 1));
	}

	private void layout() {
		left = (width - background.getWidth()) / 2 - 20;
		top = (height - background.getHeight()) / 2;
	}
}
