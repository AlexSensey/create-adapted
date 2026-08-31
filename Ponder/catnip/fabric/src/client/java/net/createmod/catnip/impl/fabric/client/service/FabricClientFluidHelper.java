package net.createmod.catnip.impl.fabric.client.service;

import net.minecraft.client.renderer.block.BlockAndTintGetter;

import org.apache.commons.lang3.NotImplementedException;
import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.client.platform.ClientFluidHelper;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.level.material.Fluid;

public final class FabricClientFluidHelper implements ClientFluidHelper {
	@Override
	public int getColor(TypedInstance<Fluid> fluid, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
		return FluidVariantRendering.getColor((FluidVariant) fluid, level, pos);
	}

	@Override
	@Nullable
	public TextureAtlasSprite getStillTexture(TypedInstance<Fluid> fluid) {
		throw new NotImplementedException(); // TODO
		//return FluidVariantRendering.getSprite((FluidVariant) fluid);
	}
}
