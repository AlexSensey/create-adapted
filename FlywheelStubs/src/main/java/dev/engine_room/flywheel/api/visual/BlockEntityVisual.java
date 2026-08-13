package dev.engine_room.flywheel.api.visual;

import java.util.function.Consumer;

import dev.engine_room.flywheel.api.instance.Instance;

public interface BlockEntityVisual<T> extends Visual {
    default void update(float partialTick) {
    }

    default void collectCrumblingInstances(Consumer<Instance> consumer) {
    }
}
