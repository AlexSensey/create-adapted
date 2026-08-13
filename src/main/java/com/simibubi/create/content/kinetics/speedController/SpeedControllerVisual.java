package com.simibubi.create.content.kinetics.speedController;

import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;

public class SpeedControllerVisual extends SingleAxisRotatingVisual<SpeedControllerBlockEntity> {
	private final TransformedInstance bracket;

	public SpeedControllerVisual(VisualizationContext context, SpeedControllerBlockEntity blockEntity,
		float partialTick) {
		super(context, blockEntity, partialTick, Models.partial(AllPartialModels.SHAFT));

		boolean alongX = blockState.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) == Direction.Axis.X;
		bracket = instancerProvider()
			.instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.SPEED_CONTROLLER_BRACKET))
			.createInstance();
		bracket.translate(getVisualPosition())
			.translate(0, 1, 0)
			.center()
			.rotate(alongX ? (float) Math.PI : (float) (Math.PI / 2), Direction.UP)
			.uncenter()
			.setChanged();
		updateBracketVisibility();
	}

	@Override
	public void update(float partialTick) {
		super.update(partialTick);
		updateBracketVisibility();
	}

	@Override
	public void tick(Context context) {
		super.tick(context);
		updateBracketVisibility();
	}

	private void updateBracketVisibility() {
		bracket.setVisible(blockEntity.hasBracket);
	}

	@Override
	public void updateLight(float partialTick) {
		super.updateLight(partialTick);
		relight(bracket);
	}

	@Override
	protected void _delete() {
		super._delete();
		bracket.delete();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		super.collectCrumblingInstances(consumer);
		consumer.accept(bracket);
	}
}
