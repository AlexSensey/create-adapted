package net.createmod.catnip.platform;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public class NeoForgeCatnipServices {
	public static final FluidRenderer FLUID_RENDERER = new FluidRenderer();

	public static class FluidRenderer {
		public void renderFluidBox(FluidStack fluidStack, float minX, float minY, float minZ, float maxX, float maxY,
			float maxZ, MultiBufferSource buffer, PoseStack poseStack, int light, boolean renderBottom,
			boolean invertGasses) {
			if (fluidStack.isEmpty())
				return;
			FluidRenderHelper.renderFluidBox((TypedInstance<Fluid>) fluidStack, minX, minY, minZ, maxX, maxY, maxZ,
				buffer, poseStack, light, renderBottom, invertGasses);
		}
	}
}
