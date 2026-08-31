package com.simibubi.create.content.contraptions.pulley;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public abstract class AbstractPulleyRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {

	private final StandaloneModelKey<BlockStateModelPart> halfRope;
	private final StandaloneModelKey<BlockStateModelPart> halfMagnet;

	public AbstractPulleyRenderer(BlockEntityRendererProvider.Context context,
		StandaloneModelKey<BlockStateModelPart> halfRope, StandaloneModelKey<BlockStateModelPart> halfMagnet) {
		super(context);
		this.halfRope = halfRope;
		this.halfMagnet = halfMagnet;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new PulleyRenderState();
	}

	@Override
	public void extractRenderState(T be, BlockEntityRenderState state, float partialTicks, Vec3 cameraPos,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
		if (state instanceof PulleyRenderState pulleyState) {
			pulleyState.blockEntity = be;
			pulleyState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);

		if (!(state instanceof PulleyRenderState pulleyState))
			return;
		if (!(pulleyState.blockEntity instanceof KineticBlockEntity be))
			return;
		if (CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;

		@SuppressWarnings("unchecked")
		T pulley = (T) be;
		float offset = getOffset(pulley, pulleyState.partialTicks);
		boolean running = isRunning(pulley);

		ms.pushPose();
		rotateForShaft(ms, getShaftAxis(pulley));
		submitPart(getCoil(), ms, collector, state.lightCoords);
		ms.popPose();

		if (running || offset == 0)
			submitAt(offset > .25f ? getMagnet() : halfMagnet, offset, ms, collector, state.lightCoords);

		float f = offset % 1;
		if (offset > .75f && (f < .25f || f > .75f))
			submitAt(halfRope, f > .75f ? f - 1 : f, ms, collector, state.lightCoords);

		if (!running)
			return;

		for (int i = 0; i < offset - 1.25f; i++)
			submitAt(getRope(), offset - i - 1, ms, collector, state.lightCoords);
	}

	private static void rotateForShaft(PoseStack ms, Direction.Axis shaftAxis) {
		ms.translate(.5, .5, .5);
		if (shaftAxis == Direction.Axis.X)
			ms.mulPose(Axis.YP.rotationDegrees(90));
		if (shaftAxis == Direction.Axis.Y)
			ms.mulPose(Axis.XP.rotationDegrees(90));
		ms.translate(-.5, -.5, -.5);
	}

	private static void submitAt(StandaloneModelKey<BlockStateModelPart> key, float offset, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		ms.pushPose();
		ms.translate(0, -offset, 0);
		submitPart(key, ms, collector, light);
		ms.popPose();
	}

	protected static void submitPart(StandaloneModelKey<BlockStateModelPart> key, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		if (part == null)
			return;
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}

	protected abstract Direction.Axis getShaftAxis(T be);

	protected abstract StandaloneModelKey<BlockStateModelPart> getCoil();

	protected abstract StandaloneModelKey<BlockStateModelPart> getRope();

	protected abstract StandaloneModelKey<BlockStateModelPart> getMagnet();

	protected abstract float getOffset(T be, float partialTicks);

	protected abstract boolean isRunning(T be);

	@Override
	protected BlockState getRenderedBlockState(T be) {
		return shaft(getShaftAxis(be));
	}

	public int getViewDistance() {
		return AllConfigs.server().kinetics.maxRopeLength.get();
	}

	protected static class PulleyRenderState extends KineticRenderState {
	}
}
