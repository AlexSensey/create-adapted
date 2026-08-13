package dev.engine_room.flywheel.lib.task;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.engine_room.flywheel.api.task.Plan;

public class PlanMap<K, C> implements Plan<C> {
    private final Map<K, Plan<? super C>> plans = new LinkedHashMap<>();

    public void add(K key, Plan<? super C> plan) {
        plans.put(key, plan);
    }

    public void remove(K key) {
        plans.remove(key);
    }

    public void clear() {
        plans.clear();
    }

    @Override
    public void execute(C context) {
        for (Plan<? super C> plan : plans.values()) {
            plan.execute(context);
        }
    }
}
