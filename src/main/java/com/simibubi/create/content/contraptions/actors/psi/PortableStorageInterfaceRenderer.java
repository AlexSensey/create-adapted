package com.simibubi.create.content.contraptions.actors.psi;

import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class PortableStorageInterfaceRenderer extends SafeBlockEntityRenderer<PortableStorageInterfaceBlockEntity> {

	public PortableStorageInterfaceRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(PortableStorageInterfaceBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new PortableStorageInterfaceRenderState();
	}

	@Override
	public void extractRenderState(PortableStorageInterfaceBlockEntity be, BlockEntityRenderState state,
		float partialTicks, net.minecraft.world.phys.Vec3 cameraPos,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof PortableStorageInterfaceRenderState psiState) {
			psiState.blockState = be.getBlockState();
			psiState.lit = be.isConnected();
			psiState.progress = be.getExtensionDistance(partialTicks);
			psiState.visualized = CreateVisualizationManager.supportsVisualization(be.getLevel());
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof PortableStorageInterfaceRenderState psiState))
			return;
		if (psiState.blockState == null || psiState.visualized)
			return;

		submit(psiState.blockState, psiState.lit, psiState.progress, ms, collector, state.lightCoords);
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		// Dynamic PSI parts are submitted through submitInContraption(); the base block is rendered by the contraption.
	}

	public static void submitInContraption(MovementContext context, PoseStack ms, SubmitNodeCollector collector) {
		float partialTicks = AnimationTickHolder.getPartialTicks();
		LerpedFloat animation = PortableStorageInterfaceMovement.getAnimation(context);
		float progress = animation.getValue(partialTicks);
		boolean lit = animation.settled();
		int light = LightCoordsUtil.getLightCoords(context.world, BlockPos.containing(context.position));
		submit(context.state, lit, progress, ms, collector, light);
	}

	private static void submit(BlockState blockState, boolean lit, float progress, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		ms.pushPose();
		rotateToFacing(ms, blockState.getValue(PortableStorageInterfaceBlock.FACING));
		ms.pushPose();
		ms.translate(0, progress * 0.5f + 0.375f, 0);
		submitPart(getMiddleKeyForState(blockState, lit), ms, collector, light);
		ms.popPose();
		ms.translate(0, progress, 0);
		submitPart(getTopKeyForState(blockState), ms, collector, light);
		ms.popPose();
	}

	private static void rotateToFacing(PoseStack ms, Direction facing) {
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.mulPose(Axis.XP.rotationDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90));
		ms.translate(-.5f, -.5f, -.5f);
	}

	static PortableStorageInterfaceBlockEntity getTargetPSI(MovementContext context) {
		String workingPos = PortableStorageInterfaceMovement._workingPos_;
		if (!context.data.contains(workingPos))
			return null;

		BlockPos pos = PortableStorageInterfaceMovement.readBlockPos(context.data, workingPos);
		BlockEntity blockEntity = context.world.getBlockEntity(pos);
		if (!(blockEntity instanceof PortableStorageInterfaceBlockEntity psi))
			return null;

		if (!psi.isTransferring())
			return null;
		return psi;
	}

	private static StandaloneModelKey<BlockStateModelPart> getMiddleKeyForState(BlockState state, boolean lit) {
		if (AllBlocks.PORTABLE_FLUID_INTERFACE.has(state))
			return lit ? CreateStandaloneModels.PORTABLE_FLUID_INTERFACE_MIDDLE_POWERED
				: CreateStandaloneModels.PORTABLE_FLUID_INTERFACE_MIDDLE;
		return lit ? CreateStandaloneModels.PORTABLE_STORAGE_INTERFACE_MIDDLE_POWERED
			: CreateStandaloneModels.PORTABLE_STORAGE_INTERFACE_MIDDLE;
	}

	static PartialModel getMiddleForState(BlockState state, boolean lit) {
		if (AllBlocks.PORTABLE_FLUID_INTERFACE.has(state))
			return lit ? AllPartialModels.PORTABLE_FLUID_INTERFACE_MIDDLE_POWERED
				: AllPartialModels.PORTABLE_FLUID_INTERFACE_MIDDLE;
		return lit ? AllPartialModels.PORTABLE_STORAGE_INTERFACE_MIDDLE_POWERED
			: AllPartialModels.PORTABLE_STORAGE_INTERFACE_MIDDLE;
	}

	static PartialModel getTopForState(BlockState state) {
		if (AllBlocks.PORTABLE_FLUID_INTERFACE.has(state))
			return AllPartialModels.PORTABLE_FLUID_INTERFACE_TOP;
		return AllPartialModels.PORTABLE_STORAGE_INTERFACE_TOP;
	}

	private static StandaloneModelKey<BlockStateModelPart> getTopKeyForState(BlockState state) {
		if (AllBlocks.PORTABLE_FLUID_INTERFACE.has(state))
			return CreateStandaloneModels.PORTABLE_FLUID_INTERFACE_TOP;
		return CreateStandaloneModels.PORTABLE_STORAGE_INTERFACE_TOP;
	}

	private static void submitPart(StandaloneModelKey<BlockStateModelPart> key, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		if (part == null)
			return;
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}

	private static class PortableStorageInterfaceRenderState extends BlockEntityRenderState {
		private BlockState blockState;
		private boolean lit;
		private float progress;
		private boolean visualized;
	}

}
