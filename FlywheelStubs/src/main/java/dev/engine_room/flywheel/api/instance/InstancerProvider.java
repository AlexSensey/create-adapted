package dev.engine_room.flywheel.api.instance;

public interface InstancerProvider {
    <T extends Instance> Instancer<T> instancer(InstanceType<T> type);
}
