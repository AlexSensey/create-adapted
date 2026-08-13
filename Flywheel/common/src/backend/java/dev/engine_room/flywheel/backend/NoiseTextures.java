package dev.engine_room.flywheel.backend;

import java.io.IOException;

import org.jetbrains.annotations.UnknownNullability;
import com.mojang.blaze3d.platform.NativeImage;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class NoiseTextures {
	public static final Identifier NOISE_TEXTURE = ResourceUtil.rl("textures/flywheel/noise/blue.png");

	@UnknownNullability
	public static DynamicTexture BLUE_NOISE;

	public static void reload(ResourceManager manager) {
		if (BLUE_NOISE != null) {
			BLUE_NOISE.close();
			BLUE_NOISE = null;
		}
		var optional = manager.getResource(NOISE_TEXTURE);

		if (optional.isEmpty()) {
			return;
		}

		try (var is = optional.get()
				.open()) {
			var image = NativeImage.read(is);

			BLUE_NOISE = new DynamicTexture(() -> "Flywheel blue noise", image);
		} catch (IOException e) {
			FlwBackend.LOGGER.error("Failed to load Flywheel blue-noise texture", e);
		}
	}
}
