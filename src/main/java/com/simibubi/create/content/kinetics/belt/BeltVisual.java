package com.simibubi.create.content.kinetics.belt;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.core.Direction;

public class BeltVisual extends KineticBlockEntityVisual<BeltBlockEntity> {
	public static final float MAGIC_SCROLL_MULTIPLIER = 1f / (31.5f * 16f);
	public static final float SCROLL_FACTOR_DIAGONAL = 3f / 8f;
	public static final float SCROLL_FACTOR_OTHERWISE = 0.5f;
	public static final float SCROLL_OFFSET_BOTTOM = 0.5f;
	public static final float SCROLL_OFFSET_OTHERWISE = 0f;

	@Nullable
    protected final RotatingInstance pulley;

    public BeltVisual(VisualizationContext context, BeltBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

		if (blockEntity.hasPulley()) {
			pulley = instancerProvider()
				.instancer(AllInstanceTypes.ROTATING, getPulleyModel())
				.createInstance();

			pulley.setup(BeltVisual.this.blockEntity, getPulleySpeed())
				.setPosition(getVisualPosition())
				.setChanged();
        } else {
			pulley = null;
		}
    }

    @Override
    public void update(float pt) {
        if (pulley != null) {
			pulley.setup(blockEntity, getPulleySpeed())
				.setChanged();
		}
    }

    @Override
    public void updateLight(float partialTick) {
        if (pulley != null) relight(pulley);
    }

    @Override
    protected void _delete() {
        if (pulley != null) {
			pulley.delete();
		}
    }

    private Model getPulleyModel() {
        Direction dir = getOrientation();

        return Models.partial(AllPartialModels.BELT_PULLEY, dir.getAxis(), (axis11, modelTransform1) -> {
            var msr = TransformStack.of(modelTransform1);
            msr.center();
            if (axis11 == Direction.Axis.X) msr.rotateYDegrees(90);
            if (axis11 == Direction.Axis.Y) msr.rotateXDegrees(90);
            msr.rotateXDegrees(90);
            msr.uncenter();
        });
    }

    private Direction getOrientation() {
        Direction dir = blockState.getValue(BeltBlock.HORIZONTAL_FACING)
                                  .getClockWise();

		if (blockState.getValue(BeltBlock.SLOPE) == BeltSlope.SIDEWAYS)
            dir = Direction.UP;

        return dir;
    }

	private float getPulleySpeed() {
		return blockEntity.getSpeed();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		if (pulley != null) {
			consumer.accept(pulley);
		}
	}
}
