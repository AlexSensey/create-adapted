package dev.engine_room.flywheel.impl.mixin.compat.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Sodium builds its block entity render lists without going through vanilla's
 * section compiler. Keep Flywheel-visualized block entities out of that list so
 * their moving parts are not rendered by both pipelines.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.ExtendedBlockEntityType", remap = false)
interface ExtendedBlockEntityTypeMixin {
	@Inject(
		method = "shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Z",
		at = @At("HEAD"),
		cancellable = true,
		remap = false
	)
	private static void flywheel$skipVanillaVisualizedBlockEntity(BlockEntityType<?> type, BlockGetter level,
		BlockPos pos, BlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
		if (VisualizationManager.supportsVisualization(blockEntity.getLevel())
			&& VisualizationHelper.skipVanillaRender(blockEntity)) {
			cir.setReturnValue(false);
		}
	}
}
