package net.createmod.catnip.impl.client.gui.element.pip;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.createmod.catnip.api.client.gui.render.pip.GuiBlockModelPartRenderState;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;

public class GuiBlockModelPartRenderer extends PictureInPictureRenderer<GuiBlockModelPartRenderState> {
	public GuiBlockModelPartRenderer(net.createmod.catnip.impl.client.render.MultiBufferSource.BufferSource bufferSource) {
	}

	@Override
	public Class<GuiBlockModelPartRenderState> getRenderStateClass() {
		return GuiBlockModelPartRenderState.class;
	}

	@Override
	protected void renderToTexture(GuiBlockModelPartRenderState state, PoseStack poseStack,
		SubmitNodeCollector collector) {
		poseStack.scale(1, 1, -1);
		transform(poseStack, state.xLocal(), state.yLocal(), state.zLocal(),
			state.xRot(), state.yRot(), state.zRot());
		submitPart(state.part(), poseStack, collector);
	}

	static void submitPart(net.minecraft.client.renderer.block.dispatch.BlockStateModelPart part,
		PoseStack poseStack, SubmitNodeCollector collector) {
		collector.submitBlockModel(poseStack, Sheets.cutoutBlockItemSheet(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
	}

	static void transform(PoseStack poseStack, float xLocal, float yLocal, float zLocal,
		float xRot, float yRot, float zRot) {
		poseStack.translate(xLocal, yLocal, zLocal);
		// This is the flip performed by the pre-26.2 GuiGameElement matrix. The PIP
		// Z convention is cancelled once by the renderer before any 3D rotation.
		poseStack.scale(1, -1, 1);
		poseStack.translate(.5, .5, .5);
		poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
		poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
		poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
		poseStack.translate(-.5, -.5, -.5);
	}

	@Override
	protected String getTextureLabel() {
		return "catnip:gui_block_model_part";
	}

	@Override
	protected float getTranslateY(int height, int guiScale) {
		return 64f * guiScale;
	}
}
