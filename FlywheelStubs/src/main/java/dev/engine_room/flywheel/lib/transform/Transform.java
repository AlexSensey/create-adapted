package dev.engine_room.flywheel.lib.transform;

public interface Transform<T> {
    @SuppressWarnings("unchecked")
    default T identity() { return (T) this; }
}
