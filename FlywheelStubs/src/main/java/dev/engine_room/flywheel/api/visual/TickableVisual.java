package dev.engine_room.flywheel.api.visual;

import dev.engine_room.flywheel.api.task.Plan;

public interface TickableVisual extends Visual {
    class Context {
    }

    default void tick(Context context) {
    }

    default Plan<Context> planTick() {
        return this::tick;
    }
}
