package net.createmod.catnip.impl.client.mixin;

import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.blaze3d.platform.Lighting;

@Mixin(Lighting.class)
public interface LightingAccessor {
	@Invoker
	void callUpdateBuffer(Lighting.Entry entry, Vector3fc light0, Vector3fc light1);
}
