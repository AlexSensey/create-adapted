package com.simibubi.create.content.schematics.cannon;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.schematics.cannon.LaunchedItem.ForBelt;
import com.simibubi.create.content.schematics.cannon.LaunchedItem.ForBlockState;
import com.simibubi.create.content.schematics.cannon.LaunchedItem.ForEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SchematicannonRenderer extends SafeBlockEntityRenderer<SchematicannonBlockEntity> {

	private List<BlockStateModelPart> connector;
	private List<BlockStateModelPart> pipe;

	public SchematicannonRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(SchematicannonBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
		MultiBufferSource buffer, int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new SchematicannonRenderState();
	}

	@Override
	public void extractRenderState(SchematicannonBlockEntity blockEntity, BlockEntityRenderState state,
		float partialTicks, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay);
		if (state instanceof SchematicannonRenderState cannonState) {
			cannonState.blockEntity = blockEntity;
			cannonState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof SchematicannonRenderState cannonState))
			return;
		SchematicannonBlockEntity blockEntity = cannonState.blockEntity;
		if (blockEntity == null || isInvalid(blockEntity))
			return;

		renderLaunchedBlocks(blockEntity, cannonState.partialTicks, poseStack, collector, state.lightCoords);
		if (VisualizationManager.supportsVisualization(blockEntity.getLevel()))
			return;
		double[] angles = getCannonAngles(blockEntity, blockEntity.getBlockPos(), cannonState.partialTicks);
		double yaw = angles[0];
		double pitch = angles[1];
		double recoil = getRecoil(blockEntity, cannonState.partialTicks);

		poseStack.pushPose();
		poseStack.translate(.5f, 0, .5f);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) yaw + 90));
		poseStack.translate(-.5f, 0, -.5f);
		submitParts(getConnector(), poseStack, collector, state.lightCoords);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(.5f, 15 / 16f, .5f);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) yaw + 90));
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) pitch));
		poseStack.translate(-.5f, -15 / 16f, -.5f);
		poseStack.translate(0, -recoil / 100, 0);
		submitParts(getPipe(), poseStack, collector, state.lightCoords);
		poseStack.popPose();
	}

	private List<BlockStateModelPart> getConnector() {
		if (connector == null) {
			BlockStateModelPart part = Minecraft.getInstance()
				.getModelManager()
				.getStandaloneModel(CreateStandaloneModels.SCHEMATICANNON_CONNECTOR);
			connector = part == null ? List.of() : List.of(part);
		}
		return connector;
	}

	private List<BlockStateModelPart> getPipe() {
		if (pipe == null) {
			BlockStateModelPart part = Minecraft.getInstance()
				.getModelManager()
				.getStandaloneModel(CreateStandaloneModels.SCHEMATICANNON_PIPE);
			pipe = part == null ? List.of() : List.of(part);
		}
		return pipe;
	}

	private static void submitParts(List<BlockStateModelPart> parts, PoseStack poseStack,
		SubmitNodeCollector collector, int light) {
		if (!parts.isEmpty())
			collector.submitBlockModel(poseStack, RenderTypes.cutoutMovingBlock(), parts,
				BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}

	public static double[] getCannonAngles(SchematicannonBlockEntity blockEntity, BlockPos pos, float partialTicks) {
		BlockPos target = blockEntity.printer.getCurrentTarget();
		if (target == null)
			return new double[] { blockEntity.defaultYaw, 40 };

		Vec3 difference = Vec3.atLowerCornerOf(target.subtract(pos));
		if (blockEntity.previousTarget != null)
			difference = Vec3.atLowerCornerOf(blockEntity.previousTarget)
				.add(Vec3.atLowerCornerOf(target.subtract(blockEntity.previousTarget))
					.scale(partialTicks))
				.subtract(Vec3.atLowerCornerOf(pos));

		double yaw = Mth.atan2(difference.x(), difference.z()) / Math.PI * 180;
		float distance = Mth.sqrt((float) (difference.x() * difference.x() + difference.z() * difference.z()));
		double pitch = Mth.atan2(distance, difference.y() * 3 + distance * 2) / Math.PI * 180 + 10;
		return new double[] { yaw, pitch };
	}

	public static double getRecoil(SchematicannonBlockEntity blockEntity, float partialTicks) {
		double recoil = 0;
		for (LaunchedItem launched : blockEntity.flyingBlocks) {
			if (launched.ticksRemaining == 0)
				continue;
			double age = launched.ticksRemaining + 1 - partialTicks;
			if (age > launched.totalTicks - 10)
				recoil = Math.max(recoil, age - launched.totalTicks + 10);
		}
		return recoil;
	}

	private static void renderLaunchedBlocks(SchematicannonBlockEntity blockEntity, float partialTicks,
		PoseStack poseStack, SubmitNodeCollector collector, int light) {
		for (LaunchedItem launched : blockEntity.flyingBlocks) {
			if (launched.ticksRemaining == 0)
				continue;
			Vec3 start = Vec3.atCenterOf(blockEntity.getBlockPos()
				.above());
			Vec3 target = Vec3.atCenterOf(launched.target);
			Vec3 distance = target.subtract(start);
			double yDifference = target.y - start.y;
			double throwHeight = Math.sqrt(distance.lengthSqr()) * .6f + yDifference;
			Vec3 cannonOffset = distance.add(0, throwHeight, 0)
				.normalize()
				.scale(2);
			start = start.add(cannonOffset);
			yDifference = target.y - start.y;
			float progress =
				((float) launched.totalTicks - (launched.ticksRemaining + 1 - partialTicks)) / launched.totalTicks;
			Vec3 horizontal = target.subtract(start)
				.scale(progress)
				.multiply(1, 0, 1);
			double yOffset = 2 * (1 - progress) * progress * throwHeight
				+ progress * progress * yDifference;
			Vec3 location = horizontal.add(.5, yOffset + 1.5, .5)
				.add(cannonOffset);

			poseStack.pushPose();
			poseStack.translate(location.x, location.y, location.z);
			poseStack.translate(.125f, .125f, .125f);
			poseStack.mulPose(Axis.YP.rotationDegrees(360 * progress));
			poseStack.mulPose(Axis.XP.rotationDegrees(360 * progress));
			poseStack.translate(-.125f, -.125f, -.125f);

			if (launched instanceof ForBlockState blockLaunch) {
				BlockState blockState = launched instanceof ForBelt ? AllBlocks.SHAFT.getDefaultState()
					: blockLaunch.state;
				poseStack.scale(.3f, .3f, .3f);
				BlockStateModel model = Minecraft.getInstance()
					.getModelManager()
					.getBlockStateModelSet()
					.get(blockState);
				List<BlockStateModelPart> parts = new ArrayList<>();
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.level != null)
					model.collectParts(minecraft.level, launched.target, blockState,
						RandomSource.create(blockState.getSeed(launched.target)), parts);
				else
					model.collectParts(RandomSource.create(blockState.getSeed(BlockPos.ZERO)), parts);
				List<BlockTintSource> tintSources = minecraft.getBlockColors()
					.getTintSources(blockState);
				int[] tints = tintSources.isEmpty() ? BlockModelRenderState.EMPTY_TINTS : new int[tintSources.size()];
				for (int i = 0; i < tintSources.size(); i++)
					tints[i] = minecraft.level == null ? tintSources.get(i).color(blockState)
						: tintSources.get(i).colorInWorld(blockState, minecraft.level, launched.target);
				boolean hasTranslucency = minecraft.level == null
					? model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT)
					: model.hasMaterialFlag(minecraft.level, launched.target, blockState, BakedQuad.FLAG_TRANSLUCENT);
				collector.submitMultiLayerBlockModel(poseStack, parts, hasTranslucency, tints, light,
					OverlayTexture.NO_OVERLAY, 0);
			} else if (launched instanceof ForEntity) {
				poseStack.scale(1.2f, 1.2f, 1.2f);
				ItemStackRenderState itemState = new ItemStackRenderState();
				Minecraft.getInstance()
					.getItemModelResolver()
					.updateForTopItem(itemState, launched.stack, ItemDisplayContext.GROUND, null, null, 0);
				itemState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
			}
			poseStack.popPose();
		}
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	private static class SchematicannonRenderState extends BlockEntityRenderState {
		private SchematicannonBlockEntity blockEntity;
		private float partialTicks;
	}
}
