package dev.engine_room.flywheel.backend.engine.uniform;

import net.minecraft.client.Minecraft;

public final class FogUniforms extends UniformWriter {
	private static final int SIZE = 4 * 7;
	static final UniformBuffer BUFFER = new UniformBuffer(Uniforms.FOG_INDEX, SIZE);

	public static void update() {
		long ptr = BUFFER.ptr();

		var fog = Minecraft.getInstance().gameRenderer.gameRenderState()
			.levelRenderState.cameraRenderState.fogData;
		var color = fog.color;

		ptr = writeFloat(ptr, color.x());
		ptr = writeFloat(ptr, color.y());
		ptr = writeFloat(ptr, color.z());
		ptr = writeFloat(ptr, color.w());
		ptr = writeFloat(ptr, fog.renderDistanceStart);
		ptr = writeFloat(ptr, fog.renderDistanceEnd);
		ptr = writeInt(ptr, 0); // 26.2 world fog is radial (sphere)

		BUFFER.markDirty();
	}
}
