package com.simibubi.create.content.equipment.symmetryWand;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.symmetryWand.mirror.EmptyMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.CrossPlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.TriplePlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

@EventBusSubscriber
public class SymmetryHandler {
	private static int tickCounter;

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onBlockPlaced(EntityPlaceEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player))
			return;
		Inventory inventory = player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++)
			if (AllItems.WAND_OF_SYMMETRY.isIn(inventory.getItem(i)))
				SymmetryWandItem.apply(player.level(), inventory.getItem(i), player, event.getPos(), event.getPlacedBlock());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onBlockDestroyed(BreakBlockEvent event) {
		if (event.getLevel().isClientSide())
			return;
		Player player = event.getPlayer();
		Inventory inventory = player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++)
			if (AllItems.WAND_OF_SYMMETRY.isIn(inventory.getItem(i)))
				SymmetryWandItem.remove(player.level(), inventory.getItem(i), player, event.getPos());
	}

	@EventBusSubscriber(value = Dist.CLIENT)
	public static class Client {
		public static void submit(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
			Minecraft minecraft = Minecraft.getInstance();
			LocalPlayer player = minecraft.player;
			if (minecraft.level == null || player == null)
				return;

			for (int i = 0; i < Inventory.getSelectionSize(); i++) {
				ItemStack stack = player.getInventory().getItem(i);
				if (!AllItems.WAND_OF_SYMMETRY.isIn(stack) || !SymmetryWandItem.isEnabled(stack))
					continue;
				SymmetryMirror mirror = SymmetryWandItem.getMirror(stack);
				if (mirror instanceof EmptyMirror)
					continue;

				StandaloneModelKey<BlockStateModelPart> modelKey = mirror instanceof TriplePlaneMirror
					? CreateStandaloneModels.SYMMETRY_TRIPLEPLANE
					: mirror instanceof CrossPlaneMirror
						? CreateStandaloneModels.SYMMETRY_CROSSPLANE
						: CreateStandaloneModels.SYMMETRY_PLANE;
				BlockStateModelPart model = minecraft.getModelManager().getStandaloneModel(modelKey);
				if (model == null)
					continue;

				BlockPos pos = BlockPos.containing(mirror.getPosition());
				float yShift = Mth.sin(AnimationTickHolder.getRenderTime() / 16f) / 5f;
				poseStack.pushPose();
				poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
				poseStack.translate(0, yShift + .2f, 0);
				mirror.applyModelTransform(poseStack);
				collector.submitBlockModel(poseStack, RenderTypes.solidMovingBlock(), List.of(model),
					BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.FULL_BRIGHT, 0, 0);
				poseStack.popPose();
			}
		}

		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			Minecraft minecraft = Minecraft.getInstance();
			LocalPlayer player = minecraft.player;
			if (minecraft.level == null || player == null || minecraft.isPaused() || ++tickCounter % 10 != 0)
				return;
			for (int i = 0; i < Inventory.getSelectionSize(); i++) {
				ItemStack stack = player.getInventory().getItem(i);
				if (!AllItems.WAND_OF_SYMMETRY.isIn(stack) || !SymmetryWandItem.isEnabled(stack))
					continue;
				SymmetryMirror mirror = SymmetryWandItem.getMirror(stack);
				if (mirror instanceof EmptyMirror)
					continue;
				RandomSource random = minecraft.level.getRandom();
				Vec3 position = mirror.getPosition().add(.5 + (random.nextDouble() - .5) * .3, .25,
					.5 + (random.nextDouble() - .5) * .3);
				minecraft.level.addParticle(ParticleTypes.END_ROD, position.x, position.y, position.z, 0,
					random.nextDouble() / 8, 0);
			}
		}

		public static void drawEffect(BlockPos from, BlockPos to) {
			ClientLevel level = Minecraft.getInstance().level;
			if (level == null || from.equals(to))
				return;
			RandomSource random = level.getRandom();
			Vec3 start = Vec3.atCenterOf(from);
			Vec3 end = Vec3.atCenterOf(to);
			Vec3 difference = end.subtract(start);
			Vec3 step = difference.normalize().scale(.8);
			int steps = (int) (difference.length() / .8);
			for (int i = 3; i < steps - 1; i++) {
				Vec3 position = start.add(step.scale(i));
				level.addParticle(new DustParticleOptions(0xffffff, 1), position.x, position.y, position.z, 0,
					random.nextDouble() * -.025, 0);
			}
			level.addParticle(ParticleTypes.END_ROD, start.x, start.y, start.z, 0, random.nextDouble() / 32, 0);
			level.addParticle(ParticleTypes.END_ROD, end.x, end.y, end.z, 0, random.nextDouble() / 32, 0);
		}
	}
}
