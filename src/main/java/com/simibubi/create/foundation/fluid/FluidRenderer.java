package com.simibubi.create.foundation.fluid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.createmod.catnip.api.client.platform.ClientFluidHelper;
import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.platform.services.ModFluidHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;

public class FluidRenderer {
	public static void renderFluidStream(FluidStack fluidStack, Direction direction, float radius, float progress,
		boolean inbound, MultiBufferSource buffer, PoseStack ms, int light) {
		if (fluidStack.isEmpty())
			return;

		if (inbound)
			direction = direction.getOpposite();

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(direction)));
		ms.mulPose(Axis.XP.rotationDegrees(direction == Direction.UP ? 180 : direction == Direction.DOWN ? 0 : 270));
		ms.translate(0, -.5, 0);

		float min = -radius;
		float max = radius;
		float y = inbound ? 1 : .5f;
		float yMin = y - Mth.clamp(progress * .5f, 0, 1);
		float yMax = y;

		VertexConsumer builder = FluidRenderHelper.getFluidBuilder(buffer);
		for (int i = 0; i < 4; i++) {
			renderFluidStreamSide(fluidStack, min, yMin, max, yMax, radius, builder, ms.last(), light);
			ms.mulPose(Axis.YP.rotationDegrees(90));
		}
		if (progress != 1)
			renderFluidStreamCap(fluidStack, min, radius, max, yMin, builder, ms.last(), light);
		ms.popPose();
	}

	public static void submitFluidStream(SubmitNodeCollector collector, FluidStack fluidStack, Direction direction,
		float radius, float progress, boolean inbound, PoseStack ms, int light) {
		if (fluidStack.isEmpty())
			return;

		if (inbound)
			direction = direction.getOpposite();

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(direction)));
		ms.mulPose(Axis.XP.rotationDegrees(direction == Direction.UP ? 180 : direction == Direction.DOWN ? 0 : 270));
		ms.translate(0, -.5, 0);

		float min = -radius;
		float max = radius;
		float y = inbound ? 1 : .5f;
		float yMin = y - Mth.clamp(progress * .5f, 0, 1);
		float yMax = y;

		int packedLight = light;
		for (int i = 0; i < 4; i++) {
			collector.submitCustomGeometry(ms, RenderTypes.translucentMovingBlock(), (pose, builder) ->
				renderFluidStreamSide(fluidStack, min, yMin, max, yMax, radius, builder, pose, packedLight));
			ms.mulPose(Axis.YP.rotationDegrees(90));
		}
		if (progress != 1)
			collector.submitCustomGeometry(ms, RenderTypes.translucentMovingBlock(), (pose, builder) ->
				renderFluidStreamCap(fluidStack, min, radius, max, yMin, builder, pose, packedLight));
		ms.popPose();
	}

	public static void renderFluidStream(FluidStack fluidStack, Direction direction, float radius, float progress,
		boolean inbound, VertexConsumer builder, PoseStack ms, int light) {
		if (fluidStack.isEmpty())
			return;

		if (inbound)
			direction = direction.getOpposite();

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(direction)));
		ms.mulPose(Axis.XP.rotationDegrees(direction == Direction.UP ? 180 : direction == Direction.DOWN ? 0 : 270));
		ms.translate(0, -.5, 0);

		float min = -radius;
		float max = radius;
		float y = inbound ? 1 : .5f;
		float yMin = y - Mth.clamp(progress * .5f, 0, 1);
		float yMax = y;

		for (int i = 0; i < 4; i++) {
			renderFluidStreamSide(fluidStack, min, yMin, max, yMax, radius, builder, ms.last(), light);
			ms.mulPose(Axis.YP.rotationDegrees(90));
		}
		if (progress != 1)
			renderFluidStreamCap(fluidStack, min, radius, max, yMin, builder, ms.last(), light);
		ms.popPose();
	}

	public static void renderFlowingTiledFace(Direction dir, float left, float down, float right, float up,
		float depth, VertexConsumer builder, PoseStack ms, int light, int color, TextureAtlasSprite texture) {
		FluidRenderHelper.renderTiledFace(dir, left, down, right, up, depth, builder, ms.last(), light, color, texture,
			0.5f);
	}

	private static void renderFluidStreamSide(FluidStack fluidStack, float hMin, float yMin, float hMax, float yMax,
		float h, VertexConsumer builder, PoseStack.Pose pose, int light) {
		TypedInstance<Fluid> typedFluid = (TypedInstance<Fluid>) fluidStack;
		FluidModel model = Minecraft.getInstance()
			.getModelManager()
			.getFluidStateModelSet()
			.get(fluidStack.getFluid()
				.defaultFluidState());

		TextureAtlasSprite flowTexture = model.flowingMaterial()
			.sprite();
		TextureAtlasSprite stillTexture = model.stillMaterial()
			.sprite();

		int color = ClientFluidHelper.INSTANCE.getColor(typedFluid, null, null);
		int blockLightIn = (light >> 4) & 0xF;
		int luminosity = Math.max(blockLightIn, ModFluidHelper.INSTANCE.getLuminosity(typedFluid));
		light = (light & 0xF00000) | luminosity << 4;

		FluidRenderHelper.renderTiledFace(Direction.SOUTH, hMin, yMin, hMax, yMax, h, builder, pose, light, color,
			flowTexture, 0.5f);
	}

	private static void renderFluidStreamCap(FluidStack fluidStack, float hMin, float radius, float hMax, float yMin,
		VertexConsumer builder, PoseStack.Pose pose, int light) {
		TypedInstance<Fluid> typedFluid = (TypedInstance<Fluid>) fluidStack;
		FluidModel model = Minecraft.getInstance()
			.getModelManager()
			.getFluidStateModelSet()
			.get(fluidStack.getFluid()
				.defaultFluidState());

		TextureAtlasSprite stillTexture = model.stillMaterial()
			.sprite();
		int color = ClientFluidHelper.INSTANCE.getColor(typedFluid, null, null);
		int blockLightIn = (light >> 4) & 0xF;
		int luminosity = Math.max(blockLightIn, ModFluidHelper.INSTANCE.getLuminosity(typedFluid));
		light = (light & 0xF00000) | luminosity << 4;

		FluidRenderHelper.renderStillTiledFace(Direction.DOWN, hMin, hMin, hMax, hMax, yMin, builder, pose, light,
			color, stillTexture);
	}
}
