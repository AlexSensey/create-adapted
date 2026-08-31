package net.createmod.catnip.api.client.render.model;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.MeshData;

public interface ShadeSeparatedResultConsumer {
	void accept(RenderPipeline pipeline, boolean shaded, MeshData data);
}
