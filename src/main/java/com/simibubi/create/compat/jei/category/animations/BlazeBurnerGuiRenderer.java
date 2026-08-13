package com.simibubi.create.compat.jei.category.animations;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.impl.client.render.MultiBufferSource.BufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class BlazeBurnerGuiRenderer extends PictureInPictureRenderer<BlazeBurnerGuiRenderState> {

	public BlazeBurnerGuiRenderer(BufferSource bufferSource) {
	}

	@Override
	public Class<BlazeBurnerGuiRenderState> getRenderStateClass() {
		return BlazeBurnerGuiRenderState.class;
	}

	@Override
	protected void renderToTexture(BlazeBurnerGuiRenderState state, PoseStack pose, SubmitNodeCollector collector) {
		pose.scale(1, 1, -1);
		pose.mulPose(Axis.XP.rotationDegrees(-15.5f));
		pose.mulPose(Axis.YP.rotationDegrees(22.5f));

		BlockStateModelPart cage = model(CreateStandaloneModels.BLAZE_BURNER_BLOCK);
		BlockStateModelPart blaze = model(state.heatLevel() == com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.SEETHING
			? CreateStandaloneModels.BLAZE_SUPER : CreateStandaloneModels.BLAZE_ACTIVE);
		BlockStateModelPart rods = model(state.heatLevel() == com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.SEETHING
			? CreateStandaloneModels.BLAZE_BURNER_SUPER_RODS_2 : CreateStandaloneModels.BLAZE_BURNER_RODS_2);
		float bob = (Mth.sin(state.animationTime() / 16f) + .5f) / 16f;

		submitPart(pose, collector, cage, 0, 1.65f, 0, 0);
		// Standalone model parts are already authored in block-local 0..1 space.
		// The old cached-part renderer needed +1 on X/Z; retaining it here moved the
		// blaze and rods one full model-width into the static icon on the right.
		submitPart(pose, collector, blaze, 0, 1.8f, 0, 180);
		submitPart(pose, collector, rods, 0, 1.7f + bob, 0, 180);

		pose.pushPose();
		transform(pose, 0, 1.8f, 0, 0);
		BlazeBurnerRenderer.submitScrollingFlame(pose, collector, state.heatLevel(), 0,
			state.animationTime(), LightCoordsUtil.FULL_BRIGHT);
		pose.popPose();
	}

	private static BlockStateModelPart model(StandaloneModelKey<BlockStateModelPart> key) {
		return Minecraft.getInstance().getModelManager().getStandaloneModel(key);
	}

	private static void submitPart(PoseStack pose, SubmitNodeCollector collector, BlockStateModelPart part,
		float x, float y, float z, float yRotation) {
		pose.pushPose();
		transform(pose, x, y, z, yRotation);
		collector.submitBlockModel(pose, Sheets.cutoutBlockItemSheet(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
	}

	private static void transform(PoseStack pose, float x, float y, float z, float yRotation) {
		pose.translate(x, y, z);
		pose.scale(1, -1, 1);
		pose.translate(.5, .5, .5);
		pose.mulPose(Axis.YP.rotationDegrees(yRotation));
		pose.translate(-.5, -.5, -.5);
	}

	@Override
	protected String getTextureLabel() {
		return "create:jei_blaze_burner";
	}

	@Override
	protected float getTranslateY(int height, int guiScale) {
		return 64f * guiScale;
	}
}
