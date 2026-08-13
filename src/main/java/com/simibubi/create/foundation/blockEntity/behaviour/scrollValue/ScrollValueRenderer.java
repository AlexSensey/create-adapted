package com.simibubi.create.foundation.blockEntity.behaviour.scrollValue;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.content.redstone.diodes.BrassDiodeBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.IconValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.TextValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.outliner.Outliner;
import net.createmod.catnip.api.client.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ScrollValueRenderer {
	private static final List<ValueBox> VISIBLE_BOXES = new ArrayList<>();

	public static void tick() {
		VISIBLE_BOXES.clear();

		Minecraft mc = Minecraft.getInstance();
		HitResult target = mc.hitResult;
		if (target == null || !(target instanceof BlockHitResult result))
			return;
		if (mc.level == null || mc.player == null)
			return;
		if (mc.player.isShiftKeyDown())
			return;

		ClientLevel world = mc.level;
		BlockPos pos = result.getBlockPos();
		Direction face = result.getDirection();
		BlockState state = world.getBlockState(pos);
		boolean highlightFound = false;

		if (!(world.getBlockEntity(pos) instanceof SmartBlockEntity sbe))
			return;
		boolean customSubmitOverlay = sbe instanceof CreativeMotorBlockEntity || sbe instanceof SpeedControllerBlockEntity
			|| sbe instanceof ValveHandleBlockEntity
			|| sbe instanceof BrassDiodeBlockEntity;

		for (BlockEntityBehaviour blockEntityBehaviour : sbe.getAllBehaviours()) {
			if (!(blockEntityBehaviour instanceof ScrollValueBehaviour behaviour))
				continue;

			if (!behaviour.isActive()) {
				Outliner.getInstance().remove(behaviour);
				continue;
			}

			ItemStack mainhandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
			boolean clipboard = behaviour.bypassesInput(mainhandItem);
			if (behaviour.needsWrench && !AllItems.WRENCH.isIn(mainhandItem) && !clipboard)
				continue;
			if (behaviour.slotPositioning instanceof ValueBoxTransform.Sided sided)
				sided.fromSide(face);
			if (!behaviour.slotPositioning.shouldRender(world, pos, state))
				continue;
			boolean highlight = (behaviour.testHit(target.getLocation())
				|| (!customSubmitOverlay && behaviour.slotPositioning instanceof ValueBoxTransform.Sided)) && !clipboard
				&& !highlightFound;

			if (customSubmitOverlay) {
				Outliner.getInstance().remove(behaviour);
				if (!highlight)
					continue;

				highlightFound = true;
				List<MutableComponent> tip = new ArrayList<>();
				tip.add(behaviour.label.copy());
				tip.add(CreateLang.translateDirect("gui.value_settings.hold_to_edit"));
				CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
				continue;
			}

			if (behaviour instanceof BulkScrollValueBehaviour bulkScrolling && AllKeys.ctrlDown()) {
				for (SmartBlockEntity smartBlockEntity : bulkScrolling.getBulk()) {
					ScrollValueBehaviour other = smartBlockEntity.getBehaviour(ScrollValueBehaviour.TYPE);
					if (other != null)
						addBox(world, smartBlockEntity.getBlockPos(), face, other, highlight);
				}
			} else
				addBox(world, pos, face, behaviour, highlight);

			if (!highlight)
				continue;

			highlightFound = true;
			List<MutableComponent> tip = new ArrayList<>();
			tip.add(behaviour.label.copy());
			tip.add(CreateLang.translateDirect("gui.value_settings.hold_to_edit"));
			CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
		}
	}

	protected static void addBox(ClientLevel world, BlockPos pos, Direction face, ScrollValueBehaviour behaviour,
								 boolean highlight) {
		AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.5f)
			.contract(0, 0, -.5f)
			.move(0, 0, -.125f);
		Component label = behaviour.label;
		ValueBox box;

		if (behaviour instanceof ScrollOptionBehaviour) {
			box = new IconValueBox(label, ((ScrollOptionBehaviour<?>) behaviour).getIconForSelected(), bb, pos);
		} else {
			box = new TextValueBox(label, bb, pos, Component.literal(behaviour.formatValue()));
		}

		box.passive(!highlight)
			.wideOutline();

		ValueBox transformedBox = box.transform(behaviour.slotPositioning);
		VISIBLE_BOXES.add(transformedBox);

		Outliner.getInstance().showOutline(behaviour, transformedBox)
			.highlightFace(face);
	}

	public static void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera) {
		for (ValueBox box : VISIBLE_BOXES)
			box.render(ms, buffer, camera, 1);
	}

}
