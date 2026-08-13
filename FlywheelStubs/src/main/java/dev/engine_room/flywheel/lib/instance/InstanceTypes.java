package dev.engine_room.flywheel.lib.instance;

import dev.engine_room.flywheel.api.instance.InstanceType;

public final class InstanceTypes {
    public static final InstanceType<TransformedInstance> TRANSFORMED = new SimpleInstanceType<>();
    public static final InstanceType<OrientedInstance> ORIENTED = new SimpleInstanceType<>();

    private InstanceTypes() {
    }
}
