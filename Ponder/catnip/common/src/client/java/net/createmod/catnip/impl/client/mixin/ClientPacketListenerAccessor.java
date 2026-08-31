package net.createmod.catnip.impl.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.multiplayer.ClientPacketListener;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {
	@Accessor("serverChunkRadius")
	int catnip$getServerChunkRadius();
}
