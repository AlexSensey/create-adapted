package com.simibubi.create.foundation.particle;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleResources.SpriteParticleRegistration;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import org.jetbrains.annotations.NotNull;

public interface ICustomParticleDataWithSprite<T extends ParticleOptions> extends ICustomParticleData<T> {

	default ParticleType<T> createType() {
		return new ParticleType<>(false) {

			@Override
			public @NotNull MapCodec<T> codec() {
				return ICustomParticleDataWithSprite.this.getCodec(this);
			}

			@Override
			public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
				return ICustomParticleDataWithSprite.this.getStreamCodec();
			}
		};
	}

	@Override
	default ParticleProvider<T> getFactory() {
		throw new IllegalAccessError("This particle type uses a metaFactory!");
	}

	public default SpriteParticleRegistration<T> getMetaFactory() {
		return null;
	}

	@Override
	public default void register(ParticleType<T> type, RegisterParticleProvidersEvent event) {
		SpriteParticleRegistration<T> factory = getMetaFactory();
		if (factory != null)
			event.registerSpriteSet(type, factory);
	}

}
