package dev.engine_room.flywheel.api.visual;

import dev.engine_room.flywheel.api.task.Plan;

public interface DynamicVisual extends Visual {
    class Context {
        public float partialTick() {
            return 0;
        }
    }

    default void beginFrame(Context context) {
    }

    default Plan<Context> planFrame() {
        return this::beginFrame;
    }
}
