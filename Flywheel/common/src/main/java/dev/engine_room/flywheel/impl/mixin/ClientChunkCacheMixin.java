package dev.engine_room.flywheel.impl.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(ClientChunkCache.class)
abstract class ClientChunkCacheMixin {
	@Shadow
	@Final
	ClientLevel level;

	@Inject(method = "onLightUpdate", at = @At("HEAD"))
	private void flywheel$onLightUpdate(LightLayer layer, SectionPos pos, CallbackInfo ci) {
		var manager = VisualizationManagerImpl.get(level);

		if (manager != null) {
			manager.onLightUpdate(pos, layer);
		}
	}

	/**
	 * Sodium does not use vanilla's SectionCompiler, so the usual hook which
	 * discovers visualized block entities is never reached. LevelChunk#setBlockEntity
	 * is also too early while packet data is being installed: at that point the
	 * chunk is not visible through ClientLevel#getChunkForCollisions and Flywheel
	 * rejects the visual. Queue everything only after ClientChunkCache has put the
	 * completed chunk into its storage.
	 */
	@Inject(method = "replaceWithPacketData", at = @At("RETURN"))
	private void flywheel$addChunkBlockEntities(CallbackInfoReturnable<LevelChunk> cir) {
		LevelChunk chunk = cir.getReturnValue();
		if (chunk == null) {
			return;
		}

		VisualizationManager manager = VisualizationManager.get(level);
		if (manager == null) {
			return;
		}

		chunk.getBlockEntities()
			.values()
			.forEach(manager.blockEntities()::queueAdd);
	}
}
