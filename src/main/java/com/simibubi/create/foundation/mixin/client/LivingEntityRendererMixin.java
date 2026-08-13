package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.equipment.armor.CardboardArmorHandler;
import com.simibubi.create.content.equipment.armor.CardboardStealthRenderState;
import com.simibubi.create.content.equipment.hats.CreateHatRenderState;
import com.simibubi.create.content.equipment.hats.EntityHats;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.trains.schedule.hat.TrainHatInfoReloadListener;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
	private void create$extractHat(LivingEntity entity, LivingEntityRenderState state, float partialTicks,
		CallbackInfo ci) {
		CreateHatRenderState hatState = (CreateHatRenderState) state;
		PartialModel hat = EntityHats.getHatFor(entity);
		int type = hat == AllPartialModels.TRAIN_HAT ? 1 : hat == AllPartialModels.LOGISTICS_HAT ? 2 : 0;
		hatState.create$setHatType(type);
		hatState.create$setHatInfo(type == 0 ? null : TrainHatInfoReloadListener.getHatInfoFor(entity));

		CardboardStealthRenderState cardboardState = (CardboardStealthRenderState) state;
		boolean stealth = entity instanceof Player && CardboardArmorHandler.testForStealth(entity);
		cardboardState.create$setCardboardStealth(stealth);
		cardboardState.create$setCardboardOnGround(entity.onGround());
		cardboardState.create$setCardboardMovement((float) entity.position()
			.subtract(entity.xOld, entity.yOld, entity.zOld)
			.length());
		if (stealth && !PackageStyles.STANDARD_BOXES.isEmpty()) {
			int index = Math.floorMod(entity.getUUID().hashCode(), PackageStyles.STANDARD_BOXES.size());
			ItemStack box = new ItemStack(PackageStyles.STANDARD_BOXES.get(index));
			Minecraft.getInstance().getItemModelResolver().updateForTopItem(cardboardState.create$getCardboardBox(), box,
				ItemDisplayContext.FIXED, entity.level(), entity, entity.getId());
		}
	}
}
