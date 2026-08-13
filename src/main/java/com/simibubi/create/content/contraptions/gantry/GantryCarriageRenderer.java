package com.simibubi.create.content.contraptions.gantry;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;

public class GantryCarriageRenderer extends KineticBlockEntityRenderer<GantryCarriageBlockEntity> {
	private List<BlockStateModelPart> cogsModel;

	public GantryCarriageRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);

		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof GantryCarriageBlockEntity be))
			return;
		if (isInvalid(be))
			return;
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		List<BlockStateModelPart> cogs = getCogsModel();
		if (cogs.isEmpty())
			return;

		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(GantryCarriageBlock.FACING);
		boolean alongFirst = blockState.getValue(GantryCarriageBlock.AXIS_ALONG_FIRST_COORDINATE);
		Axis rotationAxis = getRotationAxisOf(be);
		BlockPos visualPos = facing.getAxisDirection() == AxisDirection.POSITIVE
			? be.getBlockPos()
			: be.getBlockPos()
				.relative(facing.getOpposite());
		float angleForBE = getAngleForBE(be, visualPos, rotationAxis);

		Axis gantryAxis = Axis.X;
		for (Axis axis : Iterate.axes)
			if (axis != rotationAxis && axis != facing.getAxis())
				gantryAxis = axis;

		if (gantryAxis == Axis.X && facing == Direction.UP)
			angleForBE *= -1;
		if (gantryAxis == Axis.Y && (facing == Direction.NORTH || facing == Direction.EAST))
			angleForBE *= -1;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90));
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees((alongFirst ^ facing.getAxis() == Axis.X) ? 0 : 90));
		ms.translate(0, -9 / 16f, 0);
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-angleForBE));
		ms.translate(0, 9 / 16f, 0);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), cogs, BlockModelRenderState.EMPTY_TINTS,
			state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	public static float getAngleForBE(KineticBlockEntity be, final BlockPos pos, Axis axis) {
		float time = AnimationTickHolder.getRenderTime();
		float offset = getRotationOffsetForPosition(be, pos, axis);
		return (time * be.getSpeed() * 3f / 20 + offset) % 360;
	}

	@Override
	protected BlockState getRenderedBlockState(GantryCarriageBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	private List<BlockStateModelPart> getCogsModel() {
		if (cogsModel != null)
			return cogsModel;
		BlockStateModelPart cogs = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.GANTRY_COGS);
		return cogsModel = cogs == null ? List.of() : List.of(cogs);
	}
}
