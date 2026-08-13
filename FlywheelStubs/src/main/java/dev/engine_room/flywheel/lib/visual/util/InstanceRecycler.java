package dev.engine_room.flywheel.lib.visual.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import dev.engine_room.flywheel.api.instance.Instance;

public class InstanceRecycler<T extends Instance> {
    private final Supplier<T> factory;
    private final List<T> instances = new ArrayList<>();
    private int count;

    public InstanceRecycler() {
        this(() -> null);
    }

    public InstanceRecycler(Supplier<T> factory) {
        this.factory = factory;
    }

    public T get() {
        if (count < instances.size()) {
            return instances.get(count++);
        }
        T instance = factory.get();
        instances.add(instance);
        count++;
        return instance;
    }

    public void resetCount() {
        count = 0;
    }

    public void discardExtra() {
        while (instances.size() > count) {
            T instance = instances.remove(instances.size() - 1);
            if (instance != null) {
                instance.delete();
            }
        }
    }

    public void delete() {
        for (T instance : instances) {
            if (instance != null) {
                instance.delete();
            }
        }
        instances.clear();
        count = 0;
    }
}
