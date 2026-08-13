package com.simibubi.create.content.logistics.stockTicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import com.simibubi.create.content.trains.station.NoShadowFontWrapper;

public class StockKeeperRequestScreen extends AbstractSimiContainerScreen<StockKeeperRequestMenu> {

	private static final AllGuiTextures HEADER = AllGuiTextures.STOCK_KEEPER_REQUEST_HEADER;
	private static final AllGuiTextures BODY = AllGuiTextures.STOCK_KEEPER_REQUEST_BODY;
	private static final AllGuiTextures FOOTER = AllGuiTextures.STOCK_KEEPER_REQUEST_FOOTER;
	private static final int COLS = 9;
	private static final int SLOT = 20;
	private static final int WINDOW_WIDTH = 226;

	public static class CategoryEntry {
		public boolean hidden;
		public String name;
		public int y;
		public int targetBECategory;

		public CategoryEntry(int targetBECategory, String name, int y) {
			this.targetBECategory = targetBECategory;
			this.name = name;
			this.y = y;
		}
	}

	private final StockTickerBlockEntity blockEntity;
	private final boolean isAdmin;
	private boolean isLocked;
	public EditBox searchBox;
	private AddressEditBox addressBox;
	private String previousSearch = "";
	private int firstRow;
	private int panelHeight;
	private int visibleRows;
	private int itemsX;
	private int itemsY;
	private int orderY;
	private final boolean encodeRequester;
	private final ItemStack itemToProgram;
	private List<List<ClipboardEntry>> clipboardItems;
	private boolean canRequestCraftingPackage;

	public List<List<BigItemStack>> currentItemSource = Collections.emptyList();
	public List<List<BigItemStack>> displayedItems = new ArrayList<>();
	public List<CategoryEntry> categories = new ArrayList<>();
	public List<BigItemStack> itemsToOrder = new ArrayList<>();
	public List<CraftableBigItemStack> recipesToOrder = new ArrayList<>();
	public boolean refreshSearchNextTick;
	public boolean moveToTopNextTick;

	private List<BigItemStack> flattenedItems = new ArrayList<>();
	private final List<Rect2i> extraAreas = new ArrayList<>();

	public StockKeeperRequestScreen(StockKeeperRequestMenu container, Inventory inventory, Component title) {
		super(container, inventory, title);
		blockEntity = container.contentHolder;
		isAdmin = container.isAdmin;
		isLocked = container.isLocked;
		container.screenReference = this;
		itemToProgram = container.player.getMainHandItem();
		encodeRequester =
			AllItemTags.TABLE_CLOTHS.matches(itemToProgram) || AllBlocks.REDSTONE_REQUESTER.isIn(itemToProgram);
		if (AllBlocks.CLIPBOARD.isIn(itemToProgram)) {
			clipboardItems = ClipboardEntry.readAll(itemToProgram);
			boolean hasRequestedItems = clipboardItems.stream()
				.flatMap(List::stream)
				.anyMatch(entry -> !entry.icon.isEmpty() && entry.itemAmount > 0);
			if (!hasRequestedItems)
				clipboardItems = null;
		}
		if (blockEntity != null) {
			blockEntity.lastClientsideStockSnapshot = null;
			blockEntity.ticksSinceLastUpdate = 15;
		}
	}

	@Override
	protected void init() {
		panelHeight = minecraft.getWindow()
			.getGuiScaledHeight() - 10;
		panelHeight -= Mth.positiveModulo(panelHeight - HEADER.getHeight() - FOOTER.getHeight(), BODY.getHeight());
		panelHeight = Math.min(panelHeight,
			HEADER.getHeight() + FOOTER.getHeight() + BODY.getHeight() * 17);
		visibleRows = Math.max(3, (panelHeight - 116) / SLOT);
		setWindowSize(WINDOW_WIDTH, panelHeight);
		setWindowOffset(-50, 0);
		super.init();
		clearWidgets();

		itemsX = leftPos + (WINDOW_WIDTH - COLS * SLOT) / 2 + 1;
		itemsY = topPos + 33;
		orderY = topPos + panelHeight - 72;

		Font noShadowFont = new NoShadowFontWrapper(font);
		searchBox = new EditBox(noShadowFont, leftPos + 71, topPos + 22, 100, 9,
			CreateLang.translate("gui.stock_keeper.search_items").component());
		searchBox.setBordered(false);
		searchBox.setMaxLength(50);
		addRenderableWidget(searchBox);

		addressBox = new AddressEditBox(this, noShadowFont, leftPos + 27, topPos + panelHeight - 36, 92, 10, true);
		addressBox.setTextColor(0xff714A40);
		addressBox.setValue(blockEntity == null ? "" : blockEntity.previouslyUsedAddress);
		addRenderableWidget(addressBox);

		extraAreas.clear();
		extraAreas.add(new Rect2i(leftPos - 15, topPos, 15, panelHeight));
		extraAreas.add(new Rect2i(leftPos + imageWidth, topPos, WINDOW_WIDTH - imageWidth + 15, panelHeight));
		extraAreas.add(new Rect2i(leftPos + WINDOW_WIDTH, topPos + panelHeight - 55, 32, 48));
		refreshStock();
	}

	private void refreshStock() {
		if (blockEntity != null)
			blockEntity.refreshClientStockSnapshot();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (addressBox != null)
			addressBox.tick();
		if (blockEntity == null)
			return;

		if (blockEntity.getTicksSinceLastUpdate() > 15)
			refreshStock();

		List<List<BigItemStack>> snapshot = blockEntity.getClientStockSnapshot();
		String search = searchBox == null ? "" : searchBox.getValue();
		if (snapshot != null && (snapshot != currentItemSource || !search.equals(previousSearch))) {
			currentItemSource = snapshot;
			previousSearch = search;
			rebuildDisplayedItems(search);
			revalidateOrders();
		}
	}

	private void revalidateOrders() {
		if (blockEntity == null)
			return;
		InventorySummary summary = blockEntity.getLastClientsideStockSnapshotAsSummary();
		itemsToOrder.removeIf(order -> {
			int available = summary.getCountOf(order.stack);
			if (available == BigItemStack.INF)
				return false;
			order.count = Math.min(order.count, available);
			return order.count <= 0;
		});
		updateCraftableAmounts();
	}

	private void rebuildDisplayedItems(String search) {
		displayedItems = new ArrayList<>();
		flattenedItems = new ArrayList<>();
		if (isSchematicListMode()) {
			requestSchematicList();
			firstRow = 0;
			return;
		}
		String needle = search.toLowerCase(Locale.ROOT).strip();

		for (List<BigItemStack> category : currentItemSource) {
			List<BigItemStack> visibleCategory = new ArrayList<>();
			for (BigItemStack entry : category) {
				if (!needle.isEmpty() && !entry.stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle))
					continue;
				visibleCategory.add(entry);
				flattenedItems.add(entry);
			}
			displayedItems.add(visibleCategory);
		}
		firstRow = Mth.clamp(firstRow, 0, maxFirstRow());
	}

	private int maxFirstRow() {
		return Math.max(0, Mth.ceil(flattenedItems.size() / (float) COLS) - visibleRows);
	}

	private int hoveredStockIndex(double mouseX, double mouseY) {
		int x = (int) mouseX - itemsX;
		int y = (int) mouseY - (itemsY + 4);
		if (x < 0 || y < 0 || x >= COLS * SLOT || y >= visibleRows * SLOT)
			return -1;
		int index = (firstRow + y / SLOT) * COLS + x / SLOT;
		return index < flattenedItems.size() ? index : -1;
	}

	private BigItemStack selectedFor(ItemStack stack) {
		for (BigItemStack selected : itemsToOrder)
			if (ItemStack.isSameItemSameComponents(selected.stack, stack))
				return selected;
		return null;
	}

	private void changeOrder(BigItemStack stock, int change) {
		BigItemStack selected = selectedFor(stock.stack);
		if (selected == null) {
			if (change < 0 || itemsToOrder.size() >= COLS)
				return;
			selected = new BigItemStack(stock.stack.copyWithCount(1), 0);
			itemsToOrder.add(selected);
		}

		int available = stock.count == BigItemStack.INF ? Integer.MAX_VALUE : stock.count;
		selected.count = Mth.clamp(selected.count + change, 0, available);
		if (selected.count == 0)
			itemsToOrder.remove(selected);
	}

	private int hoveredOrderIndex(double mouseX, double mouseY) {
		if (mouseX < itemsX || mouseX >= itemsX + COLS * SLOT || mouseY < orderY || mouseY >= orderY + SLOT)
			return -1;
		int index = ((int) mouseX - itemsX) / SLOT;
		return index < itemsToOrder.size() ? index : -1;
	}

	private int hoveredRecipeIndex(double mouseX, double mouseY) {
		if (recipesToOrder.isEmpty() || mouseY < orderY - 31 || mouseY >= orderY - 31 + SLOT)
			return -1;
		int recipeX = leftPos + (WINDOW_WIDTH - SLOT * recipesToOrder.size()) / 2 + 1;
		if (mouseX < recipeX)
			return -1;
		int index = ((int) mouseX - recipeX) / SLOT;
		return index < recipesToOrder.size() ? index : -1;
	}

	private boolean isShiftDown() {
		return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
			|| InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	private boolean isControlDown() {
		return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
	}

	private int transferAmount(ItemStack stack) {
		return isShiftDown() ? stack.getMaxStackSize() : isControlDown() ? 10 : 1;
	}

	private void reduceOrder(int index, int amount) {
		if (index < 0 || index >= itemsToOrder.size())
			return;
		BigItemStack order = itemsToOrder.get(index);
		order.count -= amount;
		if (order.count <= 0)
			itemsToOrder.remove(index);
	}

	private void sendOrder() {
		if (blockEntity == null || itemsToOrder.isEmpty())
			return;
		updateCraftableAmounts();
		PackageOrderWithCrafts order = PackageOrderWithCrafts.simple(itemsToOrder);
		if (canRequestCraftingPackage && !recipesToOrder.isEmpty()) {
			List<CraftingEntry> crafts = new ArrayList<>();
			for (CraftableBigItemStack craftable : recipesToOrder) {
				if (!(craftable.recipe instanceof CraftingRecipe craftingRecipe))
					continue;
				int outputCount = craftable.getOutputCount(blockEntity.getLevel());
				int targetCrafts = craftable.count / outputCount;
				if (targetCrafts <= 0)
					continue;
				List<BigItemStack> mutableOrder = BigItemStack.duplicateWrappers(itemsToOrder);
				List<BigItemStack> pattern = convertRecipeToPackageOrderContext(craftingRecipe, mutableOrder);
				if (pattern.stream().noneMatch(entry -> !entry.stack.isEmpty()))
					continue;
				crafts.add(new CraftingEntry(new PackageOrder(pattern), targetCrafts));
			}
			if (!crafts.isEmpty())
				order = new PackageOrderWithCrafts(new PackageOrder(itemsToOrder), crafts);
		}
		ClientNetworkHelper.INSTANCE.sendToServer(new PackageOrderRequestPacket(blockEntity.getBlockPos(), order,
			addressBox.getValue(), encodeRequester));
		itemsToOrder = new ArrayList<>();
		recipesToOrder = new ArrayList<>();
		blockEntity.ticksSinceLastUpdate = 10;
		playUiSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1, 1);
		if (isSchematicListMode() || encodeRequester)
			menu.player.closeContainer();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && Mods.JEI.isLoaded() && event.x() > leftPos + 25 && event.x() <= leftPos + 40
			&& event.y() > topPos + 18 && event.y() <= topPos + 33) {
			SearchSyncMode.cycleConfig();
			playUiSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1, 1);
			return true;
		}
		if (event.button() == 0 && isAdmin && event.x() > leftPos + 186 && event.x() <= leftPos + 201
			&& event.y() > topPos + 18 && event.y() <= topPos + 33) {
			isLocked = !isLocked;
			ClientNetworkHelper.INSTANCE.sendToServer(new StockKeeperLockPacket(blockEntity.getBlockPos(), isLocked));
			playUiSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1, 1);
			return true;
		}
		if (event.button() == 0 && event.x() >= leftPos + 143 && event.x() < leftPos + 221
			&& event.y() >= topPos + panelHeight - 39 && event.y() < topPos + panelHeight - 21) {
			sendOrder();
			return true;
		}
		int recipeIndex = hoveredRecipeIndex(event.x(), event.y());
		if (recipeIndex >= 0 && (event.button() == 0 || event.button() == 1)) {
			CraftableBigItemStack craftable = recipesToOrder.get(recipeIndex);
			requestCraftable(craftable,
				(event.button() == 1 ? -1 : 1) * transferAmount(craftable.stack));
			if (craftable.count <= 0)
				recipesToOrder.remove(craftable);
			return true;
		}
		int orderIndex = hoveredOrderIndex(event.x(), event.y());
		if (orderIndex >= 0 && (event.button() == 0 || event.button() == 1)) {
			BigItemStack order = itemsToOrder.get(orderIndex);
			reduceOrder(orderIndex, transferAmount(order.stack));
			return true;
		}
		int index = hoveredStockIndex(event.x(), event.y());
		if (index >= 0 && (event.button() == 0 || event.button() == 1)) {
			BigItemStack stock = flattenedItems.get(index);
			int transfer = transferAmount(stock.stack);
			changeOrder(stock, event.button() == 1 ? -transfer : transfer);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (addressBox != null && addressBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
			return true;
		int recipeIndex = hoveredRecipeIndex(mouseX, mouseY);
		if (recipeIndex >= 0) {
			CraftableBigItemStack craftable = recipesToOrder.get(recipeIndex);
			int transfer = Mth.ceil(Math.abs(scrollY)) * (isControlDown() ? 10 : 1);
			requestCraftable(craftable, scrollY < 0 ? -transfer : transfer);
			if (craftable.count <= 0)
				recipesToOrder.remove(craftable);
			return true;
		}
		int orderIndex = hoveredOrderIndex(mouseX, mouseY);
		if (orderIndex >= 0) {
			BigItemStack order = itemsToOrder.get(orderIndex);
			int transfer = Mth.ceil(Math.abs(scrollY)) * (isControlDown() ? 10 : 1);
			if (scrollY < 0)
				reduceOrder(orderIndex, transfer);
			else {
				BigItemStack stock = findStock(order.stack);
				if (stock != null)
					changeOrder(stock, transfer);
			}
			return true;
		}
		int stockIndex = hoveredStockIndex(mouseX, mouseY);
		if (stockIndex >= 0 && (isShiftDown() || maxFirstRow() == 0)) {
			BigItemStack stock = flattenedItems.get(stockIndex);
			int transfer = Mth.ceil(Math.abs(scrollY)) * (isControlDown() ? 10 : 1);
			changeOrder(stock, scrollY < 0 ? -transfer : transfer);
			return true;
		}
		if (stockIndex >= 0 || mouseX >= itemsX && mouseX < itemsX + COLS * SLOT) {
			firstRow = Mth.clamp(firstRow - (int) Math.signum(scrollY), 0, maxFirstRow());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private BigItemStack findStock(ItemStack stack) {
		for (BigItemStack stock : flattenedItems)
			if (ItemStack.isSameItemSameComponents(stock.stack, stack))
				return stock;
		return null;
	}

	@Override
	protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
		int y = topPos;
		HEADER.render(graphics, leftPos - 15, y);
		y += HEADER.getHeight();
		int bodySlices = (panelHeight - HEADER.getHeight() - FOOTER.getHeight()) / BODY.getHeight();
		for (int row = 0; row < bodySlices; row++) {
			BODY.render(graphics, leftPos - 15, y);
			y += BODY.getHeight();
		}
		FOOTER.render(graphics, leftPos - 15, y);

		graphics.enableScissor(leftPos + 16, topPos + 17, leftPos + 216, topPos + panelHeight - 80);
		for (int sliceY = -2; sliceY < panelHeight - 72; sliceY += AllGuiTextures.STOCK_KEEPER_REQUEST_BG.getHeight())
			AllGuiTextures.STOCK_KEEPER_REQUEST_BG.render(graphics, leftPos + 22, topPos + sliceY + 18);
		AllGuiTextures.STOCK_KEEPER_REQUEST_SEARCH.render(graphics, leftPos + 42, topPos + 17);

		if (Mods.JEI.isLoaded()) {
			AllGuiTextures syncIcon = switch (AllConfigs.client().syncRecipeViewerSearch.get()) {
				case NONE -> AllGuiTextures.STOCK_KEEPER_SEARCH_SYNC_DISABLED;
				case SYNC_FROM_JEI -> AllGuiTextures.STOCK_KEEPER_SEARCH_SYNC_FROM_JEI;
				case SYNC_FROM_STOCK_KEEPER -> AllGuiTextures.STOCK_KEEPER_SEARCH_SYNC_FROM_STOCK_KEEPER;
				case SYNC_BOTH -> AllGuiTextures.STOCK_KEEPER_SEARCH_SYNC_BOTH;
			};
			syncIcon.render(graphics, leftPos + 25, topPos + 18);
		}
		if (isAdmin)
			(isLocked ? AllGuiTextures.STOCK_KEEPER_REQUEST_LOCKED : AllGuiTextures.STOCK_KEEPER_REQUEST_UNLOCKED)
				.render(graphics, leftPos + 186, topPos + 18);

		int start = firstRow * COLS;
		for (int visible = 0; visible < visibleRows * COLS; visible++) {
			int index = start + visible;
			if (index >= flattenedItems.size())
				break;
			BigItemStack entry = flattenedItems.get(index);
			int col = visible % COLS;
			int row = visible / COLS;
			int slotX = itemsX + col * SLOT;
			int slotY = itemsY + 4 + row * SLOT;
			AllGuiTextures.STOCK_KEEPER_REQUEST_SLOT.render(graphics, slotX - 1, slotY - 1);
			graphics.item(entry.stack, slotX, slotY);
			String count = entry.count == BigItemStack.INF ? "∞" : abbreviate(entry.count);
			graphics.itemDecorations(font, entry.stack, slotX, slotY, count);
		}
		graphics.disableScissor();

		if (!recipesToOrder.isEmpty()) {
			int recipeX = leftPos + (WINDOW_WIDTH - SLOT * recipesToOrder.size()) / 2 + 1;
			int recipeY = orderY - 31;
			AllGuiTextures.STOCK_KEEPER_REQUEST_BLUEPRINT_LEFT.render(graphics, recipeX - 3, recipeY - 3);
			int bannerX = recipeX + 7;
			for (int i = 0; i <= (recipesToOrder.size() - 1) * 5; i++) {
				AllGuiTextures.STOCK_KEEPER_REQUEST_BLUEPRINT_MIDDLE.render(graphics, bannerX, recipeY - 3);
				bannerX += 4;
			}
			AllGuiTextures.STOCK_KEEPER_REQUEST_BLUEPRINT_RIGHT.render(graphics, bannerX, recipeY - 3);
			for (int i = 0; i < recipesToOrder.size(); i++) {
				CraftableBigItemStack craftable = recipesToOrder.get(i);
				int x = recipeX + i * SLOT;
				graphics.item(craftable.stack, x, recipeY);
				graphics.itemDecorations(font, craftable.stack, x, recipeY, Integer.toString(craftable.count));
			}
		}

		for (int i = 0; i < itemsToOrder.size() && i < COLS; i++) {
			BigItemStack entry = itemsToOrder.get(i);
			int x = itemsX + i * SLOT;
			AllGuiTextures.STOCK_KEEPER_REQUEST_SLOT.render(graphics, x - 1, orderY - 1);
			graphics.item(entry.stack, x, orderY);
			String count = Integer.toString(entry.count);
			graphics.itemDecorations(font, entry.stack, x, orderY, count);
		}
		if (itemsToOrder.size() > COLS)
			graphics.text(font, Component.literal("[+" + (itemsToOrder.size() - COLS) + "]"),
				leftPos + WINDOW_WIDTH - 40, orderY + 21, 0xffF8F8EC, false);

		graphics.item(AllBlocks.CLIPBOARD.asStack(), leftPos + 120, topPos + panelHeight - 40);

		Component send = CreateLang.translate(encodeRequester ? "gui.stock_keeper.configure" : "gui.stock_keeper.send")
			.component();
		graphics.text(font, send, leftPos + WINDOW_WIDTH - 42 - font.width(send) / 2,
			topPos + panelHeight - 35, 0xff252525, false);
	}

	private static String abbreviate(int count) {
		if (count < 1000)
			return Integer.toString(count);
		if (count < 1_000_000)
			return (count / 1000) + "k";
		return (count / 1_000_000) + "m";
	}

	@Override
	protected void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
		if (encodeRequester)
			GuiGameElement.of(itemToProgram)
				.<GuiGameElement.GuiRenderBuilder>at(leftPos + WINDOW_WIDTH + 5, topPos + panelHeight - 70, -190)
				.scale(3.5)
				.submit(graphics);
		Component title = CreateLang.translate("gui.stock_keeper.title").component();
		graphics.text(font, title, leftPos + WINDOW_WIDTH / 2 - font.width(title) / 2, topPos + 4, 0xff714A40, false);
		if (searchBox.getValue().isBlank() && !searchBox.isFocused()) {
			Component hint = CreateLang.translate("gui.stock_keeper.search_items").component();
			graphics.text(font, hint, leftPos + WINDOW_WIDTH / 2 - font.width(hint) / 2,
				searchBox.getY(), 0xff4A2D31, false);
		}
		if (addressBox.getValue().isBlank() && !addressBox.isFocused()) {
			Component hint = CreateLang.translate("gui.stock_keeper.package_address")
				.style(ChatFormatting.ITALIC)
				.component();
			graphics.text(font, hint, addressBox.getX(), addressBox.getY(), 0xff9F8A78, false);
		}
		int recipeIndex = hoveredRecipeIndex(mouseX, mouseY);
		if (recipeIndex >= 0) {
			CraftableBigItemStack craftable = recipesToOrder.get(recipeIndex);
			graphics.setComponentTooltipForNextFrame(font,
				List.of(CreateLang.translateDirect("gui.stock_keeper.craft", craftable.stack.getHoverName()),
					Component.literal(Integer.toString(craftable.count)).withStyle(ChatFormatting.GRAY)),
				mouseX, mouseY);
			return;
		}
		int index = hoveredStockIndex(mouseX, mouseY);
		if (index < 0)
			return;
		BigItemStack entry = flattenedItems.get(index);
		List<Component> tooltip = List.of(entry.stack.getHoverName(),
			Component.literal(entry.count == BigItemStack.INF ? "∞" : Integer.toString(entry.count))
				.withStyle(ChatFormatting.GRAY),
			CreateLang.translate("gui.stock_keeper.lmb_order").style(ChatFormatting.DARK_GRAY).component());
		graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
	}

	@Override
	public void removed() {
		if (blockEntity != null)
			ClientNetworkHelper.INSTANCE.sendToServer(new PackageOrderRequestPacket(blockEntity.getBlockPos(),
				PackageOrderWithCrafts.empty(), addressBox == null ? "" : addressBox.getValue(), false));
		super.removed();
	}

	public Optional<Pair<ItemStack, Rect2i>> getHoveredIngredient(int mouseX, int mouseY) {
		int index = hoveredStockIndex(mouseX, mouseY);
		if (index < 0)
			return Optional.empty();
		int visible = index - firstRow * COLS;
		int x = itemsX + visible % COLS * SLOT;
		int y = itemsY + 4 + visible / COLS * SLOT;
		return Optional.of(Pair.of(flattenedItems.get(index).stack, new Rect2i(x, y, 18, 18)));
	}

	public boolean isSchematicListMode() {
		return clipboardItems != null;
	}

	public void requestSchematicList() {
		if (clipboardItems == null || blockEntity == null)
			return;
		itemsToOrder.clear();
		InventorySummary availableItems = blockEntity.getLastClientsideStockSnapshotAsSummary();
		for (List<ClipboardEntry> page : clipboardItems) {
			for (ClipboardEntry entry : page) {
				if (entry.icon.isEmpty() || entry.itemAmount <= 0)
					continue;
				int available = availableItems.getCountOf(entry.icon);
				int toOrder = available == BigItemStack.INF ? entry.itemAmount : Math.min(entry.itemAmount, available);
				if (toOrder <= 0)
					continue;
				BigItemStack existing = selectedFor(entry.icon);
				if (existing == null)
					itemsToOrder.add(new BigItemStack(entry.icon.copyWithCount(1), toOrder));
				else
					existing.count += toOrder;
			}
		}
	}

	public void requestCraftable(CraftableBigItemStack cbis, int requestedDifference) {
		if (!recipesToOrder.contains(cbis))
			recipesToOrder.add(cbis);
		boolean remove = requestedDifference < 0;
		if (remove)
			requestedDifference = Math.max(-cbis.count, requestedDifference);
		if (requestedDifference == 0)
			return;

		InventorySummary availableItems = blockEntity.getLastClientsideStockSnapshotAsSummary();
		Function<ItemStack, Integer> countModifier = stack -> {
			BigItemStack ordered = selectedFor(stack);
			return ordered == null ? 0 : -ordered.count;
		};
		if (remove) {
			availableItems = new InventorySummary();
			for (BigItemStack ordered : itemsToOrder)
				availableItems.add(ordered.stack, ordered.count);
			countModifier = stack -> 0;
		}

		Pair<Integer, List<List<BigItemStack>>> craftingResult =
			maxCraftable(cbis, availableItems, countModifier, remove ? -1 : COLS - itemsToOrder.size());
		int outputCount = cbis.getOutputCount(blockEntity.getLevel());
		int adjusted = Mth.ceil(Math.abs(requestedDifference) / (float) outputCount) * outputCount;
		int transferable = Math.min(adjusted, craftingResult.getFirst());
		if (transferable == 0)
			return;

		cbis.count += remove ? -transferable : transferable;
		for (List<BigItemStack> validEntries : craftingResult.getSecond()) {
			int remaining = transferable / outputCount;
			for (BigItemStack entry : validEntries) {
				if (remaining <= 0)
					break;
				int toTransfer = Math.min(remaining, entry.count);
				BigItemStack order = selectedFor(entry.stack);
				if (remove) {
					if (order != null) {
						order.count -= toTransfer;
						if (order.count <= 0)
							itemsToOrder.remove(order);
					}
				} else {
					if (order == null) {
						order = new BigItemStack(entry.stack.copyWithCount(1), 0);
						itemsToOrder.add(order);
					}
					order.count += toTransfer;
				}
				remaining -= toTransfer;
			}
		}
		updateCraftableAmounts();
	}

	private void updateCraftableAmounts() {
		if (recipesToOrder.isEmpty()) {
			canRequestCraftingPackage = false;
			return;
		}
		InventorySummary usedItems = new InventorySummary();
		InventorySummary availableItems = new InventorySummary();
		for (BigItemStack ordered : itemsToOrder)
			availableItems.add(ordered.stack, ordered.count);

		for (CraftableBigItemStack craftable : recipesToOrder) {
			Pair<Integer, List<List<BigItemStack>>> result =
				maxCraftable(craftable, availableItems, stack -> -usedItems.getCountOf(stack), -1);
			craftable.count = Math.min(craftable.count, result.getFirst());
			int outputCount = craftable.getOutputCount(blockEntity.getLevel());
			for (List<BigItemStack> validEntries : result.getSecond()) {
				int remaining = craftable.count / outputCount;
				for (BigItemStack entry : validEntries) {
					if (remaining <= 0)
						break;
					int used = Math.min(remaining, entry.count);
					usedItems.add(entry.stack, used);
					remaining -= used;
				}
			}
		}

		recipesToOrder.removeIf(recipe -> recipe.count <= 0);
		canRequestCraftingPackage = !recipesToOrder.isEmpty();
		if (canRequestCraftingPackage)
			for (BigItemStack ordered : itemsToOrder)
				if (usedItems.getCountOf(ordered.stack) != ordered.count) {
					canRequestCraftingPackage = false;
					break;
				}
	}

	private Pair<Integer, List<List<BigItemStack>>> maxCraftable(CraftableBigItemStack craftable,
		InventorySummary summary, Function<ItemStack, Integer> countModifier, int newTypeLimit) {
		List<List<BigItemStack>> validEntriesByIngredient = new ArrayList<>();
		List<BigItemStack> alreadyCreated = new ArrayList<>();
		for (Ingredient ingredient : craftable.getIngredients()) {
			if (ingredient.isEmpty())
				continue;
			List<BigItemStack> valid = new ArrayList<>();
			for (List<BigItemStack> list : summary.getItemMap().values())
				Entries: for (BigItemStack entry : list) {
					if (!ingredient.test(entry.stack))
						continue;
					for (BigItemStack visited : alreadyCreated)
						if (ItemStack.isSameItemSameComponents(visited.stack, entry.stack)) {
							valid.add(visited);
							continue Entries;
						}
					int count = summary.getCountOf(entry.stack);
					if (count < BigItemStack.INF)
						count += countModifier.apply(entry.stack);
					count = Math.min(count, 4096);
					BigItemStack candidate = new BigItemStack(entry.stack, count);
					if (candidate.count > 0) {
						valid.add(candidate);
						alreadyCreated.add(candidate);
					}
				}
			if (valid.isEmpty())
				return Pair.of(0, List.of());
			valid.sort((a, b) -> -Integer.compare(summary.getCountOf(a.stack), summary.getCountOf(b.stack)));
			validEntriesByIngredient.add(valid);
		}

		if (newTypeLimit != -1) {
			int toRemove = (int) validEntriesByIngredient.stream()
				.flatMap(List::stream)
				.filter(entry -> selectedFor(entry.stack) == null)
				.distinct()
				.count() - newTypeLimit;
			for (int i = 0; i < toRemove; i++)
				removeLeastEssentialItemStack(validEntriesByIngredient);
		}

		List<List<BigItemStack>> resolved = resolveIngredientAmounts(validEntriesByIngredient);
		int minCount = Integer.MAX_VALUE;
		for (List<BigItemStack> list : resolved) {
			int sum = 0;
			for (BigItemStack entry : list)
				sum += entry.count;
			minCount = Math.min(sum, minCount);
		}
		if (minCount == Integer.MAX_VALUE || minCount == 0)
			return Pair.of(0, List.of());
		return Pair.of(minCount * craftable.getOutputCount(blockEntity.getLevel()), resolved);
	}

	private void removeLeastEssentialItemStack(List<List<BigItemStack>> validIngredients) {
		List<BigItemStack> longest = null;
		int most = 0;
		for (List<BigItemStack> list : validIngredients) {
			int count = (int) list.stream().filter(entry -> selectedFor(entry.stack) == null).count();
			if (longest != null && count <= most)
				continue;
			longest = list;
			most = count;
		}
		if (longest == null || longest.isEmpty())
			return;
		BigItemStack chosen = null;
		for (int i = longest.size() - 1; i >= 0; i--) {
			BigItemStack entry = longest.get(i);
			if (selectedFor(entry.stack) == null) {
				chosen = entry;
				break;
			}
		}
		if (chosen != null)
			for (List<BigItemStack> list : validIngredients)
				list.remove(chosen);
	}

	private List<List<BigItemStack>> resolveIngredientAmounts(List<List<BigItemStack>> validIngredients) {
		List<List<BigItemStack>> resolved = new ArrayList<>();
		for (int i = 0; i < validIngredients.size(); i++)
			resolved.add(new ArrayList<>());
		boolean everythingTaken = false;
		while (!everythingTaken) {
			everythingTaken = true;
			Ingredients: for (int i = 0; i < validIngredients.size(); i++) {
				for (BigItemStack available : validIngredients.get(i)) {
					if (available.count == 0)
						continue;
					available.count--;
					everythingTaken = false;
					for (BigItemStack assigned : resolved.get(i))
						if (assigned.stack == available.stack) {
							assigned.count++;
							continue Ingredients;
						}
					resolved.get(i).add(new BigItemStack(available.stack, 1));
					continue Ingredients;
				}
			}
		}
		return resolved;
	}

	private List<BigItemStack> convertRecipeToPackageOrderContext(CraftingRecipe recipe, List<BigItemStack> inputs) {
		List<BigItemStack> pattern = new ArrayList<>();
		BigItemStack empty = new BigItemStack(ItemStack.EMPTY, 1);
		List<Optional<Ingredient>> ingredients;
		int width;
		int height;
		if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
			ingredients = shaped.pattern.ingredients();
			width = shaped.getWidth();
			height = shaped.getHeight();
		} else {
			List<Ingredient> expanded = CraftableBigItemStack.ingredientsOf(recipe);
			ingredients = expanded.stream().map(Optional::of).toList();
			width = Math.min(3, ingredients.size());
			height = Math.min(3, ingredients.size() / 3 + 1);
		}
		if (height == 1)
			for (int i = 0; i < 3; i++)
				pattern.add(empty);
		if (width == 1)
			pattern.add(empty);
		for (int i = 0; i < ingredients.size(); i++) {
			Optional<Ingredient> ingredient = ingredients.get(i);
			BigItemStack selected = empty;
			if (ingredient.isPresent())
				for (BigItemStack input : inputs)
					if (input.count > 0 && ingredient.get().test(input.stack)) {
						selected = new BigItemStack(input.stack, 1);
						input.count--;
						break;
					}
			pattern.add(selected);
			if (width < 3 && (i + 1) % width == 0)
				for (int j = 0; j < 3 - width && pattern.size() < 9; j++)
					pattern.add(empty);
		}
		return pattern;
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

	public enum SearchSyncMode implements StringRepresentable {
		NONE("stock_keeper_search_sync_disabled"),
		SYNC_FROM_JEI("stock_keeper_search_sync_from_jei"),
		SYNC_FROM_STOCK_KEEPER("stock_keeper_search_sync_from_stock_keeper"),
		SYNC_BOTH("stock_keeper_search_sync_both");

		public final String buttonTexture;

		SearchSyncMode(String buttonTexture) {
			this.buttonTexture = buttonTexture;
		}

		public boolean isBothOr(SearchSyncMode mode) {
			return this == SYNC_BOTH || this == mode;
		}

		public SearchSyncMode next() {
			SearchSyncMode[] values = values();
			return values[(ordinal() + 1) % values.length];
		}

		public static void cycleConfig() {
			var modeConfig = AllConfigs.client().syncRecipeViewerSearch;
			modeConfig.set(modeConfig.get().next());
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
