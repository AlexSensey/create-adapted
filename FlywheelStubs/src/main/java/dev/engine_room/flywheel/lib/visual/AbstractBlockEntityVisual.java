package dev.engine_room.flywheel.lib.visual;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;

public abstract class AbstractBlockEntityVisual<T> extends AbstractVisual implements BlockEntityVisual<T> {
    protected final VisualizationContext context;
    protected final T blockEntity;

    protected AbstractBlockEntityVisual(VisualizationContext context, T blockEntity, float partialTick) {
        this.context = context;
        this.blockEntity = blockEntity;
    }
}
