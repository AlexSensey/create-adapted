package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Objects;

@EventBusSubscriber(value = Dist.CLIENT)
public class EjectorTargetHandler {

	static BlockPos currentSelection;
	static ItemStack currentItem;
	static long lastHoveredBlockPos = -1;
	static EntityLauncher launcher;
	static AABB targetPreviewBounds;
	static AABB placementPreviewBounds;
	static int placementPreviewColor = 0x9ede73;

	@SubscribeEvent
	public static void rightClickingBlocksSelectsThem(PlayerInteractEvent.RightClickBlock event) {
		if (currentItem == null)
			return;
		BlockPos pos = event.getPos();
		Level world = event.getLevel();
		if (!world.isClientSide())
			return;
		Player player = event.getEntity();
		if (player == null || player.isSpectator() || !player.isShiftKeyDown())
			return;

		player.sendOverlayMessage(CreateLang.translateDirect("weighted_ejector.target_set")
			.withStyle(ChatFormatting.GOLD));
		currentSelection = pos;
		launcher = null;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
	}

	@SubscribeEvent
	public static void leftClickingBlocksDeselectsThem(PlayerInteractEvent.LeftClickBlock event) {
		if (currentItem == null)
			return;
		if (!event.getLevel().isClientSide())
			return;
		if (!event.getEntity()
			.isShiftKeyDown())
			return;
		BlockPos pos = event.getPos();
		if (pos.equals(currentSelection)) {
			currentSelection = null;
			launcher = null;
			event.setCanceled(true);
		}
	}

	public static void flushSettings(BlockPos pos) {
		int h = 0;
		int v = 0;

		LocalPlayer player = Minecraft.getInstance().player;
		String key = "weighted_ejector.target_not_valid";
		ChatFormatting colour = ChatFormatting.WHITE;

		if (currentSelection == null)
			key = "weighted_ejector.no_target";

		Direction validTargetDirection = getValidTargetDirection(pos);
		if (validTargetDirection == null) {
			if (player != null)
				player.sendOverlayMessage(CreateLang.translateDirect(key)
					.withStyle(colour));
			currentItem = null;
			currentSelection = null;
			return;
		}

		key = "weighted_ejector.targeting";
		colour = ChatFormatting.GREEN;
		if (player != null)
			player.sendOverlayMessage(
				CreateLang.translateDirect(key, currentSelection.getX(), currentSelection.getY(), currentSelection.getZ())
					.withStyle(colour));

		BlockPos diff = pos.subtract(currentSelection);
		h = Math.abs(diff.getX() + diff.getZ());
		v = -diff.getY();

		ClientNetworkHelper.INSTANCE.sendToServer(new EjectorPlacementPacket(h, v, pos, validTargetDirection));
		currentSelection = null;
		currentItem = null;

	}

	public static Direction getValidTargetDirection(BlockPos pos) {
		if (currentSelection == null)
			return null;
		if (VecHelper.onSameAxis(pos, currentSelection, Axis.Y))
			return null;

		int xDiff = currentSelection.getX() - pos.getX();
		int zDiff = currentSelection.getZ() - pos.getZ();
		int max = AllConfigs.server().kinetics.maxEjectorDistance.get();

		if (Math.abs(xDiff) > max || Math.abs(zDiff) > max)
			return null;

		if (xDiff == 0)
			return Direction.get(zDiff < 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE, Axis.Z);
		if (zDiff == 0)
			return Direction.get(xDiff < 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE, Axis.X);

		return null;
	}

	public static void tick() {
		targetPreviewBounds = null;
		placementPreviewBounds = null;
		Player player = Minecraft.getInstance().player;

		if (player == null)
			return;

		ItemStack heldItemMainhand = player.getMainHandItem();
		if (!AllBlocks.WEIGHTED_EJECTOR.isIn(heldItemMainhand)) {
			currentItem = null;
		} else {
			if (heldItemMainhand != currentItem) {
				currentSelection = null;
				currentItem = heldItemMainhand;
			}
			drawOutline(currentSelection);
		}

		checkForWrench(heldItemMainhand);
		drawArc();
	}

	protected static void drawArc() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null)
			return;
		boolean wrench = AllItems.WRENCH.isIn(mc.player.getMainHandItem());

		if (currentSelection == null || currentItem == null && !wrench)
			return;
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() == Type.MISS)
			return;

		BlockPos pos = hit.getBlockPos();
		if (!wrench)
			pos = pos.relative(hit.getDirection());

		int xDiff = currentSelection.getX() - pos.getX();
		int yDiff = currentSelection.getY() - pos.getY();
		int zDiff = currentSelection.getZ() - pos.getZ();
		int validX = Math.abs(zDiff) > Math.abs(xDiff) ? 0 : xDiff;
		int validZ = Math.abs(zDiff) < Math.abs(xDiff) ? 0 : zDiff;

		BlockPos validPos = currentSelection.offset(validX, yDiff, validZ);
		Direction direction = getValidTargetDirection(validPos);
		if (direction == null)
			return;
		if (launcher == null || lastHoveredBlockPos != pos.asLong()) {
			lastHoveredBlockPos = pos.asLong();
			launcher = new EntityLauncher(Math.abs(validX + validZ), yDiff);
		}

		boolean valid = xDiff == validX && zDiff == validZ;
		int colorValue = valid ? 0x9ede73 : 0xff7171;
		DustParticleOptions particle = new DustParticleOptions(colorValue, 1);
		ClientLevel world = mc.level;
		double totalFlyingTicks = launcher.getTotalFlyingTicks() + 3;
		int segments = (int) totalFlyingTicks / 3 + 1;
		double tickOffset = totalFlyingTicks / segments;

		BlockPos placementPos = currentSelection.offset(-validX, -yDiff, -validZ);
		// Zero-height boxes are discarded by the 26.2 outliner. Keep this almost
		// flat so it retains the old ground-square appearance.
		placementPreviewBounds = new AABB(0, 0, 0, 1, 1 / 64f, 1).move(placementPos);
		placementPreviewColor = colorValue;

		for (int i = 0; i < segments; i++) {
			double ticks = AnimationTickHolder.getRenderTime() / 3 % tickOffset + i * tickOffset;
			Vec3 vec = launcher.getGlobalPos(ticks, direction, pos)
				.add(xDiff - validX, 0, zDiff - validZ);
			world.addParticle(particle, vec.x, vec.y, vec.z, 0, 0, 0);
		}
	}

	private static void checkForWrench(ItemStack heldItem) {
		if (!AllItems.WRENCH.isIn(heldItem))
			return;
		HitResult objectMouseOver = Minecraft.getInstance().hitResult;
		if (!(objectMouseOver instanceof BlockHitResult result))
			return;
		BlockPos pos = result.getBlockPos();

		BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
		if (!(be instanceof EjectorBlockEntity)) {
			lastHoveredBlockPos = -1;
			currentSelection = null;
			return;
		}

		if (lastHoveredBlockPos == -1 || lastHoveredBlockPos != pos.asLong()) {
			EjectorBlockEntity ejector = (EjectorBlockEntity) be;
			if (!ejector.getTargetPosition()
				.equals(ejector.getBlockPos()))
				currentSelection = ejector.getTargetPosition();
			lastHoveredBlockPos = pos.asLong();
			launcher = null;
		}

		if (lastHoveredBlockPos != -1)
			drawOutline(currentSelection);
	}

	public static void drawOutline(BlockPos selection) {
		Level world = Minecraft.getInstance().level;
		if (selection == null)
			return;

		BlockPos pos = selection;
		BlockState state = world.getBlockState(pos);
		VoxelShape shape = state.getShape(world, pos);
		AABB boundingBox = shape.isEmpty() ? new AABB(BlockPos.ZERO) : shape.bounds();
		targetPreviewBounds = boundingBox.move(pos);
	}

	public static AABB getTargetPreviewBounds() {
		return targetPreviewBounds;
	}

	public static AABB getPlacementPreviewBounds() {
		return placementPreviewBounds;
	}

	public static int getPlacementPreviewColor() {
		return placementPreviewColor;
	}

}
