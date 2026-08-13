package com.simibubi.create.content.contraptions.minecart;

import com.simibubi.create.AllAttachmentTypes;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;

import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public class MinecartSim2020 {

	public static boolean canAddMotion(AbstractMinecart cart) {
		MinecartController controller = cart.getData(AllAttachmentTypes.MINECART_CONTROLLER);
		return !controller.isPresent() || !controller.isStalled();
	}

	public static Vec3 predictNextPositionOf(AbstractMinecart cart) {
		return cart.position()
			.add(VecHelper.clamp(cart.getDeltaMovement(), 1f));
	}

	public static Vec3 getRailVec(RailShape shape) {
		return switch (shape) {
			case ASCENDING_NORTH, ASCENDING_SOUTH, NORTH_SOUTH -> new Vec3(0, 0, 1);
			case ASCENDING_EAST, ASCENDING_WEST, EAST_WEST -> new Vec3(1, 0, 0);
			case NORTH_EAST, SOUTH_WEST -> new Vec3(1, 0, 1).normalize();
			case NORTH_WEST, SOUTH_EAST -> new Vec3(1, 0, -1).normalize();
		};
	}
}
