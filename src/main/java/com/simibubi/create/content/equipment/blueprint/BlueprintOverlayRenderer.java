package com.simibubi.create.content.equipment.blueprint;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.blueprint.BlueprintEntity.BlueprintSection;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.tableCloth.BlueprintOverlayShopContext;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem.ShoppingList;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

// TODO - Split up into specific overlays
public class BlueprintOverlayRenderer {

	public static final GuiLayer OVERLAY = BlueprintOverlayRenderer::renderOverlay;

	static boolean active;
	static boolean empty;
	static boolean noOutput;
	static boolean lastSneakState;
	static BlueprintSection lastTargetedSection;
	static BlueprintOverlayShopContext shopContext;

	static final Map<ItemStack, ItemStack[]> cachedRenderedFilters = new IdentityHashMap<>();
	static final List<Pair<ItemStack, Boolean>> ingredients = new ArrayList<>();
	static final List<ItemStack> results = new ArrayList<>();
	static boolean resultCraftable;

	static Component chainStatus = Component.empty();
	static int chainStatusColor = 0xFFFFFF;

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();

		BlueprintSection last = lastTargetedSection;
		lastTargetedSection = null;
		active = false;
		noOutput = false;
		shopContext = null;
		chainStatus = Component.empty();

		if (mc.gameMode == null || mc.player == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
			return;

		HitResult mouseOver = mc.hitResult;
		if (!(mouseOver instanceof EntityHitResult entityRay)
			|| entityRay.getType() != Type.ENTITY
			|| !(entityRay.getEntity() instanceof BlueprintEntity blueprintEntity))
			return;

		BlueprintSection sectionAt = blueprintEntity.getSectionAt(entityRay.getLocation()
			.subtract(blueprintEntity.position()));

		active = true;
		boolean sneak = mc.player.isShiftKeyDown();
		if (sectionAt != last || AnimationTickHolder.getTicks() % 10 == 0 || lastSneakState != sneak)
			rebuild(sectionAt, sneak);

		lastTargetedSection = sectionAt;
		lastSneakState = sneak;
	}

	public static void displayTrackRequirements(PlacementInfo info, ItemStack pavementItem) {
		if (active)
			return;
		prepareCustomOverlay();

		int tracks = info.requiredTracks;
		while (tracks > 0) {
			ingredients.add(Pair.of(new ItemStack(info.trackMaterial.getBlock(), Math.min(64, tracks)),
				info.hasRequiredTracks));
			tracks -= 64;
		}

		int pavement = info.requiredPavement;
		while (pavement > 0) {
			ingredients.add(Pair.of(pavementItem.copyWithCount(Math.min(64, pavement)),
				info.hasRequiredPavement));
			pavement -= 64;
		}
	}

	public static void displayChainRequirements(Item chainItem, int count, boolean fulfilled) {
		if (active)
			return;
		prepareCustomOverlay();

		while (count > 0) {
			ingredients.add(Pair.of(new ItemStack(chainItem, Math.min(64, count)), fulfilled));
			count -= 64;
		}
	}

	public static void displayChainStatus(Component status, int color) {
		if (!active)
			prepareCustomOverlay();
		chainStatus = status;
		chainStatusColor = color;
	}

	public static void displayClothShop(TableClothBlockEntity dce, int alreadyPurchased, ShoppingList list) {
		if (active)
			return;
		prepareCustomOverlay();
		noOutput = false;

		shopContext = new BlueprintOverlayShopContext(false, dce.getStockLevelForTrade(list), alreadyPurchased);
		ingredients.add(Pair.of(dce.getPaymentItem()
				.copyWithCount(dce.getPaymentAmount()),
			!dce.getPaymentItem()
				.isEmpty() && shopContext.stockLevel() > shopContext.purchases()));
		for (BigItemStack entry : dce.requestData.encodedRequest().stacks())
			results.add(entry.stack.copyWithCount(entry.count));
	}

	public static void displayShoppingList(Couple<InventorySummary> bakedList) {
		if (active || bakedList == null)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;
		prepareCustomOverlay();
		noOutput = false;

		shopContext = new BlueprintOverlayShopContext(true, 1, 0);
		for (BigItemStack entry : bakedList.getSecond()
			.getStacksByCount())
			ingredients.add(Pair.of(entry.stack.copyWithCount(entry.count), canAfford(mc.player, entry)));
		for (BigItemStack entry : bakedList.getFirst()
			.getStacksByCount())
			results.add(entry.stack.copyWithCount(entry.count));
	}

	private static boolean canAfford(Player player, BigItemStack entry) {
		int itemsPresent = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack item = player.getInventory().getItem(slot);
			if (!item.isEmpty() && ItemStack.isSameItemSameComponents(item, entry.stack))
				itemsPresent += item.getCount();
		}
		return itemsPresent >= entry.count;
	}

	private static void prepareCustomOverlay() {
		active = true;
		empty = false;
		noOutput = true;
		ingredients.clear();
		results.clear();
		resultCraftable = false;
		shopContext = null;
	}

	public static void rebuild(BlueprintSection sectionAt, boolean sneak) {
		cachedRenderedFilters.clear();
		ItemStackHandler items = sectionAt.getItems();
		empty = true;
		for (int i = 0; i < 9; i++) {
			if (!items.getStackInSlot(i).isEmpty()) {
				empty = false;
				break;
			}
		}

		ingredients.clear();
		results.clear();
		resultCraftable = false;
		if (empty)
			return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null)
			return;

		ItemStackHandler playerInv = new ItemStackHandler(mc.player.getInventory().getContainerSize());
		for (int i = 0; i < playerInv.getSlots(); i++)
			playerInv.setStackInSlot(i, mc.player.getInventory().getItem(i).copy());

		ItemStackHandler availableItems = new ItemStackHandler(64);
		ItemStackHandler missingItems = new ItemStackHandler(64);
		int amountCrafted = 0;
		boolean firstPass = true;

		while (true) {
			boolean success = true;
			List<ItemStack> newlyAdded = new ArrayList<>();
			List<ItemStack> newlyMissing = new ArrayList<>();

			Search:
			for (int i = 0; i < 9; i++) {
				FilterItemStack requestedItem = FilterItemStack.of(items.getStackInSlot(i));
				if (requestedItem.isEmpty())
					continue;

				for (int slot = 0; slot < playerInv.getSlots(); slot++) {
					if (!requestedItem.test(mc.level, playerInv.getStackInSlot(slot)))
						continue;
					ItemStack currentItem = playerInv.extractItem(slot, 1, false);
					newlyAdded.add(currentItem);
					continue Search;
				}

				success = false;
				newlyMissing.add(requestedItem.item());
			}

			if (success) {
				ItemStack result = items.getStackInSlot(9).copy();

				if (result.isEmpty()) {
					success = false;
				} else if (result.getCount() + amountCrafted > 64) {
					success = false;
				} else {
					amountCrafted += result.getCount();
					if (results.isEmpty())
						results.add(result.copy());
					else
						results.getFirst().grow(result.getCount());
					resultCraftable = true;
				}
			}

			if (success || firstPass) {
				newlyAdded.forEach(stack -> ItemHandlerHelper.insertItemStacked(availableItems, stack, false));
				newlyMissing.forEach(stack -> ItemHandlerHelper.insertItemStacked(missingItems, stack, false));
			}

			if (!success) {
				if (firstPass) {
					results.clear();
					if (!items.getStackInSlot(9).isEmpty())
						results.add(items.getStackInSlot(9));
					resultCraftable = false;
				}
				break;
			}

			firstPass = false;
			if (!sneak)
				break;
		}

		appendNonEmpty(availableItems, true);
		appendNonEmpty(missingItems, false);
	}

	private static void appendNonEmpty(ItemStackHandler handler, boolean available) {
		for (int i = 0; i < handler.getSlots(); i++) {
			ItemStack stack = handler.getStackInSlot(i);
			if (!stack.isEmpty())
				ingredients.add(Pair.of(stack, available));
		}
	}

	public static void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.screen() != null || !active || empty)
			return;

		boolean invalidShop = shopContext != null && (ingredients.isEmpty()
			|| ingredients.getFirst().getFirst().isEmpty() || shopContext.stockLevel() == 0);
		int width = 21 * ingredients.size();
		if (!noOutput)
			width += 21 * results.size() + 30;

		int x = (graphics.guiWidth() - width) / 2;
		int y = graphics.guiHeight() - 100;
		int startX = x;

		if (shopContext != null) {
			graphics.fill(x - 2, y + 1, x + width + 2, y + 20, 0x55000000);
			AllGuiTextures.TRADE_OVERLAY.render(graphics, graphics.guiWidth() / 2 - 48, y - 19);
			if (shopContext.purchases() > 0) {
				graphics.item(AllItems.SHOPPING_LIST.asStack(), graphics.guiWidth() / 2 + 20, y - 20);
				graphics.text(mc.font, Component.literal("x" + shopContext.purchases()),
					graphics.guiWidth() / 2 + 36, y - 16, 0xFFEEEEEE, true);
			}
		}

		for (Pair<ItemStack, Boolean> pair : ingredients) {
			(pair.getSecond() ? AllGuiTextures.HOTSLOT_ACTIVE : AllGuiTextures.HOTSLOT).render(graphics, x, y);
			ItemStack stack = pair.getFirst();
			String count = shopContext != null && !shopContext.checkout() || pair.getSecond() ? null
				: ChatFormatting.GOLD + Integer.toString(stack.getCount());
			drawItemStack(graphics, mc, x, y, stack, count);
			x += 21;
		}

		if (!noOutput) {
			x += 5;
			(invalidShop ? AllGuiTextures.HOTSLOT_ARROW_BAD : AllGuiTextures.HOTSLOT_ARROW).render(graphics, x, y + 4);
			x += 25;

			if (results.isEmpty()) {
				AllGuiTextures.HOTSLOT.render(graphics, x, y);
				drawItemStack(graphics, mc, x, y, net.minecraft.world.item.Items.BARRIER.getDefaultInstance(), null);
			} else {
				for (ItemStack result : results) {
					AllGuiTextures slot = resultCraftable ? AllGuiTextures.HOTSLOT_SUPER_ACTIVE : AllGuiTextures.HOTSLOT;
					if (!invalidShop && shopContext != null && shopContext.stockLevel() > shopContext.purchases())
						slot = AllGuiTextures.HOTSLOT_ACTIVE;
					slot.render(graphics, resultCraftable ? x - 1 : x, resultCraftable ? y - 1 : y);
					drawItemStack(graphics, mc, x, y, result, null);
					x += 21;
				}
			}
		}

		if (!chainStatus.getString().isEmpty())
			graphics.centeredText(mc.font, chainStatus, graphics.guiWidth() / 2, y + 28,
				0xFF000000 | chainStatusColor);

		if (shopContext != null && !shopContext.checkout() && !results.isEmpty()) {
			List<ItemStack> tooltipCandidates = results.stream()
				.filter(stack -> stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL)
					.size() > 1)
				.toList();
			if (!tooltipCandidates.isEmpty()) {
				int index = (AnimationTickHolder.getTicks() / 40) % tooltipCandidates.size();
				List<Component> lines = tooltipCandidates.get(index)
					.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);
				graphics.setComponentTooltipForNextFrame(mc.font, lines, startX + width, y);
			}
		}
	}

	public static void drawItemStack(GuiGraphicsExtractor graphics, Minecraft mc, int x, int y,
		ItemStack itemStack, String count) {
		if (itemStack.getItem() instanceof FilterItem) {
			int step = AnimationTickHolder.getTicks() / 10;
			ItemStack[] matches = getItemsMatchingFilter(itemStack);
			if (matches.length > 0)
				itemStack = matches[step % matches.length];
		}

		graphics.item(itemStack, x + 3, y + 3);
		graphics.itemDecorations(mc.font, itemStack, x + 3, y + 3, count);
	}

	private static ItemStack[] getItemsMatchingFilter(ItemStack filter) {
		return cachedRenderedFilters.computeIfAbsent(filter, itemStack -> {
			if (itemStack.getItem() instanceof FilterItem filterItem)
				return filterItem.getFilterItems(itemStack);
			return new ItemStack[0];
		});
	}
}
