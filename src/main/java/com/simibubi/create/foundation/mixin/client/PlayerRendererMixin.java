package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.foundation.item.CustomArmPoseItem;

import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;

@Mixin(AvatarRenderer.class)
public class PlayerRendererMixin {
	@Inject(
		method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void create$onGetArmPose(Avatar avatar, ItemStack stack, InteractionHand hand,
		CallbackInfoReturnable<ArmPose> cir) {
		if (stack.getItem() instanceof CustomArmPoseItem armPoseProvider
			&& armPoseProvider.shouldUseCrossbowHold(stack, avatar, hand))
			cir.setReturnValue(ArmPose.CROSSBOW_HOLD);
	}
}
