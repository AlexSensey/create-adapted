package net.createmod.catnip.impl.neoforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.createmod.catnip.api.client.event.LevelRendererReloadCallback;
import net.minecraft.client.renderer.LevelRenderer;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@Inject(
		method = "allChanged",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ViewArea;repositionCamera(Lnet/minecraft/core/SectionPos;)V"
		)
	)
	private void onReload(CallbackInfo ci) {
		LevelRendererReloadCallback.EVENT.invoker().onReload();
	}
}
