package com.simibubi.create.content.equipment.armor;

import java.util.List;

import com.simibubi.create.AllItems;

import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.gui.GuiLayer;

public class RemainingAirOverlay {
	public static final RemainingAirOverlay INSTANCE = new RemainingAirOverlay();
	public static final GuiLayer OVERLAY = INSTANCE::render;

	public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
			return;

		LocalPlayer player = mc.player;
		if (player == null || player.isCreative() || mc.level == null)
			return;
		if (!DivingHelmetItem.isWornBy(player))
			return;
		List<ItemStack> backtanks = BacktankUtil.getAllWithAir(player);
		if (backtanks.isEmpty())
			return;

		boolean inLava = player.isInLava() || player.isEyeInFluid(FluidTags.LAVA);
		boolean inWater = player.isEyeInFluid(FluidTags.WATER);
		boolean inBubbleColumn = player.level()
			.getBlockState(BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()))
			.is(Blocks.BUBBLE_COLUMN);
		if ((!inWater && !inLava && !inBubbleColumn)
			|| (MobEffectUtil.hasWaterBreathing(player) || player.getAbilities().invulnerable) && !inLava)
			return;

		int timeLeft = backtanks.stream()
			.mapToInt(BacktankUtil::getAir)
			.sum();
		ItemStack backtank = backtanks.getFirst();
		int x = guiGraphics.guiWidth() / 2 + 90;
		int y = guiGraphics.guiHeight() - 53 + (AllItems.NETHERITE_BACKTANK.isIn(backtank) ? 9 : 0);

		guiGraphics.item(backtank, x, y);
		Component text = Component.literal(StringUtil.formatTickDuration(Math.max(0, timeLeft - 1) * 20,
			mc.level.tickRateManager().tickrate()));
		int color = 0xFFFFFFFF;
		if (timeLeft < 60 && timeLeft % 2 == 0)
			color = 0xFF000000 | Color.mixColors(0xFF0000, 0xFFFFFF, Math.max(timeLeft / 60f, .25f));
		guiGraphics.text(mc.font, text, x + 16, y + 5, color);
	}

	public static ItemStack getDisplayedBacktank(LocalPlayer player) {
		List<ItemStack> backtanks = BacktankUtil.getAllWithAir(player);
		return backtanks.isEmpty() ? AllItems.COPPER_BACKTANK.asStack() : backtanks.getFirst();
	}
}
