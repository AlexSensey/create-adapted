package com.simibubi.create.content.contraptions.bearing;

import org.jetbrains.annotations.Nullable;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;


public class StabilizedBearingMovementBehaviour implements MovementBehaviour {

	@Override
	public ItemStack canBeDisabledVia(MovementContext context) {
		return null;
	}

	@Override
	public boolean disableBlockEntityRendering() {
		return true;
	}

	@Nullable
	@Override
	public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld,
									MovementContext movementContext) {
		return new StabilizedBearingVisual(visualizationContext, simulationWorld, movementContext);
	}

	static float getCounterRotationAngle(MovementContext context, Direction facing, float renderPartialTicks) {
		if (!context.contraption.canBeStabilized(facing, context.localPos))
			return 0;

		float offset = 0;
		Direction.Axis axis = facing.getAxis();
		AbstractContraptionEntity entity = context.contraption.entity;

		if (entity instanceof ControlledContraptionEntity controlledCE) {
			if (context.contraption.canBeStabilized(facing, context.localPos))
				offset = -controlledCE.getAngle(renderPartialTicks);

		} else if (entity instanceof OrientedContraptionEntity orientedCE) {
			if (axis.isVertical())
				offset = -orientedCE.getViewYRot(renderPartialTicks);
			else {
				if (orientedCE.isInitialOrientationPresent() && orientedCE.getInitialOrientation()
					.getAxis() == axis)
					offset = -orientedCE.getViewXRot(renderPartialTicks);
			}
		}
		return offset;
	}

}
