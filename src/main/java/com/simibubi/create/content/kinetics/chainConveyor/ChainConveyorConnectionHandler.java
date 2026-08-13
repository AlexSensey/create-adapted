package com.simibubi.create.content.kinetics.chainConveyor;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.client.render.SuperRenderTypeBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(Dist.CLIENT)
public class ChainConveyorConnectionHandler {

	private static BlockPos firstPos;
	private static ResourceKey<Level> firstDim;
	private static String lastStatusKey;
	private static long lastStatusGameTime;
	private static String previewFailureKey;
	private static BlockPos previewSource;
	private static BlockPos previewTarget;
	private static int previewColor = 0xB8B8B8;
	private static boolean showPreviewLine;
	private static Vec3 previewChainAStart;
	private static Vec3 previewChainAEnd;
	private static Vec3 previewChainBStart;
	private static Vec3 previewChainBEnd;

	public static boolean onRightClick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return false;
		if (!isChain(mc.player.getMainHandItem()))
			return false;
		if (firstPos == null)
			return false;

		boolean missed = getTargetedConveyorPos(mc) == null;
		if (!mc.player.isShiftKeyDown() && !missed)
			return false;

		firstPos = null;
		firstDim = null;
		lastStatusKey = null;
		sendStatus(mc.player, "chain_conveyor.selection_cleared");
		return true;
	}

	public static boolean onUseKey() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null)
			return false;

		ItemStack stack = getHeldChain(mc.player);
		if (stack.isEmpty())
			return false;

		BlockPos pos = getTargetedConveyorPos(mc);
		if (pos == null)
			return onRightClick();

		return selectOrConnect(mc.level, pos, mc.player, stack);
	}

	@SubscribeEvent
	public static void onItemUsedOnBlock(PlayerInteractEvent.RightClickBlock event) {
		ItemStack itemStack = event.getItemStack();
		BlockPos pos = event.getPos();
		Level level = event.getLevel();
		Player player = event.getEntity();
		BlockState blockState = level.getBlockState(pos);

		if (!AllBlocks.CHAIN_CONVEYOR.has(blockState))
			return;
		if (!isChain(itemStack))
			return;
		if (!player.mayBuild() || player instanceof FakePlayer)
			return;

		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.CONSUME);

		if (!level.isClientSide())
			return;

		selectOrConnect(level, pos, player, itemStack);
	}

	private static boolean isChain(ItemStack itemStack) {
		return itemStack.is(Items.IRON_CHAIN);
	}

	private static ItemStack getHeldChain(Player player) {
		ItemStack stack = player.getMainHandItem();
		if (isChain(stack))
			return stack;
		stack = player.getOffhandItem();
		if (isChain(stack))
			return stack;
		return ItemStack.EMPTY;
	}

	private static boolean selectOrConnect(Level level, BlockPos pos, Player player, ItemStack itemStack) {
		if (level.getBlockEntity(pos) instanceof ChainConveyorBlockEntity ccbe
			&& ccbe.connections.size() >= AllConfigs.server().kinetics.maxChainConveyorConnections.get()) {
			sendStatus(player, "chain_conveyor.cannot_add_more_connections", ChatFormatting.RED);
			return true;
		}

		if (firstPos == null || firstDim != level.dimension()) {
			firstPos = pos;
			firstDim = level.dimension();
			lastStatusKey = null;
			player.swing(player.getUsedItemHand());
			displayPreviewStatus("chain_conveyor.select_second", ChatFormatting.WHITE);
			return true;
		}

		boolean success = validateAndConnect(level, pos, player, itemStack, false);
		firstPos = null;
		firstDim = null;
		lastStatusKey = null;

		if (!success) {
			AllSoundEvents.DENY.play(level, player, pos);
			return true;
		}

		SoundType soundtype = Blocks.IRON_CHAIN.defaultBlockState()
			.getSoundType();
		level.playSound(player, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS,
			(soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
		return true;
	}

	public static void clientTick() {
		previewSource = null;
		previewTarget = null;
		showPreviewLine = false;
		previewColor = 0xB8B8B8;
		previewChainAStart = null;
		previewChainAEnd = null;
		previewChainBStart = null;
		previewChainBEnd = null;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null)
			return;

		ItemStack stack = getHeldChain(player);

		if (firstPos == null)
			return;

		BlockEntity sourceLift = player.level()
			.getBlockEntity(firstPos);

		if (firstDim != player.level()
			.dimension() || !(sourceLift instanceof ChainConveyorBlockEntity)) {
			firstPos = null;
			firstDim = null;
			lastStatusKey = null;
			sendStatus(player, "chain_conveyor.selection_cleared");
			return;
		}

		Level level = player.level();

		if (stack.isEmpty())
			return;

		BlockPos pos = getTargetedConveyorPos(mc);
		if (pos == null) {
			previewSource = firstPos;
			previewTarget = firstPos;
			displayPreviewStatus("chain_conveyor.select_second", ChatFormatting.WHITE);
			return;
		}

		if (pos.equals(firstPos)) {
			previewSource = firstPos;
			previewTarget = firstPos;
			displayPreviewStatus("chain_conveyor.select_second", ChatFormatting.WHITE);
			return;
		}

		previewFailureKey = null;
		boolean success = validateAndConnect(level, pos, player, stack, true);

		int color = success ? 0x5F9F45 : 0xB85A4A;
		previewColor = color;
		previewSource = firstPos;
		previewTarget = pos;
		showPreviewLine = true;
		updatePreviewChains(level, firstPos, pos);

		String statusKey = success ? "chain_conveyor.valid_connection" : previewFailureKey;
		if (statusKey != null)
			displayPreviewStatus(statusKey, success ? ChatFormatting.GREEN : ChatFormatting.RED);
	}

	public static void drawConnectionPreview(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera) {
		// Preview outlines are queued from clientTick and rendered by the shared Outliner.
	}


	private static BlockPos getTargetedConveyorPos(Minecraft mc) {
		if (mc.level == null)
			return null;
		if (ChainConveyorInteractionHandler.selectedLift != null)
			return ChainConveyorInteractionHandler.selectedLift;
		if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == Type.BLOCK
			&& AllBlocks.CHAIN_CONVEYOR.has(mc.level.getBlockState(bhr.getBlockPos())))
			return bhr.getBlockPos();
		return null;
	}

	static BlockPos getPreviewSource() {
		return previewSource;
	}

	static BlockPos getPreviewTarget() {
		return previewTarget;
	}

	static int getPreviewColor() {
		return previewColor;
	}

	static boolean shouldShowPreviewLine() {
		return showPreviewLine;
	}

	static Vec3 getPreviewChainAStart() {
		return previewChainAStart;
	}

	static Vec3 getPreviewChainAEnd() {
		return previewChainAEnd;
	}

	static Vec3 getPreviewChainBStart() {
		return previewChainBStart;
	}

	static Vec3 getPreviewChainBEnd() {
		return previewChainBEnd;
	}

	private static void updatePreviewChains(Level level, BlockPos source, BlockPos target) {
		BlockPos connection = target.subtract(source);
		float offBranchDistance = 35f;
		boolean reversed = level.getBlockEntity(source) instanceof ChainConveyorBlockEntity be && be.getSpeed() < 0;
		float direction = Mth.RAD_TO_DEG * (float) Mth.atan2(connection.getX(), connection.getZ());
		float angle = wrapAngle(direction - offBranchDistance * (reversed ? -1 : 1));
		float oppositeAngle = wrapAngle(angle + 180 + 2 * offBranchDistance * (reversed ? -1 : 1));

		Vec3 start = Vec3.atBottomCenterOf(source)
			.add(VecHelper.rotate(new Vec3(0, 0, 1.25), angle, Axis.Y))
			.add(0, 6 / 16f, 0);
		Vec3 end = Vec3.atBottomCenterOf(target)
			.add(VecHelper.rotate(new Vec3(0, 0, 1.25), oppositeAngle, Axis.Y))
			.add(0, 6 / 16f, 0);

		Vec3 diff = end.subtract(start);
		if (diff.lengthSqr() < 1e-4)
			return;

		Vec3 normal = diff.cross(new Vec3(0, 1, 0));
		if (normal.lengthSqr() < 1e-4)
			normal = new Vec3(1, 0, 0);
		normal = normal.normalize();

		Vec3 origin = Vec3.atCenterOf(source);
		Vec3 offset = start.subtract(origin);
		Vec3 oppositeStart = origin.add(offset.add(normal.scale(-2 * normal.dot(offset))));
		Vec3 oppositeEnd = oppositeStart.add(diff);

		previewChainAStart = start;
		previewChainAEnd = end;
		previewChainBStart = oppositeStart;
		previewChainBEnd = oppositeEnd;
	}

	private static float wrapAngle(float angle) {
		angle %= 360;
		if (angle < 0)
			angle += 360;
		return angle;
	}

	public static boolean validateAndConnect(LevelAccessor level, BlockPos pos, Player player, ItemStack chain,
		boolean simulate) {
		if (simulate)
			previewFailureKey = null;
		if (!simulate && player.isShiftKeyDown()) {
			sendStatus(player, "chain_conveyor.selection_cleared");
			return false;
		}

		if (firstPos == null)
			return false;
		if (pos.equals(firstPos))
			return false;
		if (!pos.closerThan(firstPos, AllConfigs.server().kinetics.maxChainConveyorLength.get()))
			return fail("chain_conveyor.too_far", simulate);
		if (pos.closerThan(firstPos, 2.5))
			return fail("chain_conveyor.too_close", simulate);

		Vec3 diff = Vec3.atLowerCornerOf(pos.subtract(firstPos));
		double horizontalDistance = diff.multiply(1, 0, 1)
			.length() - 1.5;

		if (horizontalDistance <= 0)
			return fail("chain_conveyor.cannot_connect_vertically", simulate);
		if (Math.abs(diff.y) / horizontalDistance > 1)
			return fail("chain_conveyor.too_steep", simulate);

		ChainConveyorBlock chainConveyorBlock = AllBlocks.CHAIN_CONVEYOR.get();
		ChainConveyorBlockEntity sourceLift = chainConveyorBlock.getBlockEntity(level, firstPos);
		ChainConveyorBlockEntity targetLift = chainConveyorBlock.getBlockEntity(level, pos);

		if (sourceLift == null || targetLift == null)
			return fail("chain_conveyor.blocks_invalid", simulate);
		if (targetLift.connections.size() >= AllConfigs.server().kinetics.maxChainConveyorConnections.get())
			return fail("chain_conveyor.cannot_add_more_connections", simulate);
		if (targetLift.connections.contains(firstPos.subtract(pos)))
			return fail("chain_conveyor.already_connected", simulate);

		int chainCost = ChainConveyorBlockEntity.getChainCost(pos.subtract(firstPos));
		boolean hasEnough = player.isCreative()
			|| ChainConveyorBlockEntity.getChainsFromInventory(player, chain, chainCost, true);
		if (simulate)
			BlueprintOverlayRenderer.displayChainRequirements(chain.getItem(), chainCost, hasEnough);

		if (!player.isCreative()) {
			if (!hasEnough)
				return fail("chain_conveyor.not_enough_chains", simulate);
		}

		if (simulate)
			return true;

		ClientNetworkHelper.INSTANCE.sendToServer(new ChainConveyorConnectionPacket(firstPos, pos, chain, true));

		clearStatus(player);
		firstPos = null;
		firstDim = null;
		lastStatusKey = null;
		return true;
	}

	private static boolean fail(String message) {
		return fail(message, false);
	}

	private static boolean fail(String message, boolean simulate) {
		if (simulate) {
			previewFailureKey = message;
			return false;
		}
		displayPreviewStatus(message, ChatFormatting.RED);
		return false;
	}

	private static void displayPreviewStatus(String key, ChatFormatting formatting) {
		int color = switch (formatting) {
		case RED -> 0xB85A4A;
		case GREEN -> 0x5F9F45;
		default -> 0xB8B8B8;
		};
		MutableComponent text = CreateLang.translateDirect(key)
			.withStyle(formatting);
		BlueprintOverlayRenderer.displayChainStatus(text, color);
	}

	private static void sendStatus(Player player, String key) {
		if (isDuplicateStatus(player, key))
			return;
		player.sendSystemMessage(CreateLang.translateDirect(key));
	}

	private static void sendStatus(Player player, String key, ChatFormatting formatting) {
		if (isDuplicateStatus(player, key))
			return;
		player.sendSystemMessage(CreateLang.translateDirect(key)
			.withStyle(formatting));
	}

	private static void clearStatus(Player player) {
		lastStatusKey = null;
	}

	private static boolean isDuplicateStatus(Player player, String key) {
		long now = player.level()
			.getGameTime();
		if (key.equals(lastStatusKey) && now - lastStatusGameTime < 40)
			return true;
		lastStatusKey = key;
		lastStatusGameTime = now;
		return false;
	}

}
