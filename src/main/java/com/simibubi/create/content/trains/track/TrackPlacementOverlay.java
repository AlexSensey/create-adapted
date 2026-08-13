package com.simibubi.create.content.trains.track;

import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.client.gui.GuiLayer;

public class TrackPlacementOverlay implements GuiLayer {
	public static final TrackPlacementOverlay INSTANCE = new TrackPlacementOverlay();

	@Override
	public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
			return;
		if (TrackPlacement.hoveringPos == null)
			return;
		if (TrackPlacement.cached == null || TrackPlacement.cached.curve == null || !TrackPlacement.cached.valid)
			return;
		if (TrackPlacement.extraTipWarmup < 4)
			return;

		boolean active = mc.options.keySprint.isDown();
		MutableComponent text = CreateLang.translateDirect("track.hold_for_smooth_curve",
			Component.keybind("key.sprint")
				.withStyle(active ? ChatFormatting.WHITE : ChatFormatting.GRAY));
		int x = (guiGraphics.guiWidth() - mc.font.width(text)) / 2;
		int y = guiGraphics.guiHeight() - 61;
		int alpha = Mth.floor(Mth.clamp((TrackPlacement.extraTipWarmup - 4) / 3f, .1f, 1) * 255);
		guiGraphics.text(mc.font, text, x, y, alpha << 24 | 0x4ADB4A, false);
	}
}
