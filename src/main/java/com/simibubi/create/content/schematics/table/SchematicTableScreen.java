package com.simibubi.create.content.schematics.table;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;
import static com.simibubi.create.foundation.gui.AllGuiTextures.SCHEMATIC_TABLE_PROGRESS;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.Create;
import com.simibubi.create.content.schematics.client.ClientSchematicLoader;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.CreatePaths;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class SchematicTableScreen extends AbstractSimiContainerScreen<SchematicTableMenu> {

	private final Component uploading = CreateLang.translateDirect("gui.schematicTable.uploading");
	private final Component finished = CreateLang.translateDirect("gui.schematicTable.finished");
	private final Component refresh = CreateLang.translateDirect("gui.schematicTable.refresh");
	private final Component folder = CreateLang.translateDirect("gui.schematicTable.open_folder");
	private final Component noSchematics = CreateLang.translateDirect("gui.schematicTable.noSchematics");
	private final Component availableSchematicsTitle =
		CreateLang.translateDirect("gui.schematicTable.availableSchematics");
	private final AllGuiTextures background = AllGuiTextures.SCHEMATIC_TABLE;
	private final ItemStack renderedItem = AllBlocks.SCHEMATIC_TABLE.asStack();

	private ScrollInput schematicsArea;
	private IconButton confirmButton;
	private Label schematicsLabel;
	private float progress;
	private float chasingProgress;
	private float lastChasingProgress;
	private List<Rect2i> extraAreas = Collections.emptyList();

	public SchematicTableScreen(SchematicTableMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Override
	protected void init() {
		setWindowSize(background.getWidth(),
			background.getHeight() + 4 + AllGuiTextures.PLAYER_INVENTORY.getHeight());
		setWindowOffset(-11, 8);
		super.init();

		CreateClient.SCHEMATIC_SENDER.refresh();
		int x = leftPos;
		int y = topPos + 2;
		schematicsLabel = new Label(x + 51, y + 26, CommonComponents.EMPTY).withShadow();
		addRenderableWidget(schematicsLabel);
		rebuildSchematicSelector(CreateClient.SCHEMATIC_SENDER.getAvailableSchematics());

		confirmButton = new IconButton(x + 44, y + 56, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			if (!menu.canWrite() || schematicsArea == null)
				return;
			lastChasingProgress = chasingProgress = progress = 0;
			List<Component> available = CreateClient.SCHEMATIC_SENDER.getAvailableSchematics();
			if (schematicsArea.getState() >= 0 && schematicsArea.getState() < available.size())
				CreateClient.SCHEMATIC_SENDER.startNewUpload(available.get(schematicsArea.getState())
					.getString());
		});

		IconButton folderButton = new IconButton(x + 20, y + 21, AllIcons.I_OPEN_FOLDER);
		folderButton.withCallback(SchematicTableScreen::openSchematicsFolder);
		folderButton.setToolTip(folder);

		IconButton refreshButton = new IconButton(x + 206, y + 21, AllIcons.I_REFRESH);
		refreshButton.withCallback(() -> {
			CreateClient.SCHEMATIC_SENDER.refresh();
			rebuildSchematicSelector(CreateClient.SCHEMATIC_SENDER.getAvailableSchematics());
		});
		refreshButton.setToolTip(refresh);

		addRenderableWidget(confirmButton);
		addRenderableWidget(folderButton);
		addRenderableWidget(refreshButton);
		extraAreas = ImmutableList.of(
			new Rect2i(x + background.getWidth(), y + background.getHeight() - 40, 48, 48),
			new Rect2i(refreshButton.getX(), refreshButton.getY(), refreshButton.getWidth(),
				refreshButton.getHeight()));
	}

	private static void openSchematicsFolder() {
		String path = CreatePaths.SCHEMATICS_DIR.toAbsolutePath().toString();
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		try {
			if (os.contains("win"))
				new ProcessBuilder("explorer.exe", path).start();
			else if (os.contains("mac"))
				new ProcessBuilder("open", path).start();
			else
				new ProcessBuilder("xdg-open", path).start();
		} catch (IOException | SecurityException e) {
			Create.LOGGER.error("Could not open schematics folder {}", path, e);
		}
	}

	private void rebuildSchematicSelector(List<Component> availableSchematics) {
		if (schematicsArea != null)
			removeWidget(schematicsArea);
		if (availableSchematics.isEmpty()) {
			schematicsArea = null;
			schematicsLabel.text = CommonComponents.EMPTY;
			return;
		}
		schematicsArea = new SelectionScrollInput(leftPos + 45, topPos + 23, 139, 18)
			.forOptions(availableSchematics)
			.titled(availableSchematicsTitle.copy())
			.writingTo(schematicsLabel);
		schematicsArea.onChanged();
		addRenderableWidget(schematicsArea);
	}

	@Override
	protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
		int inventoryX = getLeftOfCentered(PLAYER_INVENTORY.getWidth());
		int inventoryY = topPos + background.getHeight() + 4;
		renderPlayerInventory(graphics, inventoryX, inventoryY);
		int x = leftPos;
		int y = topPos;
		background.render(graphics, x, y);

		Component titleText = menu.contentHolder.isUploading ? uploading
			: menu.getSlot(1).hasItem() ? finished : title;
		graphics.text(font, titleText, x + (background.getWidth() - 8 - font.width(titleText)) / 2,
			y + 4, 0xFF505050, false);
		if (schematicsArea == null)
			graphics.text(font, noSchematics, x + 54, y + 26, 0xFFD3D3D3, false);

		GuiGameElement.of(renderedItem)
			.scale(3)
			.at(x + background.getWidth(), y + background.getHeight() - 40, -200)
			.submit(graphics);

		int width = (int) (SCHEMATIC_TABLE_PROGRESS.getWidth()
			* Mth.lerp(partialTicks, lastChasingProgress, chasingProgress));
		graphics.blit(RenderPipelines.GUI_TEXTURED, SCHEMATIC_TABLE_PROGRESS.location, x + 70, y + 59,
			SCHEMATIC_TABLE_PROGRESS.getStartX(), SCHEMATIC_TABLE_PROGRESS.getStartY(), width,
			SCHEMATIC_TABLE_PROGRESS.getHeight(), 256, 256);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		boolean uploadFinished = menu.getSlot(1)
			.hasItem();
		if (menu.contentHolder.isUploading || uploadFinished) {
			if (uploadFinished) {
				chasingProgress = lastChasingProgress = progress = 1;
			} else {
				lastChasingProgress = chasingProgress;
				progress = menu.contentHolder.uploadingProgress;
				chasingProgress += (progress - chasingProgress) * .5f;
			}
			confirmButton.active = false;
			schematicsLabel.colored(0xCCDDFF);
			String uploadingName = menu.contentHolder.uploadingSchematic;
			schematicsLabel.text = uploadingName == null ? CommonComponents.EMPTY : Component.literal(uploadingName);
			if (schematicsArea != null)
				schematicsArea.visible = false;
			return;
		}

		progress = 0;
		chasingProgress = lastChasingProgress = 0;
		confirmButton.active = true;
		schematicsLabel.colored(0xFFFFFF);
		if (schematicsArea != null) {
			schematicsArea.writingTo(schematicsLabel);
			schematicsArea.visible = true;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (schematicsArea != null && schematicsArea.visible && schematicsArea.isMouseOver(mouseX, mouseY)
			&& schematicsArea.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
			return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}
