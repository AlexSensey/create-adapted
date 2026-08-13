package com.simibubi.create.content.contraptions.minecart;

import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;

import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public class CouplingPhysics {

	public static void tick(Level world) {
		CouplingHandler.forEachLoadedCoupling(world, coupling -> tickCoupling(world, coupling));
	}

	private static void tickCoupling(Level world, Couple<MinecartController> coupling) {
		Couple<AbstractMinecart> carts = coupling.map(MinecartController::cart);
		if (carts.getFirst() == null || carts.getSecond() == null)
			return;

		TickRateManager tickRateManager = world.tickRateManager();
		if (tickRateManager.isEntityFrozen(carts.getFirst()) && tickRateManager.isEntityFrozen(carts.getSecond()))
			return;

		float couplingLength = coupling.getFirst()
			.getCouplingLength(true);
		softCollisionStep(world, carts, couplingLength);
		if (!world.isClientSide())
			hardCollisionStep(world, carts, couplingLength);
	}

	private static void hardCollisionStep(Level world, Couple<AbstractMinecart> carts, double couplingLength) {
		if (!MinecartSim2020.canAddMotion(carts.get(false)) && MinecartSim2020.canAddMotion(carts.get(true)))
			carts = carts.swap();

		boolean firstLoop = true;
		for (boolean current : new boolean[] { true, false, true }) {
			AbstractMinecart cart = carts.get(current);
			AbstractMinecart otherCart = carts.get(!current);
			float stress = (float) (couplingLength - cart.position()
				.distanceTo(otherCart.position()));
			if (Math.abs(stress) < 1 / 8f)
				continue;

			Vec3 pos = cart.position();
			Vec3 link = otherCart.position()
				.subtract(pos);
			float correctionMagnitude = firstLoop ? -stress / 2f : -stress;
			if (!MinecartSim2020.canAddMotion(cart))
				correctionMagnitude /= 2;

			RailShape shape = getRailShape(world, cart, cart.getCurrentBlockPosOrRailBelow());
			Vec3 correction = shape == null
				? link.normalize().scale(correctionMagnitude)
				: followLinkOnRail(link, pos, correctionMagnitude, MinecartSim2020.getRailVec(shape)).subtract(pos);
			correction = VecHelper.clamp(correction, Math.min(1.75f, getMaxSpeed(world, cart)));

			cart.move(MoverType.SELF, correction);
			cart.setDeltaMovement(cart.getDeltaMovement()
				.scale(.95f));
			firstLoop = false;
		}
	}

	private static void softCollisionStep(Level world, Couple<AbstractMinecart> carts, double couplingLength) {
		Couple<Float> maxSpeed = carts.map(cart -> getMaxSpeed(world, cart));
		Couple<Boolean> canAddMotion = carts.map(MinecartSim2020::canAddMotion);
		Couple<Vec3> motions = carts.map(Entity::getDeltaMovement);
		motions.replaceWithParams(VecHelper::clamp, Couple.create(1f, 1f));
		Couple<Vec3> nextPositions = carts.map(MinecartSim2020::predictNextPositionOf);

		Couple<RailShape> shapes = carts.mapWithContext((cart, current) ->
			getRailShape(world, cart, BlockPos.containing(nextPositions.get(current))));
		float futureStress = (float) (couplingLength - nextPositions.getFirst()
			.distanceTo(nextPositions.getSecond()));
		if (Mth.equal(futureStress, 0D))
			return;

		for (boolean current : Iterate.trueAndFalse) {
			if (!canAddMotion.get(current))
				continue;
			Vec3 pos = nextPositions.get(current);
			Vec3 link = nextPositions.get(!current)
				.subtract(pos);
			float correctionMagnitude = -futureStress / 2f;
			if (canAddMotion.get(current) != canAddMotion.get(!current))
				correctionMagnitude = !canAddMotion.get(current) ? 0 : correctionMagnitude * 2;

			RailShape shape = shapes.get(current);
			Vec3 correction = shape == null
				? link.normalize().scale(correctionMagnitude)
				: followLinkOnRail(link, pos, correctionMagnitude, MinecartSim2020.getRailVec(shape)).subtract(pos);
			correction = VecHelper.clamp(correction, maxSpeed.get(current));
			motions.set(current, motions.get(current)
				.add(correction));
		}

		motions.replaceWithParams(VecHelper::clamp, maxSpeed);
		carts.forEachWithParams(Entity::setDeltaMovement, motions);
	}

	private static RailShape getRailShape(Level world, AbstractMinecart cart, BlockPos candidate) {
		for (BlockPos pos : new BlockPos[] { candidate, candidate.below(), candidate.above() }) {
			BlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof BaseRailBlock rail)
				return rail.getRailDirection(state, world, pos, cart);
		}
		return null;
	}

	private static float getMaxSpeed(Level world, AbstractMinecart cart) {
		if (world instanceof ServerLevel serverLevel)
			return (float) cart.getBehavior()
				.getMaxSpeed(serverLevel);
		return .4f;
	}

	private static Vec3 followLinkOnRail(Vec3 link, Vec3 cart, float diffToReduce, Vec3 railAxis) {
		double dotProduct = railAxis.dot(link);
		if (Double.isNaN(dotProduct) || dotProduct == 0 || diffToReduce == 0)
			return cart;

		Vec3 axis = railAxis.scale(-Math.signum(dotProduct));
		Vec3 center = cart.add(link);
		double radius = link.length() - diffToReduce;
		Vec3 intersectSphere = VecHelper.intersectSphere(cart, axis, center, radius);
		if (intersectSphere == null)
			return cart.add(VecHelper.project(link, axis));
		return intersectSphere;
	}
}
