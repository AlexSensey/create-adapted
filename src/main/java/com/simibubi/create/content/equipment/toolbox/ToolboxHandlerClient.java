package com.simibubi.create.content.equipment.toolbox;

import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_HOTBAR_OFF;
import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_HOTBAR_ON;
import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_SELECTED_OFF;
import static com.simibubi.create.foundation.gui.AllGuiTextures.TOOLBELT_SELECTED_ON;

import java.util.Comparator;
import java.util.List;

import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.gui.GuiLayer;

public class ToolboxHandlerClient {
	public static final GuiLayer OVERLAY = ToolboxHandlerClient::renderOverlay;
	static int COOLDOWN = 0;

	public static void clientTick() {
		if (COOLDOWN > 0 && !AllKeys.TOOLBELT.isPressed())
			COOLDOWN--;
	}

	public static boolean onPickItem() {
		return false;
	}

	public static void onKeyInput(int key, boolean pressed) {
		if (!pressed || !AllKeys.TOOLBELT.doesModifierAndCodeMatch(key) || COOLDOWN > 0)
			return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
			return;
		LocalPlayer player = mc.player;
		if (player == null)
			return;
		Level level = player.level();

		List<ToolboxBlockEntity> toolboxes = ToolboxHandler.getNearest(level, player, 8);
		toolboxes.sort(Comparator.comparing(ToolboxBlockEntity::getUniqueId));
		CompoundTag compound = player.getPersistentData().getCompound("CreateToolboxData")
			.orElseGet(CompoundTag::new);
		String slotKey = String.valueOf(player.getInventory().getSelectedSlot());

		if (compound.contains(slotKey)) {
			CompoundTag data = compound.getCompoundOrEmpty(slotKey);
			BlockPos pos = ToolboxHandler.readBlockPos(data.getCompoundOrEmpty("Pos"));
			double max = ToolboxHandler.getMaxRange(player);
			if (ToolboxHandler.distance(player.position(), pos) < max * max) {
				BlockEntity blockEntity = level.getBlockEntity(pos);
				if (blockEntity instanceof ToolboxBlockEntity selected) {
					RadialToolboxMenu screen = new RadialToolboxMenu(toolboxes,
						RadialToolboxMenu.State.SELECT_ITEM_UNEQUIP, selected);
					screen.prevSlot(data.getIntOr("Slot", 0));
					ScreenOpener.open(screen);
					return;
				}
			}
			ScreenOpener.open(new RadialToolboxMenu(List.of(), RadialToolboxMenu.State.DETACH, null));
			return;
		}

		if (toolboxes.isEmpty())
			return;
		ScreenOpener.open(toolboxes.size() == 1
			? new RadialToolboxMenu(toolboxes, RadialToolboxMenu.State.SELECT_ITEM, toolboxes.getFirst())
			: new RadialToolboxMenu(toolboxes, RadialToolboxMenu.State.SELECT_BOX, null));
	}

	public static void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
			return;
		Player player = mc.player;
		if (player == null)
			return;
		CompoundTag compound = player.getPersistentData().getCompound("CreateToolboxData")
			.orElseGet(CompoundTag::new);
		if (compound.isEmpty())
			return;

		int x = graphics.guiWidth() / 2 - 90;
		int y = graphics.guiHeight() - 23;
		for (int slot = 0; slot < 9; slot++) {
			String key = String.valueOf(slot);
			if (!compound.contains(key))
				continue;
			BlockPos pos = ToolboxHandler.readBlockPos(compound.getCompoundOrEmpty(key).getCompoundOrEmpty("Pos"));
			double max = ToolboxHandler.getMaxRange(player);
			boolean selected = player.getInventory().getSelectedSlot() == slot;
			int offset = selected ? 1 : 0;
			AllGuiTextures texture = ToolboxHandler.distance(player.position(), pos) < max * max
				? selected ? TOOLBELT_SELECTED_ON : TOOLBELT_HOTBAR_ON
				: selected ? TOOLBELT_SELECTED_OFF : TOOLBELT_HOTBAR_OFF;
			texture.render(graphics, x + 20 * slot - offset, y + offset);
		}
	}
}
