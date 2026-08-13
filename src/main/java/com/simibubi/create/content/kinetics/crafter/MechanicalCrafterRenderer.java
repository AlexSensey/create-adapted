package com.simibubi.create.content.kinetics.crafter;

import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity.Phase;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.Pointing;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class MechanicalCrafterRenderer extends KineticBlockEntityRenderer<MechanicalCrafterBlockEntity> {

	private List<BlockStateModelPart> cogwheelModel;
	private List<BlockStateModelPart> lidModel;
	private List<BlockStateModelPart> arrowModel;
	private List<BlockStateModelPart> beltFrameModel;
	private List<BlockStateModelPart> beltModel;

	public MechanicalCrafterRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(MechanicalCrafterBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof MechanicalCrafterBlockEntity be))
			return;
		if (isInvalid(be))
			return;

		float partialTicks = kineticState.partialTicks;
		renderItems(be, partialTicks, ms, collector, state.lightCoords);
		renderParts(be, partialTicks, ms, collector, state.lightCoords);
	}

	private void renderItems(MechanicalCrafterBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		Direction facing = be.getBlockState()
			.getValue(HORIZONTAL_FACING);
		Vec3 vec = Vec3.atLowerCornerOf(facing.getUnitVec3i())
			.scale(.58)
			.add(.5, .5, .5);

		if (be.phase == Phase.EXPORTING) {
			Direction targetDirection = MechanicalCrafterBlock.getTargetDirection(be.getBlockState());
			float progress =
				Mth.clamp((1000 - be.countDown + be.getCountDownSpeed() * partialTicks) / 1000f, 0, 1);
			vec = vec.add(Vec3.atLowerCornerOf(targetDirection.getUnitVec3i())
				.scale(progress * .75f));
		}

		ms.pushPose();
		ms.translate(vec.x, vec.y, vec.z);
		ms.scale(.5f, .5f, .5f);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		renderItemStacks(be, partialTicks, ms, collector, light);
		ms.popPose();
	}

	private static void renderItemStacks(MechanicalCrafterBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		if (be.phase == Phase.IDLE) {
			ItemStack stack = be.getInventory()
				.getItem(0);
			if (stack.isEmpty())
				return;

			ms.pushPose();
			ms.translate(0, 0, -1 / 256f);
			ms.mulPose(Axis.YP.rotationDegrees(180));
			submitItem(stack, ms, collector, light);
			ms.popPose();
			return;
		}

		GroupedItems items = be.groupedItems;
		float distance = .5f;

		ms.pushPose();
		if (be.phase == Phase.CRAFTING) {
			items = be.groupedItemsBeforeCraft;
			items.calcStats();
			float progress =
				Mth.clamp((2000 - be.countDown + be.getCountDownSpeed() * partialTicks) / 1000f, 0, 1);
			float earlyProgress = Mth.clamp(progress * 2, 0, 1);
			float lateProgress = Mth.clamp(progress * 2 - 1, 0, 1);

			ms.scale(1 - lateProgress, 1 - lateProgress, 1 - lateProgress);
			Vec3 centering =
				new Vec3(-items.minX + (-items.width + 1) / 2f, -items.minY + (-items.height + 1) / 2f, 0)
					.scale(earlyProgress);
			ms.translate(centering.x * .5f, centering.y * .5f, 0);
			distance += (-4 * (progress - .5f) * (progress - .5f) + 1) * .25f;
		}

		boolean onlyRenderFirst = be.phase == Phase.INSERTING || be.phase == Phase.CRAFTING && be.countDown < 1000;
		float spacing = distance;
		for (var entry : items.grid.entrySet()) {
			Pair<Integer, Integer> pair = entry.getKey();
			if (onlyRenderFirst && (pair.getLeft() != 0 || pair.getRight() != 0))
				continue;

			ms.pushPose();
			int x = pair.getKey();
			int y = pair.getValue();
			ms.translate(x * spacing, y * spacing, 0);

			int offset = 0;
			if (be.phase == Phase.EXPORTING && be.getBlockState()
				.hasProperty(MechanicalCrafterBlock.POINTING)) {
				Pointing value = be.getBlockState()
					.getValue(MechanicalCrafterBlock.POINTING);
				offset = value == Pointing.UP ? -1 : value == Pointing.LEFT ? 2 : value == Pointing.RIGHT ? -2 : 1;
			}

			ms.mulPose(Axis.YP.rotationDegrees(180));
			ms.translate(0, 0, (x + y * 3 + offset * 9) / 1024f);
			submitItem(entry.getValue(), ms, collector, light);
			ms.popPose();
		}
		ms.popPose();

		if (be.phase != Phase.CRAFTING)
			return;

		items = be.groupedItems;
		float progress = Mth.clamp((1000 - be.countDown + be.getCountDownSpeed() * partialTicks) / 1000f, 0, 1);
		float earlyProgress = Mth.clamp(progress * 2, 0, 1);
		float lateProgress = Mth.clamp(progress * 2 - 1, 0, 1);

		ms.pushPose();
		ms.mulPose(Axis.ZP.rotationDegrees(earlyProgress * 2 * 360));
		float upScaling = earlyProgress * 1.125f;
		float downScaling = 1 + (1 - lateProgress) * .125f;
		ms.scale(upScaling, upScaling, upScaling);
		ms.scale(downScaling, downScaling, downScaling);

		for (var entry : items.grid.entrySet()) {
			Pair<Integer, Integer> pair = entry.getKey();
			if (pair.getLeft() != 0 || pair.getRight() != 0)
				continue;
			ms.pushPose();
			ms.mulPose(Axis.YP.rotationDegrees(180));
			submitItem(entry.getValue(), ms, collector, light);
			ms.popPose();
		}
		ms.popPose();
	}

	private void renderParts(MechanicalCrafterBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		BlockState blockState = be.getBlockState();

		List<BlockStateModelPart> cogwheel = getModel(CreateStandaloneModels.SHAFTLESS_COGWHEEL, cogwheelModel);
		cogwheelModel = cogwheel;
		if (!VisualizationManager.supportsVisualization(be.getLevel()) && !cogwheel.isEmpty()) {
			ms.pushPose();
			transformRotatingModel(be, ms, partialTicks);
			if (blockState.getValue(HORIZONTAL_FACING)
				.getAxis() == Direction.Axis.X)
				rotateCentered(ms, 90, Direction.UP);
			rotateCentered(ms, 90, Direction.EAST);
			collector.submitBlockModel(ms, RenderTypes.solidMovingBlock(), cogwheel, BlockModelRenderState.EMPTY_TINTS,
				light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		Direction targetDirection = MechanicalCrafterBlock.getTargetDirection(blockState);
		BlockPos pos = be.getBlockPos();

		if ((be.covered || be.phase != Phase.IDLE) && be.phase != Phase.CRAFTING && be.phase != Phase.INSERTING) {
			lidModel = submitTransformed(CreateStandaloneModels.MECHANICAL_CRAFTER_LID, lidModel, blockState, ms,
				collector, light);
		}

		if (MechanicalCrafterBlock.isValidTarget(be.getLevel(), pos.relative(targetDirection), blockState)) {
			beltModel = submitTransformed(CreateStandaloneModels.MECHANICAL_CRAFTER_BELT, beltModel, blockState, ms,
				collector, light);
			beltFrameModel = submitTransformed(CreateStandaloneModels.MECHANICAL_CRAFTER_BELT_FRAME, beltFrameModel,
				blockState, ms, collector, light);
		} else {
			arrowModel = submitTransformed(CreateStandaloneModels.MECHANICAL_CRAFTER_ARROW, arrowModel, blockState, ms,
				collector, light);
		}
	}

	private List<BlockStateModelPart> submitTransformed(StandaloneModelKey<BlockStateModelPart> key,
		List<BlockStateModelPart> cached, BlockState blockState, PoseStack ms, SubmitNodeCollector collector, int light) {
		List<BlockStateModelPart> model = getModel(key, cached);
		if (model.isEmpty())
			return model;

		ms.pushPose();
		transformCrafterPart(ms, blockState);
		collector.submitBlockModel(ms, RenderTypes.solidMovingBlock(), model, BlockModelRenderState.EMPTY_TINTS, light,
			OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
		return model;
	}

	private static void transformCrafterPart(PoseStack ms, BlockState state) {
		float xRot = state.getValue(MechanicalCrafterBlock.POINTING)
			.getXRotation();
		float yRot = AngleHelper.horizontalAngle(state.getValue(HORIZONTAL_FACING));
		rotateCentered(ms, yRot + 90, Direction.UP);
		rotateCentered(ms, xRot, Direction.EAST);
	}

	private static void rotateCentered(PoseStack ms, float degrees, Direction axis) {
		ms.translate(.5, .5, .5);
		if (axis == Direction.UP)
			ms.mulPose(Axis.YP.rotationDegrees(degrees));
		else if (axis == Direction.EAST)
			ms.mulPose(Axis.XP.rotationDegrees(degrees));
		ms.translate(-.5, -.5, -.5);
	}

	private static void submitItem(ItemStack stack, PoseStack ms, SubmitNodeCollector collector, int light) {
		if (stack.isEmpty())
			return;

		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, stack, ItemDisplayContext.FIXED, null, null, 0);
		itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
	}

	private static List<BlockStateModelPart> getModel(StandaloneModelKey<BlockStateModelPart> key,
		List<BlockStateModelPart> cached) {
		if (cached != null)
			return cached;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		return model == null ? List.of() : List.of(model);
	}
}
