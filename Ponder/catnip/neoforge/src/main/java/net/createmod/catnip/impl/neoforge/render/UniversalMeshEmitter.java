package net.createmod.catnip.impl.neoforge.render;

import org.jetbrains.annotations.UnknownNullability;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.client.render.model.ShadeSeparatedBufferSource;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;

import net.neoforged.neoforge.client.model.quad.MutableQuad;

// Modified from https://github.com/Engine-Room/Flywheel/blob/2f67f54c8898d91a48126c3c753eefa6cd224f84/forge/src/lib/java/dev/engine_room/flywheel/lib/model/baked/MeshEmitter.java
class UniversalMeshEmitter implements VertexConsumer {
	@UnknownNullability
	private ShadeSeparatedBufferSource bufferSource;
	@UnknownNullability
	private ChunkSectionLayer defaultLayer;
	@UnknownNullability
	private boolean currentShade;

	public void prepare(ShadeSeparatedBufferSource bufferSource, ChunkSectionLayer layer) {
		this.bufferSource = bufferSource;
		this.defaultLayer = layer;
	}

	public void clear() {
		bufferSource = null;
	}

	@Override
	public void putBakedQuad(Pose pose, BakedQuad quad, QuadInstance instance) {
		var buffer = bufferSource.getBuffer(quad.materialInfo().layer(), quad.materialInfo().shade());
		buffer.putBakedQuad(pose, quad, instance);
	}

	@Override
	public void putMutableQuad(Pose pose, MutableQuad quad, QuadInstance instance) {
		var buffer = bufferSource.getBuffer(quad.chunkLayer(), quad.shade());
		buffer.putMutableQuad(pose, quad, instance);
	}

	@Override
	public void putBlockBakedQuad(float x, float y, float z, BakedQuad quad, QuadInstance instance) {
		var buffer = bufferSource.getBuffer(quad.materialInfo().layer(), quad.materialInfo().shade());
		buffer.putBlockBakedQuad(x, y, z, quad, instance);
	}

	@Override
	public VertexConsumer addVertex(float v, float v1, float v2) {
		var buffer = bufferSource.getBuffer(defaultLayer, currentShade);
		return buffer.addVertex(v, v1, v2);
	}

	@Override
	public VertexConsumer setColor(int i, int i1, int i2, int i3) {
		var buffer = bufferSource.getBuffer(defaultLayer, currentShade);
		return buffer.setColor(i, i1, i2, i3);
	}

	@Override
	public VertexConsumer setColor(int i) {
		var buffer = bufferSource.getBuffer(defaultLayer, currentShade);
		return buffer.setColor(i);
	}

	@Override
	public VertexConsumer setUv(float v, float v1) {
		var buffer = bufferSource.getBuffer(defaultLayer, currentShade);
		return buffer.setUv(v, v1);
	}

	@Override
	public VertexConsumer setUv1(int i, int i1) {
		var buffer = bufferSource.getBuffer(defaultLayer, currentShade);
		return buffer.setUv1(i, i1);
	}

	@Override
	public VertexConsumer setUv2(int i, int i1) {
		var buffer = bufferSource.getBuffer(defaultLayer, currentShade);
		return buffer.setUv2(i, i1);
	}

	@Override
	public VertexConsumer setNormal(float v, float v1, float v2) {
		var buffer = bufferSource.getBuffer(defaultLayer, currentShade);
		return buffer.setNormal(v, v1, v2);
	}

	@Override
	public VertexConsumer setLineWidth(float v) {
		var buffer = bufferSource.getBuffer(defaultLayer, currentShade);
		return buffer.setLineWidth(v);
	}
}
