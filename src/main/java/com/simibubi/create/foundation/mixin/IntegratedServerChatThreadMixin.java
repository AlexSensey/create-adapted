package com.simibubi.create.foundation.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep chat/UI updates off the server thread, including the path before stopServer saves the world. */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerChatThreadMixin {

	@Shadow @Final private Minecraft minecraft;

	@Shadow
	private void updatePermissionAndChatAbilities(LocalPlayer player) {
		throw new AssertionError();
	}

	@Inject(method = "updatePermissionAndChatAbilities", at = @At("HEAD"), cancellable = true)
	private void create$updateChatOnClientThread(LocalPlayer player, CallbackInfo ci) {
		if (minecraft.isSameThread())
			return;
		minecraft.execute(() -> {
			// A queued shutdown update must not touch a player from a different session.
			if (minecraft.player == player)
				updatePermissionAndChatAbilities(player);
		});
		ci.cancel();
	}
}
