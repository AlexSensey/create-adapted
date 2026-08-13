package dev.engine_room.flywheel.lib.transform;

public class TransformStack<T> implements Affine<TransformStack<T>> {
    private final Object stack;

    private TransformStack(Object stack) {
        this.stack = stack;
    }

    public static TransformStack<?> of(Object stack) {
        return new TransformStack<>(stack);
    }

    public TransformStack<T> pushPose() {
        invoke(stack, "pushPose");
        return this;
    }

    public TransformStack<T> popPose() {
        invoke(stack, "popPose");
        return this;
    }

    @Override
    public TransformStack<T> translate(double x, double y, double z) {
        invoke(stack, "translate", new Class<?>[] { double.class, double.class, double.class }, x, y, z);
        return this;
    }

    public TransformStack<T> translate(Object vec) {
        double[] xyz = readVec3(vec);
        if (xyz != null)
            return translate(xyz[0], xyz[1], xyz[2]);
        return this;
    }

    public TransformStack<T> translateX(double x) {
        return translate(x, 0, 0);
    }

    public TransformStack<T> translateY(double y) {
        return translate(0, y, 0);
    }

    public TransformStack<T> translateZ(double z) {
        return translate(0, 0, z);
    }

    public TransformStack<T> translateBack(Object vec) {
        double[] xyz = readVec3(vec);
        if (xyz != null)
            return translate(-xyz[0], -xyz[1], -xyz[2]);
        return this;
    }

    public TransformStack<T> scale(double scale) {
        return scale(scale, scale, scale);
    }

    public TransformStack<T> scale(double x, double y, double z) {
        invoke(stack, "scale", new Class<?>[] { float.class, float.class, float.class }, (float) x, (float) y,
            (float) z);
        return this;
    }

    public TransformStack<T> center() {
        return translate(.5, .5, .5);
    }

    public TransformStack<T> uncenter() {
        return translate(-.5, -.5, -.5);
    }

    public TransformStack<T> nudge(int seed) {
        return this;
    }

    public TransformStack<T> rotate(float radians, Object axis) {
        Object axisObject = toAxis(axis);
        Object rotation = invoke(axisObject, "rotation", new Class<?>[] { float.class }, radians);
        if (rotation != null)
            invokeCompatible(stack, "mulPose", rotation);
        return this;
    }

    public TransformStack<T> rotateCentered(float radians, Object axis) {
        return center().rotate(radians, axis).uncenter();
    }

    @Override
    public TransformStack<T> rotateX(float radians) {
        return rotate(radians, axisConstant("XP"));
    }

    @Override
    public TransformStack<T> rotateY(float radians) {
        return rotate(radians, axisConstant("YP"));
    }

    @Override
    public TransformStack<T> rotateZ(float radians) {
        return rotate(radians, axisConstant("ZP"));
    }

    public TransformStack<T> rotateXDegrees(float degrees) {
        return rotateX((float) Math.toRadians(degrees));
    }

    public TransformStack<T> rotateYDegrees(float degrees) {
        return rotateY((float) Math.toRadians(degrees));
    }

    public TransformStack<T> rotateZDegrees(float degrees) {
        return rotateZ((float) Math.toRadians(degrees));
    }

    private static Object toAxis(Object axis) {
        if (axis == null)
            return axisConstant("YP");
        String className = axis.getClass().getName();
        if (className.equals("com.mojang.math.Axis"))
            return axis;
        String name = axis instanceof Enum<?> e ? e.name() : "";
        if (className.equals("net.minecraft.core.Direction"))
            return switch (name) {
                case "EAST" -> axisConstant("XP");
                case "WEST" -> axisConstant("XN");
                case "UP" -> axisConstant("YP");
                case "DOWN" -> axisConstant("YN");
                case "SOUTH" -> axisConstant("ZP");
                case "NORTH" -> axisConstant("ZN");
                default -> axisConstant("YP");
            };
        if (className.equals("net.minecraft.core.Direction$Axis"))
            return switch (name) {
                case "X" -> axisConstant("XP");
                case "Y" -> axisConstant("YP");
                case "Z" -> axisConstant("ZP");
                default -> axisConstant("YP");
            };
        return axisConstant("YP");
    }

    private static Object axisConstant(String name) {
        try {
            return Class.forName("com.mojang.math.Axis")
                .getField(name)
                .get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static double[] readVec3(Object vec) {
        if (vec == null || !vec.getClass().getName().equals("net.minecraft.world.phys.Vec3"))
            return null;
        try {
            return new double[] {
                vec.getClass().getField("x").getDouble(vec),
                vec.getClass().getField("y").getDouble(vec),
                vec.getClass().getField("z").getDouble(vec)
            };
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Object invoke(Object target, String method) {
        return invoke(target, method, new Class<?>[0]);
    }

    private static Object invoke(Object target, String method, Class<?>[] parameterTypes, Object... args) {
        if (target == null)
            return null;
        try {
            return target.getClass()
                .getMethod(method, parameterTypes)
                .invoke(target, args);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static void invokeCompatible(Object target, String method, Object arg) {
        if (target == null || arg == null)
            return;
        for (java.lang.reflect.Method candidate : target.getClass().getMethods()) {
            if (!candidate.getName().equals(method) || candidate.getParameterCount() != 1)
                continue;
            Class<?> parameter = candidate.getParameterTypes()[0];
            if (!parameter.isAssignableFrom(arg.getClass()))
                continue;
            try {
                candidate.invoke(target, arg);
                return;
            } catch (ReflectiveOperationException ignored) {
                return;
            }
        }
    }
}
