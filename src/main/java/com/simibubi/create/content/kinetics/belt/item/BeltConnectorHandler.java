package com.simibubi.create.content.kinetics.belt.item;

import java.util.LinkedList;
import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public class BeltConnectorHandler {
	private static int particleCooldown;
	private static final RandomSource RANDOM = RandomSource.create();

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		Level level = mc.level;
		if (player == null || level == null)
			return;

		ItemStack stack = getHeldBelt(player);
		if (stack.isEmpty() || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != Type.BLOCK)
			return;

		if (particleCooldown-- > 0)
			return;
		particleCooldown = 3;

		BlockPos first = stack.get(AllDataComponents.BELT_FIRST_SHAFT);
		if (first == null)
			return;

		if (!level.getBlockState(first)
			.hasProperty(BlockStateProperties.AXIS))
			return;

		Axis axis = level.getBlockState(first)
			.getValue(BlockStateProperties.AXIS);
		BlockPos target = hit.getBlockPos();
		if (level.getBlockState(target)
			.canBeReplaced())
			return;
		if (!ShaftBlock.isShaft(level.getBlockState(target)))
			target = target.relative(hit.getDirection());
		if (!target.closerThan(first, AllConfigs.server().kinetics.maxBeltLength.get()))
			return;

		boolean validTarget = BeltConnectorItem.validateAxis(level, target);
		boolean canConnect = validTarget && BeltConnectorItem.canConnect(level, first, target);

		spawnBeltPathPreview(level, first, target, axis, canConnect);
	}

	private static ItemStack getHeldBelt(LocalPlayer player) {
		ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (AllItems.BELT_CONNECTOR.isIn(main))
			return main;
		ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
		return AllItems.BELT_CONNECTOR.isIn(off) ? off : ItemStack.EMPTY;
	}

	private static void spawnBeltPathPreview(Level level, BlockPos first, BlockPos target, Axis axis,
		boolean canConnect) {
		List<Vec3> path = getPreviewPath(first, target, axis);
		if (path.isEmpty())
			return;
		DustParticleOptions particle = previewParticle(canConnect);
		int spacing = Math.max(1, path.size() / 14);

		for (int i = 0; i < path.size(); i += spacing) {
			Vec3 center = path.get(i)
				.add(.5, .5, .5);
			level.addParticle(particle, center.x + jitter(level), center.y + .18 + jitter(level) * .5,
				center.z + jitter(level), 0, .01, 0);
		}
	}

	private static List<Vec3> getPreviewPath(BlockPos first, BlockPos target, Axis axis) {
		Vec3 start = Vec3.atLowerCornerOf(first);
		Vec3 end = Vec3.atLowerCornerOf(target);
		Vec3 actualDiff = end.subtract(start);
		end = end.subtract(axis.choose(actualDiff.x, 0, 0), axis.choose(0, actualDiff.y, 0),
			axis.choose(0, 0, actualDiff.z));
		Vec3 diff = end.subtract(start);

		double x = Math.abs(diff.x);
		double y = Math.abs(diff.y);
		double z = Math.abs(diff.z);
		float length = (float) Math.max(x, Math.max(y, z));
		if (length == 0)
			return List.of(start);

		Vec3 step = diff.normalize();
		int sames = ((x == y) ? 1 : 0) + ((y == z) ? 1 : 0) + ((z == x) ? 1 : 0);
		if (sames == 0)
			step = closestValidStep(axis, step);

		if (axis == Axis.Y && step.x != 0 && step.z != 0)
			return List.of();

		step = new Vec3(Math.signum(step.x), Math.signum(step.y), Math.signum(step.z));
		List<Vec3> path = new LinkedList<>();
		for (float f = 0; f < length; f += .0625f)
			path.add(start.add(step.scale(f)));
		return path;
	}

	private static Vec3 closestValidStep(Axis axis, Vec3 step) {
		List<Vec3> validDiffs = new LinkedList<>();
		for (int i = -1; i <= 1; i++)
			for (int j = -1; j <= 1; j++)
				for (int k = -1; k <= 1; k++) {
					if (axis.choose(i, j, k) != 0)
						continue;
					if (axis == Axis.Y && i != 0 && k != 0)
						continue;
					if (i == 0 && j == 0 && k == 0)
						continue;
					validDiffs.add(new Vec3(i, j, k));
				}

		Vec3 closestStep = Vec3.ZERO;
		double closest = Double.MAX_VALUE;
		for (Vec3 validDiff : validDiffs) {
			double distanceTo = step.distanceTo(validDiff);
			if (distanceTo >= closest)
				continue;
			closest = distanceTo;
			closestStep = validDiff;
		}
		return closestStep;
	}

	private static double jitter(Level level) {
		return (RANDOM.nextDouble() - .5) * .45;
	}

	private static DustParticleOptions previewParticle(boolean canConnect) {
		return new DustParticleOptions(canConnect ? 0x6f8f68 : 0x9b5b54, .85f);
	}
}
