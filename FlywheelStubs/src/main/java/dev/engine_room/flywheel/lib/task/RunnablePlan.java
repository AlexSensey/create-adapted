package dev.engine_room.flywheel.lib.task;

import dev.engine_room.flywheel.api.task.Plan;

public final class RunnablePlan {
    private RunnablePlan() {
    }

    public static <C> Plan<C> of(java.util.function.Consumer<C> consumer) {
        return consumer::accept;
    }

    public static <C> Plan<C> of(Runnable runnable) {
        return context -> runnable.run();
    }
}
