package com.simibubi.create.content.kinetics.mechanicalArm;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class ArmRenderer extends KineticBlockEntityRenderer<ArmBlockEntity> {
	private List<BlockStateModelPart> cogModel;
	private List<BlockStateModelPart> baseModel;
	private List<BlockStateModelPart> lowerBodyModel;
	private List<BlockStateModelPart> upperBodyModel;
	private List<BlockStateModelPart> clawBaseModel;
	private List<BlockStateModelPart> clawBaseGogglesModel;
	private List<BlockStateModelPart> upperClawGripModel;
	private List<BlockStateModelPart> lowerClawGripModel;
	private static boolean warnedMissingModels;

	public ArmRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ArmBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new ArmRenderState();
	}

	@Override
	public void extractRenderState(ArmBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);
		if (state instanceof ArmRenderState armState) {
			armState.blockEntity = be;
			armState.blockState = be.getBlockState();
			armState.heldItem = be.heldItem.copy();
			armState.phase = be.phase;
			armState.goggles = be.goggles;
			armState.partialTicks = partialTicks;
			armState.baseAngle = be.baseAngle.getValue(partialTicks);
			armState.lowerArmAngle = be.lowerArmAngle.getValue(partialTicks);
			armState.upperArmAngle = be.upperArmAngle.getValue(partialTicks);
			armState.headAngle = be.headAngle.getValue(partialTicks);
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof ArmRenderState armState))
			return;
		ArmBlockEntity be = armState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		boolean usingFlywheel = CreateVisualizationManager.supportsVisualization(be.getLevel());
		if (!usingFlywheel)
			submitCog(be, armState.partialTicks, ms, collector, state.lightCoords);
		if (!usingFlywheel || !armState.heldItem.isEmpty())
			submitArm(be, armState, ms, collector, state.lightCoords, !usingFlywheel);
		renderSelectionModeOverlay(be, ms, collector);
	}

	private void submitCog(ArmBlockEntity be, float partialTicks, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		List<BlockStateModelPart> cog = getCogModel();
		if (cog.isEmpty())
			return;

		ms.pushPose();
		transformRotatingModel(be, ms, partialTicks);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), cog, BlockModelRenderState.EMPTY_TINTS, light,
			OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private void submitArm(ArmBlockEntity be, ArmRenderState state, PoseStack ms, SubmitNodeCollector collector,
		int light, boolean renderGeometry) {
		ItemStack item = state.heldItem;
		boolean hasItem = !item.isEmpty();
		boolean isBlockItem = hasItem && item.getItem() instanceof BlockItem;
		BlockState blockState = state.blockState;

		float baseAngle;
		float lowerArmAngle;
		float upperArmAngle;
		float headAngle;
		int color;
		boolean inverted = blockState.getValue(ArmBlock.CEILING);

		boolean rave = state.phase == Phase.DANCING && be.getSpeed() != 0;
		if (rave) {
			float renderTick = AnimationTickHolder.getRenderTime() + (be.hashCode() % 64);
			baseAngle = (renderTick * 10) % 360;
			lowerArmAngle = Mth.lerp((Mth.sin(renderTick / 4) + 1) / 2, -45, 15);
			upperArmAngle = Mth.lerp((Mth.sin(renderTick / 8) + 1) / 4, -45, 95);
			headAngle = -lowerArmAngle;
			color = 0xFFFFFF;
		} else {
			baseAngle = state.baseAngle;
			lowerArmAngle = state.lowerArmAngle - 135;
			upperArmAngle = state.upperArmAngle - 90;
			headAngle = state.headAngle;
			color = 0xFFFFFF;
		}

		PoseStack msLocal = new PoseStack();
		center(msLocal);
		if (inverted)
			msLocal.mulPose(Axis.XP.rotationDegrees(180));

		renderArm(ms, msLocal, collector, blockState, color, baseAngle, lowerArmAngle, upperArmAngle, headAngle,
			state.goggles, inverted && state.goggles, hasItem, isBlockItem, light, renderGeometry);

		if (hasItem)
			submitHeldItem(item, isBlockItem, ms, msLocal, collector, light);
	}

	private void renderArm(PoseStack ms, PoseStack msLocal, SubmitNodeCollector collector,
		BlockState blockState, int color, float baseAngle, float lowerArmAngle, float upperArmAngle, float headAngle,
		boolean goggles, boolean inverted, boolean hasItem, boolean isBlockItem, int light, boolean renderGeometry) {
		transformBase(msLocal, baseAngle);
		if (renderGeometry)
			submitPart(ms, msLocal, collector, getBaseModel(), light);

		transformLowerArm(msLocal, lowerArmAngle);
		if (renderGeometry)
			submitPart(ms, msLocal, collector, getLowerBodyModel(), light);

		transformUpperArm(msLocal, upperArmAngle);
		if (renderGeometry)
			submitPart(ms, msLocal, collector, getUpperBodyModel(), light);

		transformHead(msLocal, headAngle);
		if (inverted)
			msLocal.mulPose(Axis.ZP.rotationDegrees(180));
		if (renderGeometry)
			submitPart(ms, msLocal, collector, goggles ? getClawBaseGogglesModel() : getClawBaseModel(), light);

		if (inverted)
			msLocal.mulPose(Axis.ZP.rotationDegrees(180));

		for (int flip : Iterate.positiveAndNegative) {
			msLocal.pushPose();
			transformClawHalf(msLocal, hasItem, isBlockItem, flip);
			if (renderGeometry)
				submitPart(ms, msLocal, collector, flip > 0 ? getLowerClawGripModel() : getUpperClawGripModel(), light);
			msLocal.popPose();
		}
	}

	private static void submitPart(PoseStack ms, PoseStack msLocal, SubmitNodeCollector collector,
		List<BlockStateModelPart> model, int light) {
		if (model == null || model.isEmpty())
			return;

		PoseStack renderPose = compose(ms, msLocal);
		collector.submitBlockModel(renderPose, RenderTypes.cutoutMovingBlock(), model,
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}

	private static void submitHeldItem(ItemStack item, boolean isBlockItem, PoseStack ms, PoseStack msLocal,
		SubmitNodeCollector collector, int light) {
		msLocal.pushPose();
		float itemScale = isBlockItem ? .5f : .625f;
		msLocal.mulPose(Axis.XP.rotationDegrees(90));
		msLocal.translate(0, isBlockItem ? -9 / 16f : -10 / 16f, 0);
		msLocal.scale(itemScale, itemScale, itemScale);

		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, item, ItemDisplayContext.FIXED, null, null, 0);
		itemState.submit(compose(ms, msLocal), collector, light, OverlayTexture.NO_OVERLAY, 0);
		msLocal.popPose();
	}

	private static PoseStack compose(PoseStack root, PoseStack local) {
		PoseStack renderPose = new PoseStack();
		renderPose.last()
			.pose()
			.set(root.last()
				.pose())
			.mul(local.last()
				.pose());
		renderPose.last()
			.normal()
			.set(root.last()
				.normal())
			.mul(local.last()
				.normal());
		return renderPose;
	}

	private static void center(PoseStack ms) {
		ms.translate(.5, .5, .5);
	}

	private static void renderSelectionModeOverlay(ArmBlockEntity be, PoseStack ms, SubmitNodeCollector collector) {
		if (be.selectionMode == null || !be.selectionMode.isActive())
			return;

		Minecraft mc = Minecraft.getInstance();
		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit))
			return;

		BlockPos pos = be.getBlockPos();
		if (!blockHit.getBlockPos()
			.equals(pos))
			return;

		Direction side = blockHit.getDirection();
		BlockState state = be.getBlockState();
		ValueBoxTransform slot = be.selectionMode.getSlotPositioning();
		if (slot instanceof ValueBoxTransform.Sided sided)
			sided.fromSide(side);
		if (!slot.shouldRender(be.getLevel(), pos, state))
			return;

		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(pos));
		if (!slot.testHit(be.getLevel(), pos, state, localHit))
			return;

		Vec3 offset = slot.getLocalOffset(be.getLevel(), pos, state);
		if (offset == null)
			return;

		Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
		AllIcons icon = be.selectionMode.get()
			.getIcon();

		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d,
			offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateSelectionModeOverlay(ms, side);
		ms.mulPose(Axis.ZP.rotationDegrees(180));
		ms.scale(-1, 1, 1);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), ArmRenderer::renderSelectionModeFrame);

		ms.scale(.25f, .25f, .25f);
		ms.translate(-.5f, -.5f, 1 / 256f);
		collector.submitCustomGeometry(ms, RenderTypes.textSeeThrough(AllIcons.ICON_ATLAS),
			(pose, consumer) -> icon.renderDoubleSided(pose, consumer, 0xDDDDDD));
		ms.popPose();
	}

	private static void rotateSelectionModeOverlay(PoseStack ms, Direction face) {
		switch (face) {
		case SOUTH -> {
		}
		case NORTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
		case EAST -> ms.mulPose(Axis.YP.rotationDegrees(90));
		case WEST -> ms.mulPose(Axis.YP.rotationDegrees(270));
		case UP -> ms.mulPose(Axis.XP.rotationDegrees(270));
		case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(90));
		}
	}

	private static void renderSelectionModeFrame(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		renderThreePixelCorner(pose, consumer, 5, 5, 1, 1, color);
		renderThreePixelCorner(pose, consumer, 10, 5, -1, 1, color);
		renderThreePixelCorner(pose, consumer, 5, 10, 1, -1, color);
		renderThreePixelCorner(pose, consumer, 10, 10, -1, -1, color);
	}

	private static void renderThreePixelCorner(Pose pose, VertexConsumer consumer, int x, int y, int xStep, int yStep,
		int color) {
		flatPixelXY(pose, consumer, x, y, color);
		flatPixelXY(pose, consumer, x + xStep, y, color);
		flatPixelXY(pose, consumer, x, y + yStep, color);
	}

	private static void flatPixelXY(Pose pose, VertexConsumer consumer, int x, int y, int color) {
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, x * pixel - .5f, y * pixel - .5f, (x + 1) * pixel - .5f,
			(y + 1) * pixel - .5f, 0, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1,
		float z, int color) {
		consumer.addVertex(pose, x0, y0, z)
			.setColor(color);
		consumer.addVertex(pose, x1, y0, z)
			.setColor(color);
		consumer.addVertex(pose, x1, y1, z)
			.setColor(color);
		consumer.addVertex(pose, x0, y1, z)
			.setColor(color);
		consumer.addVertex(pose, x0, y1, z)
			.setColor(color);
		consumer.addVertex(pose, x1, y1, z)
			.setColor(color);
		consumer.addVertex(pose, x1, y0, z)
			.setColor(color);
		consumer.addVertex(pose, x0, y0, z)
			.setColor(color);
	}

	public static void transformClawHalf(PoseStack ms, boolean hasItem, boolean isBlockItem, int flip) {
		ms.translate(0, -flip * (hasItem ? isBlockItem ? 3 / 16f : 5 / 64f : 1 / 16f), -6 / 16d);
	}

	public static void transformHead(PoseStack ms, float headAngle) {
		ms.translate(0, 0, -15 / 16d);
		ms.mulPose(Axis.XP.rotationDegrees(headAngle - 45f));
	}

	public static void transformUpperArm(PoseStack ms, float upperArmAngle) {
		ms.translate(0, 0, -14 / 16d);
		ms.mulPose(Axis.XP.rotationDegrees(upperArmAngle - 90));
	}

	public static void transformLowerArm(PoseStack ms, float lowerArmAngle) {
		ms.translate(0, 2 / 16d, 0);
		ms.mulPose(Axis.XP.rotationDegrees(lowerArmAngle + 135));
	}

	public static void transformBase(PoseStack ms, float baseAngle) {
		ms.translate(0, 4 / 16d, 0);
		ms.mulPose(Axis.YP.rotationDegrees(baseAngle));
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	private List<BlockStateModelPart> getCogModel() {
		return cogModel = getModel(CreateStandaloneModels.MECHANICAL_ARM_COG, cogModel);
	}

	private List<BlockStateModelPart> getBaseModel() {
		return baseModel = getModel(CreateStandaloneModels.MECHANICAL_ARM_BASE, baseModel);
	}

	private List<BlockStateModelPart> getLowerBodyModel() {
		return lowerBodyModel = getModel(CreateStandaloneModels.MECHANICAL_ARM_LOWER_BODY, lowerBodyModel);
	}

	private List<BlockStateModelPart> getUpperBodyModel() {
		return upperBodyModel = getModel(CreateStandaloneModels.MECHANICAL_ARM_UPPER_BODY, upperBodyModel);
	}

	private List<BlockStateModelPart> getClawBaseModel() {
		return clawBaseModel = getModel(CreateStandaloneModels.MECHANICAL_ARM_CLAW_BASE, clawBaseModel);
	}

	private List<BlockStateModelPart> getClawBaseGogglesModel() {
		return clawBaseGogglesModel =
			getModel(CreateStandaloneModels.MECHANICAL_ARM_CLAW_BASE_GOGGLES, clawBaseGogglesModel);
	}

	private List<BlockStateModelPart> getUpperClawGripModel() {
		return upperClawGripModel =
			getModel(CreateStandaloneModels.MECHANICAL_ARM_CLAW_GRIP_UPPER, upperClawGripModel);
	}

	private List<BlockStateModelPart> getLowerClawGripModel() {
		return lowerClawGripModel =
			getModel(CreateStandaloneModels.MECHANICAL_ARM_CLAW_GRIP_LOWER, lowerClawGripModel);
	}

	private static List<BlockStateModelPart> getModel(StandaloneModelKey<BlockStateModelPart> key,
		List<BlockStateModelPart> cached) {
		if (cached != null && !cached.isEmpty())
			return cached;
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		if (part == null) {
			if (!warnedMissingModels) {
				warnedMissingModels = true;
				Create.LOGGER.warn("Mechanical Arm standalone model is missing: {}", key);
			}
			return List.of();
		}
		return List.of(part);
	}

	private static class ArmRenderState extends KineticRenderState {
		private ArmBlockEntity blockEntity;
		private BlockState blockState;
		private ItemStack heldItem = ItemStack.EMPTY;
		private Phase phase = Phase.SEARCH_INPUTS;
		private boolean goggles;
		private float baseAngle;
		private float lowerArmAngle;
		private float upperArmAngle;
		private float headAngle;
	}
}
