package com.simibubi.create.content.logistics.packagerLink;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionHandler;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.render.SelectionBoxRenderer;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LogisticallyLinkedClientHandler {

	private static UUID previouslyHeldFrequency;
	private static final List<AABB> linkPreviewBounds = new ArrayList<>();
	private static final Map<FactoryPanelBehaviour, PanelPreview> panelPreviewBounds = new IdentityHashMap<>();

	public static void tick() {
		previouslyHeldFrequency = null;
		linkPreviewBounds.clear();

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			panelPreviewBounds.clear();
			return;
		}
		panelPreviewBounds.entrySet()
			.removeIf(entry -> {
				FactoryPanelBehaviour panel = entry.getKey();
				return panel.blockEntity.isRemoved() || !panel.isActive()
					|| mc.level.getBlockEntity(panel.getPos()) != panel.blockEntity
					|| panel.panelBE().panels.get(panel.getPanelPosition().slot()) != panel;
			});
		ItemStack mainHandItem = player.getMainHandItem();
		if (!(mainHandItem.getItem() instanceof LogisticallyLinkedBlockItem)
			|| !LogisticallyLinkedBlockItem.isTuned(mainHandItem)) {
			panelPreviewBounds.clear();
			return;
		}

		UUID uuid = LogisticallyLinkedBlockItem.networkFromStack(mainHandItem);
		if (uuid == null) {
			panelPreviewBounds.clear();
			return;
		}
		previouslyHeldFrequency = uuid;

		for (LogisticallyLinkedBehaviour behaviour : LogisticallyLinkedBehaviour.getAllPresent(uuid, false, true)) {
			SmartBlockEntity be = behaviour.blockEntity;
			VoxelShape shape = be.getBlockState()
				.getShape(player.level(), be.getBlockPos());
			if (shape.isEmpty() || !player.blockPosition().closerThan(be.getBlockPos(), 64))
				continue;

			linkPreviewBounds.add(shape.bounds()
				.move(be.getBlockPos())
				.inflate(1 / 64f));
		}
	}

	public static void tickPanel(FactoryPanelBehaviour fpb) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		UUID heldFrequency = getHeldFrequency(player);
		if (heldFrequency == null || !heldFrequency.equals(fpb.network)) {
			panelPreviewBounds.remove(fpb);
			return;
		}
		if (!player.blockPosition().closerThan(fpb.getPos(), 64)) {
			panelPreviewBounds.remove(fpb);
			return;
		}

		var state = fpb.blockEntity.getBlockState();
		panelPreviewBounds.put(fpb, new PanelPreview(
			FactoryPanelConnectionHandler.getBB(state, fpb.getPanelPosition()),
			FactoryPanelBlock.connectedDirection(state)));
	}

	private static UUID getHeldFrequency(LocalPlayer player) {
		if (player == null)
			return null;
		ItemStack heldItem = player.getMainHandItem();
		if (!(heldItem.getItem() instanceof LogisticallyLinkedBlockItem)
			|| !LogisticallyLinkedBlockItem.isTuned(heldItem))
			return null;
		return LogisticallyLinkedBlockItem.networkFromStack(heldItem);
	}

	public static void submitPreviewBounds(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		int color = AnimationTickHolder.getTicks() % 16 < 8 ? 0x708DAD : 0x90ADCD;
		for (AABB bounds : linkPreviewBounds)
			SelectionBoxRenderer.submit(ms, collector, camera, bounds, color);
		for (PanelPreview preview : panelPreviewBounds.values())
			SelectionBoxRenderer.submitExtrudedFrame(ms, collector, camera, preview.bounds(), preview.outward(),
				0xFF000000 | color, 1 / 32f);
	}

	private record PanelPreview(AABB bounds, Direction outward) {}
}
