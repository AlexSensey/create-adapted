package net.createmod.catnip.impl.fabric.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.createmod.catnip.api.client.event.AtlasStitchedCallback;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {
	@Inject(method = "upload", at = @At("TAIL"))
	private void afterUpload(SpriteLoader.Preparations preparations, CallbackInfo ci) {
		AtlasStitchedCallback.EVENT.invoker().afterStitch((TextureAtlas) (Object) this);
	}
}
