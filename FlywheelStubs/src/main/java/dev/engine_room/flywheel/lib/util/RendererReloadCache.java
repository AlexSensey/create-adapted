package dev.engine_room.flywheel.lib.util;

import java.util.function.Function;

public class RendererReloadCache<K, V> {
    private final Function<K, V> factory;

    public RendererReloadCache(Function<K, V> factory) {
        this.factory = factory;
    }

    public V get(K key) {
        return factory.apply(key);
    }

    public void clear() {
    }
}
