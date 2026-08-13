package dev.engine_room.flywheel.api.instance;

public interface InstanceHandle<T extends Instance> {
    T get();

    default void delete() {
    }
}
