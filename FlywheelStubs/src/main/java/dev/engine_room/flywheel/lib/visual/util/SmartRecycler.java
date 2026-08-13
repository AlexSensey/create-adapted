package dev.engine_room.flywheel.lib.visual.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import dev.engine_room.flywheel.api.instance.Instance;

public class SmartRecycler<K, T extends Instance> {
    private final Function<K, T> factory;
    private final Map<K, T> instances = new LinkedHashMap<>();

    public SmartRecycler(Function<K, T> factory) {
        this.factory = factory;
    }

    public T get(K key) {
        return instances.computeIfAbsent(key, factory);
    }

    public void resetCount() {
    }

    public void discardExtra() {
    }

    public void delete() {
        for (T instance : instances.values()) {
            if (instance != null) {
                instance.delete();
            }
        }
        instances.clear();
    }
}
