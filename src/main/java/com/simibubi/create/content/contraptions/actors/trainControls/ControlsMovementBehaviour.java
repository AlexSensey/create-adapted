package com.simibubi.create.content.contraptions.actors.trainControls;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.minecraft.world.item.ItemStack;


public class ControlsMovementBehaviour implements MovementBehaviour {

	// TODO: rendering the levers should be specific to Carriage Contraptions -
	static class LeverAngles {
		LerpedFloat steering = LerpedFloat.linear();
		LerpedFloat speed = LerpedFloat.linear();
		LerpedFloat equipAnimation = LerpedFloat.linear();
	}

	@Override
	public ItemStack canBeDisabledVia(MovementContext context) {
		return null;
	}

	@Override
	public void stopMoving(MovementContext context) {
		context.contraption.entity.stopControlling(context.localPos);
		MovementBehaviour.super.stopMoving(context);
	}

	@Override
	public void tick(MovementContext context) {
		MovementBehaviour.super.tick(context);
		if (!context.world.isClientSide())
			return;
		if (!(context.temporaryData instanceof LeverAngles))
			context.temporaryData = new LeverAngles();
		LeverAngles angles = (LeverAngles) context.temporaryData;
		angles.steering.tickChaser();
		angles.speed.tickChaser();
		angles.equipAnimation.tickChaser();
	}

}
