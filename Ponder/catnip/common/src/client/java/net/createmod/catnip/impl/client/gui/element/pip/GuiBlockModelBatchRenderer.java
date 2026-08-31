package net.createmod.catnip.impl.client.gui.element.pip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.createmod.catnip.api.client.gui.render.pip.GuiBlockModelBatchRenderState;
import net.createmod.catnip.api.client.render.FluidRenderHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource.BufferSource;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.LightCoordsUtil;

public class GuiBlockModelBatchRenderer extends PictureInPictureRenderer<GuiBlockModelBatchRenderState> {
	public GuiBlockModelBatchRenderer(BufferSource bufferSource) {
	}

	@Override
	public Class<GuiBlockModelBatchRenderState> getRenderStateClass() {
		return GuiBlockModelBatchRenderState.class;
	}

	@Override
	protected void renderToTexture(GuiBlockModelBatchRenderState state, PoseStack poseStack,
		SubmitNodeCollector collector) {
		// PictureInPictureRenderer introduces a negative Z scale for its usual item
		// convention. GuiGameElement's old immediate-mode matrix did not. Cancel it
		// once on the parent, before the mechanism rotation, so both depth ordering
		// and the old X/Y/local hierarchy are preserved exactly.
		poseStack.scale(1, 1, -1);
		// The old immediate renderer applied the mechanism's camera rotation once to
		// the parent PoseStack. Keep it outside the per-part transforms so animated
		// shafts rotate around their own centre instead of orbiting the mechanism.
		if (state.rotateAroundBlockCenter()) {
			poseStack.scale(1, -1, 1);
			poseStack.translate(.5, .5, .5);
			poseStack.mulPose(Axis.ZP.rotationDegrees(state.globalZRot()));
			poseStack.mulPose(Axis.XP.rotationDegrees(state.globalXRot()));
			poseStack.mulPose(Axis.YP.rotationDegrees(state.globalYRot()));
			poseStack.translate(-.5, -.5, -.5);
		} else {
			poseStack.mulPose(Axis.XP.rotationDegrees(state.globalXRot()));
			poseStack.mulPose(Axis.YP.rotationDegrees(state.globalYRot()));
			poseStack.mulPose(Axis.ZP.rotationDegrees(state.globalZRot()));
		}
		for (GuiBlockModelBatchRenderState.Entry entry : state.entries()) {
			poseStack.pushPose();
			if (entry.fluid() != null) {
				poseStack.translate(entry.xLocal(), entry.yLocal(), entry.zLocal());
				poseStack.scale(1, -1, 1);
				poseStack.scale(entry.localScale(), entry.localScale(), entry.localScale());
				poseStack.translate(entry.postX(), entry.postY(), entry.postZ());
				FluidRenderHelper.submitFluidBox(collector, entry.fluid(),
					entry.minX(), entry.minY(), entry.minZ(), entry.maxX(), entry.maxY(), entry.maxZ(),
					poseStack, LightCoordsUtil.FULL_BRIGHT, false, true);
			} else if (state.rotateAroundBlockCenter()) {
				poseStack.translate(entry.xLocal(), entry.yLocal(), entry.zLocal());
			} else {
				GuiBlockModelPartRenderer.transform(poseStack,
					entry.xLocal(), entry.yLocal(), entry.zLocal(),
					entry.xRot(), entry.yRot(), entry.zRot());
			}
			if (entry.part() != null)
				GuiBlockModelPartRenderer.submitPart(entry.part(), poseStack, collector);
			else if (entry.state() != null)
				GuiBlockModelRenderer.submitBlockState(entry.state(), poseStack, collector, entry.cullBackFaces());
			poseStack.popPose();
		}
	}

	@Override
	protected String getTextureLabel() {
		return "catnip:gui_block_model_batch";
	}

	@Override
	protected float getTranslateY(int height, int guiScale) {
		// y0 is -64, so placing the model origin at pixel 64 maps it to GUI y=0,
		// exactly like the old immediate PoseStack renderer. The target still extends
		// to +80 below that origin to keep the full animation visible.
		return 64f * guiScale;
	}
}
