package dev.engine_room.flywheel.lib.task;

import dev.engine_room.flywheel.api.task.Plan;

public final class ForEachPlan {
    private ForEachPlan() {
    }

    public static <C, T> Plan<C> of(java.util.function.Supplier<? extends Iterable<T>> iterable,
        java.util.function.BiConsumer<T, C> consumer) {
        return context -> {
            for (T value : iterable.get()) {
                consumer.accept(value, context);
            }
        };
    }

    public static <C, T> Plan<C> of(Iterable<T> iterable, java.util.function.BiConsumer<T, C> consumer) {
        return of(() -> iterable, consumer);
    }
}
