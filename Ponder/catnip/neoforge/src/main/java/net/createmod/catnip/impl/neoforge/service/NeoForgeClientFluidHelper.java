package net.createmod.catnip.impl.neoforge.service;

import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.client.platform.ClientFluidHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

public class NeoForgeClientFluidHelper implements ClientFluidHelper {
	@Override
	public int getColor(TypedInstance<Fluid> fluid, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
		FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.typeHolder().value().defaultFluidState());
		FluidTintSource source = model.fluidTintSource();

		if (source != null) {
			if (level != null && pos != null) {
				return source.colorInWorld(fluid.typeHolder().value().defaultFluidState(), level.getBlockState(pos), level, pos);
			} else {
				return source.colorAsStack((FluidStack) fluid);
			}
		}

		return 0xFFFFFFFF;
	}

	@Override
	public @Nullable TextureAtlasSprite getStillTexture(TypedInstance<Fluid> fluid) {
		FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.typeHolder().value().defaultFluidState());
		return model.stillMaterial().sprite();
	}
}
