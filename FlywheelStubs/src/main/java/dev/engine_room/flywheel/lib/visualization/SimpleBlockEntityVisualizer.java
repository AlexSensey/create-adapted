package dev.engine_room.flywheel.lib.visualization;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;

public class SimpleBlockEntityVisualizer<T> implements BlockEntityVisualizer<T> {
    public interface Factory<T> {
        BlockEntityVisual<T> create(VisualizationContext context, T blockEntity, float partialTick);
    }

    public SimpleBlockEntityVisualizer(Factory<T> factory) {
    }
}
