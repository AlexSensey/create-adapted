package dev.engine_room.flywheel.impl.event;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import dev.engine_room.flywheel.api.backend.RenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.phys.Vec3;

public record RenderContextImpl(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers, Matrix4fc modelView,
								Matrix4fc projection, Matrix4fc viewProjection, Camera camera,
								Vec3 cameraPosition,
								float partialTick) implements RenderContext {
	public static RenderContextImpl create(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers, Matrix4fc modelView, Matrix4fc projection, Camera camera, float partialTick) {
		return create(renderer, level, buffers, modelView, projection, camera, camera.position(), partialTick);
	}

	public static RenderContextImpl create(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
		Matrix4fc modelView, Matrix4fc projection, Camera camera, Vec3 cameraPosition, float partialTick) {
		Matrix4f viewProjection = new Matrix4f(projection);
		viewProjection.mul(modelView);

		return new RenderContextImpl(renderer, level, buffers, modelView, projection, viewProjection, camera,
			cameraPosition, partialTick);
	}
}
