package net.createmod.catnip.impl.client.render;

import java.util.Map;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.rendertype.RenderType;

public interface MultiBufferSource {
	VertexConsumer getBuffer(RenderType renderType);

	static BufferSource immediateWithBuffers(Map<RenderType, ByteBufferBuilder> fixedBuffers, ByteBufferBuilder fallback) {
		return new BufferSource(fixedBuffers, fallback);
	}

	class BufferSource implements MultiBufferSource {
		private final Map<RenderType, ByteBufferBuilder> fixedBuffers;
		private final ByteBufferBuilder fallback;

		public BufferSource(Map<RenderType, ByteBufferBuilder> fixedBuffers, ByteBufferBuilder fallback) {
			this.fixedBuffers = fixedBuffers;
			this.fallback = fallback;
		}

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			ByteBufferBuilder buffer = fixedBuffers.getOrDefault(renderType, fallback);
			return new BufferBuilder(buffer, renderType.primitiveTopology(), renderType.format());
		}

		public void endBatch() {
		}

		public void endBatch(RenderType renderType) {
		}
	}
}
