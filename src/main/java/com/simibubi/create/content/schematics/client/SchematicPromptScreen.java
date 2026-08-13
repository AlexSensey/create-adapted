package com.simibubi.create.content.schematics.client;

import org.lwjgl.glfw.GLFW;

import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class SchematicPromptScreen extends AbstractSimiScreen {

	private final AllGuiTextures background = AllGuiTextures.SCHEMATIC_PROMPT;
	private final Component convertLabel = CreateLang.translateDirect("schematicAndQuill.convert");
	private final Component abortLabel = CreateLang.translateDirect("action.discard");
	private final Component confirmLabel = CreateLang.translateDirect("action.saveToFile");

	private EditBox nameField;

	public SchematicPromptScreen() {
		super(CreateLang.translateDirect("schematicAndQuill.title"));
	}

	@Override
	public void init() {
		int x = (width - background.getWidth()) / 2;
		int y = (height - background.getHeight()) / 2 + 2;

		nameField = new EditBox(font, x + 49, y + 26, 131, 10, CommonComponents.EMPTY);
		nameField.setTextColor(-1);
		nameField.setTextColorUneditable(-1);
		nameField.setBordered(false);
		nameField.setMaxLength(35);
		nameField.setFocused(true);
		setFocused(nameField);
		addRenderableWidget(nameField);

		IconButton abort = new IconButton(x + 7, y + 53, AllIcons.I_TRASH);
		abort.withCallback(() -> {
			CreateClient.SCHEMATIC_AND_QUILL_HANDLER.discard();
			onClose();
		});
		abort.setToolTip(abortLabel);
		addRenderableWidget(abort);

		IconButton confirm = new IconButton(x + 158, y + 53, AllIcons.I_CONFIRM);
		confirm.withCallback(() -> confirm(false));
		confirm.setToolTip(confirmLabel);
		addRenderableWidget(confirm);

		IconButton convert = new IconButton(x + 180, y + 53, AllIcons.I_SCHEMATIC);
		convert.withCallback(() -> confirm(true));
		convert.setToolTip(convertLabel);
		addRenderableWidget(convert);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		int x = (width - background.getWidth()) / 2;
		int y = (height - background.getHeight()) / 2;
		background.render(graphics, x, y);
		graphics.text(font, title, x + (background.getWidth() - 8 - font.width(title)) / 2, y + 4,
			0xFF505050, false);

		GuiGameElement.of(SchematicItem.emptyStack())
			.at(x + 22, y + 24, 0)
			.submit(graphics);
		GuiGameElement.of(AllItems.SCHEMATIC_AND_QUILL.asStack())
			.scale(3)
			.at(x + background.getWidth() + 6, y + background.getHeight() - 38, -200)
			.submit(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
			confirm(false);
			return true;
		}
		return super.keyPressed(event);
	}

	private void confirm(boolean convertImmediately) {
		if (nameField == null || nameField.getValue()
			.isBlank())
			return;
		CreateClient.SCHEMATIC_AND_QUILL_HANDLER.saveSchematic(nameField.getValue(), convertImmediately);
		onClose();
	}
}
