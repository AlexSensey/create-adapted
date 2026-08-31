package net.createmod.catnip.impl.fabric.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
	@Invoker("makeParticle")
	<T extends ParticleOptions> Particle catnip$makeParticle(T particleOptions, double x, double y, double z, double mx, double my, double mz);
}
