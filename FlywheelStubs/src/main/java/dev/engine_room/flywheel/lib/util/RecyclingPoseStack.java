package dev.engine_room.flywheel.lib.util;

public class RecyclingPoseStack implements AutoCloseable {
    public static RecyclingPoseStack of(Object stack) {
        return new RecyclingPoseStack();
    }

    @Override
    public void close() {
    }
}
