package com.simibubi.create.foundation.blockEntity.behaviour;

import java.util.List;

import com.simibubi.create.AllBlocks;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class ValueSettingsClient implements GuiLayer {
	public int interactHeldTicks = -1;
	public BlockPos interactHeldPos = null;
	public BehaviourType<?> interactHeldBehaviour = null;
	public InteractionHand interactHeldHand = null;
	public Direction interactHeldFace = null;

	public List<MutableComponent> lastHoverTip;
	public int hoverTicks;
	public int hoverWarmup;

	public void cancelIfWarmupAlreadyStarted(PlayerInteractEvent.RightClickBlock event) {
		if (interactHeldTicks != -1 && event.getPos()
			.equals(interactHeldPos)) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.FAIL);
		}
	}

	public void startInteractionWith(BlockPos pos, BehaviourType<?> behaviourType, InteractionHand hand,
		Direction side) {
		interactHeldTicks = 0;
		interactHeldPos = pos;
		interactHeldBehaviour = behaviourType;
		interactHeldHand = hand;
		interactHeldFace = side;
	}

	public void cancelInteraction() {
		interactHeldTicks = -1;
	}

	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null)
			return;

		if (hoverWarmup > 0)
			hoverWarmup--;
		if (hoverTicks > 0)
			hoverTicks--;
		if (interactHeldTicks == -1)
			return;

		Player player = mc.player;
		if (!ValueSettingsInputHandler.canInteract(player) || AllBlocks.CLIPBOARD.isIn(player.getMainHandItem())) {
			cancelInteraction();
			return;
		}

		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHitResult) || !blockHitResult.getBlockPos()
			.equals(interactHeldPos)) {
			cancelInteraction();
			return;
		}

		BlockEntityBehaviour behaviour = BlockEntityBehaviour.get(mc.level, interactHeldPos, interactHeldBehaviour);
		if (!(behaviour instanceof ValueSettingsBehaviour valueSettingBehaviour)
			|| valueSettingBehaviour.bypassesInput(player.getMainHandItem())) {
			cancelInteraction();
			return;
		}
		if (valueSettingBehaviour.getSlotPositioning() instanceof ValueBoxTransform.Sided sided)
			sided.fromSide(interactHeldFace);
		if (!valueSettingBehaviour.testHit(blockHitResult.getLocation())) {
			cancelInteraction();
			return;
		}

		if (!mc.options.keyUse.isDown()) {
			ClientNetworkHelper.INSTANCE.sendToServer(new ValueSettingsPacket(interactHeldPos, 0, 0, interactHeldHand,
				blockHitResult, interactHeldFace, false, valueSettingBehaviour.netId()));
			valueSettingBehaviour.onShortInteract(player, interactHeldHand, interactHeldFace, blockHitResult);
			cancelInteraction();
			return;
		}

		if (interactHeldTicks > 3)
			player.swinging = false;
		if (interactHeldTicks++ < 5)
			return;

		ScreenOpener.open(new ValueSettingsScreen(interactHeldPos, valueSettingBehaviour.createBoard(player, blockHitResult),
			valueSettingBehaviour.getValueSettings(), valueSettingBehaviour::newSettingHovered, valueSettingBehaviour.netId()));
		interactHeldTicks = -1;
	}

	public void showHoverTip(List<MutableComponent> tip) {
		if (hoverWarmup < 6) {
			hoverWarmup += 2;
			return;
		}
		hoverWarmup++;
		hoverTicks = hoverTicks == 0 ? 11 : Math.max(hoverTicks, 6);
		lastHoverTip = tip;
	}

	@Override
	public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null)
			return;

		if (!ValueSettingsInputHandler.canInteract(mc.player))
			return;
		if (hoverTicks == 0 || lastHoverTip == null)
			return;

		int x = guiGraphics.guiWidth() / 2;
		int y = guiGraphics.guiHeight() - 75 - lastHoverTip.size() * 12;
		float alpha = hoverTicks > 5 ? (11 - hoverTicks) / 5f : Math.min(1, hoverTicks / 5f);

		Color color = new Color(0xffffff);
		Color titleColor = new Color(0xFBDC7D);
		color.setAlpha(alpha);
		titleColor.setAlpha(alpha);

		for (int i = 0; i < lastHoverTip.size(); i++) {
			MutableComponent component = lastHoverTip.get(i);
			guiGraphics.text(mc.font, component, x - mc.font.width(component) / 2, y,
				(i == 0 ? titleColor : color).getRGB());
			y += 12;
		}
	}
}
