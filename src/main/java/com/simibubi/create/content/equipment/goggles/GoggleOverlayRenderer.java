package com.simibubi.create.content.equipment.goggles;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.equipment.goggles.IHaveCustomOverlayIcon;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.gui.GuiLayer;

public class GoggleOverlayRenderer {

	public static final GuiLayer OVERLAY = GoggleOverlayRenderer::renderOverlay;
	public static int hoverTicks = 0;
	public static BlockPos lastHovered = null;

	public static void renderOverlay(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR
			|| mc.player == null || mc.level == null)
			return;

		if (!(mc.hitResult instanceof BlockHitResult result)) {
			resetHover();
			return;
		}

		ClientLevel world = mc.level;
		BlockPos targetedPos = result.getBlockPos();
		if (!targetedPos.equals(lastHovered))
			hoverTicks = 0;
		lastHovered = targetedPos;
		hoverTicks++;

		BlockPos pos = proxiedOverlayPosition(world, targetedPos);
		BlockEntity be = world.getBlockEntity(pos);
		boolean wearingGoggles = GogglesItem.isWearingGoggles(mc.player);
		boolean isShifting = mc.player.isShiftKeyDown();
		boolean hasGoggleInformation = be instanceof IHaveGoggleInformation;
		boolean hasHoveringInformation = be instanceof IHaveHoveringInformation;
		boolean goggleAddedInformation = false;
		boolean hoverAddedInformation = false;

		ItemStack icon = AllItems.GOGGLES.asStack();
		List<Component> tooltip = new ArrayList<>();
		if (be instanceof IHaveCustomOverlayIcon customOverlayIcon)
			icon = customOverlayIcon.getIcon(isShifting);

		if (hasGoggleInformation && wearingGoggles)
			goggleAddedInformation = ((IHaveGoggleInformation) be).addToGoggleTooltip(tooltip, isShifting);

		if (hasHoveringInformation) {
			if (!tooltip.isEmpty())
				tooltip.add(CommonComponents.EMPTY);
			hoverAddedInformation = ((IHaveHoveringInformation) be).addToTooltip(tooltip, isShifting);
			if (goggleAddedInformation && !hoverAddedInformation)
				tooltip.removeLast();
		}

		if (be instanceof IDisplayAssemblyExceptions exceptions && exceptions.addExceptionToTooltip(tooltip)) {
			hasHoveringInformation = true;
			hoverAddedInformation = true;
		}

		if (!hasHoveringInformation && TrainRelocator.addToTooltip(tooltip, isShifting)) {
			hasHoveringInformation = true;
			hoverAddedInformation = true;
		}

		if (hasGoggleInformation && !goggleAddedInformation && hasHoveringInformation && !hoverAddedInformation) {
			resetHover();
			return;
		}

		addPistonPoleLength(world, pos, wearingGoggles, tooltip);
		if (tooltip.isEmpty()) {
			resetHover();
			return;
		}

		renderTooltip(guiGraphics, deltaTracker, mc, icon, tooltip);
	}

	private static void addPistonPoleLength(ClientLevel world, BlockPos pos, boolean wearingGoggles,
		List<Component> tooltip) {
		BlockState state = world.getBlockState(pos);
		if (!wearingGoggles || !AllBlocks.PISTON_EXTENSION_POLE.has(state))
			return;

		Direction[] directions = Iterate.directionsInAxis(state.getValue(PistonExtensionPoleBlock.FACING).getAxis());
		int poles = 1;
		boolean pistonFound = false;
		for (Direction direction : directions) {
			int attached = PistonExtensionPoleBlock.PlacementHelper.get().attachedPoles(world, pos, direction);
			poles += attached;
			pistonFound |= world.getBlockState(pos.relative(direction, attached + 1))
				.getBlock() instanceof MechanicalPistonBlock;
		}

		if (!pistonFound)
			return;
		if (!tooltip.isEmpty())
			tooltip.add(CommonComponents.EMPTY);
		CreateLang.translate("gui.goggles.pole_length").text(" " + poles).forGoggles(tooltip);
	}

	private static void renderTooltip(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, Minecraft mc,
		ItemStack icon, List<Component> tooltip) {
		int textWidth = tooltip.stream().mapToInt(mc.font::width).max().orElse(0);
		int textHeight = tooltip.size() * 10 + 4;
		int width = guiGraphics.guiWidth();
		int height = guiGraphics.guiHeight();
		CClient config = AllConfigs.client();
		int x = width / 2 + config.overlayOffsetX.get();
		int y = height / 2 + config.overlayOffsetY.get();
		x = Mth.clamp(x, 5, Math.max(5, width - textWidth - 25));
		y = Mth.clamp(y, 22, Math.max(22, height - textHeight - 8));

		float fade = Mth.clamp((hoverTicks + deltaTracker.getGameTimeDeltaPartialTick(false)) / 24f, 0, 1);
		x += Math.round(Math.pow(1 - fade, 3) * Math.signum(config.overlayOffsetX.get() + .5f) * 8);
		int background = config.overlayCustomColor.get() ? config.overlayBackgroundColor.get() : 0xF0100010;
		int borderTop = config.overlayCustomColor.get() ? config.overlayBorderColorTop.get() : 0x505000FF;
		int borderBottom = config.overlayCustomColor.get() ? config.overlayBorderColorBot.get() : 0x5028007F;
		background = scaleAlpha(background, fade);
		borderTop = scaleAlpha(borderTop, fade);
		borderBottom = scaleAlpha(borderBottom, fade);

		int left = x - 4;
		int top = y - 4;
		int right = x + textWidth + 8;
		int bottom = y + textHeight;
		guiGraphics.fill(left, top, right, bottom, background);
		guiGraphics.fill(left, top, right, top + 1, borderTop);
		guiGraphics.fill(left, bottom - 1, right, bottom, borderBottom);
		guiGraphics.fillGradient(left, top + 1, left + 1, bottom - 1, borderTop, borderBottom);
		guiGraphics.fillGradient(right - 1, top + 1, right, bottom - 1, borderTop, borderBottom);
		guiGraphics.nextStratum();
		guiGraphics.item(icon, x + 10, y - 16);
		guiGraphics.nextStratum();

		int textAlpha = Math.max(4, Math.round(fade * 255));
		for (int line = 0; line < tooltip.size(); line++)
			guiGraphics.text(mc.font, tooltip.get(line), x, y + line * 10, textAlpha << 24 | 0xFFFFFF, true);
	}

	private static int scaleAlpha(int color, float alpha) {
		return color & 0x00FFFFFF | Math.round((color >>> 24) * alpha) << 24;
	}

	private static void resetHover() {
		lastHovered = null;
		hoverTicks = 0;
	}

	public static BlockPos proxiedOverlayPosition(Level level, BlockPos pos) {
		BlockState targetedState = level.getBlockState(pos);
		if (targetedState.getBlock() instanceof IProxyHoveringInformation proxy)
			return proxy.getInformationSource(level, pos, targetedState);
		return pos;
	}
}
