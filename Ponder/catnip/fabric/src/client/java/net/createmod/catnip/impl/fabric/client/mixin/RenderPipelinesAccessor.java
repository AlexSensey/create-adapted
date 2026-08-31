package net.createmod.catnip.impl.fabric.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.renderer.RenderPipelines;

import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
	@Accessor
	static Map<Identifier, RenderPipeline> getPIPELINES_BY_LOCATION() {
		throw new AbstractMethodError();
	}
}
