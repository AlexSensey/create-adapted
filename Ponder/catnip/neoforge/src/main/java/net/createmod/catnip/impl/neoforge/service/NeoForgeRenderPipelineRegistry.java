package net.createmod.catnip.impl.neoforge.service;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.createmod.catnip.api.client.render.RenderPipelineRegistry;

import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.ArrayList;
import java.util.List;

public final class NeoForgeRenderPipelineRegistry implements RenderPipelineRegistry {
	private static final List<RenderPipeline> toRegister = new ArrayList<>();
	private static boolean frozen;

	@Override
	public synchronized void register(RenderPipeline pipeline) {
		if (frozen) {
			throw new IllegalStateException("Pipeline registered too late: " + pipeline.getLocation());
		}

		toRegister.add(pipeline);
	}

	public static void registerEvent(RegisterRenderPipelinesEvent event) {
		frozen = true;
		toRegister.forEach(event::registerPipeline);
	}
}
