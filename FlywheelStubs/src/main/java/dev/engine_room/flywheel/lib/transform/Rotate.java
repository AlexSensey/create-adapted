package dev.engine_room.flywheel.lib.transform;

public interface Rotate<T> {
    @SuppressWarnings("unchecked")
    default T rotateX(float radians) { return (T) this; }

    @SuppressWarnings("unchecked")
    default T rotateY(float radians) { return (T) this; }

    @SuppressWarnings("unchecked")
    default T rotateZ(float radians) { return (T) this; }
}
