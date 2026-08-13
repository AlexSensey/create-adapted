package com.simibubi.create.foundation.events;

import com.simibubi.create.CreateClient;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.elevator.ElevatorControlsHandler;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchHandler;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorConnectionHandler;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler;
import com.simibubi.create.content.kinetics.chainConveyor.ChainPackageInteractionHandler;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionHandler;
import com.simibubi.create.content.logistics.packagePort.PackagePortTargetSelectionHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import com.simibubi.create.content.equipment.symmetryWand.ClearSymmetryWandPacket;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.simibubi.create.content.equipment.zapper.ZapperItem;
import com.simibubi.create.foundation.networking.LeftClickPacket;
import com.simibubi.create.content.trains.TrainHUD;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.content.trains.track.CurvedTrackInteraction;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(Dist.CLIENT)
public class InputEvents {

	@SubscribeEvent
	public static void onKeyInput(InputEvent.Key event) {
		if (Minecraft.getInstance().gui.screen() != null)
			return;
		CreateClient.SCHEMATIC_HANDLER.onKeyInput(event.getKey(), event.getAction() != 0);
		ToolboxHandlerClient.onKeyInput(event.getKey(), event.getAction() != 0);
		RadialWrenchHandler.onKeyInput(event.getKey(), event.getAction() != 0);
	}

	@SubscribeEvent
	public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		if (Minecraft.getInstance().gui.screen() != null)
			return;
		double delta = event.getScrollDeltaY();
		if (CreateClient.SCHEMATIC_HANDLER.mouseScrolled(delta)
			|| CreateClient.SCHEMATIC_AND_QUILL_HANDLER.mouseScrolled(delta)
			|| TrainHUD.onScroll(delta)
			|| ElevatorControlsHandler.onScroll(delta))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onMouseInput(InputEvent.MouseButton.Pre event) {
		if (Minecraft.getInstance().gui.screen() != null)
			return;
		int button = event.getButton();
		boolean pressed = event.getAction() != 0;
		RadialWrenchHandler.onKeyInput(button, pressed);
		if (CreateClient.SCHEMATIC_HANDLER.onMouseInput(button, pressed)
			|| CreateClient.SCHEMATIC_AND_QUILL_HANDLER.onMouseInput(button, pressed))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
		Minecraft mc = Minecraft.getInstance();
		if (CurvedTrackInteraction.onClickInput(event)) {
			event.setCanceled(true);
			return;
		}

		KeyMapping key = event.getKeyMapping();
		InteractionHand symmetryHand = getSymmetryWandHand(mc);
		if (key == mc.options.keyUse && mc.player != null && !mc.player.isShiftKeyDown()
			&& symmetryHand != null && (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS
				|| isLookingAtSymmetryMirror(mc, mc.player.getItemInHand(symmetryHand)))) {
			mc.player.getItemInHand(symmetryHand).set(AllDataComponents.SYMMETRY_WAND_ENABLE, false);
			ClientNetworkHelper.INSTANCE.sendToServer(new ClearSymmetryWandPacket(symmetryHand));
			event.setSwingHand(false);
			event.setCanceled(true);
			return;
		}
		if (key == mc.options.keyAttack && mc.player != null
			&& mc.player.getMainHandItem().getItem() instanceof ZapperItem) {
			// Select the looked-at material without starting vanilla block breaking or its
			// first-person digging animation. The server performs the same ray trace.
			ClientNetworkHelper.INSTANCE.sendToServer(LeftClickPacket.INSTANCE);
			event.setSwingHand(false);
			event.setCanceled(true);
			return;
		}
		if (key == mc.options.keyUse || key == mc.options.keyAttack) {
			if (CreateClient.GLUE_HANDLER.onMouseInput(key == mc.options.keyAttack)) {
				event.setCanceled(true);
				return;
			}
		}

		if (key == mc.options.keyUse && ChainConveyorConnectionHandler.onUseKey()) {
			event.setCanceled(true);
			return;
		}

		if (key == mc.options.keyUse && ChainConveyorConnectionHandler.onRightClick()) {
			event.setCanceled(true);
			return;
		}

		if (key == mc.options.keyUse && FactoryPanelConnectionHandler.onRightClick()) {
			event.setCanceled(true);
			return;
		}

		if (!event.isUseItem())
			return;

		TrainRelocator.onClicked(event);
		if (event.isCanceled())
			return;

		if (ChainConveyorInteractionHandler.onUse()) {
			event.setCanceled(true);
			return;
		}
		if (PackagePortTargetSelectionHandler.onUse()) {
			event.setCanceled(true);
			return;
		}

		if (mc.player != null) {
			ItemStack heldItem = mc.player.getItemInHand(event.getHand());
			if (AllItems.WRENCH.isIn(heldItem) || heldItem.is(Items.IRON_CHAIN)
				|| AllBlocks.PACKAGE_FROGPORT.isIn(heldItem))
				return;
		}

		if (ChainPackageInteractionHandler.onUse()) {
			event.setSwingHand(false);
			event.setCanceled(true);
		}
	}

	private static InteractionHand getSymmetryWandHand(Minecraft minecraft) {
		if (minecraft.player == null)
			return null;
		if (minecraft.player.getMainHandItem().getItem() instanceof SymmetryWandItem)
			return InteractionHand.MAIN_HAND;
		if (minecraft.player.getOffhandItem().getItem() instanceof SymmetryWandItem)
			return InteractionHand.OFF_HAND;
		return null;
	}

	private static boolean isLookingAtSymmetryMirror(Minecraft minecraft, ItemStack wand) {
		if (minecraft.player == null || !SymmetryWandItem.isEnabled(wand))
			return false;
		Vec3 mirrorPosition = SymmetryWandItem.getMirror(wand).getPosition();
		AABB bounds = new AABB(mirrorPosition.x, mirrorPosition.y, mirrorPosition.z,
			mirrorPosition.x + 1, mirrorPosition.y + 1, mirrorPosition.z + 1).inflate(.25);
		Vec3 eye = minecraft.player.getEyePosition();
		Vec3 end = eye.add(minecraft.player.getViewVector(1)
			.scale(minecraft.player.blockInteractionRange() + 1));
		return bounds.clip(eye, end).isPresent();
	}
}
