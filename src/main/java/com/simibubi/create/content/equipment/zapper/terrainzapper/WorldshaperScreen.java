package com.simibubi.create.content.equipment.zapper.terrainzapper;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.zapper.ConfigureZapperPacket;
import com.simibubi.create.content.equipment.zapper.PlacementPatterns;
import com.simibubi.create.content.equipment.zapper.ZapperScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class WorldshaperScreen extends ZapperScreen {

	private final Component placementSection = CreateLang.translateDirect("gui.terrainzapper.placement");
	private final Component toolSection = CreateLang.translateDirect("gui.terrainzapper.tool");
	private final List<Component> brushOptions =
		CreateLang.translatedOptions("gui.terrainzapper.brush", "cuboid", "sphere", "cylinder", "surface", "cluster");

	private TerrainBrushes currentBrush;
	private final int[] currentBrushParams = {1, 1, 1};
	private TerrainTools currentTool;
	private PlacementOptions currentPlacement;
	private ScrollInput brushInput;
	private boolean followDiagonals;
	private boolean acrossMaterials;
	private final List<AbstractWidget> dynamicWidgets = new ArrayList<>();

	public WorldshaperScreen(ItemStack zapper, InteractionHand hand) {
		super(AllGuiTextures.TERRAINZAPPER, zapper, hand);
		screenTitle = zapper.getHoverName();
		fontColor = 0x767676;
		currentBrush = zapper.getOrDefault(AllDataComponents.SHAPER_BRUSH, TerrainBrushes.Cuboid);
		if (zapper.has(AllDataComponents.SHAPER_BRUSH_PARAMS)) {
			BlockPos params = zapper.get(AllDataComponents.SHAPER_BRUSH_PARAMS);
			currentBrushParams[0] = params.getX();
			currentBrushParams[1] = params.getY();
			currentBrushParams[2] = params.getZ();
			followDiagonals = params.getY() == 0;
			acrossMaterials = params.getZ() == 0;
		}
		currentTool = zapper.getOrDefault(AllDataComponents.SHAPER_TOOL, TerrainTools.Fill);
		currentPlacement = zapper.getOrDefault(AllDataComponents.SHAPER_PLACEMENT_OPTIONS, PlacementOptions.Merged);
	}

	@Override
	protected void init() {
		super.init();
		Label brushLabel = new Label(left + 61, top + 25, CommonComponents.EMPTY).colored(0xff444444);
		brushInput = new SelectionScrollInput(left + 56, top + 20, 77, 18)
			.forOptions(brushOptions)
			.titled(CreateLang.translateDirect("gui.terrainzapper.brush"))
			.writingTo(brushLabel)
			.setState(currentBrush.ordinal())
			.calling(index -> {
				currentBrush = TerrainBrushes.values()[index];
				rebuildBrushControls();
			});
		addRenderableWidget(brushLabel);
		addRenderableWidget(brushInput);
		rebuildBrushControls();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		// 26.2 no longer reliably dispatches wheel input to non-container widgets.
		if (brushInput != null && brushInput.isMouseOver(mouseX, mouseY)
			&& brushInput.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
			return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void rebuildBrushControls() {
		for (AbstractWidget widget : dynamicWidgets)
			removeWidget(widget);
		dynamicWidgets.clear();
		Brush brush = currentBrush.get();

		for (int index = 0; index < brush.amtParams; index++) {
			final int param = index;
			Label label = new Label(left + 65 + 20 * index, top + 45, CommonComponents.EMPTY)
				.colored(0xff444444);
			ScrollInput input = new ScrollInput(left + 56 + 20 * index, top + 40, 18, 18)
				.withRange(brush.getMin(index), brush.getMax(index) + 1)
				.writingTo(label)
				.titled(brush.getParamLabel(index).plainCopy())
				.setState(Math.max(brush.getMin(index), currentBrushParams[index]))
				.calling(value -> {
					currentBrushParams[param] = value;
					label.setX(left + 65 + 20 * param - font.width(label.text) / 2);
				});
			input.onChanged();
			addDynamic(label);
			addDynamic(input);
		}

		TerrainTools[] tools = brush.getSupportedTools();
		boolean supported = false;
		for (TerrainTools tool : tools)
			supported |= tool == currentTool;
		if (!supported)
			currentTool = tools[0];
		List<IconButton> toolButtons = new ArrayList<>();
		for (int i = 0; i < tools.length; i++) {
			TerrainTools tool = tools[i];
			IconButton button = new IconButton(left + 7 + i * 18, top + 79, tool.icon);
			button.green = tool == currentTool;
			button.withCallback(() -> {
				toolButtons.forEach(b -> b.green = false);
				button.green = true;
				currentTool = tool;
			});
			button.setToolTip(CreateLang.translateDirect("gui.terrainzapper.tool." + tool.translationKey));
			toolButtons.add(button);
			addDynamic(button);
		}

		if (brush.hasConnectivityOptions()) {
			IconButton diagonal = new IconButton(left + 79, top + 79, AllIcons.I_FOLLOW_DIAGONAL);
			diagonal.green = followDiagonals;
			diagonal.withCallback(() -> diagonal.green = followDiagonals = !followDiagonals);
			diagonal.setToolTip(CreateLang.translateDirect("gui.terrainzapper.searchDiagonal"));
			addDynamic(diagonal);
			IconButton materials = new IconButton(left + 97, top + 79, AllIcons.I_FOLLOW_MATERIAL);
			materials.green = acrossMaterials;
			materials.withCallback(() -> materials.green = acrossMaterials = !acrossMaterials);
			materials.setToolTip(CreateLang.translateDirect("gui.terrainzapper.searchFuzzy"));
			addDynamic(materials);
		}

		if (brush.hasPlacementOptions()) {
			List<IconButton> placementButtons = new ArrayList<>();
			PlacementOptions[] values = PlacementOptions.values();
			for (int i = 0; i < values.length; i++) {
				PlacementOptions option = values[i];
				IconButton button = new IconButton(left + 136 + i * 18, top + 79, option.icon);
				button.green = option == currentPlacement;
				button.withCallback(() -> {
					placementButtons.forEach(b -> b.green = false);
					button.green = true;
					currentPlacement = option;
				});
				button.setToolTip(CreateLang.translateDirect("gui.terrainzapper.placement." + option.translationKey));
				placementButtons.add(button);
				addDynamic(button);
			}
		}
	}

	private void addDynamic(AbstractWidget widget) {
		dynamicWidgets.add(widget);
		addRenderableWidget(widget);
	}

	@Override
	protected void drawOnBackground(GuiGraphicsExtractor graphics, int x, int y) {
		super.drawOnBackground(graphics, x, y);
		graphics.text(font, toolSection, x + 7, y + 69, 0xff767676, false);
		if (currentBrush.get().hasPlacementOptions())
			graphics.text(font, placementSection, x + 136, y + 69, 0xff767676, false);
		for (int index = currentBrush.get().amtParams; index < 3; index++)
			AllGuiTextures.TERRAINZAPPER_INACTIVE_PARAM.render(graphics, x + 56 + 20 * index, y + 40);
	}

	@Override
	protected ConfigureZapperPacket getConfigurationPacket() {
		int y = currentBrush.get().hasConnectivityOptions() && followDiagonals ? 0 : currentBrushParams[1];
		int z = currentBrush.get().hasConnectivityOptions() && acrossMaterials ? 0 : currentBrushParams[2];
		return new ConfigureWorldshaperPacket(hand, currentPattern, currentBrush,
			currentBrushParams[0], y, z, currentTool, currentPlacement);
	}
}
