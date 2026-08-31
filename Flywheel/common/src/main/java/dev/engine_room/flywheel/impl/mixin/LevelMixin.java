package dev.engine_room.flywheel.impl.mixin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import dev.engine_room.flywheel.impl.extension.LevelExtension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.entity.LevelEntityGetter;

@Mixin(Level.class)
abstract class LevelMixin implements LevelExtension {
	private final Set<BlockEntity> flywheel$loadedBlockEntities = ConcurrentHashMap.newKeySet();

	@Shadow
	protected abstract LevelEntityGetter<Entity> getEntities();

	@Override
	public Iterable<Entity> flywheel$getAllLoadedEntities() {
		return getEntities().getAll();
	}

	@Override
	public Iterable<BlockEntity> flywheel$getAllLoadedBlockEntities() {
		return flywheel$loadedBlockEntities;
	}

	@Override
	public void flywheel$trackBlockEntity(BlockEntity blockEntity) {
		flywheel$loadedBlockEntities.add(blockEntity);
	}

	@Override
	public void flywheel$untrackBlockEntity(BlockEntity blockEntity) {
		flywheel$loadedBlockEntities.remove(blockEntity);
	}
}
