package dev.engine_room.flywheel.impl.mixin.visualmanage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Minecraft 26.2 extracts block entity render states before submitting them.
 * SectionCompiler cancellation is not sufficient with Sodium, which has its own
 * section compiler, so suppress the vanilla state at the common dispatcher.
 */
@Mixin(BlockEntityRenderDispatcher.class)
abstract class BlockEntityRenderDispatcherMixin {
	@Inject(
		method = "tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Z)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void flywheel$skipVanillaVisualizedBlockEntitySodium(BlockEntity blockEntity, float partialTicks,
		ModelFeatureRenderer.CrumblingOverlay breakProgress, boolean isGloballyRendered,
		CallbackInfoReturnable<BlockEntityRenderState> cir) {
		flywheel$skipVanillaVisualizedBlockEntity(blockEntity, cir);
	}

	@Inject(
		method = "tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;ZLnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void flywheel$skipVanillaVisualizedBlockEntity(BlockEntity blockEntity, float partialTicks,
		ModelFeatureRenderer.CrumblingOverlay breakProgress, boolean isGloballyRendered, Frustum frustum,
		CallbackInfoReturnable<BlockEntityRenderState> cir) {
		flywheel$skipVanillaVisualizedBlockEntity(blockEntity, cir);
	}

	private static void flywheel$skipVanillaVisualizedBlockEntity(BlockEntity blockEntity,
		CallbackInfoReturnable<BlockEntityRenderState> cir) {
		if (VisualizationManager.supportsVisualization(blockEntity.getLevel())
			&& VisualizationHelper.skipVanillaRender(blockEntity)) {
			cir.setReturnValue(null);
		}
	}
}
