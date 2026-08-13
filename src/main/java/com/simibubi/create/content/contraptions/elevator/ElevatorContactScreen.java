package com.simibubi.create.content.contraptions.elevator;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.simibubi.create.content.decoration.slidingDoor.DoorControl;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.TooltipArea;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;

public class ElevatorContactScreen extends AbstractSimiScreen {

	private final AllGuiTextures background;
	private final BlockPos pos;

	private EditBox shortNameInput;
	private EditBox longNameInput;
	private IconButton confirm;

	private String shortName;
	private String longName;
	private DoorControl doorControl;
	private int left;
	private int top;
	private int windowWidth;
	private int windowHeight;

	public ElevatorContactScreen(BlockPos pos, String prevShortName, String prevLongName, DoorControl prevDoorControl) {
		super(CreateLang.translateDirect("elevator_contact.title"));
		this.pos = pos;
		this.shortName = prevShortName;
		this.longName = prevLongName;
		this.doorControl = prevDoorControl;
		this.background = AllGuiTextures.ELEVATOR_CONTACT;
	}

	@Override
	public void init() {
		layout();
		super.init();

		int x = left;
		int y = top;

		confirm = new IconButton(x + 200, y + 58, AllIcons.I_CONFIRM);
		confirm.withCallback(this::confirm);
		addRenderableWidget(confirm);

		shortNameInput = editBox(33, 30, 4);
		shortNameInput.setValue(shortName);
		centerShortNameInput(x);
		shortNameInput.setResponder(s -> {
			shortName = s;
			centerShortNameInput(x);
		});
		shortNameInput.setFocused(true);
		setFocused(shortNameInput);
		shortNameInput.setHighlightPos(0);

		longNameInput = editBox(63, 140, 30);
		longNameInput.setValue(longName);
		longNameInput.setResponder(s -> longName = s);

		MutableComponent lmbToEdit = CreateLang.translate("gui.schedule.lmb_edit")
			.style(ChatFormatting.DARK_GRAY)
			.style(ChatFormatting.ITALIC)
			.component();

		addRenderableOnly(new TooltipArea(x + 21, y + 23, 30, 18)
			.withTooltip(List.of(CreateLang.translate("elevator_contact.floor_identifier")
				.style(ChatFormatting.BLUE)
				.component(), lmbToEdit)));

		addRenderableOnly(new TooltipArea(x + 57, y + 23, 147, 18)
			.withTooltip(List.of(CreateLang.translate("elevator_contact.floor_description")
					.style(ChatFormatting.BLUE)
					.component(),
				CreateLang.translate("crafting_blueprint.optional")
					.style(ChatFormatting.GRAY)
					.component(),
				lmbToEdit)));

		Pair<ScrollInput, Label> doorControlWidgets =
			DoorControl.createWidget(x + 58, y + 57, mode -> doorControl = mode, doorControl);
		addRenderableWidget(doorControlWidgets.getFirst());
		addRenderableWidget(doorControlWidgets.getSecond());
	}

	private void layout() {
		windowWidth = background.getWidth() + 30;
		windowHeight = background.getHeight();
		left = (width - windowWidth) / 2;
		top = (height - windowHeight) / 2;
	}

	private void centerShortNameInput(int x) {
		int centeredX = x + (shortName.isEmpty() ? 34 : 36 - font.width(shortName) / 2);
		shortNameInput.setX(centeredX);
	}

	private EditBox editBox(int x, int width, int chars) {
		EditBox editBox = new EditBox(font, left + x, top + 30, width, 10, CommonComponents.EMPTY);
		editBox.setTextColor(-1);
		editBox.setTextColorUneditable(-1);
		editBox.setBordered(false);
		editBox.setMaxLength(chars);
		editBox.setFocused(false);
		addRenderableWidget(editBox);
		return editBox;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		layout();
		int x = left;
		int y = top;

		background.render(graphics, x, y);

		graphics.text(font, title, x + (background.getWidth() - 8) / 2 - font.width(title) / 2, y + 6, 0x2F3738, false);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean consumed = super.mouseClicked(event, doubleClick);
		double mouseX = event.x();
		double mouseY = event.y();

		if (!shortNameInput.isFocused()) {
			int length = shortNameInput.getValue()
				.length();
			shortNameInput.setHighlightPos(length);
			shortNameInput.setCursorPosition(length);
		}

		if (shortNameInput.isHoveredOrFocused())
			longNameInput.setFocused(false);

		if (!consumed && mouseX > left + 22 && mouseY > top + 24 && mouseX < left + 50
			&& mouseY < top + 40) {
			setFocused(shortNameInput);
			shortNameInput.setFocused(true);
			return true;
		}

		return consumed;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (super.keyPressed(event))
			return true;
		if (event.key() == GLFW.GLFW_KEY_ENTER) {
			confirm();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc()) {
			onClose();
			return true;
		}
		return false;
	}

	private void confirm() {
		ClientNetworkHelper.INSTANCE.sendToServer(new ElevatorContactEditPacket(pos, shortName, longName, doorControl));
		onClose();
	}

}
