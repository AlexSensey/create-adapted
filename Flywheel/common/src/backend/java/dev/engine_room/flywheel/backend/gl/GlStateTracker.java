package dev.engine_room.flywheel.backend.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import com.mojang.blaze3d.opengl.GlStateManager;

import dev.engine_room.flywheel.backend.Samplers;

import dev.engine_room.flywheel.backend.gl.buffer.GlBufferType;

/**
 * Tracks bound buffers/vbos because GlStateManager doesn't do that for us.
 */
public class GlStateTracker {
	private static final int[] BUFFERS = new int[GlBufferType.values().length];
	private static int vao;
	private static int program;

	public static int getBuffer(GlBufferType type) {
		return BUFFERS[type.ordinal()];
	}

	public static int getVertexArray() {
		return vao;
	}

	public static int getProgram() {
		return program;
	}

	public static void _setBuffer(GlBufferType type, int id) {
		BUFFERS[type.ordinal()] = id;
	}

	public static void _setVertexArray(int id) {
		vao = id;
	}

	public static void _setProgram(int id) {
		program = id;
	}

	public static State getRestoreState() {
		// Minecraft 26.2 no longer routes all state changes through GlStateManager, so the old mixin-based cache can
		// be stale before Flywheel starts drawing. Read the driver state at the boundary instead. Restoring cached zero
		// values here used to leave Flywheel's shader/framebuffer active for subsequent world and GUI rendering.
		vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
		program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		GlBufferType[] values = GlBufferType.values();
		for (int i = 0; i < values.length; i++) {
			BUFFERS[i] = GL11.glGetInteger(values[i].glBindingEnum);
		}

		int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		int textureUnitCount = Samplers.NOISE.number + 1;
		int[] textures2d = new int[textureUnitCount];
		int[] textures2dArray = new int[textureUnitCount];
		int[] samplers = new int[textureUnitCount];
		for (int i = 0; i < textureUnitCount; i++) {
			GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
			textures2d[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			textures2dArray[i] = GL11.glGetInteger(GL30.GL_TEXTURE_BINDING_2D_ARRAY);
			samplers[i] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
		}
		GlStateManager._activeTexture(activeTexture);

		int[] viewport = new int[4];
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

		return new State(BUFFERS.clone(), vao, program, activeTexture,
				GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
				GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
				GL11.glIsEnabled(GL11.GL_DEPTH_TEST), GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
				GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK), GL11.glIsEnabled(GL11.GL_BLEND),
				GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
				GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
				GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA),
				GL11.glIsEnabled(GL11.GL_CULL_FACE), GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL),
				GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR), GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS),
				textures2d, textures2dArray, samplers, viewport);
	}

	public static void bindVao(int vao) {
		if (vao != GlStateTracker.vao) {
			GL30.glBindVertexArray(vao);
			GlStateTracker.vao = vao;
		}
	}

	public static void bindBuffer(GlBufferType type, int buffer) {
		if (BUFFERS[type.ordinal()] != buffer || type == GlBufferType.ELEMENT_ARRAY_BUFFER) {
			GL15.glBindBuffer(type.glEnum, buffer);
			BUFFERS[type.ordinal()] = buffer;
		}
	}

	public record State(int[] buffers, int vao, int program, int activeTexture, int drawFramebuffer,
			int readFramebuffer, boolean depthTest, int depthFunc, boolean depthMask, boolean blend,
			int blendSrcRgb, int blendDstRgb, int blendSrcAlpha, int blendDstAlpha, int blendEquationRgb,
			int blendEquationAlpha, boolean cullFace, boolean polygonOffsetFill, float polygonOffsetFactor,
			float polygonOffsetUnits, int[] textures2d, int[] textures2dArray, int[] samplers,
			int[] viewport) implements AutoCloseable {
		public void restore() {
			GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);

			if (vao != GlStateTracker.vao) {
				GL30.glBindVertexArray(vao);
				GlStateTracker.vao = vao;
			}

			GlBufferType[] values = GlBufferType.values();

			for (int i = 0; i < values.length; i++) {
				if (buffers[i] != GlStateTracker.BUFFERS[i] && values[i] != GlBufferType.ELEMENT_ARRAY_BUFFER) {
					GL15.glBindBuffer(values[i].glEnum, buffers[i]);
					GlStateTracker.BUFFERS[i] = buffers[i];
				}
			}

			if (program != GlStateTracker.program) {
				GL20.glUseProgram(program);
				GlStateTracker.program = program;
			}

			for (int i = 0; i < textures2d.length; i++) {
				GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
				GlStateManager._bindTexture(textures2d[i]);
				GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textures2dArray[i]);
				GL33.glBindSampler(i, samplers[i]);
			}
			GlStateManager._activeTexture(activeTexture);
			GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

			setEnabled(GL11.GL_DEPTH_TEST, depthTest);
			GL11.glDepthFunc(depthFunc);
			GL11.glDepthMask(depthMask);
			setEnabled(GL11.GL_BLEND, blend);
			GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
			GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
			setEnabled(GL11.GL_CULL_FACE, cullFace);
			GL11.glPolygonOffset(polygonOffsetFactor, polygonOffsetUnits);
			setEnabled(GL11.GL_POLYGON_OFFSET_FILL, polygonOffsetFill);
		}

		private static void setEnabled(int capability, boolean enabled) {
			if (enabled) {
				GL11.glEnable(capability);
			} else {
				GL11.glDisable(capability);
			}
		}

		@Override
		public void close() {
			restore();
		}
	}
}
