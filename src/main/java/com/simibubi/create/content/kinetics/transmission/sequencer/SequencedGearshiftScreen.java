package com.simibubi.create.content.kinetics.transmission.sequencer;

import java.util.Collection;
import java.util.Vector;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.computercraft.ComputerScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class SequencedGearshiftScreen extends AbstractSimiScreen {

	private final ItemStack renderedItem = AllBlocks.SEQUENCED_GEARSHIFT.asStack();
	private final AllGuiTextures background = AllGuiTextures.SEQUENCER;

	private final SequencedGearshiftBlockEntity be;
	private final Vector<Instruction> instructions;

	private IconButton confirmButton;
	private Vector<Vector<ScrollInput>> inputs;
	private int left;
	private int top;

	public SequencedGearshiftScreen(SequencedGearshiftBlockEntity be) {
		super(CreateLang.translateDirect("gui.sequenced_gearshift.title"));
		this.instructions = be.instructions;
		this.be = be;
	}

	@Override
	protected void init() {
		if (be.computerBehaviour.hasAttachedComputer()) {
			ScreenOpener.open(new ComputerScreen(title, this::renderAdditional, this,
				be.computerBehaviour::hasAttachedComputer));
			return;
		}

		layout();
		super.init();

		inputs = new Vector<>(5);
		for (int row = 0; row < inputs.capacity(); row++)
			inputs.add(new Vector<>(3));

		for (int row = 0; row < instructions.size(); row++)
			initInputsOfRow(row, left, top);

		confirmButton = new IconButton(left + background.getWidth() - 33, top + background.getHeight() - 24,
			AllIcons.I_CONFIRM);
		confirmButton.withCallback(this::onClose);
		addRenderableWidget(confirmButton);
	}

	private void layout() {
		left = (width - background.getWidth()) / 2 - 20;
		top = (height - background.getHeight()) / 2;
	}

	public void initInputsOfRow(int row, int backgroundX, int backgroundY) {
		int x = backgroundX + 30;
		int y = backgroundY + 20;
		int rowHeight = 22;

		Vector<ScrollInput> rowInputs = inputs.get(row);
		removeWidgets(rowInputs);
		rowInputs.clear();

		int index = row;
		Instruction instruction = instructions.get(row);

		ScrollInput type = new SelectionScrollInput(x, y + rowHeight * row, 50, 18)
			.forOptions(SequencerInstructions.getOptions())
			.calling(state -> instructionUpdated(index, state))
			.setState(instruction.instruction.ordinal())
			.titled(CreateLang.translateDirect("gui.sequenced_gearshift.instruction"));
		ScrollInput value = new ScrollInput(x + 58, y + rowHeight * row, 28, 18)
			.calling(state -> instruction.value = state);
		ScrollInput direction = new SelectionScrollInput(x + 88, y + rowHeight * row, 28, 18)
			.forOptions(InstructionSpeedModifiers.getOptions())
			.calling(state -> instruction.speedModifier = InstructionSpeedModifiers.values()[state])
			.titled(CreateLang.translateDirect("gui.sequenced_gearshift.speed"));

		rowInputs.add(type);
		rowInputs.add(value);
		rowInputs.add(direction);

		addRenderableWidgets(rowInputs);
		updateParamsOfRow(row);
	}

	public void updateParamsOfRow(int row) {
		Instruction instruction = instructions.get(row);
		Vector<ScrollInput> rowInputs = inputs.get(row);
		SequencerInstructions def = instruction.instruction;
		boolean hasValue = def.hasValueParameter;
		boolean hasModifier = def.hasSpeedParameter;

		ScrollInput value = rowInputs.get(1);
		value.active = value.visible = hasValue;
		if (hasValue)
			value.withRange(1, def.maxValue + 1)
				.titled(CreateLang.translateDirect(def.parameterKey))
				.withShiftStep(def.shiftStep)
				.setState(instruction.value)
				.onChanged();

		if (def == SequencerInstructions.DELAY)
			value.withStepFunction(context -> {
				int v = context.currentValue;
				if (!context.forward)
					v--;
				if (v < 20)
					return context.shift ? 20 : 1;
				return context.shift ? 100 : 20;
			});
		else
			value.withStepFunction(value.standardStep());

		ScrollInput modifier = rowInputs.get(2);
		modifier.active = modifier.visible = hasModifier;
		if (hasModifier)
			modifier.setState(instruction.speedModifier.ordinal());
	}

	@Override
	public void tick() {
		super.tick();

		if (be.computerBehaviour.hasAttachedComputer())
			ScreenOpener.open(new ComputerScreen(title, this::renderAdditional, this,
				be.computerBehaviour::hasAttachedComputer));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		layout();
		renderWindow(graphics, mouseX, mouseY, partialTicks);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	protected void renderWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		background.render(graphics, left, top);

		for (int row = 0; row < instructions.capacity(); row++) {
			AllGuiTextures toDraw = AllGuiTextures.SEQUENCER_EMPTY;
			int yOffset = toDraw.getHeight() * row;
			if (row >= instructions.size()) {
				toDraw.render(graphics, left, top + 16 + yOffset);
				continue;
			}

			Instruction instruction = instructions.get(row);
			SequencerInstructions def = instruction.instruction;
			backgroundFor(def).render(graphics, left, top + 16 + yOffset);

			label(graphics, 36, yOffset - 1, CreateLang.translateDirect(def.translationKey));
			if (def.hasValueParameter) {
				String text = def.formatValue(instruction.value);
				int stringWidth = font.width(text);
				label(graphics, 90 + (12 - stringWidth / 2), yOffset - 1, Component.literal(text));
			}
			if (def.hasSpeedParameter)
				label(graphics, 127, yOffset - 1, instruction.speedModifier.label);
		}

		graphics.text(font, title, left + (background.getWidth() - 8) / 2 - font.width(title) / 2, top + 4,
			0xFF592424, false);
		renderAdditional(graphics, mouseX, mouseY, partialTicks, left, top, background);
	}

	private static AllGuiTextures backgroundFor(SequencerInstructions instruction) {
		return switch (instruction) {
			case TURN_ANGLE, TURN_DISTANCE -> AllGuiTextures.SEQUENCER_INSTRUCTION;
			case DELAY -> AllGuiTextures.SEQUENCER_DELAY;
			case AWAIT -> AllGuiTextures.SEQUENCER_AWAIT;
			case END -> AllGuiTextures.SEQUENCER_END;
		};
	}

	private void renderAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, int guiLeft,
		int guiTop, AllGuiTextures background) {
		renderAdditional((GuiGraphicsExtractor) graphics, mouseX, mouseY, partialTicks, guiLeft, guiTop, background);
	}

	private void renderAdditional(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, int guiLeft,
		int guiTop, AllGuiTextures background) {
		GuiGameElement.GuiRenderBuilder item = GuiGameElement.of(renderedItem);
		item.scale(5);
		item.at(guiLeft + background.getWidth() + 6, guiTop + background.getHeight() - 56, 100);
		item.submit(graphics);
	}

	private void label(GuiGraphicsExtractor graphics, int x, int y, Component text) {
		graphics.text(font, text, left + x, top + 26 + y, 0xFFFFFFEE, false);
	}

	public void sendPacket() {
		ClientNetworkHelper.INSTANCE.sendToServer(new ConfigureSequencedGearshiftPacket(be.getBlockPos(), instructions));
	}

	@Override
	public void removed() {
		sendPacket();
		super.removed();
	}

	private void instructionUpdated(int index, int state) {
		SequencerInstructions newValue = SequencerInstructions.values()[state];
		instructions.get(index).instruction = newValue;
		instructions.get(index).value = newValue.defaultValue;
		updateParamsOfRow(index);

		if (newValue == SequencerInstructions.END) {
			for (int i = instructions.size() - 1; i > index; i--) {
				instructions.remove(i);
				Vector<ScrollInput> rowInputs = inputs.get(i);
				removeWidgets(rowInputs);
				rowInputs.clear();
			}
			return;
		}

		if (index + 1 < instructions.capacity() && index + 1 == instructions.size()) {
			instructions.add(new Instruction(SequencerInstructions.END));
			initInputsOfRow(index + 1, left, top);
		}
	}

	private <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(Collection<W> widgets) {
		for (W widget : widgets)
			addRenderableWidget(widget);
	}

	private void removeWidgets(Collection<? extends GuiEventListener> widgets) {
		for (GuiEventListener widget : widgets)
			removeWidget(widget);
	}
}
