package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.foundation.render.PlayerSkyhookRenderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends HumanoidRenderState> {
	@Shadow
	@Final
	public ModelPart body;

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("RETURN"))
	private void create$afterSetupAnim(T renderState, CallbackInfo callbackInfo) {
		PlayerSkyhookRenderer.afterSetupAnim(renderState, (HumanoidModel<?>) (Object) this);
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("HEAD"))
	private void create$beforeSetupAnim(T renderState, CallbackInfo callbackInfo) {
		PlayerSkyhookRenderer.beforeSetupAnim(renderState, (HumanoidModel<?>) (Object) this);
	}
}
