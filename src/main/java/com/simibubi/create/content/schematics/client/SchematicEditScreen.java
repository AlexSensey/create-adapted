package com.simibubi.create.content.schematics.client;

import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.FilteringEditBox;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public class SchematicEditScreen extends AbstractSimiScreen {

	private final List<Component> rotationOptions =
		CreateLang.translatedOptions("schematic.rotation", "none", "cw90", "cw180", "cw270");
	private final List<Component> mirrorOptions =
		CreateLang.translatedOptions("schematic.mirror", "none", "leftRight", "frontBack");
	private final Component rotationLabel = CreateLang.translateDirect("schematic.rotation");
	private final Component mirrorLabel = CreateLang.translateDirect("schematic.mirror");
	private final AllGuiTextures background = AllGuiTextures.SCHEMATIC;
	private final SchematicHandler handler = CreateClient.SCHEMATIC_HANDLER;

	private EditBox xInput;
	private EditBox yInput;
	private EditBox zInput;
	private ScrollInput rotationArea;
	private ScrollInput mirrorArea;

	public SchematicEditScreen() {
		super(Component.empty());
	}

	@Override
	protected void init() {
		int x = (width - background.getWidth()) / 2 - 6;
		int y = (height - background.getHeight()) / 2 + 2;

		xInput = coordinateInput(x + 50, y + 26);
		yInput = coordinateInput(x + 90, y + 26);
		zInput = coordinateInput(x + 130, y + 26);

		BlockPos anchor = handler.isDeployed() ? handler.getTransformation()
			.getAnchor() : minecraft.player.blockPosition();
		xInput.setValue(Integer.toString(anchor.getX()));
		yInput.setValue(Integer.toString(anchor.getY()));
		zInput.setValue(Integer.toString(anchor.getZ()));

		StructurePlaceSettings settings = handler.getTransformation()
			.toSettings();
		Label rotationValue = new Label(x + 50, y + 48, CommonComponents.EMPTY).withShadow();
		rotationArea = new SelectionScrollInput(x + 45, y + 43, 118, 18).forOptions(rotationOptions)
			.titled(rotationLabel.copy())
			.setState(settings.getRotation()
				.ordinal())
			.writingTo(rotationValue);

		Label mirrorValue = new Label(x + 50, y + 70, CommonComponents.EMPTY).withShadow();
		mirrorArea = new SelectionScrollInput(x + 45, y + 65, 118, 18).forOptions(mirrorOptions)
			.titled(mirrorLabel.copy())
			.setState(settings.getMirror()
				.ordinal())
			.writingTo(mirrorValue);

		addRenderableWidget(xInput);
		addRenderableWidget(yInput);
		addRenderableWidget(zInput);
		addRenderableWidget(rotationValue);
		addRenderableWidget(mirrorValue);
		addRenderableWidget(rotationArea);
		addRenderableWidget(mirrorArea);
		IconButton confirm =
			new IconButton(x + background.getWidth() - 33, y + background.getHeight() - 26, AllIcons.I_CONFIRM);
		confirm.withCallback(this::onClose);
		addRenderableWidget(confirm);
	}

	private EditBox coordinateInput(int x, int y) {
		FilteringEditBox input = new FilteringEditBox(font, x, y, 34, 10, CommonComponents.EMPTY);
		input.setMaxLength(7);
		input.setBordered(false);
		input.setTextColor(0xFFFFFFFF);
		input.setFilter(value -> {
			if (value.isEmpty() || value.equals("-"))
				return true;
			try {
				Integer.parseInt(value);
				return true;
			} catch (NumberFormatException ignored) {
				return false;
			}
		});
		return input;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		int x = (width - background.getWidth()) / 2 - 6;
		int y = (height - background.getHeight()) / 2;
		background.render(graphics, x, y);
		String schematicName = handler.getCurrentSchematicName();
		graphics.text(font, schematicName,
			x + (background.getWidth() - 8 - font.width(schematicName)) / 2, y + 4, 0xFF505050, false);
		GuiGameElement.of(SchematicItem.emptyStack())
			.scale(3)
			.at(x + background.getWidth() + 6, y + background.getHeight() - 40, -200)
			.submit(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public void removed() {
		super.removed();
		if (xInput == null || yInput == null || zInput == null)
			return;
		try {
			BlockPos newLocation = new BlockPos(Integer.parseInt(xInput.getValue()),
				Integer.parseInt(yInput.getValue()), Integer.parseInt(zInput.getValue()));
			StructurePlaceSettings settings = new StructurePlaceSettings()
				.setRotation(Rotation.values()[rotationArea.getState()])
				.setMirror(Mirror.values()[mirrorArea.getState()]);
			ItemStack item = handler.getActiveSchematicItem();
			if (!item.isEmpty()) {
				item.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
				item.set(AllDataComponents.SCHEMATIC_ANCHOR, newLocation);
				item.set(AllDataComponents.SCHEMATIC_ROTATION, settings.getRotation());
				item.set(AllDataComponents.SCHEMATIC_MIRROR, settings.getMirror());
			}
			handler.getTransformation()
				.init(newLocation, settings, handler.getBounds());
			handler.markDirty();
			handler.deploy();
		} catch (NumberFormatException ignored) {
		}
	}
}
