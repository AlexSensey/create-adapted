package com.simibubi.create.content.logistics.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket.Option;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class AttributeFilterScreen extends AbstractFilterScreen<AttributeFilterMenu> {

	private static final String PREFIX = "gui.attribute_filter.";

	private final Component addDESC = CreateLang.translateDirect(PREFIX + "add_attribute");
	private final Component addInvertedDESC = CreateLang.translateDirect(PREFIX + "add_inverted_attribute");
	private final Component allowDisN = CreateLang.translateDirect(PREFIX + "allow_list_disjunctive");
	private final Component allowDisDESC = CreateLang.translateDirect(PREFIX + "allow_list_disjunctive.description");
	private final Component allowConN = CreateLang.translateDirect(PREFIX + "allow_list_conjunctive");
	private final Component allowConDESC = CreateLang.translateDirect(PREFIX + "allow_list_conjunctive.description");
	private final Component denyN = CreateLang.translateDirect(PREFIX + "deny_list");
	private final Component denyDESC = CreateLang.translateDirect(PREFIX + "deny_list.description");
	private final Component referenceH = CreateLang.translateDirect(PREFIX + "add_reference_item");
	private final Component noSelectedT = CreateLang.translateDirect(PREFIX + "no_selected_attributes");
	private final Component selectedT = CreateLang.translateDirect(PREFIX + "selected_attributes");

	private IconButton whitelistDis;
	private IconButton whitelistCon;
	private IconButton blacklist;
	private IconButton add;
	private IconButton addInverted;

	private ItemStack lastItemScanned = ItemStack.EMPTY;
	private final List<ItemAttribute> attributesOfItem = new ArrayList<>();
	private final List<Component> selectedAttributes = new ArrayList<>();
	private SelectionScrollInput attributeSelector;
	private Label attributeSelectorLabel;

	public AttributeFilterScreen(AttributeFilterMenu menu, Inventory inv, Component title) {
		super(menu, inv, title, AllGuiTextures.ATTRIBUTE_FILTER);
	}

	@Override
	protected void init() {
		setWindowOffset(-11, 7);
		super.init();

		int x = leftPos;
		int y = topPos;

		whitelistDis = new IconButton(x + 38, y + 61, AllIcons.I_WHITELIST_OR);
		whitelistDis.withCallback(() -> {
			menu.whitelistMode = AttributeFilterWhitelistMode.WHITELIST_DISJ;
			sendOptionUpdate(Option.WHITELIST);
		});
		whitelistDis.setToolTip(allowDisN);

		whitelistCon = new IconButton(x + 56, y + 61, AllIcons.I_WHITELIST_AND);
		whitelistCon.withCallback(() -> {
			menu.whitelistMode = AttributeFilterWhitelistMode.WHITELIST_CONJ;
			sendOptionUpdate(Option.WHITELIST2);
		});
		whitelistCon.setToolTip(allowConN);

		blacklist = new IconButton(x + 74, y + 61, AllIcons.I_WHITELIST_NOT);
		blacklist.withCallback(() -> {
			menu.whitelistMode = AttributeFilterWhitelistMode.BLACKLIST;
			sendOptionUpdate(Option.BLACKLIST);
		});
		blacklist.setToolTip(denyN);
		addRenderableWidgets(blacklist, whitelistCon, whitelistDis);

		add = new IconButton(x + 182, y + 26, AllIcons.I_ADD);
		add.withCallback(() -> handleAddedAttribute(false));
		add.setToolTip(addDESC);
		addInverted = new IconButton(x + 200, y + 26, AllIcons.I_ADD_INVERTED_ATTRIBUTE);
		addInverted.withCallback(() -> handleAddedAttribute(true));
		addInverted.setToolTip(addInvertedDESC);
		addRenderableWidgets(add, addInverted);

		attributeSelectorLabel = new Label(x + 43, y + 31, CommonComponents.EMPTY).colored(0xF3EBDE)
			.withShadow();
		attributeSelector = new SelectionScrollInput(x + 39, y + 26, 137, 18);
		attributeSelector.forOptions(List.of(CommonComponents.EMPTY));
		attributeSelector.removeCallback();
		referenceItemChanged(menu.ghostInventory.getStackInSlot(0));
		addRenderableWidgets(attributeSelector, attributeSelectorLabel);

		selectedAttributes.clear();
		selectedAttributes.add((menu.selectedAttributes.isEmpty() ? noSelectedT : selectedT).plainCopy()
			.withStyle(ChatFormatting.YELLOW));
		menu.selectedAttributes.forEach(attribute -> selectedAttributes.add(Component.literal("- ")
			.append(attribute.attribute()
				.format(attribute.inverted()))
			.withStyle(ChatFormatting.GRAY)));

		handleIndicators();
	}

	private void referenceItemChanged(ItemStack stack) {
		lastItemScanned = stack.copy();
		attributesOfItem.clear();

		if (stack.isEmpty() || minecraft.level == null) {
			attributeSelector.active = false;
			attributeSelector.visible = false;
			attributeSelectorLabel.setText(referenceH.plainCopy()
				.withStyle(ChatFormatting.ITALIC));
			add.active = false;
			addInverted.active = false;
			attributeSelector.calling(ignored -> {});
			return;
		}

		attributeSelector.titled(Component.literal(stack.getHoverName()
				.getString() + "...")
			.withStyle(style -> style.withColor(ScrollInput.HEADER_RGB.getRGB())));
		for (ItemAttributeType type : CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE)
			attributesOfItem.addAll(type.getAllAttributes(stack, minecraft.level));

		if (attributesOfItem.isEmpty()) {
			attributeSelector.forOptions(List.of(CommonComponents.EMPTY));
			attributeSelectorLabel.setText(CommonComponents.EMPTY);
			attributeSelector.active = false;
			attributeSelector.visible = true;
			add.active = false;
			addInverted.active = false;
			return;
		}

		List<Component> options = attributesOfItem.stream()
			.map(attribute -> attribute.format(false))
			.collect(Collectors.toList());
		attributeSelector.forOptions(options);
		attributeSelector.active = true;
		attributeSelector.visible = true;
		attributeSelector.setState(0);
		attributeSelector.calling(index -> updateSelectedAttribute(options, index));
		attributeSelector.onChanged();
	}

	private void updateSelectedAttribute(List<Component> options, int index) {
		if (index < 0 || index >= attributesOfItem.size())
			return;
		attributeSelectorLabel.setTextAndTrim(options.get(index), true, 112);
		ItemAttribute selected = attributesOfItem.get(index);
		HolderLookup.Provider registries = minecraft.level.registryAccess();
		CompoundTag selectedTag = ItemAttribute.saveStatic(selected, registries);
		for (ItemAttribute.ItemAttributeEntry existing : menu.selectedAttributes) {
			if (ItemAttribute.saveStatic(existing.attribute(), registries)
				.equals(selectedTag)) {
				add.active = false;
				addInverted.active = false;
				return;
			}
		}
		add.active = true;
		addInverted.active = true;
	}

	@Override
	protected void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		ItemStack stack = menu.ghostInventory.getStackInSlot(1);
		if (!stack.isEmpty())
			graphics.itemDecorations(font, stack, leftPos + 16, topPos + 62,
				String.valueOf(Math.max(0, selectedAttributes.size() - 1)));

		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
		if (menu.getCarried()
			.isEmpty() && hoveredSlot != null && hoveredSlot.hasItem() && hoveredSlot.index == 37)
			graphics.setComponentTooltipForNextFrame(font, selectedAttributes, mouseX, mouseY);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		ItemStack stackInSlot = menu.ghostInventory.getStackInSlot(0);
		if (!ItemStack.isSameItemSameComponents(stackInSlot, lastItemScanned))
			referenceItemChanged(stackInSlot);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (attributeSelector != null && attributeSelector.visible && attributeSelector.active
			&& attributeSelector.isMouseOver(mouseX, mouseY)
			&& attributeSelector.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
			return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	protected List<IconButton> getTooltipButtons() {
		return Arrays.asList(blacklist, whitelistCon, whitelistDis);
	}

	@Override
	protected List<MutableComponent> getTooltipDescriptions() {
		return Arrays.asList(denyDESC.plainCopy(), allowConDESC.plainCopy(), allowDisDESC.plainCopy());
	}

	protected boolean handleAddedAttribute(boolean inverted) {
		int index = attributeSelector.getState();
		if (index < 0 || index >= attributesOfItem.size() || minecraft.level == null)
			return false;

		add.active = false;
		addInverted.active = false;
		ItemAttribute itemAttribute = attributesOfItem.get(index);
		CompoundTag tag = ItemAttribute.saveStatic(itemAttribute, minecraft.level.registryAccess());
		ClientNetworkHelper.INSTANCE.sendToServer(
			new FilterScreenPacket(inverted ? Option.ADD_INVERTED_TAG : Option.ADD_TAG, tag));
		menu.appendSelectedAttribute(itemAttribute, inverted);
		if (menu.selectedAttributes.size() == 1)
			selectedAttributes.set(0, selectedT.plainCopy()
				.withStyle(ChatFormatting.YELLOW));
		selectedAttributes.add(Component.literal("- ")
			.append(itemAttribute.format(inverted))
			.withStyle(ChatFormatting.GRAY));
		return true;
	}

	@Override
	protected void contentsCleared() {
		selectedAttributes.clear();
		selectedAttributes.add(noSelectedT.plainCopy()
			.withStyle(ChatFormatting.YELLOW));
		if (!lastItemScanned.isEmpty()) {
			add.active = true;
			addInverted.active = true;
		}
	}

	@Override
	protected boolean isButtonEnabled(IconButton button) {
		if (button == blacklist)
			return menu.whitelistMode != AttributeFilterWhitelistMode.BLACKLIST;
		if (button == whitelistCon)
			return menu.whitelistMode != AttributeFilterWhitelistMode.WHITELIST_CONJ;
		if (button == whitelistDis)
			return menu.whitelistMode != AttributeFilterWhitelistMode.WHITELIST_DISJ;
		return true;
	}
}
