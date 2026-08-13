package com.simibubi.create.content.redstone.displayLink;

import com.simibubi.create.AllDataComponents;

import net.createmod.catnip.api.client.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClickToLinkClient {
	private static BlockPos lastShownPos = null;
	private static AABB lastShownAABB = null;
	private static ClickToLinkBlockItem locallySelectedItem = null;
	private static BlockPos locallySelectedPos = null;
	private static boolean previewVisible = false;

	public static void select(ClickToLinkBlockItem item, BlockPos pos) {
		locallySelectedItem = item;
		locallySelectedPos = pos;
		lastShownPos = null;
		lastShownAABB = null;
	}

	public static void clearSelection() {
		locallySelectedItem = null;
		locallySelectedPos = null;
		lastShownPos = null;
		lastShownAABB = null;
		previewVisible = false;
	}

	public static void tick() {
		previewVisible = false;
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			clearSelection();
			return;
		}
		ItemStack heldItemMainhand = player.getMainHandItem();
		if (!(heldItemMainhand.getItem() instanceof ClickToLinkBlockItem blockItem)) {
			clearSelection();
			return;
		}

		ClickToLinkBlockItem.ClickToLinkData data = heldItemMainhand.get(AllDataComponents.CLICK_TO_LINK_DATA);
		BlockPos selectedPos;
		if (data != null)
			selectedPos = data.selectedPos();
		else if (locallySelectedItem == blockItem && locallySelectedPos != null)
			selectedPos = locallySelectedPos;
		else
			return;

		if (!selectedPos.equals(lastShownPos)) {
			lastShownAABB = blockItem.getSelectionBounds(selectedPos);
			lastShownPos = selectedPos;
		}

		if (lastShownAABB == null)
			return;
		previewVisible = true;
		Outliner.getInstance().showAABB("target", lastShownAABB)
			.colored(0xffcb74)
			.lineWidth(1 / 16f);
	}

	public static AABB getPreviewBounds() {
		return previewVisible ? lastShownAABB : null;
	}

	public static AABB getSelectionBounds(BlockPos pos) {
		Level world = Minecraft.getInstance().level;
		BlockState state = world.getBlockState(pos);
		VoxelShape shape = state.getShape(world, pos);
		return shape.isEmpty() ? new AABB(BlockPos.ZERO) : shape.bounds()
			.move(pos);
	}
}
