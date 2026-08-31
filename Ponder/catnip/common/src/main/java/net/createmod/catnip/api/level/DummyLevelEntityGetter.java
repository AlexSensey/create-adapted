package net.createmod.catnip.api.level;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

public class DummyLevelEntityGetter<T extends EntityAccess> implements LevelEntityGetter<T> {
	@Override
	public T get(int id) {
		return null;
	}

	@Override
	public T get(UUID id) {
		return null;
	}

	@Override
	public Iterable<T> getAll() {
		return Collections.emptyList();
	}

	@Override
	public <U extends T> void get(EntityTypeTest<T, U> type, AbortableIterationConsumer<U> consumer) {}

	@Override
	public void get(AABB bb, Consumer<T> output) {}

	@Override
	public <U extends T> void get(EntityTypeTest<T, U> type, AABB bb, AbortableIterationConsumer<U> consumer) {}
}
