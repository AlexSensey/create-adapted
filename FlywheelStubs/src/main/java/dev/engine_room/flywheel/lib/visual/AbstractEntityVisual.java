package dev.engine_room.flywheel.lib.visual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;

public abstract class AbstractEntityVisual<T> extends AbstractVisual {
    protected final VisualizationContext context;
    protected final T entity;

    protected AbstractEntityVisual(VisualizationContext context, T entity, float partialTick) {
        this.context = context;
        this.entity = entity;
    }
}
