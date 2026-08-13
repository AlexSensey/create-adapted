package com.simibubi.create.content.logistics.stockTicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.packagerLink.WiFiParticle;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.CreateLang;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class StockTickerBlockEntity extends StockCheckingBlockEntity implements IHaveHoveringInformation, Clearable {
	public AbstractComputerBehaviour computerBehaviour;

	// Player-interface Feature
	protected List<List<BigItemStack>> lastClientsideStockSnapshot;
	protected InventorySummary lastClientsideStockSnapshotAsSummary;
	protected List<BigItemStack> newlyReceivedStockSnapshot;
	protected String previouslyUsedAddress;
	protected int activeLinks;
	protected int ticksSinceLastUpdate;
	protected List<ItemStack> categories;
	protected Map<UUID, List<Integer>> hiddenCategoriesByPlayer;

	// Shop feature
	protected SmartInventory receivedPayments;
	protected ResourceHandler<ItemResource> receivedPaymentsCapability;

	public StockTickerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		previouslyUsedAddress = "";
		receivedPayments = new SmartInventory(27, this, 64, false);
		categories = new ArrayList<>();
		hiddenCategoriesByPlayer = new HashMap<>();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
			Capabilities.Item.BLOCK,
			AllBlockEntityTypes.STOCK_TICKER.get(),
			(be, context) -> be.getReceivedPaymentsCapability()
		);

		if (Mods.COMPUTERCRAFT.isLoaded()) {
			event.registerBlockEntity(
				PeripheralCapability.get(),
				AllBlockEntityTypes.STOCK_TICKER.get(),
				(be, context) -> be.computerBehaviour.getPeripheralCapability()
			);
		}
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.add(computerBehaviour = ComputerCraftProxy.behaviour(this));
	}

	@Override
	public void invalidate() {
		super.invalidate();
		receivedPaymentsCapability = null;
		computerBehaviour.removePeripheral();
	}

	public void refreshClientStockSnapshot() {
		ticksSinceLastUpdate = 0;
		ClientNetworkHelper.INSTANCE.sendToServer(new LogisticalStockRequestPacket(worldPosition));
	}

	public IItemHandler getReceivedPaymentsHandler() {
		return receivedPayments;
	}

	public ResourceHandler<ItemResource> getReceivedPaymentsCapability() {
		if (receivedPaymentsCapability == null)
			receivedPaymentsCapability = new ReceivedPaymentsResourceHandler();
		return receivedPaymentsCapability;
	}

	public List<List<BigItemStack>> getClientStockSnapshot() {
		return lastClientsideStockSnapshot;
	}

	public InventorySummary getLastClientsideStockSnapshotAsSummary() {
		return lastClientsideStockSnapshotAsSummary;
	}

	public int getTicksSinceLastUpdate() {
		return ticksSinceLastUpdate;
	}

	@Override
	public boolean broadcastPackageRequest(RequestType type, PackageOrderWithCrafts order, IdentifiedInventory ignoredHandler, String address) {
		boolean result = super.broadcastPackageRequest(type, order, ignoredHandler, address);
		previouslyUsedAddress = address;
		notifyUpdate();
		return result;
	}

	@Override
	public InventorySummary getRecentSummary() {
		InventorySummary recentSummary = super.getRecentSummary();
		int contributingLinks = recentSummary.contributingLinks;
		if (activeLinks != contributingLinks && !isRemoved()) {
			activeLinks = contributingLinks;
			sendData();
		}
		return recentSummary;
	}

	@Override
	public void tick() {
		super.tick();
		if (level.isClientSide()) {
			if (ticksSinceLastUpdate < 100)
				ticksSinceLastUpdate += 1;
			return;
		}
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putString("PreviousAddress", previouslyUsedAddress);
		tag.put("ReceivedPayments", receivedPayments.serializeNBT(registries));
		tag.put("Categories", writeItemList(categories, registries));
		tag.put("HiddenCategories", NBTHelper.writeCompoundList(hiddenCategoriesByPlayer.entrySet(), entry -> {
			CompoundTag categoryTag = new CompoundTag();
			categoryTag.putString("Id", entry.getKey()
				.toString());
			categoryTag.putIntArray("Indices", entry.getValue()
				.stream()
				.mapToInt(Integer::intValue)
				.toArray());
			return categoryTag;
		}));

		if (clientPacket)
			tag.putInt("ActiveLinks", activeLinks);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		previouslyUsedAddress = tag.getStringOr("PreviousAddress", "");
		receivedPayments.deserializeNBT(registries, tag.getCompoundOrEmpty("ReceivedPayments"));
		categories = readItemList(tag.getListOrEmpty("Categories"), registries);
		categories.removeIf(stack -> !stack.isEmpty() && !(stack.getItem() instanceof FilterItem));
		hiddenCategoriesByPlayer.clear();
		NBTHelper.iterateCompoundList(tag.getListOrEmpty("HiddenCategories"), categoryTag -> {
			try {
				UUID id = UUID.fromString(categoryTag.getStringOr("Id", ""));
				List<Integer> indices = IntStream.of(categoryTag.getIntArray("Indices")
						.orElse(new int[0]))
					.boxed()
					.toList();
				hiddenCategoriesByPlayer.put(id, indices);
			} catch (IllegalArgumentException ignored) {
			}
		});

		if (clientPacket)
			activeLinks = tag.getIntOr("ActiveLinks", 0);
	}

	private static List<ItemStack> readItemList(ListTag list, HolderLookup.Provider registries) {
		List<ItemStack> stacks = new ArrayList<>(list.size());
		NBTHelper.iterateCompoundList(list, itemTag -> stacks.add(ItemStack.OPTIONAL_CODEC
			.decode(registries.createSerializationContext(NbtOps.INSTANCE), itemTag)
			.map(result -> result.getFirst())
			.result()
			.orElse(ItemStack.EMPTY)));
		return stacks;
	}

	private static ListTag writeItemList(List<ItemStack> stacks, HolderLookup.Provider registries) {
		return NBTHelper.writeCompoundList(stacks, stack -> ItemStack.OPTIONAL_CODEC
			.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
			.result()
			.filter(CompoundTag.class::isInstance)
			.map(CompoundTag.class::cast)
			.orElseGet(CompoundTag::new));
	}

	public void receiveStockPacket(List<BigItemStack> stacks, boolean endOfTransmission) {
		if (newlyReceivedStockSnapshot == null)
			newlyReceivedStockSnapshot = new ArrayList<>();
		newlyReceivedStockSnapshot.addAll(stacks);

		if (!endOfTransmission)
			return;

		lastClientsideStockSnapshotAsSummary = new InventorySummary();
		lastClientsideStockSnapshot = new ArrayList<>();

		for (BigItemStack bigStack : newlyReceivedStockSnapshot)
			lastClientsideStockSnapshotAsSummary.add(bigStack);

		for (ItemStack filter : categories) {
			List<BigItemStack> inCategory = new ArrayList<>();
			if (!filter.isEmpty()) {
				FilterItemStack filterItemStack = FilterItemStack.of(filter);
				for (Iterator<BigItemStack> iterator = newlyReceivedStockSnapshot.iterator(); iterator.hasNext(); ) {
					BigItemStack bigStack = iterator.next();
					if (!filterItemStack.test(level, bigStack.stack))
						continue;
					inCategory.add(bigStack);
					iterator.remove();
				}
			}
			lastClientsideStockSnapshot.add(inCategory);
		}

		List<BigItemStack> unsorted = new ArrayList<>(newlyReceivedStockSnapshot);
		lastClientsideStockSnapshot.add(unsorted);
		newlyReceivedStockSnapshot = null;
	}

	public boolean isKeeperPresent() {
		for (int yOffset : Iterate.zeroAndOne) {
			for (Direction side : Iterate.horizontalDirections) {
				BlockPos seatPos = worldPosition.below(yOffset)
					.relative(side);
				for (SeatEntity seatEntity : level.getEntitiesOfClass(SeatEntity.class, new AABB(seatPos)))
					if (seatEntity.isVehicle())
						return true;
				if (yOffset == 0 && AllBlockEntityTypes.HEATER.is(level.getBlockEntity(seatPos)))
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		if (receivedPayments.isEmpty())
			return false;

		CreateLang.translate("stock_ticker.contains_payments")
			.style(ChatFormatting.WHITE)
			.forGoggles(tooltip);

		InventorySummary summary = new InventorySummary();
		for (int i = 0; i < receivedPayments.getSlots(); i++)
			summary.add(receivedPayments.getStackInSlot(i));
		for (BigItemStack entry : summary.getStacksByCount())
			CreateLang.builder()
				.text(entry.stack.getHoverName().getString() + " x" + entry.count)
				.style(ChatFormatting.GREEN)
				.forGoggles(tooltip);

		CreateLang.translate("stock_ticker.click_to_retrieve")
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		return true;
	}

	@Override
	public void clearContent() {
		categories.clear();
		receivedPayments.clearContent();
	}

	@Override
	public void destroy() {
		ItemHelper.dropContents(level, worldPosition, receivedPayments);
		for (ItemStack filter : categories)
			if (!filter.isEmpty() && filter.getItem() instanceof FilterItem)
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
					filter);
		super.destroy();
	}

	public void playEffect() {
		AllSoundEvents.STOCK_LINK.playAt(level, worldPosition, 1.0f, 1.0f, false);
		Vec3 vec3 = Vec3.atCenterOf(worldPosition);
		level.addParticle(new WiFiParticle.Data(), vec3.x, vec3.y, vec3.z, 1, 1, 1);
	}

	public class CategoryMenuProvider implements MenuProvider {
		@Override
		public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
			return StockKeeperCategoryMenu.create(pContainerId, pPlayerInventory, StockTickerBlockEntity.this);
		}

		@Override
		public Component getDisplayName() {
			return Component.empty();
		}
	}

	public class RequestMenuProvider implements MenuProvider {
		@Override
		public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
			return StockKeeperRequestMenu.create(pContainerId, pPlayerInventory, StockTickerBlockEntity.this);
		}

		@Override
		public Component getDisplayName() {
			return Component.empty();
		}
	}

	private class ReceivedPaymentsResourceHandler implements ResourceHandler<ItemResource> {
		private final List<SlotJournal> journals;

		private ReceivedPaymentsResourceHandler() {
			journals = new ArrayList<>();
			for (int slot = 0; slot < receivedPayments.getSlots(); slot++)
				journals.add(new SlotJournal(slot));
		}

		@Override
		public int size() {
			return receivedPayments.getSlots();
		}

		@Override
		public ItemResource getResource(int index) {
			return ItemResource.of(receivedPayments.getStackInSlot(index));
		}

		@Override
		public long getAmountAsLong(int index) {
			return receivedPayments.getStackInSlot(index)
				.getCount();
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			if (!resource.isEmpty() && !isValid(index, resource))
				return 0;
			return receivedPayments.getSlotLimit(index);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			if (resource.isEmpty())
				return true;
			return receivedPayments.isItemValid(index, resource.toStack(1));
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack remainder = receivedPayments.insertItem(index, resource.toStack(amount), true);
			int inserted = amount - remainder.getCount();
			if (inserted <= 0)
				return 0;

			journals.get(index)
				.updateSnapshots(transaction);
			receivedPayments.insertItem(index, resource.toStack(inserted), false);
			return inserted;
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack current = receivedPayments.getStackInSlot(index);
			if (!resource.matches(current))
				return 0;

			ItemStack extracted = receivedPayments.extractItem(index, amount, true);
			if (extracted.isEmpty())
				return 0;

			journals.get(index)
				.updateSnapshots(transaction);
			receivedPayments.extractItem(index, extracted.getCount(), false);
			return extracted.getCount();
		}

		private class SlotJournal extends SnapshotJournal<ItemStack> {
			private final int slot;

			private SlotJournal(int slot) {
				this.slot = slot;
			}

			@Override
			protected ItemStack createSnapshot() {
				return receivedPayments.getStackInSlot(slot)
					.copy();
			}

			@Override
			protected void revertToSnapshot(ItemStack snapshot) {
				receivedPayments.setStackInSlot(slot, snapshot);
			}
		}
	}
}
