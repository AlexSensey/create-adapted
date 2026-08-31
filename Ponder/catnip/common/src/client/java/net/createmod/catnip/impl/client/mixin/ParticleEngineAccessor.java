package net.createmod.catnip.impl.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
	@Accessor
	static List<ParticleRenderType> getRENDER_ORDER() {
		throw new AbstractMethodError();
	}

	@Invoker
	ParticleGroup<?> callCreateParticleGroup(ParticleRenderType type);
}
