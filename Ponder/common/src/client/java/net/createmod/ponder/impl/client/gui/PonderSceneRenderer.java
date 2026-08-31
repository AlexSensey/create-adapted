package net.createmod.ponder.impl.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.api.client.render.SuperRenderTypeBuffer;
import net.createmod.ponder.api.client.scene.PonderScene;
import net.createmod.ponder.api.client.scene.PonderScene.SceneTransform;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.createmod.catnip.impl.client.render.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;

public class PonderSceneRenderer extends PictureInPictureRenderer<PonderSceneRenderState> {
	public PonderSceneRenderer(BufferSource bufferSource) {
	}

	@Override
	protected void renderToTexture(PonderSceneRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
		poseStack.pushPose();
		poseStack.setIdentity();

		renderScene(state, poseStack, submitNodeCollector);

		poseStack.popPose();
	}

	private void renderScene(PonderSceneRenderState state, PoseStack poseStack, SubmitNodeCollector queue) {
		float partialTicks = state.partialTicks();
		float previousPartialTicks = AnimationTickHolder.pushPartialTicks(partialTicks);
		int previousTicks = AnimationTickHolder.pushTicks(PonderUI.ponderTicks);
		SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
		PonderScene scene = state.scene();

		try {
			poseStack.pushPose();
			// SceneTransform adds +200 Z. Keep the resulting scene centred around
			// zero: the 26.2 PIP projection only spans -1000..1000 and GUI scaling
			// otherwise pushes the rear half of larger scenes through the near plane.
			poseStack.translate(0, 0, -200);
			SceneTransform transform = scene.getTransform();
			transform.updateScreenParams(state.width(), state.height(), state.slide(), state.window().guiScale);
			transform.apply(poseStack, partialTicks);
			transform.updateSceneRVE(partialTicks);
			scene.renderScene(buffer, queue, poseStack, partialTicks);
			buffer.draw();

//		// coords for debug
//		if (PonderIndex.editingModeActive() && !userViewMode) {
//			poseStack.scale(-1, -1, 1);
//			poseStack.scale(1 / 16f, 1 / 16f, 1 / 16f);
//			poseStack.translate(1, -8, -1 / 64f);
//
//			// X AXIS
//			poseStack.pushPose();
//			poseStack.translate(4, -3, 0);
//			poseStack.translate(0, 0, -2 / 1024f);
//			for (int x = 0; x <= bounds.getXSpan(); x++) {
//				poseStack.translate(-16, 0, 0);
//				graphics.text(font, x == bounds.getXSpan() ? "x" : "" + x, 0, 0, 0xFFFFFFFF, false);
//			}
//			poseStack.popPose();
//
//			// Z AXIS
//			poseStack.pushPose();
//			poseStack.scale(-1, 1, 1);
//			poseStack.translate(0, -3, -4);
//			poseStack.mulPose(Axis.YP.rotationDegrees(-90));
//			poseStack.translate(-8, -2, 2 / 64f);
//			for (int z = 0; z <= bounds.getZSpan(); z++) {
//				poseStack.translate(16, 0, 0);
//				graphics.text(font, z == bounds.getZSpan() ? "z" : "" + z, 0, 0, 0xFFFFFFFF, false);
//			}
//			poseStack.popPose();
//
//			// DIRECTIONS
//			poseStack.pushPose();
//			poseStack.translate(bounds.getXSpan() * -8, 0, bounds.getZSpan() * 8);
//			poseStack.mulPose(Axis.YP.rotationDegrees(-90));
//			for (Direction d : Iterate.horizontalDirections) {
//				poseStack.mulPose(Axis.YP.rotationDegrees(90));
//				poseStack.pushPose();
//				poseStack.translate(0, 0, bounds.getZSpan() * 16);
//				poseStack.mulPose(Axis.XP.rotationDegrees(-90));
//				graphics.text(font, d.name().substring(0, 1), 0, 0, 0x66FFFFFF, false);
//				graphics.text(font, "|", 2, 10, 0x44FFFFFF, false);
//				graphics.text(font, ".", 2, 14, 0x22FFFFFF, false);
//				poseStack.popPose();
//			}
//			poseStack.popPose();
//			buffer.draw();
//		}

			poseStack.popPose();
		} finally {
			AnimationTickHolder.restoreTicks(previousTicks);
			AnimationTickHolder.restorePartialTicks(previousPartialTicks);
		}
	}

	@Override
	protected String getTextureLabel() {
		return "PonderScene";
	}

	@Override
	public Class<PonderSceneRenderState> getRenderStateClass() {
		return PonderSceneRenderState.class;
	}
}
