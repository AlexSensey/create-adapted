package dev.engine_room.flywheel.backend.engine.uniform;

import org.joml.Vector3f;

import dev.engine_room.flywheel.api.backend.RenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.util.ARGB;

public final class LevelUniforms extends UniformWriter {
	private static final int SIZE = 16 * 4 + 4 * 12;
	static final UniformBuffer BUFFER = new UniformBuffer(Uniforms.LEVEL_INDEX, SIZE);

	public static final Vector3f LIGHT0_DIRECTION = new Vector3f();
	public static final Vector3f LIGHT1_DIRECTION = new Vector3f();

	private LevelUniforms() {
	}

	public static void update(RenderContext context) {
		long ptr = BUFFER.ptr();

		ClientLevel level = context.level();
		float partialTick = context.partialTick();

		var levelState = net.minecraft.client.Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState;
		var sky = levelState.skyRenderState;
		int skyColor = sky.skyColor;
		int cloudColor = levelState.cloudColor;
		ptr = writeVec4(ptr, ARGB.redFloat(skyColor), ARGB.greenFloat(skyColor), ARGB.blueFloat(skyColor), 1f);
		ptr = writeVec4(ptr, ARGB.redFloat(cloudColor), ARGB.greenFloat(cloudColor), ARGB.blueFloat(cloudColor), 1f);

		// Minecraft 26.2 uploads level lighting through Lighting's own UBO
		// instead of GlStateManager.setupLevelDiffuseLighting(). Mirror the
		// vanilla directions because Flywheel uses its own uniform block.
		if (level.dimensionType().cardinalLightType() == CardinalLighting.Type.NETHER) {
			LIGHT0_DIRECTION.set(0.2f, 1.0f, -0.7f).normalize();
			LIGHT1_DIRECTION.set(-0.2f, -1.0f, 0.7f).normalize();
		} else {
			LIGHT0_DIRECTION.set(0.2f, 1.0f, -0.7f).normalize();
			LIGHT1_DIRECTION.set(-0.2f, 1.0f, 0.7f).normalize();
		}
		ptr = writeVec3(ptr, LIGHT0_DIRECTION);
		ptr = writeVec3(ptr, LIGHT1_DIRECTION);

		long dayTime = levelState.gameTime;
		long levelDay = dayTime / 24000L;
		float timeOfDay = (float) (dayTime - levelDay * 24000L) / 24000f;
		ptr = writeInt(ptr, (int) (levelDay % 0x7FFFFFFFL));
		ptr = writeFloat(ptr, timeOfDay);

		ptr = writeInt(ptr, level.dimensionType().hasSkyLight() ? 1 : 0);

		ptr = writeFloat(ptr, sky.sunAngle);

		ptr = writeFloat(ptr, sky.starBrightness);
		ptr = writeInt(ptr, sky.moonPhase.index());

		ptr = writeInt(ptr, sky.rainBrightness < 1 ? 1 : 0);
		ptr = writeFloat(ptr, 1 - sky.rainBrightness);
		ptr = writeInt(ptr, 0);
		ptr = writeFloat(ptr, 0);

		ptr = writeFloat(ptr, 1 - sky.rainBrightness);

		ptr = writeInt(ptr, level.dimensionType().ambientLight() > 0 ? 1 : 0);

		// TODO: use defines for custom dimension ids
        int dimensionId;
        ResourceKey<Level> dimension = level.dimension();
        if (Level.OVERWORLD.equals(dimension)) {
            dimensionId = 0;
        } else if (Level.NETHER.equals(dimension)) {
            dimensionId = 1;
        } else if (Level.END.equals(dimension)) {
            dimensionId = 2;
        } else {
            dimensionId = -1;
        }
        ptr = writeInt(ptr, dimensionId);

		BUFFER.markDirty();
    }
}
