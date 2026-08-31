package net.createmod.catnip.impl.client.gui.element.pip;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.gui.render.pip.GuiBlockModelRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.Sheets;
import net.createmod.catnip.impl.client.render.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public class GuiBlockModelRenderer extends PictureInPictureRenderer<GuiBlockModelRenderState> {
	public GuiBlockModelRenderer(BufferSource bufferSource) {
	}

	@Override
	public Class<GuiBlockModelRenderState> getRenderStateClass() {
		return GuiBlockModelRenderState.class;
	}

	@Override
	protected void renderToTexture(GuiBlockModelRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
		poseStack.scale(1, 1, -1);
		GuiBlockModelPartRenderer.transform(poseStack,
			renderState.xLocal(), renderState.yLocal(), renderState.zLocal(),
			renderState.xRot(), renderState.yRot(), renderState.zRot());
		submitBlockState(renderState.state(), poseStack, submitNodeCollector);

		// Keep the dynamic block-entity parts in the same PIP target as the static
		// block model. Separate targets lose their shared depth/origin and make
		// shafts and other attachments appear beside the block in GUI previews.
		if (renderState.blockEntity() != null) {
			BlockEntityRenderState blockEntityState = Minecraft.getInstance()
				.getBlockEntityRenderDispatcher()
				.tryExtractRenderState(renderState.blockEntity(),
					Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks(), null, false);
			if (blockEntityState != null)
				Minecraft.getInstance().getBlockEntityRenderDispatcher()
					.getRenderer(blockEntityState)
					.submit(blockEntityState, poseStack, submitNodeCollector, new CameraRenderState());
		}
	}

	static void submitBlockState(net.minecraft.world.level.block.state.BlockState state, PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector) {
		submitBlockState(state, poseStack, submitNodeCollector, false);
	}

	static void submitBlockState(net.minecraft.world.level.block.state.BlockState state, PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector, boolean cullBackFaces) {
		var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(42), parts);
		if (!parts.isEmpty())
			submitNodeCollector.submitBlockModel(poseStack,
				cullBackFaces ? RenderTypes.cutoutMovingBlock() : Sheets.cutoutBlockItemSheet(), parts,
				BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, 0);
	}

	@Override
	protected String getTextureLabel() {
		return "catnip:gui_block_model";
	}

	@Override
	protected float getTranslateY(int height, int guiScale) {
		return 64f * guiScale;
	}
}
