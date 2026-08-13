package dev.engine_room.flywheel.api.task;

@FunctionalInterface
public interface Plan<C> {
    void execute(C context);

    default Plan<C> then(Plan<? super C> next) {
        return context -> {
            execute(context);
            next.execute(context);
        };
    }
}
