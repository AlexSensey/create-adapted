package dev.engine_room.flywheel.backend.engine;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33;

import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;

import dev.engine_room.flywheel.backend.Samplers;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class TextureBinder {
	public static void bind(Identifier resourceLocation) {
		GlStateManager._bindTexture(byName(resourceLocation));
	}

	public static void bindLightAndOverlay() {
		var gameRenderer = Minecraft.getInstance().gameRenderer;
		var nearestSampler = (GlSampler) RenderSystem.getSamplerCache()
			.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
				FilterMode.NEAREST, FilterMode.NEAREST, false);
		var linearSampler = (GlSampler) RenderSystem.getSamplerCache()
			.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
				FilterMode.LINEAR, FilterMode.LINEAR, false);

		Samplers.OVERLAY.makeActive();
		GlStateManager._bindTexture(((GlTextureView) gameRenderer.overlayTexture().getTextureView()).glId());
		GL33.glBindSampler(Samplers.OVERLAY.number, nearestSampler.getId());

		Samplers.LIGHT.makeActive();
		GlStateManager._bindTexture(((GlTextureView) gameRenderer.levelLightmap()).glId());
		GL33.glBindSampler(Samplers.LIGHT.number, linearSampler.getId());
	}

	public static void resetLightAndOverlay() {
		Samplers.LIGHT.makeActive();
		GlStateManager._bindTexture(0);
		GL33.glBindSampler(Samplers.LIGHT.number, 0);
		Samplers.OVERLAY.makeActive();
		GlStateManager._bindTexture(0);
		GL33.glBindSampler(Samplers.OVERLAY.number, 0);
	}

	/**
	 * Get a built-in texture by its resource location.
	 *
	 * @param texture The texture's resource location.
	 * @return The texture.
	 */
	public static int byName(Identifier texture) {
		var gpuTexture = Minecraft.getInstance()
				.getTextureManager()
				.getTexture(texture)
				.getTexture();
		return ((GlTexture) gpuTexture).glId();
	}
}
