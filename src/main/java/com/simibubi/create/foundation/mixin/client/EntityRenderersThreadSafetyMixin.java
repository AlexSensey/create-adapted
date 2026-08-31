package com.simibubi.create.foundation.mixin.client;

import java.util.Map;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Serializes legacy entity-renderer registrations performed from parallel
 * FMLClientSetupEvent listeners. Minecraft's provider map is not thread-safe,
 * so a direct registration from one older mod can otherwise race another mod
 * and corrupt Object2ObjectOpenHashMap while it is being resized.
 */
@Mixin(EntityRenderers.class)
public abstract class EntityRenderersThreadSafetyMixin {

	@Shadow
	@Final
	private static Map<EntityType<?>, EntityRendererProvider<?>> PROVIDERS;

	/**
	 * @author Create: Adapted
	 * @reason Protect the shared renderer map from parallel legacy mod setup.
	 */
	@Overwrite
	public static synchronized <T extends Entity> void register(EntityType<? extends T> type,
		EntityRendererProvider<T> renderer) {
		PROVIDERS.put(type, renderer);
	}
}
