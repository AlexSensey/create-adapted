package com.simibubi.create.content.redstone.displayLink;

import java.util.Collections;
import java.util.List;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.ModularGuiLine;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.api.client.gui.widget.ElementWidget;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.data.Couple;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DisplayLinkScreen extends AbstractSimiScreen {

	private final AllGuiTextures background = AllGuiTextures.DATA_GATHERER;
	private final DisplayLinkBlockEntity blockEntity;
	private final Couple<ModularGuiLine> configWidgets;

	private BlockState sourceState;
	private BlockState targetState;
	private List<DisplaySource> sources;
	private DisplayTarget target;

	private ScrollInput sourceTypeSelector;
	private Label sourceTypeLabel;
	private ScrollInput targetLineSelector;
	private Label targetLineLabel;
	private AbstractSimiWidget sourceWidget;
	private AbstractSimiWidget targetWidget;
	private int left;
	private int top;

	public DisplayLinkScreen(DisplayLinkBlockEntity be) {
		super(CreateLang.translateDirect("display_link.title"));
		blockEntity = be;
		sources = Collections.emptyList();
		configWidgets = Couple.create(ModularGuiLine::new);
	}

	@Override
	protected void init() {
		layout();
		super.init();
		clearWidgets();
		initGathererOptions();

		IconButton confirmButton =
			new IconButton(left + background.getWidth() - 33, top + background.getHeight() - 24,
				AllIcons.I_CONFIRM);
		confirmButton.withCallback(this::onClose);
		addRenderableWidget(confirmButton);
	}

	@Override
	public void tick() {
		super.tick();
		if (minecraft.level == null)
			return;
		BlockState newSource = minecraft.level.getBlockState(blockEntity.getSourcePosition());
		BlockState newTarget = minecraft.level.getBlockState(blockEntity.getTargetPosition());
		if (sourceState == null || targetState == null || sourceState.getBlock() != newSource.getBlock()
			|| targetState.getBlock() != newTarget.getBlock())
			initGathererOptions();
	}

	private void initGathererOptions() {
		if (!(minecraft.level instanceof ClientLevel level))
			return;

		sourceState = level.getBlockState(blockEntity.getSourcePosition());
		targetState = level.getBlockState(blockEntity.getTargetPosition());
		sources = DisplaySource.getAll(level, blockEntity.getSourcePosition());
		target = DisplayTarget.get(level, blockEntity.getTargetPosition());

		removeNullable(targetLineSelector);
		removeNullable(targetLineLabel);
		removeNullable(sourceTypeSelector);
		removeNullable(sourceTypeLabel);
		removeNullable(sourceWidget);
		removeNullable(targetWidget);
		configWidgets.forEach(line -> line.forEach(this::removeNullable));
		configWidgets.forEach(ModularGuiLine::clear);

		targetLineSelector = null;
		sourceTypeSelector = null;

		int x = left;
		int y = top;
		ItemStack sourceIcon = new ItemStack(sourceState.getBlock().asItem());
		ItemStack targetIcon = new ItemStack(targetState.getBlock().asItem());
		if (sourceIcon.isEmpty())
			sourceIcon = new ItemStack(Items.BARRIER);
		if (targetIcon.isEmpty())
			targetIcon = new ItemStack(Items.BARRIER);

		if (target != null) {
			DisplayTargetStats stats = target.provideStats(new DisplayLinkContext(level, blockEntity));
			int rows = Math.max(1, stats.maxRows());
			int startIndex = Math.min(blockEntity.targetLine, rows - 1);
			targetLineLabel = new Label(x + 65, y + 109, CommonComponents.EMPTY).withShadow();
			targetLineLabel.text = target.getLineOptionText(startIndex);
			if (rows > 1) {
				targetLineSelector = new ScrollInput(x + 61, y + 105, 135, 16).withRange(0, rows)
					.titled(CreateLang.translateDirect("display_link.display_on"))
					.inverted()
					.calling(i -> targetLineLabel.text = target.getLineOptionText(i))
					.setState(startIndex);
				addRenderableWidget(targetLineSelector);
			}
			addRenderableWidget(targetLineLabel);
		}

		sourceWidget = new ElementWidget(x + 37, y + 26)
			.showingElement(GuiGameElement.of(sourceIcon));
		sourceWidget.getToolTip().addAll(List.of(
			CreateLang.translateDirect("display_link.reading_from"),
			sourceState.getBlock().getName(),
			CreateLang.translateDirect("display_link.attached_side")
		));
		addRenderableWidget(sourceWidget);

		targetWidget = new ElementWidget(x + 37, y + 105)
			.showingElement(GuiGameElement.of(targetIcon));
		targetWidget.getToolTip().addAll(List.of(
			CreateLang.translateDirect("display_link.writing_to"),
			targetState.getBlock().getName(),
			CreateLang.translateDirect("display_link.targeted_location")
		));
		addRenderableWidget(targetWidget);

		if (!sources.isEmpty()) {
			int startIndex = Math.max(sources.indexOf(blockEntity.activeSource), 0);
			sourceTypeLabel = new Label(x + 65, y + 30, CommonComponents.EMPTY).withShadow();
			sourceTypeLabel.text = sources.get(startIndex).getName();

			if (sources.size() > 1) {
				List<Component> options = sources.stream().map(DisplaySource::getName).toList();
				sourceTypeSelector = new SelectionScrollInput(x + 61, y + 26, 135, 16).forOptions(options)
					.writingTo(sourceTypeLabel)
					.titled(CreateLang.translateDirect("display_link.information_type"))
					.calling(this::initGathererSourceSubOptions)
					.setState(startIndex);
				sourceTypeSelector.onChanged();
				addRenderableWidget(sourceTypeSelector);
			} else {
				initGathererSourceSubOptions(0);
			}
			addRenderableWidget(sourceTypeLabel);
		}
	}

	private void initGathererSourceSubOptions(int index) {
		if (sources.isEmpty() || minecraft.level == null)
			return;
		DisplaySource source = sources.get(index);
		source.populateData(new DisplayLinkContext(blockEntity.getLevel(), blockEntity));

		if (targetLineSelector != null)
			targetLineSelector.titled(source instanceof SingleLineDisplaySource
				? CreateLang.translateDirect("display_link.display_on")
				: CreateLang.translateDirect("display_link.display_on_multiline"));

		configWidgets.forEach(line -> {
			line.forEach(this::removeNullable);
			line.clear();
		});
		DisplayLinkContext context = new DisplayLinkContext(minecraft.level, blockEntity);
		configWidgets.forEachWithContext((line, first) -> source.initConfigurationWidgets(context,
			new ModularGuiLineBuilder(font, line, left + 60, top + (first ? 51 : 72)), first));
		configWidgets.forEach(line ->
			line.loadValues(blockEntity.getSourceConfig(), this::addRenderableWidget, this::addRenderableOnly));
	}

	@Override
	public void onClose() {
		CompoundTag sourceData = new CompoundTag();
		if (!sources.isEmpty()) {
			DisplaySource source = sources.get(sourceTypeSelector == null ? 0 : sourceTypeSelector.getState());
			Identifier id = CreateBuiltInRegistries.DISPLAY_SOURCE.getKey(source);
			if (id != null)
				sourceData.putString("Id", id.toString());
			configWidgets.forEach(line -> line.saveValues(sourceData));
		}
		ClientNetworkHelper.INSTANCE.sendToServer(new DisplayLinkConfigurationPacket(blockEntity.getBlockPos(),
			sourceData, targetLineSelector == null ? 0 : targetLineSelector.getState()));
		super.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		layout();
		renderWindow26(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	private void renderWindow26(GuiGraphicsExtractor graphics) {
		int x = left;
		int y = top;
		background.render(graphics, x, y);
		MutableComponent header = CreateLang.translateDirect("display_link.title");
		graphics.text(font, header, x + background.getWidth() / 2 - font.width(header) / 2, y + 4, 0xFF592424,
			false);

		if (sources.isEmpty())
			graphics.text(font, CreateLang.translateDirect("display_link.no_source"), x + 65, y + 30, 0xFFD3D3D3,
				false);
		if (target == null)
			graphics.text(font, CreateLang.translateDirect("display_link.no_target"), x + 65, y + 109, 0xFFD3D3D3,
				false);

		graphics.pose().pushMatrix();
		graphics.pose().translate(0, top + 46);
		configWidgets.getFirst().renderWidgetBG(left, graphics);
		graphics.pose().translate(0, 21);
		configWidgets.getSecond().renderWidgetBG(left, graphics);
		graphics.pose().popMatrix();
	}

	protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		// Rendering is extracted through GuiGraphicsExtractor in 26.2.
	}

	private void removeNullable(GuiEventListener listener) {
		if (listener != null)
			removeWidget(listener);
	}

	private void layout() {
		left = (width - background.getWidth()) / 2;
		top = (height - background.getHeight()) / 2;
	}
}
