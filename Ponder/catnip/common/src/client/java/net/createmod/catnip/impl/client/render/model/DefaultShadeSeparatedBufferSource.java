package net.createmod.catnip.impl.client.render.model;

import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.createmod.catnip.api.client.render.model.ShadeSeparatedBufferSource;
import net.createmod.catnip.api.client.render.model.ShadeSeparatedResultConsumer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class DefaultShadeSeparatedBufferSource implements ShadeSeparatedBufferSource {
	private static final ChunkSectionLayer[] CHUNK_LAYERS = ChunkSectionLayer.values();
	private static final int CHUNK_LAYER_AMOUNT = CHUNK_LAYERS.length;

	private final MeshEmitter[] emitters = new MeshEmitter[CHUNK_LAYER_AMOUNT];
	private final Reference2ReferenceMap<ChunkSectionLayer, MeshEmitter> emitterMap = new Reference2ReferenceOpenHashMap<>();

	public DefaultShadeSeparatedBufferSource() {
		for (int layerIndex = 0; layerIndex < CHUNK_LAYER_AMOUNT; layerIndex++) {
			ChunkSectionLayer layer = CHUNK_LAYERS[layerIndex];
			MeshEmitter emitter = new MeshEmitter(layer);
			emitters[layerIndex] = emitter;
			emitterMap.put(layer, emitter);
		}
	}

	public void prepare(ShadeSeparatedResultConsumer resultConsumer) {
		for (MeshEmitter emitter : emitters) {
			emitter.prepare(resultConsumer);
		}
	}

	public void end() {
		for (MeshEmitter emitter : emitters) {
			emitter.end();
		}
	}

	@Override
	public VertexConsumer getBuffer(ChunkSectionLayer chunkRenderType, boolean shade) {
		return emitterMap.get(chunkRenderType).getBuffer(shade);
	}
}
