package com.simibubi.create.content.kinetics.chainConveyor;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.render.SpecialModels;

import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ChainConveyorVisual extends SingleAxisRotatingVisual<ChainConveyorBlockEntity> implements SimpleTickableVisual {

	private final List<TransformedInstance> guards = new ArrayList<>();

	public ChainConveyorVisual(VisualizationContext context, ChainConveyorBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick, Models.partial(AllPartialModels.CHAIN_CONVEYOR_SHAFT));

		setupGuards();
	}

	@Override
	public void update(float pt) {
		super.update(pt);

		setupGuards();
	}

	@Override
	public void tick(TickableVisual.Context context) {
		blockEntity.tickBoxVisuals();
	}

	private void deleteGuards() {
		for (TransformedInstance guard : guards) {
			guard.delete();
		}
		guards.clear();
	}

	private void setupGuards() {
		deleteGuards();

		var wheelInstancer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, SpecialModels.chunkDiffuse(AllPartialModels.CHAIN_CONVEYOR_WHEEL));
		var guardInstancer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, SpecialModels.chunkDiffuse(AllPartialModels.CHAIN_CONVEYOR_GUARD));

		TransformedInstance wheel = wheelInstancer.createInstance();
		
		wheel.translate(getVisualPosition())
			.light(rotatingModel.light)
			.setChanged();
		
		guards.add(wheel);
		
		for (BlockPos blockPos : blockEntity.connections) {
			ChainConveyorBlockEntity.ConnectionStats stats = blockEntity.connectionStats.get(blockPos);
			if (stats == null) {
				continue;
			}

			Vec3 diff = stats.end()
				.subtract(stats.start());
			double yaw = Mth.RAD_TO_DEG * Mth.atan2(diff.x, diff.z);

			TransformedInstance guard = guardInstancer.createInstance();
			guard.translate(getVisualPosition())
				.center()
				.rotateYDegrees((float) yaw)
				.uncenter()
				.light(rotatingModel.light)
				.setChanged();

			guards.add(guard);
		}
	}

	@Override
	public void updateLight(float partialTick) {
		super.updateLight(partialTick);
		for (TransformedInstance guard : guards) {
			relight(guard);
		}
	}

	@Override
	protected void _delete() {
		super._delete();
		deleteGuards();
	}
}
