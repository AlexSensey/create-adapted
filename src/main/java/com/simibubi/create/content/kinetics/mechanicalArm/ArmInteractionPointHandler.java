package com.simibubi.create.content.kinetics.mechanicalArm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.createmod.catnip.api.client.render.SuperRenderTypeBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ArmInteractionPointHandler {

	private static final int INPUT_COLOR = 0x7FCDE0;
	private static final int OUTPUT_COLOR = 0xDDC166;

	static List<ArmInteractionPoint> currentSelection = new ArrayList<>();
	static Map<BlockPos, SuperGlueEntity> previewEntities = new HashMap<>();
	static Map<SuperGlueEntity, Integer> previewEntityColors = new IdentityHashMap<>();
	static ItemStack currentItem;

	static long lastBlockPos = -1;
	static long lastSelectionGameTime = -1;
	static BlockPos lastSelectionPos;
	static int nextPreviewEntityId = Integer.MIN_VALUE + 7000;

	@SubscribeEvent
	public static void rightClickingBlocksSelectsThem(PlayerInteractEvent.RightClickBlock event) {
		Level world = event.getLevel();
		if (!world.isClientSide())
			return;
		Player player = event.getEntity();
		if (!selectPoint(world, player, event.getPos()))
			return;

		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
	}

	public static boolean selectPoint(Level world, Player player, BlockPos pos) {
		if (!world.isClientSide())
			return false;
		if (player == null || player.isSpectator())
			return false;
		ItemStack heldItem = player.getMainHandItem();
		if (!isHoldingArm(heldItem))
			return false;
		if (lastSelectionGameTime == world.getGameTime() && pos.equals(lastSelectionPos))
			return true;
		if (currentItem == null)
			currentItem = heldItem;

		ArmInteractionPoint selected = getSelected(pos);
		BlockState state = world.getBlockState(pos);

		if (selected == null) {
			ArmInteractionPoint point = ArmInteractionPoint.create(world, pos, state);
			if (point == null)
				return false;
			selected = point;
			put(point);
		}

		selected.cycleMode();
		lastSelectionGameTime = world.getGameTime();
		lastSelectionPos = pos;
		showSelectionOutline(selected, 40);
		validateSelection(currentSelection);

		Mode mode = selected.getMode();
		Component message = Component.translatable("create." + mode.getTranslationKey(),
			CreateLang.blockName(state)
				.style(ChatFormatting.WHITE)
				.component());
		Minecraft.getInstance().gui.hud.setOverlayMessage(message, false);
		return true;
	}

	@SubscribeEvent
	public static void leftClickingBlocksDeselectsThem(PlayerInteractEvent.LeftClickBlock event) {
		if (!event.getLevel().isClientSide())
			return;
		Player player = event.getEntity();
		if (player == null || !isHoldingArm(player.getMainHandItem()))
			return;
		BlockPos pos = event.getPos();
		if (remove(pos) != null) {
			validateSelection(currentSelection);
			event.setCanceled(true);
		}
	}

	public static void flushSettings(BlockPos pos) {
		if (currentSelection == null)
			return;

		int removed = 0;
		for (Iterator<ArmInteractionPoint> iterator = currentSelection.iterator(); iterator.hasNext(); ) {
			ArmInteractionPoint point = iterator.next();
			if (point.getPos().closerThan(pos, ArmBlockEntity.getRange()))
				continue;
			iterator.remove();
			removed++;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (removed > 0) {
			Component message = CreateLang.builder()
				.translate("mechanical_arm.points_outside_range", removed)
				.style(ChatFormatting.RED)
				.component();
			if (player != null)
				player.sendOverlayMessage(message);
		} else {
			int inputs = 0;
			int outputs = 0;
			for (ArmInteractionPoint armInteractionPoint : currentSelection) {
				if (armInteractionPoint.getMode() == Mode.DEPOSIT)
					outputs++;
				else
					inputs++;
			}
			if (inputs + outputs > 0 && player != null) {
				Component message = CreateLang.builder()
					.translate("mechanical_arm.summary", inputs, outputs)
					.style(ChatFormatting.WHITE)
					.component();
				player.sendOverlayMessage(message);
			}
		}

		ClientNetworkHelper.INSTANCE.sendToServer(new ArmPlacementPacket(currentSelection, pos));
		clearSelection();
		currentItem = null;
	}

	public static void tick() {
		Player player = Minecraft.getInstance().player;

		if (player == null)
			return;

		ItemStack heldItemMainhand = player.getMainHandItem();
		if (AllItems.WRENCH.isIn(heldItemMainhand)) {
			currentItem = null;
			checkForWrench(heldItemMainhand);
			return;
		}

		lastBlockPos = -1;
		if (!isHoldingArm(heldItemMainhand)) {
			currentItem = null;
			clearSelection();
		} else {
			if (currentItem == null)
				currentItem = heldItemMainhand;

			validateSelection(currentSelection);
		}
	}

	private static void checkForWrench(ItemStack heldItem) {
		if (!AllItems.WRENCH.isIn(heldItem)) {
			return;
		}

		HitResult objectMouseOver = Minecraft.getInstance().hitResult;
		if (!(objectMouseOver instanceof BlockHitResult result)) {
			return;
		}

		BlockPos pos = result.getBlockPos();

		BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
		if (!(be instanceof ArmBlockEntity)) {
			lastBlockPos = -1;
			clearSelection();
			return;
		}

		if (lastBlockPos == -1 || lastBlockPos != pos.asLong()) {
			clearSelection();
			ArmBlockEntity arm = (ArmBlockEntity) be;
			arm.inputs.forEach(ArmInteractionPointHandler::put);
			arm.outputs.forEach(ArmInteractionPointHandler::put);
			lastBlockPos = pos.asLong();
		}

		if (lastBlockPos != -1) {
			validateSelection(currentSelection);
		}
	}

	private static void validateSelection(Collection<ArmInteractionPoint> selection) {
		for (Iterator<ArmInteractionPoint> iterator = selection.iterator(); iterator.hasNext();) {
			ArmInteractionPoint point = iterator.next();
			if (!point.isValid()) {
				iterator.remove();
				removePreview(point.getPos());
				continue;
			}
			point.keepAlive();
			showSelectionOutline(point);
		}
	}

	private static void showSelectionOutline(ArmInteractionPoint point) {
		showSelectionOutline(point, 1);
	}

	private static void showSelectionOutline(ArmInteractionPoint point, int ttl) {
		Level level = point.getLevel();
		BlockPos pos = point.getPos();
		BlockState state = level.getBlockState(pos);
		VoxelShape shape = state.getShape(level, pos);
		AABB boundingBox = shape.isEmpty() ? new AABB(pos) : shape.bounds()
			.move(pos);
		boundingBox = expandFunnelOutline(state, boundingBox);
		Outliner.getInstance()
			.showAABB(new SelectionOutlineSlot(pos), boundingBox, ttl)
			.colored(selectionColor(point))
			.lineWidth(1 / 16f);
		showPreviewEntity(point, boundingBox);
	}

	private static AABB expandFunnelOutline(BlockState state, AABB boundingBox) {
		if (!(state.getBlock() instanceof AbstractFunnelBlock))
			return boundingBox;

		Direction facing = AbstractFunnelBlock.getFunnelFacing(state);
		if (facing == null)
			return boundingBox;

		double extra = 1 / 16d;
		return switch (facing) {
			case EAST -> new AABB(boundingBox.minX, boundingBox.minY, boundingBox.minZ,
				boundingBox.maxX + extra, boundingBox.maxY, boundingBox.maxZ);
			case WEST -> new AABB(boundingBox.minX - extra, boundingBox.minY, boundingBox.minZ,
				boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
			case UP -> new AABB(boundingBox.minX, boundingBox.minY, boundingBox.minZ,
				boundingBox.maxX, boundingBox.maxY + extra, boundingBox.maxZ);
			case DOWN -> new AABB(boundingBox.minX, boundingBox.minY - extra, boundingBox.minZ,
				boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
			case SOUTH -> new AABB(boundingBox.minX, boundingBox.minY, boundingBox.minZ,
				boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ + extra);
			case NORTH -> new AABB(boundingBox.minX, boundingBox.minY, boundingBox.minZ - extra,
				boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
		};
	}

	private static void showPreviewEntity(ArmInteractionPoint point, AABB boundingBox) {
		Level level = point.getLevel();
		ClientLevel clientLevel = Minecraft.getInstance().level;
		if (level != clientLevel || clientLevel == null)
			return;

		BlockPos pos = point.getPos();
		SuperGlueEntity entity = previewEntities.get(pos);
		int color = selectionColor(point);
		if (entity == null || entity.isRemoved() || entity.level() != level) {
			entity = new SuperGlueEntity(level, boundingBox);
			entity.setId(nextPreviewEntityId++);
			previewEntities.put(pos, entity);
			previewEntityColors.put(entity, color);
			clientLevel.addEntity(entity);
			return;
		}

		previewEntityColors.put(entity, color);
		if (entity.getBoundingBox().equals(boundingBox))
			return;
		entity.setBoundingBox(boundingBox);
		entity.resetPositionToBB();
	}

	private static void clearSelection() {
		currentSelection.clear();
		clearPreviews();
	}

	private static void clearPreviews() {
		for (SuperGlueEntity entity : previewEntities.values())
			entity.discard();
		previewEntities.clear();
		previewEntityColors.clear();
	}

	private static void removePreview(BlockPos pos) {
		SuperGlueEntity entity = previewEntities.remove(pos);
		if (entity != null) {
			previewEntityColors.remove(entity);
			entity.discard();
		}
	}

	public static void renderSelection(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera) {
		// Selection outlines are submitted to Outliner from tick(); direct vertices from this stage are unsafe in 26.2.
	}

	private static int selectionColor(ArmInteractionPoint point) {
		return point.getMode() == Mode.TAKE ? INPUT_COLOR : OUTPUT_COLOR;
	}

	public static int getPreviewColor(SuperGlueEntity entity) {
		return previewEntityColors.getOrDefault(entity, -1);
	}

	private record SelectionOutlineSlot(BlockPos pos) {
	}

	public static boolean isHoldingArm(ItemStack stack) {
		return !stack.isEmpty() && (AllBlocks.MECHANICAL_ARM.isIn(stack) || stack.is(AllBlocks.MECHANICAL_ARM.asItem()));
	}

	private static void put(ArmInteractionPoint point) {
		currentSelection.add(point);
	}

	private static ArmInteractionPoint remove(BlockPos pos) {
		ArmInteractionPoint result = getSelected(pos);
		if (result != null) {
			currentSelection.remove(result);
			removePreview(pos);
		}
		return result;
	}

	private static ArmInteractionPoint getSelected(BlockPos pos) {
		for (ArmInteractionPoint point : currentSelection)
			if (point.getPos()
				.equals(pos))
				return point;
		return null;
	}

}
