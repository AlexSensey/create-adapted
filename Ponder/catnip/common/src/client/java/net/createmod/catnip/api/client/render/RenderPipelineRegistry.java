package net.createmod.catnip.api.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.createmod.catnip.api.platform.ServiceHelper;

/// Registry for custom [RenderPipeline]s.
///
/// Threadsafe on Neoforge.
public interface RenderPipelineRegistry {
	RenderPipelineRegistry INSTANCE = ServiceHelper.load(RenderPipelineRegistry.class);

	/// Register the given pipeline based on its [location][RenderPipeline#getLocation()].
	/// @throws IllegalArgumentException if a pipeline with that ID has already been registered
	void register(RenderPipeline pipeline);
}
