package com.simibubi.create.content.equipment.clipboard;

import java.util.UUID;

import com.simibubi.create.AllDataComponents;

import org.jetbrains.annotations.Nullable;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class ClipboardClient {
	private ClipboardClient() {}

	public static void openScreen(Player player, DataComponentMap components, @Nullable BlockPos pos) {
		if (Minecraft.getInstance().player == player)
			ScreenOpener.open(new ClipboardScreen(player.getInventory().getSelectedSlot(), components, pos));
	}

	public static void readUpdate(ClipboardBlockEntity clipboard, CompoundTag tag) {
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.gui.screen() instanceof ClipboardScreen screen) || mc.player == null)
			return;
		String editor = tag.getStringOr("LastEdit", "");
		if (!editor.isBlank()) {
			try {
				if (UUID.fromString(editor).equals(mc.player.getUUID()))
					return;
			} catch (IllegalArgumentException ignored) {}
		}
		if (!clipboard.getBlockPos().equals(screen.targetedBlock))
			return;
		screen.reopenWith(clipboard.components()
			.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY));
	}
}
