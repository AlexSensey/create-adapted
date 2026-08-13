package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.redstone.rail.ControllerRailBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(AbstractMinecart.class)
public class MinecartRailInteractionMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void create$onMinecartRailTick(CallbackInfo ci) {
		AbstractMinecart cart = (AbstractMinecart) (Object) this;
		Level level = cart.level();
		BlockPos pos = cart.getCurrentBlockPosOrRailBelow();
		BlockState state = level.getBlockState(pos);

		if (state.getBlock() instanceof CartAssemblerBlock cartAssembler) {
			cartAssembler.onMinecartPass(state, level, pos, cart);
			return;
		}
		if (state.getBlock() instanceof ControllerRailBlock controllerRail)
			controllerRail.onMinecartPass(state, level, pos, cart);
	}
}
