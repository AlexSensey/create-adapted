package com.simibubi.create.content.kinetics.deployer;

import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class DeployerBlockEntity extends KineticBlockEntity implements Clearable {
	protected State state;
	protected Mode mode;
	protected ItemStack heldItem;
	protected DeployerFakePlayer player;
	protected int timer;
	protected float reach;
	protected boolean fistBump = false;
	protected List<ItemStack> overflowItems = new ArrayList<>();
	protected FilteringBehaviour filtering;
	protected boolean redstoneLocked;
	protected UUID owner;
	private IItemHandlerModifiable invHandler;
	private ResourceHandler<ItemResource> itemResourceCapability;
	private ListTag deferredInventoryList;

	private LerpedFloat animatedOffset;

	public BeltProcessingBehaviour processingBehaviour;

	enum State {
		WAITING, EXPANDING, RETRACTING, DUMPING;
	}

	enum Mode {
		PUNCH, USE
	}

	public DeployerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		this.state = State.WAITING;
		mode = Mode.USE;
		heldItem = ItemStack.EMPTY;
		redstoneLocked = false;
		animatedOffset = LerpedFloat.linear()
			.startWithValue(0);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, AllBlockEntityTypes.DEPLOYER.get(),
			(be, context) -> be.getItemResourceCapability());
	}

	private ResourceHandler<ItemResource> getItemResourceCapability() {
		initHandler();
		if (invHandler == null)
			return null;
		if (itemResourceCapability == null)
			itemResourceCapability = new DeployerResourceHandler();
		return itemResourceCapability;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		filtering = new FilteringBehaviour(this, new DeployerFilterSlot());
		behaviours.add(filtering);
		processingBehaviour =
			new BeltProcessingBehaviour(this).whenItemEnters((s, i) -> BeltDeployerCallbacks.onItemReceived(s, i, this))
				.whileItemHeld((s, i) -> BeltDeployerCallbacks.whenItemHeld(s, i, this));
		behaviours.add(processingBehaviour);

		registerAwardables(behaviours, AllAdvancements.TRAIN_CASING, AllAdvancements.ANDESITE_CASING,
			AllAdvancements.BRASS_CASING, AllAdvancements.COPPER_CASING, AllAdvancements.FIST_BUMP,
			AllAdvancements.DEPLOYER, AllAdvancements.SELF_DEPLOYING);
	}

	@Override
	public void initialize() {
		super.initialize();
		initHandler();
	}

	private void initHandler() {
		if (invHandler != null)
			return;
		if (level instanceof ServerLevel sLevel) {
			player = new DeployerFakePlayer(sLevel, owner);
			if (deferredInventoryList != null) {
				ItemStackWithSlot.CODEC.listOf()
					.parse(sLevel.registryAccess().createSerializationContext(NbtOps.INSTANCE), deferredInventoryList)
					.resultOrPartial(error -> Create.LOGGER.error("Failed to load deployer inventory: {}", error))
					.ifPresent(entries -> entries.forEach(entry -> {
						if (entry.isValidInContainer(player.getInventory().getContainerSize()))
							player.getInventory().setItem(entry.slot(), entry.stack());
					}));
				deferredInventoryList = null;
				heldItem = player.getMainHandItem().copy();
				sendData();
			} else {
				player.setItemInHand(InteractionHand.MAIN_HAND, heldItem.copy());
			}
			Vec3 initialPos = VecHelper.getCenterOf(worldPosition.relative(getBlockState().getValue(FACING)));
			player.setPos(initialPos.x, initialPos.y, initialPos.z);
		}
		invHandler = createHandler();
	}

	protected void onExtract(ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
		heldItem = stack.copy();
		sendData();
		setChanged();
	}

	void swapHeldItem(ItemStack stack) {
		if (player == null)
			return;
		player.stopUsingItem();
		player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
		heldItem = stack.copy();

		// A manual swap may happen while the previous item is expanding or
		// retracting. Restart from a clean cycle so the arm cannot remain stuck
		// with state belonging to the previous item.
		state = State.WAITING;
		timer = 0;
		fistBump = false;
		setChanged();
		sendData();
	}

	protected int getTimerSpeed() {
		return (int) (getSpeed() == 0 ? 0 : Mth.clamp(Math.abs(getSpeed() * 2), 8, 512));
	}

	@Override
	public void tick() {
		super.tick();

		if (getSpeed() == 0)
			return;
		if (!level.isClientSide() && player != null && player.blockBreakingProgress != null) {
			if (level.isEmptyBlock(player.blockBreakingProgress.getKey())) {
				level.destroyBlockProgress(player.getId(), player.blockBreakingProgress.getKey(), -1);
				player.blockBreakingProgress = null;
			}
		}
		if (timer > 0) {
			timer -= getTimerSpeed();
			return;
		}
		if (level.isClientSide())
			return;
		if (player == null)
			return;

		ItemStack stack = player.getMainHandItem();
		if (state == State.WAITING) {
			if (!overflowItems.isEmpty()) {
				timer = getTimerSpeed() * 10;
				return;
			}

			boolean changed = false;
			Inventory inventory = player.getInventory();
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				if (overflowItems.size() > 10)
					break;
				ItemStack item = inventory.getItem(i);
				if (item.isEmpty())
					continue;
				if (item != stack || !filtering.test(item)) {
					overflowItems.add(item);
					inventory.setItem(i, ItemStack.EMPTY);
					changed = true;
				}
			}

			if (changed) {
				sendData();
				timer = getTimerSpeed() * 10;
				return;
			}

			Direction facing = getBlockState().getValue(FACING);
			if (mode == Mode.USE
				&& !DeployerHandler.shouldActivate(stack, level, worldPosition.relative(facing, 2), facing)) {
				timer = getTimerSpeed() * 10;
				return;
			}

			// Check for advancement conditions
			if (mode == Mode.PUNCH && !fistBump && startFistBump(facing))
				return;
			if (redstoneLocked)
				return;

			start();
			return;
		}

		if (state == State.EXPANDING) {
			if (fistBump)
				triggerFistBump();
			activate();

			state = State.RETRACTING;
			timer = 1000;
			sendData();
			return;
		}

		if (state == State.RETRACTING) {
			state = State.WAITING;
			timer = 500;
			sendData();
			return;
		}

	}

	protected void start() {
		state = State.EXPANDING;
		Vec3 movementVector = getMovementVector();
		Vec3 rayOrigin = VecHelper.getCenterOf(worldPosition)
			.add(movementVector.scale(3 / 2f));
		Vec3 rayTarget = VecHelper.getCenterOf(worldPosition)
			.add(movementVector.scale(5 / 2f));
		ClipContext rayTraceContext = new ClipContext(rayOrigin, rayTarget, Block.OUTLINE, Fluid.NONE, player);
		BlockHitResult result = level.clip(rayTraceContext);
		reach = (float) (.5f + Math.min(result.getLocation()
			.subtract(rayOrigin)
			.length(), .75f));
		timer = 1000;
		sendData();
	}

	public boolean startFistBump(Direction facing) {
		int i = 0;
		DeployerBlockEntity partner = null;

		for (i = 2; i < 5; i++) {
			BlockPos otherDeployer = worldPosition.relative(facing, i);
			if (!level.isLoaded(otherDeployer))
				return false;
			BlockEntity other = level.getBlockEntity(otherDeployer);
			if (other instanceof DeployerBlockEntity dpe) {
				partner = dpe;
				break;
			}
		}

		if (partner == null)
			return false;

		if (level.getBlockState(partner.getBlockPos())
			.getValue(FACING)
			.getOpposite() != facing || partner.mode != Mode.PUNCH)
			return false;
		if (partner.getSpeed() == 0)
			return false;

		for (DeployerBlockEntity be : Arrays.asList(this, partner)) {
			be.fistBump = true;
			be.reach = ((i - 2)) * .5f;
			be.timer = 1000;
			be.state = State.EXPANDING;
			be.sendData();
		}

		return true;
	}

	public void triggerFistBump() {
		int i = 0;
		DeployerBlockEntity deployerBlockEntity = null;
		for (i = 2; i < 5; i++) {
			BlockPos pos = worldPosition.relative(getBlockState().getValue(FACING), i);
			if (!level.isLoaded(pos))
				return;
			if (level.getBlockEntity(pos) instanceof DeployerBlockEntity dpe) {
				deployerBlockEntity = dpe;
				break;
			}
		}

		if (deployerBlockEntity == null)
			return;
		if (!deployerBlockEntity.fistBump || deployerBlockEntity.state != State.EXPANDING)
			return;
		if (deployerBlockEntity.timer > 0)
			return;

		fistBump = false;
		deployerBlockEntity.fistBump = false;
		deployerBlockEntity.state = State.RETRACTING;
		deployerBlockEntity.timer = 1000;
		deployerBlockEntity.sendData();
		award(AllAdvancements.FIST_BUMP);

		BlockPos soundLocation = BlockPos.containing(Vec3.atCenterOf(worldPosition)
			.add(Vec3.atCenterOf(deployerBlockEntity.getBlockPos()))
			.scale(.5f));
		level.playSound(null, soundLocation, SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.BLOCKS, .75f, .75f);
	}

	protected void activate() {
		Vec3 movementVector = getMovementVector();
		Direction direction = getBlockState().getValue(FACING);
		Vec3 center = VecHelper.getCenterOf(worldPosition);
		BlockPos clickedPos = worldPosition.relative(direction, 2);
		player.setXRot(direction == Direction.UP ? -90 : direction == Direction.DOWN ? 90 : 0);
		player.setYRot(direction.toYRot());

		if (direction == Direction.DOWN
			&& BlockEntityBehaviour.get(level, clickedPos, TransportedItemStackHandlerBehaviour.TYPE) != null)
			return; // Belt processing handled in BeltDeployerCallbacks

		DeployerHandler.activate(player, center, clickedPos, movementVector, mode);
		award(AllAdvancements.DEPLOYER);

		if (player != null) {
			int count = heldItem.getCount();
			heldItem = player.getMainHandItem();
			if (count != heldItem.getCount())
				setChanged();
		}
	}

	protected Vec3 getMovementVector() {
		if (!AllBlocks.DEPLOYER.has(getBlockState()))
			return Vec3.ZERO;
		return Vec3.atLowerCornerOf(getBlockState().getValue(FACING)
			.getUnitVec3i());
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		state = readEnum(compound, "State", State.class, State.WAITING);
		mode = readEnum(compound, "Mode", Mode.class, Mode.USE);
		timer = compound.getIntOr("Timer", 0);
		redstoneLocked = compound.getBooleanOr("Powered", false);
		deferredInventoryList = compound.getListOrEmpty("Inventory");
		heldItem = readItemStack(compound, registries, "HeldItem");
		overflowItems.clear();
		for (Tag tag : compound.getListOrEmpty("OverflowItems")) {
			if (!(tag instanceof CompoundTag stackTag))
				continue;
			ItemStack stack = ItemStack.OPTIONAL_CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE),
					stackTag)
				.result()
				.map(result -> result.getFirst())
				.orElse(ItemStack.EMPTY);
			if (!stack.isEmpty())
				overflowItems.add(stack);
		}
		owner = null;
		String ownerId = compound.getStringOr("Owner", "");
		if (!ownerId.isBlank())
			try {
				owner = UUID.fromString(ownerId);
			} catch (IllegalArgumentException ignored) {}

		super.read(compound, registries, clientPacket);

		if (!clientPacket)
			return;
		fistBump = compound.getBooleanOr("Fistbump", false);
		reach = compound.getFloatOr("Reach", 0);
		ItemStack particleStack = readItemStack(compound, registries, "Particle");
		if (!particleStack.isEmpty())
			SandPaperItem.spawnParticles(VecHelper.getCenterOf(worldPosition)
				.add(getMovementVector().scale(reach + 1)), particleStack, this.level);
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		writeEnum(compound, "Mode", mode);
		writeEnum(compound, "State", state);
		compound.putInt("Timer", timer);
		compound.putBoolean("Powered", redstoneLocked);
		if (owner != null)
			compound.putString("Owner", owner.toString());

		if (player != null) {
			List<ItemStackWithSlot> inventory = new ArrayList<>();
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				ItemStack stack = player.getInventory().getItem(slot);
				if (!stack.isEmpty())
					inventory.add(new ItemStackWithSlot(slot, stack.copy()));
			}
			ItemStackWithSlot.CODEC.listOf()
				.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), inventory)
				.resultOrPartial(error -> Create.LOGGER.error("Failed to save deployer inventory: {}", error))
				.ifPresent(tag -> compound.put("Inventory", tag));
			heldItem = player.getMainHandItem()
				.copy();
			writeItemStack(compound, registries, "HeldItem", heldItem);
		} else if (deferredInventoryList != null) {
			compound.put("Inventory", deferredInventoryList);
			writeItemStack(compound, registries, "HeldItem", heldItem);
		} else {
			writeItemStack(compound, registries, "HeldItem", heldItem);
		}
		writeOverflowItems(compound, registries);

		super.write(compound, registries, clientPacket);

		if (!clientPacket)
			return;
		compound.putBoolean("Fistbump", fistBump);
		compound.putFloat("Reach", reach);
		if (player == null)
			return;
		if (player.spawnedItemEffects != null)
			writeItemStack(compound, registries, "Particle", player.spawnedItemEffects);
		player.spawnedItemEffects = null;
	}

	@Override
	public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
		writeEnum(tag, "Mode", mode);
		super.writeSafe(tag, registries);
	}

	private static <E extends Enum<E>> E readEnum(CompoundTag tag, String key, Class<E> enumClass, E fallback) {
		String value = tag.getStringOr(key, fallback.name());
		try {
			return Enum.valueOf(enumClass, value);
		} catch (IllegalArgumentException e) {
			return fallback;
		}
	}

	private static void writeEnum(CompoundTag tag, String key, Enum<?> value) {
		tag.putString(key, value.name());
	}

	private static ItemStack readItemStack(CompoundTag tag, HolderLookup.Provider registries, String key) {
		Tag stackTag = tag.get(key);
		if (stackTag == null)
			return ItemStack.EMPTY;
		return ItemStack.OPTIONAL_CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), stackTag)
			.result()
			.map(result -> result.getFirst())
			.orElse(ItemStack.EMPTY);
	}

	private static void writeItemStack(CompoundTag tag, HolderLookup.Provider registries, String key, ItemStack stack) {
		if (stack.isEmpty())
			return;
		ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
			.result()
			.ifPresent(stackTag -> tag.put(key, stackTag));
	}

	private void writeOverflowItems(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (ItemStack stack : overflowItems) {
			if (stack.isEmpty())
				continue;
			ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
				.result()
				.filter(CompoundTag.class::isInstance)
				.map(CompoundTag.class::cast)
				.ifPresent(list::add);
		}
		if (!list.isEmpty())
			tag.put("OverflowItems", list);
	}

	private IItemHandlerModifiable createHandler() {
		return new DeployerItemHandler(this);
	}

	public void redstoneUpdate() {
		if (level.isClientSide())
			return;
		boolean blockPowered = level.hasNeighborSignal(worldPosition);
		if (blockPowered == redstoneLocked)
			return;
		redstoneLocked = blockPowered;
		sendData();
	}

	public PartialModel getHandPose() {
		return mode == Mode.PUNCH ? AllPartialModels.DEPLOYER_HAND_PUNCHING
			: heldItem.isEmpty() ? AllPartialModels.DEPLOYER_HAND_POINTING : AllPartialModels.DEPLOYER_HAND_HOLDING;
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return super.createRenderBoundingBox().inflate(3);
	}

	public void discardPlayer() {
		if (player == null)
			return;
		player.getInventory()
			.dropAll();
		overflowItems.forEach(itemstack -> player.drop(itemstack, true, false));
		player.discard();
		player = null;
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (invHandler != null || itemResourceCapability != null)
			invalidateCapabilities();
		itemResourceCapability = null;
	}

	@Override
	public void clearContent() {
		filtering.setFilter(ItemStack.EMPTY);
	}

	public void changeMode() {
		mode = mode == Mode.PUNCH ? Mode.USE : Mode.PUNCH;
		setChanged();
		sendData();
	}

	@Override
	public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		if (super.addToTooltip(tooltip, isPlayerSneaking))
			return true;
		if (getSpeed() == 0)
			return false;
		if (overflowItems.isEmpty())
			return false;
		TooltipHelper.addHint(tooltip, "hint.full_deployer");
		return true;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		CreateLang.translate("tooltip.deployer.header")
			.forGoggles(tooltip);

		CreateLang.translate("tooltip.deployer." + (mode == Mode.USE ? "using" : "punching"))
			.style(ChatFormatting.YELLOW)
			.forGoggles(tooltip);

		if (!heldItem.isEmpty())
			CreateLang.translate("tooltip.deployer.contains", heldItem.getHoverName()
					.getString(), heldItem.getCount())
				.style(ChatFormatting.GREEN)
				.forGoggles(tooltip);

		float stressAtBase = calculateStressApplied();
		if (StressImpact.isEnabled() && !Mth.equal(stressAtBase, 0)) {
			tooltip.add(CommonComponents.EMPTY);
			addStressImpactStats(tooltip, stressAtBase);
		}

		return true;
	}

	public float getHandOffset(float partialTicks) {
		if (isVirtual())
			return animatedOffset.getValue(partialTicks);

		float progress = 0;
		int timerSpeed = getTimerSpeed();
		PartialModel handPose = getHandPose();

		if (state == State.EXPANDING) {
			progress = 1 - (timer - partialTicks * timerSpeed) / 1000f;
			if (fistBump)
				progress *= progress;
		}
		if (state == State.RETRACTING)
			progress = (timer - partialTicks * timerSpeed) / 1000f;
		float handLength = handPose == AllPartialModels.DEPLOYER_HAND_POINTING ? 0
			: handPose == AllPartialModels.DEPLOYER_HAND_HOLDING ? 4 / 16f : 3 / 16f;
		float distance = Math.min(Mth.clamp(progress, 0, 1) * (reach + handLength), 21 / 16f);

		return distance;
	}

	public void setAnimatedOffset(float offset) {
		animatedOffset.setValue(offset);
	}

	ItemStackHandler recipeInv = new ItemStackHandler(2);

	@Nullable
	public RecipeHolder<? extends Recipe<? extends RecipeInput>> getRecipe(ItemStack stack) {
		if (player == null || level == null)
			return null;

		ItemStack heldItemMainhand = player.getMainHandItem();
		if (heldItemMainhand.getItem() instanceof SandPaperItem) {
			Optional<RecipeHolder<Recipe<RecipeInput>>> polishingRecipe = checkRecipe(AllRecipeTypes.SANDPAPER_POLISHING, new SingleRecipeInput(stack), level);
			if (polishingRecipe.isPresent()) {
				return polishingRecipe.get();
			}
		}

		recipeInv.setStackInSlot(0, stack);
		recipeInv.setStackInSlot(1, heldItemMainhand);

		DeployerRecipeSearchEvent event = new DeployerRecipeSearchEvent(this, new RecipeWrapper(recipeInv));

		event.addRecipe(() -> SequencedAssemblyRecipe.getRecipe(level, event.getInventory(),
			AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class), 100);
		event.addRecipe(() -> checkRecipe(AllRecipeTypes.DEPLOYING, event.getInventory(), level), 50);
		event.addRecipe(() -> checkRecipe(AllRecipeTypes.ITEM_APPLICATION, event.getInventory(), level), 50);

		NeoForge.EVENT_BUS.post(event);
		return event.getRecipe();
	}

	private Optional<RecipeHolder<Recipe<RecipeInput>>> checkRecipe(AllRecipeTypes type, RecipeInput inv, Level level) {
		return type.find(inv, level).filter(AllRecipeTypes.CAN_BE_AUTOMATED);
	}

	public DeployerFakePlayer getPlayer() {
		return player;
	}

	private class DeployerResourceHandler implements ResourceHandler<ItemResource> {
		private final SnapshotJournal<DeployerInventorySnapshot> journal = new SnapshotJournal<>() {
			@Override
			protected DeployerInventorySnapshot createSnapshot() {
				List<ItemStack> overflow = new ArrayList<>();
				for (ItemStack stack : overflowItems)
					overflow.add(stack.copy());
				ItemStack playerHeldItem = player == null ? ItemStack.EMPTY : player.getMainHandItem()
					.copy();
				return new DeployerInventorySnapshot(overflow, playerHeldItem, heldItem.copy());
			}

			@Override
			protected void revertToSnapshot(DeployerInventorySnapshot snapshot) {
				overflowItems.clear();
				for (ItemStack stack : snapshot.overflowItems)
					overflowItems.add(stack.copy());
				if (player != null)
					player.setItemInHand(InteractionHand.MAIN_HAND, snapshot.playerHeldItem.copy());
				heldItem = snapshot.heldItem.copy();
				setChanged();
				sendData();
			}
		};

		@Override
		public int size() {
			return invHandler.getSlots();
		}

		@Override
		public ItemResource getResource(int index) {
			return ItemResource.of(invHandler.getStackInSlot(index));
		}

		@Override
		public long getAmountAsLong(int index) {
			return invHandler.getStackInSlot(index)
				.getCount();
		}

		@Override
		public long getCapacityAsLong(int index, ItemResource resource) {
			if (!resource.isEmpty() && !isValid(index, resource))
				return 0;
			return invHandler.getSlotLimit(index);
		}

		@Override
		public boolean isValid(int index, ItemResource resource) {
			if (resource.isEmpty())
				return true;
			return invHandler.isItemValid(index, resource.toStack(1));
		}

		@Override
		public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack remainder = invHandler.insertItem(index, resource.toStack(amount), true);
			int inserted = amount - remainder.getCount();
			if (inserted <= 0)
				return 0;

			journal.updateSnapshots(transaction);
			invHandler.insertItem(index, resource.toStack(inserted), false);
			return inserted;
		}

		@Override
		public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
			if (resource.isEmpty() || amount <= 0)
				return 0;

			ItemStack current = invHandler.getStackInSlot(index);
			if (!resource.matches(current))
				return 0;

			ItemStack extracted = invHandler.extractItem(index, amount, true);
			if (extracted.isEmpty())
				return 0;

			journal.updateSnapshots(transaction);
			invHandler.extractItem(index, extracted.getCount(), false);
			return extracted.getCount();
		}
	}

	private record DeployerInventorySnapshot(List<ItemStack> overflowItems, ItemStack playerHeldItem,
											 ItemStack heldItem) {
	}
}
