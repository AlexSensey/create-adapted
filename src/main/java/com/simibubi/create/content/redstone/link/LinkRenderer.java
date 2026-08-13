package com.simibubi.create.content.redstone.link;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LinkRenderer {
	private static final float BUTTON_ICON_SCALE = .06f;
	private static final float BUTTON_FRAME_SCALE = .28f;
	private static final float BUTTON_SURFACE_Y = 3.5f / 16f;
	private static final float FIRST_BUTTON_Z = 5.5f / 16f;
	private static final float SECOND_BUTTON_Z = 10.5f / 16f;
	private static final float FIRST_WALL_BUTTON_Y = 10.5f / 16f;
	private static final float SECOND_WALL_BUTTON_Y = 5.5f / 16f;
	private static final float BUTTON_ICON_SURFACE_OFFSET = 1 / 512f;

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null)
			return;
		HitResult target = mc.hitResult;
		if (target == null || !(target instanceof BlockHitResult result))
			return;

		ClientLevel world = mc.level;
		BlockPos pos = result.getBlockPos();

		LinkBehaviour behaviour = BlockEntityBehaviour.get(world, pos, LinkBehaviour.TYPE);
		if (behaviour == null)
			return;

		Component freq1 = CreateLang.translateDirect("logistics.firstFrequency");
		Component freq2 = CreateLang.translateDirect("logistics.secondFrequency");

		for (boolean first : Iterate.trueAndFalse) {
			AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.25f);
			Component label = first ? freq1 : freq2;
			boolean hit = behaviour.testHit(first, target.getLocation());
			ValueBoxTransform transform = first ? behaviour.firstSlot : behaviour.secondSlot;

			ValueBox box = new ValueBox(label, bb, pos).passive(!hit);
			boolean empty = behaviour.getNetworkKey()
				.get(first)
				.getStack()
				.isEmpty();

			if (!empty)
				box.wideOutline();

			Outliner.getInstance().showOutline(Pair.of(Boolean.valueOf(first), pos), box.transform(transform))
				.highlightFace(result.getDirection());

			if (!hit)
				continue;

			List<MutableComponent> tip = new ArrayList<>();
			tip.add(label.copy());
			tip.add(
				CreateLang.translateDirect(empty ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace"));
			CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
		}
	}

	public static void submitOnBlockEntity(SmartBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		if (be == null || be.isRemoved())
			return;

		Entity cameraEntity = Minecraft.getInstance()
			.getCameraEntity();
		float max = AllConfigs.client().filterItemRenderDistance.getF();
		if (!be.isVirtual() && cameraEntity != null && cameraEntity.position()
			.distanceToSqr(VecHelper.getCenterOf(be.getBlockPos())) > (max * max))
			return;

		LinkBehaviour behaviour = be.getBehaviour(LinkBehaviour.TYPE);
		if (behaviour == null)
			return;

		for (boolean first : Iterate.trueAndFalse) {
			ItemStack stack = first ? behaviour.frequencyFirst.getStack() : behaviour.frequencyLast.getStack();
			boolean hovered = isSlotHovered(be, behaviour, first);

			ms.pushPose();
			submitFrequencyItem(stack, be.getBlockState()
				.getValue(RedstoneLinkBlock.FACING), first, hovered, ms, collector, light);
			ms.popPose();
		}
	}

	private static boolean isSlotHovered(SmartBlockEntity be, LinkBehaviour behaviour, boolean first) {
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.hitResult instanceof BlockHitResult hit))
			return false;
		if (!hit.getBlockPos()
			.equals(be.getBlockPos()))
			return false;
		return behaviour.testHit(first, hit.getLocation());
	}

	private static void submitFrequencyItem(ItemStack stack, Direction facing, boolean first, boolean hovered, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		switch (facing) {
			case DOWN -> {
				ms.translate(.5f, 1 - BUTTON_SURFACE_Y, first ? SECOND_BUTTON_Z : FIRST_BUTTON_Z);
				ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
			}
			case EAST -> {
				ms.translate(BUTTON_SURFACE_Y, first ? FIRST_WALL_BUTTON_Y : SECOND_WALL_BUTTON_Y, .5f);
				ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
			}
			case NORTH -> {
				ms.translate(.5f, first ? FIRST_WALL_BUTTON_Y : SECOND_WALL_BUTTON_Y, 1 - BUTTON_SURFACE_Y);
				ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
			}
			case SOUTH -> ms.translate(.5f, first ? FIRST_WALL_BUTTON_Y : SECOND_WALL_BUTTON_Y, BUTTON_SURFACE_Y);
			case UP -> {
				ms.translate(.5f, BUTTON_SURFACE_Y, first ? FIRST_BUTTON_Z : SECOND_BUTTON_Z);
				ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
			}
			case WEST -> {
				ms.translate(1 - BUTTON_SURFACE_Y, first ? FIRST_WALL_BUTTON_Y : SECOND_WALL_BUTTON_Y, .5f);
				ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
			}
		}

		if (hovered)
			submitValueBoxFrame(ms, collector);
		if (stack.isEmpty())
			return;

		ValueBoxRenderer.submitItemIntoValueBox(stack, ms, collector, light, BUTTON_ICON_SCALE,
			BUTTON_ICON_SURFACE_OFFSET);
	}

	private static void submitValueBoxFrame(PoseStack ms, SubmitNodeCollector collector) {
		ms.pushPose();
		ms.translate(0, 0, BUTTON_ICON_SURFACE_OFFSET);
		ms.scale(BUTTON_FRAME_SCALE, BUTTON_FRAME_SCALE, BUTTON_FRAME_SCALE);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> renderValueBoxCorners(pose, consumer));
		ms.popPose();
	}

	private static void renderValueBoxCorners(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		renderTwoPixelDot(pose, consumer, 3, 3, color);
		renderTwoPixelDot(pose, consumer, 11, 3, color);
		renderTwoPixelDot(pose, consumer, 3, 11, color);
		renderTwoPixelDot(pose, consumer, 11, 11, color);
	}

	private static void renderTwoPixelDot(Pose pose, VertexConsumer consumer, int x, int y, int color) {
		valueBoxPixelXY(pose, consumer, x, y, color);
		valueBoxPixelXY(pose, consumer, x + 1, y, color);
		valueBoxPixelXY(pose, consumer, x, y + 1, color);
		valueBoxPixelXY(pose, consumer, x + 1, y + 1, color);
	}

	private static void valueBoxPixelXY(Pose pose, VertexConsumer consumer, int x, int y, int color) {
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, x * pixel - .5f, y * pixel - .5f, (x + 1) * pixel - .5f,
			(y + 1) * pixel - .5f, 0, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1,
		float z, int color) {
		consumer.addVertex(pose, x0, y0, z)
			.setColor(color);
		consumer.addVertex(pose, x1, y0, z)
			.setColor(color);
		consumer.addVertex(pose, x1, y1, z)
			.setColor(color);
		consumer.addVertex(pose, x0, y1, z)
			.setColor(color);
		consumer.addVertex(pose, x0, y1, z)
			.setColor(color);
		consumer.addVertex(pose, x1, y1, z)
			.setColor(color);
		consumer.addVertex(pose, x1, y0, z)
			.setColor(color);
		consumer.addVertex(pose, x0, y0, z)
			.setColor(color);
	}

}
