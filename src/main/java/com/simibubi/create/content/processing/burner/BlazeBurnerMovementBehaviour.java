package com.simibubi.create.content.processing.burner;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;


public class BlazeBurnerMovementBehaviour implements MovementBehaviour {

	@Override
	public ItemStack canBeDisabledVia(MovementContext context) {
		return null;
	}

	@Override
	public void tick(MovementContext context) {
		if (context.world.isClientSide())
			CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> BlazeBurnerMovementClient.tick(this, context));
	}

	public void invalidate(MovementContext context) {
		context.data.remove("Conductor");
	}

	boolean shouldRender(MovementContext context) {
		return context.state.getOptionalValue(BlazeBurnerBlock.HEAT_LEVEL)
			.orElse(HeatLevel.NONE) != HeatLevel.NONE;
	}

	LerpedFloat getHeadAngle(MovementContext context) {
		if (!(context.temporaryData instanceof LerpedFloat))
			context.temporaryData = LerpedFloat.angular()
				.startWithValue(BlazeBurnerMovementClient.getTargetAngle(this, context));
		return (LerpedFloat) context.temporaryData;
	}

	boolean shouldRenderHat(MovementContext context) {
		CompoundTag data = context.data;
		if (!data.contains("Conductor"))
			data.putBoolean("Conductor", determineIfConducting(context));
		return data.getBooleanOr("Conductor", false) && (context.contraption.entity instanceof CarriageContraptionEntity cce)
			&& cce.hasSchedule();
	}

	private boolean determineIfConducting(MovementContext context) {
		Contraption contraption = context.contraption;
		if (!(contraption instanceof CarriageContraption carriageContraption))
			return false;
		Direction assemblyDirection = carriageContraption.getAssemblyDirection();
		for (Direction direction : Iterate.directionsInAxis(assemblyDirection.getAxis()))
			if (carriageContraption.inControl(context.localPos, direction))
				return true;
		return false;
	}

	@Override
	public boolean disableBlockEntityRendering() {
		return true;
	}

}
