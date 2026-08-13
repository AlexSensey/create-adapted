package dev.engine_room.flywheel.impl.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import dev.engine_room.flywheel.impl.event.LevelRenderMatrices;
import net.minecraft.client.renderer.GameRenderer;

/**
 * In 26.2 view bobbing and hurt camera motion are multiplied into a local copy
 * of the level projection. CameraRenderState only retains the unmodified base
 * projection, so capture the matrix actually uploaded for world rendering.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@ModifyArg(
		method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;",
			ordinal = 0
		),
		index = 0
	)
	private Matrix4f flywheel$captureLevelProjection(Matrix4f projection) {
		LevelRenderMatrices.captureProjection(projection);
		return projection;
	}
}
