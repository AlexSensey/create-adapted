package dev.engine_room.flywheel.api.instance;

public interface Instancer<T extends Instance> {
    T createInstance();
}
