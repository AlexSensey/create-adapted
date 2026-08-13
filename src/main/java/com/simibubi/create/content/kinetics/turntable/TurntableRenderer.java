package com.simibubi.create.content.kinetics.turntable;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction.Axis;

public class TurntableRenderer extends KineticBlockEntityRenderer<TurntableBlockEntity> {
	private List<BlockStateModelPart> turntableModel;

	public TurntableRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof TurntableBlockEntity be))
			return;
		if (isInvalid(be))
			return;

		List<BlockStateModelPart> turntable = getTurntableModel();
		if (turntable.isEmpty())
			return;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(Axis.Y, getAngleForBe(be, be.getBlockPos(), Axis.Y, kineticState.partialTicks)));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), turntable, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	@Override
	protected void renderSafe(TurntableBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	private List<BlockStateModelPart> getTurntableModel() {
		if (turntableModel != null)
			return turntableModel;
		BlockStateModelPart turntable = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.TURNTABLE);
		return turntableModel = turntable == null ? List.of() : List.of(turntable);
	}
}
