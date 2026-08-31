package dev.engine_room.flywheel.impl.mixin.visualmanage;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.extension.LevelExtension;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
	@Shadow
	@Final
	Level level;

	@Inject(method = "setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At("TAIL"))
	private void flywheel$onBlockEntityAdded(BlockEntity blockEntity, CallbackInfo ci) {
		// In 26.2 setBlockEntity() removes a previous block entity only after the
		// map insertion. Queueing ADD at the insertion point produced ADD(new),
		// REMOVE(old) for an in-place replacement, allowing the old visual to clear
		// the new shaft's position entry. Wait until replacement is complete.
		// Do not query Level#getBlockEntity here. During initial chunk promotion,
		// that call requests the same chunk while its FULL future is still being
		// completed on the server thread, deadlocking world creation. At TAIL the
		// replacement has already completed, so the passed instance is authoritative.
		if (blockEntity.isRemoved()) {
			return;
		}
		VisualizationManager manager = VisualizationManager.get(level);
		LevelExtension.trackBlockEntity(level, blockEntity);
		if (manager != null)
			manager.blockEntities().queueAdd(blockEntity);
	}
}
