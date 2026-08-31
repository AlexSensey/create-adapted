package dev.engine_room.flywheel.impl.extension;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface LevelExtension {
	/**
	 * Get an iterator over all entities in this level.
	 *
	 * <p>
	 *     Normally, this would be accomplished by {@link ClientLevel#entitiesForRendering}, but the output of that
	 *     method does not include entities that are rendered by Flywheel. This interface provides a workaround.
	 * </p>
	 * @return An iterator over all entities in the level, including entities that are rendered by Flywheel.
	 */
	Iterable<Entity> flywheel$getAllLoadedEntities();

	Iterable<BlockEntity> flywheel$getAllLoadedBlockEntities();

	void flywheel$trackBlockEntity(BlockEntity blockEntity);

	void flywheel$untrackBlockEntity(BlockEntity blockEntity);

	static Iterable<Entity> getAllLoadedEntities(Level level) {
		return ((LevelExtension) level).flywheel$getAllLoadedEntities();
	}

	static Iterable<BlockEntity> getAllLoadedBlockEntities(Level level) {
		return ((LevelExtension) level).flywheel$getAllLoadedBlockEntities();
	}

	static void trackBlockEntity(Level level, BlockEntity blockEntity) {
		((LevelExtension) level).flywheel$trackBlockEntity(blockEntity);
	}

	static void untrackBlockEntity(Level level, BlockEntity blockEntity) {
		((LevelExtension) level).flywheel$untrackBlockEntity(blockEntity);
	}
}
