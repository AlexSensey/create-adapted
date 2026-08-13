package dev.engine_room.flywheel.backend.engine;

import java.util.Comparator;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL33;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;

import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.backend.Samplers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class MaterialRenderState {
	public static final Comparator<Material> COMPARATOR = MaterialRenderState::compare;

	private MaterialRenderState() {
	}

	public static void setup(Material material) {
		setupTexture(material);
		setupBackfaceCulling(material.backfaceCulling());
		setupPolygonOffset(material.polygonOffset());
		setupDepthTest(material.depthTest());
		setupTransparency(material.transparency());
		setupWriteMask(material.writeMask());
	}

	public static void setupOit(Material material) {
		setupTexture(material);
		setupBackfaceCulling(material.backfaceCulling());
		setupPolygonOffset(material.polygonOffset());
		setupDepthTest(material.depthTest());

		WriteMask mask = material.writeMask();
		boolean writeColor = mask.color();
		GL11.glColorMask(writeColor, writeColor, writeColor, writeColor);
	}

	private static void setupTexture(Material material) {
		Samplers.DIFFUSE.makeActive();
		AbstractTexture texture = Minecraft.getInstance()
				.getTextureManager()
				.getTexture(material.texture());
		var gpuTexture = (GlTexture) texture.getTexture();
		FilterMode filter = material.blur() ? FilterMode.LINEAR : FilterMode.NEAREST;
		var sampler = (GlSampler) RenderSystem.getSamplerCache()
			.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, filter, filter, material.mipmap());
		GlStateManager._bindTexture(gpuTexture.glId());
		GL33.glBindSampler(Samplers.DIFFUSE.number, sampler.getId());
	}

	private static void setupBackfaceCulling(boolean backfaceCulling) {
		if (backfaceCulling) {
			GL11.glEnable(GL11.GL_CULL_FACE);
		} else {
			GL11.glDisable(GL11.GL_CULL_FACE);
		}
	}

	private static void setupPolygonOffset(boolean polygonOffset) {
		if (polygonOffset) {
			GL11.glPolygonOffset(-1.0F, -10.0F);
			GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
		} else {
			GL11.glPolygonOffset(0.0F, 0.0F);
			GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
		}
	}

	private static void setupDepthTest(DepthTest depthTest) {
		switch (depthTest) {
		case OFF -> {
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		}
		case NEVER -> {
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_NEVER);
		}
		case LESS -> {
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_GREATER);
		}
		case EQUAL -> {
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_EQUAL);
		}
		case LEQUAL -> {
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_GEQUAL);
		}
		case GREATER -> {
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_LESS);
		}
		case NOTEQUAL -> {
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_NOTEQUAL);
		}
		case GEQUAL -> {
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
		}
		case ALWAYS -> {
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_ALWAYS);
		}
		}
	}

	private static void setupTransparency(Transparency transparency) {
		switch (transparency) {
		case OPAQUE -> {
			GL11.glDisable(GL11.GL_BLEND);
		}
		case ADDITIVE -> {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
		}
		case LIGHTNING -> {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		}
		case GLINT -> {
			GL11.glEnable(GL11.GL_BLEND);
			GL14.glBlendFuncSeparate(GL11.GL_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
		}
		case CRUMBLING -> {
			GL11.glEnable(GL11.GL_BLEND);
			GL14.glBlendFuncSeparate(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO);
		}
		// Minecraft 26's render graph does not allow Flywheel's legacy raw-GL OIT
		// framebuffer chain to safely take over the active render target.  Backends
		// that do not support OIT are required to treat this as translucent.
		case TRANSLUCENT, ORDER_INDEPENDENT -> {
			GL11.glEnable(GL11.GL_BLEND);
			GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}
		}
	}

	private static void setupWriteMask(WriteMask mask) {
		GL11.glDepthMask(mask.depth());
		boolean writeColor = mask.color();
		GL11.glColorMask(writeColor, writeColor, writeColor, writeColor);
	}

	public static void reset() {
		resetTexture();
		resetBackfaceCulling();
		resetPolygonOffset();
		resetDepthTest();
		resetTransparency();
		resetWriteMask();
	}

	private static void resetTexture() {
		Samplers.DIFFUSE.makeActive();
		GlStateManager._bindTexture(0);
		GL33.glBindSampler(Samplers.DIFFUSE.number, 0);
	}

	private static void resetBackfaceCulling() {
		GL11.glEnable(GL11.GL_CULL_FACE);
	}

	private static void resetPolygonOffset() {
		GL11.glPolygonOffset(0.0F, 0.0F);
		GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
	}

	private static void resetDepthTest() {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_GEQUAL);
	}

	private static void resetTransparency() {
		GL11.glDisable(GL11.GL_BLEND);
		GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
	}

	private static void resetWriteMask() {
		GL11.glDepthMask(true);
		GL11.glColorMask(true, true, true, true);
	}

	public static boolean materialEquals(Material lhs, Material rhs) {
		if (lhs == rhs) {
			return true;
		}

		// Not here because ubershader: useLight, useOverlay, diffuse, fog shader, ambient occlusion
		// Everything in the comparator should be here.
		// @formatter:off
		return lhs.blur() == rhs.blur()
				&& lhs.mipmap() == rhs.mipmap()
				&& lhs.backfaceCulling() == rhs.backfaceCulling()
				&& lhs.polygonOffset() == rhs.polygonOffset()
				&& lhs.depthTest() == rhs.depthTest()
				&& lhs.transparency() == rhs.transparency()
				&& lhs.writeMask() == rhs.writeMask()
				&& lhs.light().source().equals(rhs.light().source())
				&& lhs.texture().equals(rhs.texture())
				&& lhs.cutout().source().equals(rhs.cutout().source())
				&& lhs.shaders().fragmentSource().equals(rhs.shaders().fragmentSource())
				&& lhs.shaders().vertexSource().equals(rhs.shaders().vertexSource());
		// @formatter:on
	}

	public static boolean materialIsAllNonNull(@Nullable Material material) {
		// We do not trust people to give us valid NotNull objects.
		// @formatter:off
		return material != null &&
				material.shaders() != null &&
				material.shaders().fragmentSource() != null &&
				material.shaders().vertexSource() != null &&
				material.fog() != null &&
				material.fog().source() != null &&
				material.cutout() != null &&
				material.cutout().source() != null &&
				material.light() != null &&
				material.light().source() != null &&
				material.texture() != null &&
				material.depthTest() != null &&
				material.transparency() != null &&
				material.writeMask() != null &&
				material.cardinalLightingMode() != null;
		// @formatter:on
	}

	public static int compare(Material lhs, Material rhs) {
		if (lhs == rhs) {
			return 0;
		}

		int cmp;
		cmp = lhs.transparency()
				.compareTo(rhs.transparency());
		if (cmp != 0) {
			return cmp;
		}
		cmp = lhs.light()
				.source()
				.compareTo(rhs.light()
						.source());
		if (cmp != 0) {
			return cmp;
		}
		cmp = lhs.cutout()
				.source()
				.compareTo(rhs.cutout()
						.source());
		if (cmp != 0) {
			return cmp;
		}
		cmp = lhs.shaders()
				.fragmentSource()
				.compareTo(rhs.shaders()
						.fragmentSource());
		if (cmp != 0) {
			return cmp;
		}
		cmp = lhs.shaders()
				.vertexSource()
				.compareTo(rhs.shaders()
						.vertexSource());
		if (cmp != 0) {
			return cmp;
		}
		cmp = lhs.texture()
				.compareTo(rhs.texture());
		if (cmp != 0) {
			return cmp;
		}
		cmp = Boolean.compare(lhs.blur(), rhs.blur());
		if (cmp != 0) {
			return cmp;
		}
		cmp = Boolean.compare(lhs.mipmap(), rhs.mipmap());
		if (cmp != 0) {
			return cmp;
		}
		cmp = Boolean.compare(lhs.backfaceCulling(), rhs.backfaceCulling());
		if (cmp != 0) {
			return cmp;
		}
		cmp = Boolean.compare(lhs.polygonOffset(), rhs.polygonOffset());
		if (cmp != 0) {
			return cmp;
		}
		cmp = lhs.depthTest()
				.compareTo(rhs.depthTest());
		if (cmp != 0) {
			return cmp;
		}
		cmp = lhs.writeMask()
				.compareTo(rhs.writeMask());
		if (cmp != 0) {
			return cmp;
		}
		return 0;
	}
}
