package dev.engine_room.flywheel.lib.task;

import dev.engine_room.flywheel.api.task.Plan;

public final class NestedPlan {
    private NestedPlan() {
    }

    @SafeVarargs
    public static <C> Plan<C> of(Plan<? super C>... plans) {
        return context -> {
            for (Plan<? super C> plan : plans) {
                if (plan != null) {
                    plan.execute(context);
                }
            }
        };
    }
}
