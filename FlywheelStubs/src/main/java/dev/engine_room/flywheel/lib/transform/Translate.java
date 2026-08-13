package dev.engine_room.flywheel.lib.transform;

public interface Translate<T> {
    @SuppressWarnings("unchecked")
    default T translate(double x, double y, double z) { return (T) this; }
}
