package net.createmod.catnip.impl.fabric.client.service;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.createmod.catnip.api.client.render.RenderPipelineRegistry;
import net.createmod.catnip.impl.fabric.client.mixin.RenderPipelinesAccessor;
import net.minecraft.resources.Identifier;

import java.util.Map;

public final class FabricRenderPipelineRegistry implements RenderPipelineRegistry {
	@Override
	public void register(RenderPipeline pipeline) {
		Map<Identifier, RenderPipeline> registry = RenderPipelinesAccessor.getPIPELINES_BY_LOCATION();
		Identifier id = pipeline.getLocation();

		if (registry.containsKey(id)) {
			throw new IllegalArgumentException("Duplicate pipeline registration for " + id);
		}

		registry.put(id, pipeline);
	}
}
