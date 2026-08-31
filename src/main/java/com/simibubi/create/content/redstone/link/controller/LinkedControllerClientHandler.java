package com.simibubi.create.content.redstone.link.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.lang.FontHelper.Palette;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.gui.GuiLayer;

public class LinkedControllerClientHandler {

	public static void toggleInLectern(java.util.UUID previousUser, java.util.UUID user, net.minecraft.core.BlockPos pos) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return;
		if (user == null && player.getUUID().equals(previousUser))
			deactivateInLectern();
		else if (previousUser == null && player.getUUID().equals(user))
			activateInLectern(pos);
	}

	public static final GuiLayer OVERLAY = LinkedControllerClientHandler::renderOverlay;

	public static Mode MODE = Mode.IDLE;
	public static int PACKET_RATE = 5;
	public static Collection<Integer> currentlyPressed = new HashSet<>();
	private static BlockPos lecternPos;
	private static BlockPos selectedLocation = BlockPos.ZERO;
	private static int packetCooldown;

	public static void toggleBindMode(BlockPos location) {
		if (MODE == Mode.IDLE) {
			MODE = Mode.BIND;
			selectedLocation = location;
			return;
		}
		MODE = Mode.IDLE;
		onReset();
	}

	public static void toggle() {
		if (MODE == Mode.IDLE) {
			MODE = Mode.ACTIVE;
			lecternPos = null;
			return;
		}
		MODE = Mode.IDLE;
		onReset();
	}

	public static void activateInLectern(BlockPos lecternAt) {
		if (MODE == Mode.IDLE) {
			MODE = Mode.ACTIVE;
			lecternPos = lecternAt;
		}
	}

	public static void deactivateInLectern() {
		if (MODE == Mode.ACTIVE && inLectern()) {
			MODE = Mode.IDLE;
			onReset();
		}
	}

	public static boolean inLectern() {
		return lecternPos != null;
	}

	protected static void onReset() {
		ControlsUtil.getControls()
			.forEach(kb -> kb.setDown(ControlsUtil.isActuallyPressed(kb)));
		packetCooldown = 0;
		selectedLocation = BlockPos.ZERO;

		if (inLectern())
			ClientNetworkHelper.INSTANCE.sendToServer(new LinkedControllerStopLecternPacket(lecternPos));
		lecternPos = null;

		if (!currentlyPressed.isEmpty())
			ClientNetworkHelper.INSTANCE.sendToServer(new LinkedControllerInputPacket(currentlyPressed, false));
		currentlyPressed.clear();

		LinkedControllerItemModel.resetButtons();
	}

	public static void tick() {
		LinkedControllerItemModel.tick();
		if (MODE == Mode.IDLE)
			return;
		if (packetCooldown > 0)
			packetCooldown--;

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null)
			return;

		ItemStack heldItem = player.getMainHandItem();
		if (player.isSpectator()) {
			MODE = Mode.IDLE;
			onReset();
			return;
		}

		if (!inLectern() && !AllItems.LINKED_CONTROLLER.isIn(heldItem)) {
			heldItem = player.getOffhandItem();
			if (!AllItems.LINKED_CONTROLLER.isIn(heldItem)) {
				MODE = Mode.IDLE;
				onReset();
				return;
			}
		}

		if (inLectern() && AllBlocks.LECTERN_CONTROLLER.get()
			.getBlockEntityOptional(mc.level, lecternPos)
			.map(be -> !be.isUsedBy(mc.player))
			.orElse(true)) {
			deactivateInLectern();
			return;
		}

		if (mc.gui.screen() != null) {
			MODE = Mode.IDLE;
			onReset();
			return;
		}

		if (InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
			MODE = Mode.IDLE;
			onReset();
			return;
		}

		List<KeyMapping> controls = ControlsUtil.getControls();
		Collection<Integer> pressedKeys = new HashSet<>();
		for (int i = 0; i < controls.size(); i++) {
			if (ControlsUtil.isActuallyPressed(controls.get(i)))
				pressedKeys.add(i);
		}

		Collection<Integer> newKeys = new HashSet<>(pressedKeys);
		Collection<Integer> releasedKeys = new HashSet<>(currentlyPressed);
		newKeys.removeAll(releasedKeys);
		releasedKeys.removeAll(pressedKeys);

		if (MODE == Mode.ACTIVE) {
			if (!releasedKeys.isEmpty()) {
				ClientNetworkHelper.INSTANCE.sendToServer(new LinkedControllerInputPacket(releasedKeys, false, lecternPos));
				AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1f, .5f, true);
			}

			if (!newKeys.isEmpty()) {
				ClientNetworkHelper.INSTANCE.sendToServer(new LinkedControllerInputPacket(newKeys, true, lecternPos));
				packetCooldown = PACKET_RATE;
				AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1f, .75f, true);
			}

			if (packetCooldown == 0 && !pressedKeys.isEmpty()) {
				ClientNetworkHelper.INSTANCE.sendToServer(new LinkedControllerInputPacket(pressedKeys, true, lecternPos));
				packetCooldown = PACKET_RATE;
			}
		}

		if (MODE == Mode.BIND) {
			VoxelShape shape = mc.level.getBlockState(selectedLocation).getShape(mc.level, selectedLocation);
			if (!shape.isEmpty())
				Outliner.getInstance().showAABB("controller", shape.bounds().move(selectedLocation))
					.colored(0xB73C2D)
					.lineWidth(1 / 16f);

			for (Integer integer : newKeys) {
				LinkBehaviour linkBehaviour = BlockEntityBehaviour.get(mc.level, selectedLocation, LinkBehaviour.TYPE);
				if (linkBehaviour != null) {
					ClientNetworkHelper.INSTANCE.sendToServer(new LinkedControllerBindPacket(integer, selectedLocation));
					mc.gui.hud.setOverlayMessage(CreateLang.translateDirect("linked_controller.key_bound", controls.get(integer)
						.getTranslatedKeyMessage()
						.getString()), false);
				}
				MODE = Mode.IDLE;
				onReset();
				break;
			}
		}

		currentlyPressed = pressedKeys;
		controls.forEach(kb -> kb.setDown(false));
	}

	public static void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (MODE != Mode.BIND)
			return;

		Object[] keys = new Object[6];
		List<KeyMapping> controls = ControlsUtil.getControls();
		for (int i = 0; i < controls.size(); i++)
			keys[i] = controls.get(i).getTranslatedKeyMessage().getString();

		List<Component> tooltip = new ArrayList<>();
		tooltip.add(CreateLang.translateDirect("linked_controller.bind_mode").withStyle(ChatFormatting.GOLD));
		tooltip.addAll(TooltipHelper.cutTextComponent(
			CreateLang.translateDirect("linked_controller.press_keybind", keys), Palette.ALL_GRAY));

		int tooltipWidth = 0;
		for (Component line : tooltip)
			tooltipWidth = Math.max(tooltipWidth, mc.font.width(line));
		int x = graphics.guiWidth() / 3 - tooltipWidth / 2;
		int y = graphics.guiHeight() - tooltip.size() * mc.font.lineHeight - 24;
		graphics.setComponentTooltipForNextFrame(mc.font, tooltip, x, y);
	}

	public enum Mode {
		IDLE, ACTIVE, BIND
	}

}
