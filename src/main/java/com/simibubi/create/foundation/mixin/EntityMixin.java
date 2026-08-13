package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.AllFluids;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.equipment.armor.NetheriteDivingHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

@Mixin(value = Entity.class, priority = 1500)
public abstract class EntityMixin {
	@Shadow
	public abstract BlockPos getOnPos();

	@Shadow
	private Level level;

	@Shadow
	public abstract BlockState getBlockStateOn();

	@ModifyReturnValue(method = "fireImmune()Z", at = @At("RETURN"))
	private boolean create$onFireImmune(boolean original) {
		return ((Entity) (Object) this).getPersistentData().getBooleanOr(NetheriteDivingHandler.FIRE_IMMUNE_KEY, false) || original;
	}

	/*
	 * NeoForge 26.2 has not yet restored its entity/custom-fluid interaction
	 * patches. Treat Create's explicitly water-like fluids as water for the
	 * vanilla movement checks until that support is available upstream.
	 */
	@ModifyReturnValue(method = "isInWater()Z", at = @At("RETURN"))
	private boolean create$isInWaterLikeFluid(boolean original) {
		return original || create$isInCreateWaterLikeFluid(((Entity) (Object) this).blockPosition());
	}

	@ModifyReturnValue(method = "isUnderWater()Z", at = @At("RETURN"))
	private boolean create$isUnderWaterLikeFluid(boolean original) {
		Entity entity = (Entity) (Object) this;
		BlockPos eyePos = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
		return original || create$isInCreateWaterLikeFluid(eyePos);
	}

	@ModifyReturnValue(method = "getFluidHeight(Lnet/minecraft/tags/TagKey;)D", at = @At("RETURN"))
	private double create$getWaterLikeFluidHeight(double original, TagKey<Fluid> fluidTag) {
		if (fluidTag != FluidTags.WATER)
			return original;

		Entity entity = (Entity) (Object) this;
		BlockPos pos = entity.blockPosition();
		FluidState state = create$getLoadedFluidState(pos);
		if (state == null || !create$isCreateWaterLikeFluid(state))
			return original;

		double surface = pos.getY() + state.getHeight(level, pos);
		return Math.max(original, Math.max(0, surface - entity.getY()));
	}

	@Inject(method = "updateSwimming()V", at = @At("RETURN"))
	private void create$updateWaterLikeSwimming(CallbackInfo ci) {
		Entity entity = (Entity) (Object) this;
		if (entity.isSprinting() && !entity.isPassenger() && create$isInCreateWaterLikeFluid(entity.blockPosition()))
			entity.setSwimming(true);
	}

	private boolean create$isInCreateWaterLikeFluid(BlockPos pos) {
		FluidState state = create$getLoadedFluidState(pos);
		return state != null && create$isCreateWaterLikeFluid(state);
	}

	/*
	 * Entity serialization can run on a world-generation worker. Calling
	 * Level#getFluidState there may synchronously request the chunk currently
	 * being generated and deadlock the worker against the server thread during
	 * shutdown. ServerChunkCache#getChunk also hops to the server thread even
	 * when chunk loading is disabled, so never perform this query from a server
	 * worker at all. During normal entity ticking this runs on the server thread;
	 * client rendering remains safe as well.
	 */
	private FluidState create$getLoadedFluidState(BlockPos pos) {
		if (level instanceof ServerLevel serverLevel && !serverLevel.getServer()
			.isSameThread())
			return null;
		if (!level.hasChunkAt(pos))
			return null;
		return level.getFluidState(pos);
	}

	private static boolean create$isCreateWaterLikeFluid(FluidState state) {
		if (state.isEmpty() || !state.getFluidType().getIsWaterLike())
			return false;
		Fluid fluid = state.getType();
		return AllFluids.HONEY.is(fluid) || AllFluids.CHOCOLATE.is(fluid);
	}

	@ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getOnPosLegacy()Lnet/minecraft/core/BlockPos;"))
	private BlockPos create$fixSeatBouncing(BlockPos original) {
		return getBlockStateOn().getBlock() instanceof SeatBlock ? getOnPos() : original;
	}
}
