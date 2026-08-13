package dev.engine_room.flywheel.lib.model.baked;

public class PartialModel {
    private final Object id;

    private PartialModel(Object id) {
        this.id = id;
    }

    public static PartialModel of(Object id) {
        return new PartialModel(id);
    }

    public Object get() {
        return id;
    }
}
