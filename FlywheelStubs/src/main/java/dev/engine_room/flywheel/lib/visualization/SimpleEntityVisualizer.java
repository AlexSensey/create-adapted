package dev.engine_room.flywheel.lib.visualization;

public class SimpleEntityVisualizer<T> {
    public interface Factory<T> {
        Object create(Object context, T entity, float partialTick);
    }

    public SimpleEntityVisualizer(Factory<T> factory) {
    }
}
