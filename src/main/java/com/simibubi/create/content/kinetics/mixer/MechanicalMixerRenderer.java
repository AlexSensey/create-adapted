package com.simibubi.create.content.kinetics.mixer;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class MechanicalMixerRenderer extends KineticBlockEntityRenderer<MechanicalMixerBlockEntity> {

	private List<BlockStateModelPart> cogwheelModel;
	private List<BlockStateModelPart> poleModel;
	private List<BlockStateModelPart> headModel;

	public MechanicalMixerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	public boolean shouldRenderOffScreen(MechanicalMixerBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(MechanicalMixerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof MechanicalMixerBlockEntity be))
			return;
		if (isInvalid(be))
			return;
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		renderCogwheel(be, kineticState.partialTicks, ms, collector, state.lightCoords);
		renderPoleAndHead(be, kineticState.partialTicks, ms, collector, state.lightCoords);
	}

	private void renderCogwheel(MechanicalMixerBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		List<BlockStateModelPart> cogwheel = getCogwheelModel();
		if (cogwheel.isEmpty())
			return;

		ms.pushPose();
		transformRotatingModel(be, ms, partialTicks);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), cogwheel, BlockModelRenderState.EMPTY_TINTS,
			light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private void renderPoleAndHead(MechanicalMixerBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		float renderedHeadOffset = be.getRenderedHeadOffset(partialTicks);

		List<BlockStateModelPart> pole = getPoleModel();
		if (!pole.isEmpty()) {
			ms.pushPose();
			ms.translate(0, -renderedHeadOffset, 0);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), pole, BlockModelRenderState.EMPTY_TINTS,
				light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		List<BlockStateModelPart> head = getHeadModel();
		if (head.isEmpty())
			return;

		float speed = be.getRenderedHeadRotationSpeed(partialTicks);
		float time = AnimationTickHolder.getRenderTime();
		float angle = ((time * speed * 6 / 10f) % 360) / 180 * (float) Math.PI;

		ms.pushPose();
		ms.translate(0, -renderedHeadOffset, 0);
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotation(angle));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), head, BlockModelRenderState.EMPTY_TINTS, light,
			OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private List<BlockStateModelPart> getCogwheelModel() {
		if (cogwheelModel != null)
			return cogwheelModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SHAFTLESS_COGWHEEL);
		return cogwheelModel = model == null ? List.of() : List.of(model);
	}

	private List<BlockStateModelPart> getPoleModel() {
		if (poleModel != null)
			return poleModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.MECHANICAL_MIXER_POLE);
		return poleModel = model == null ? List.of() : List.of(model);
	}

	private List<BlockStateModelPart> getHeadModel() {
		if (headModel != null)
			return headModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.MECHANICAL_MIXER_HEAD);
		return headModel = model == null ? List.of() : List.of(model);
	}
}
