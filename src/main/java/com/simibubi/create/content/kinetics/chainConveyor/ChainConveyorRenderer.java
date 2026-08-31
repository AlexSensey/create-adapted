package com.simibubi.create.content.kinetics.chainConveyor;

import java.util.List;
import java.util.Map.Entry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity.ConnectionStats;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage.ChainConveyorPackagePhysicsData;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ChainConveyorRenderer extends KineticBlockEntityRenderer<ChainConveyorBlockEntity> {

	public static final Identifier CHAIN_LOCATION = Identifier.withDefaultNamespace("textures/block/iron_chain.png");
	public static final int MIP_DISTANCE = 48;
	private List<BlockStateModelPart> shaftModel;
	private List<BlockStateModelPart> wheelModel;
	private List<BlockStateModelPart> guardModel;

	public ChainConveyorRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ChainConveyorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof ChainConveyorBlockEntity be))
			return;
		if (isInvalid(be))
			return;

		renderChains(be, ms, collector);
		if (CreateVisualizationManager.supportsVisualization(be.getLevel())) {
			// Package models use the item atlas and cannot be instanced as block partials on 26.2.
			// Keep them on the compatibility path while Flywheel renders the aligned shaft and wheel.
			renderPackages(be, kineticState.partialTicks, ms, collector);
			return;
		}
		renderWheel(ms, collector, state.lightCoords);
		renderPackages(be, kineticState.partialTicks, ms, collector);
	}

	private void renderPackages(ChainConveyorBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector) {
		for (ChainConveyorPackage box : be.loopingPackages)
			renderPackage(be, box, partialTicks, ms, collector);
		for (Entry<BlockPos, List<ChainConveyorPackage>> entry : be.travellingPackages.entrySet())
			for (ChainConveyorPackage box : entry.getValue())
				renderPackage(be, box, partialTicks, ms, collector);
	}

	private void renderPackage(ChainConveyorBlockEntity be, ChainConveyorPackage box, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector) {
		if (box.worldPosition == null || box.item == null || box.item.isEmpty())
			return;

		ChainConveyorPackagePhysicsData physicsData = box.physicsData(be.getLevel());
		if (physicsData.prevPos == null || physicsData.pos == null || physicsData.prevTargetPos == null
			|| physicsData.targetPos == null)
			return;

		Vec3 position = physicsData.prevPos.lerp(physicsData.pos, partialTicks);
		Vec3 targetPosition = physicsData.prevTargetPos.lerp(physicsData.targetPos, partialTicks);
		float yaw = AngleHelper.angleLerp(partialTicks, physicsData.prevYaw, physicsData.yaw);
		Vec3 localTarget = targetPosition.subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
		Vec3 dangle = targetPosition.add(0, .5, 0)
			.subtract(position);
		double yawRadians = Math.toRadians(-yaw);
		double rotatedX = dangle.x * Math.cos(yawRadians) + dangle.z * Math.sin(yawRadians);
		double rotatedZ = -dangle.x * Math.sin(yawRadians) + dangle.z * Math.cos(yawRadians);
		float zRot = Mth.clamp(Mth.wrapDegrees((float) Mth.atan2(-rotatedX, dangle.y) * Mth.RAD_TO_DEG) / 2,
			-25, 25);
		float xRot = Mth.clamp(Mth.wrapDegrees((float) Mth.atan2(rotatedZ, dangle.y) * Mth.RAD_TO_DEG) / 2,
			-25, 25);

		int light = getLight(be.getLevel(), BlockPos.containing(position));
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null)
			return;

		ItemStackRenderState boxState = new ItemStackRenderState();
		minecraft.getItemModelResolver()
			.updateForTopItem(boxState, box.item, ItemDisplayContext.FIXED, level, null, 0);

		int width = Math.round(PackageItem.getWidth(box.item) * 16);
		int height = Math.round(PackageItem.getHeight(box.item) * 16);
		Identifier riggingId = Create.asResource("chain_package_rigging/" + width + "x" + height);
		ItemModel riggingModel = minecraft.getModelManager()
			.getItemModel(riggingId);
		ItemStackRenderState riggingState = new ItemStackRenderState();
		riggingModel.update(riggingState, box.item, minecraft.getItemModelResolver(), ItemDisplayContext.FIXED, level,
			null, 0);

		renderPackageState(ms, collector, riggingState, localTarget, yaw, zRot, xRot, physicsData.flipped, light, box);
		renderPackageState(ms, collector, boxState, localTarget, yaw, zRot, xRot, false, light, box);
	}

	private static void renderPackageState(PoseStack ms, SubmitNodeCollector collector, ItemStackRenderState itemState,
		Vec3 localTarget, float yaw, float zRot, float xRot, boolean flipped, int light, ChainConveyorPackage box) {
		ms.pushPose();
		ms.translate(localTarget.x, localTarget.y, localTarget.z);
		ms.translate(0, 10 / 16f, 0);
		ms.mulPose(Axis.YP.rotationDegrees(yaw));
		ms.mulPose(Axis.ZP.rotationDegrees(zRot));
		ms.mulPose(Axis.XP.rotationDegrees(xRot));
		if (flipped)
			ms.mulPose(Axis.YP.rotationDegrees(180));
		ms.translate(0, -PackageItem.getHookDistance(box.item) + 7 / 16f, 0);
		// The 26.2 item-model pipeline uses a different effective suspension origin
		// than Create's original partial-model path. Each rigging model extends its
		// hook 2 pixels past the style offset, so 6 pixels puts its tip on the chain.
		ms.translate(0, 6 / 16f, 0);
		itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	@Override
	protected BlockState getRenderedBlockState(ChainConveyorBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	@Override
	protected List<BlockStateModelPart> getRotatingModelParts(ChainConveyorBlockEntity be, BlockState renderedState) {
		return getShaftModel();
	}

	public static void renderChain(PoseStack ms, MultiBufferSource buffer, float animation, float length, int light1,
		int light2, boolean far) {}

	private void renderChains(ChainConveyorBlockEntity be, PoseStack ms, SubmitNodeCollector collector) {
		be.prepareStats();
		if (be.connections.isEmpty() || be.connectionStats == null)
			return;

		float speed = Math.abs(be.getSpeed());
		float animation = 0;
		if (speed > 0) {
			float time = AnimationTickHolder.getRenderTime() / (360f / speed);
			time %= 1;
			if (time < 0)
				time += 1;
			animation = time - .5f;
		}

		for (BlockPos connection : be.connections) {
			if (!shouldRenderConnection(be.getBlockPos(), connection))
				continue;

			ConnectionStats stats = be.connectionStats.get(connection);
			if (stats == null)
				continue;

			Vec3 diff = stats.end()
				.subtract(stats.start());
			double yaw = Mth.RAD_TO_DEG * Mth.atan2(diff.x, diff.z);
			double pitch = Mth.RAD_TO_DEG * Mth.atan2(diff.y, diff.multiply(1, 0, 1)
				.length());

			BlockPos tilePos = be.getBlockPos();
			int light1 = getLight(be.getLevel(), tilePos);
			int light2 = getLight(be.getLevel(), tilePos.offset(connection));
			boolean far = Minecraft.getInstance().level == be.getLevel() && !Minecraft.getInstance()
				.gameRenderer.mainCamera().position()
					.closerThan(Vec3.atCenterOf(tilePos)
						.add(connection.getX() / 2f, connection.getY() / 2f, connection.getZ() / 2f), MIP_DISTANCE);

			if (!CreateVisualizationManager.supportsVisualization(be.getLevel())) {
				renderConnectionGuard(ms, collector, (float) yaw, light1);
				renderConnectionGuard(ms, collector, connection, (float) yaw + 180, light2);
			}

			float chainAnimation = animation;
			float length = stats.chainLength();

			renderChainStrand(ms, collector, tilePos, stats.start(), (float) yaw, (float) pitch, chainAnimation, length,
				light1, light2, far);

			Vec3 direction = stats.end()
				.subtract(stats.start());
			Vec3 origin = Vec3.atCenterOf(tilePos);
			Vec3 normal = direction.cross(new Vec3(0, 1, 0))
				.normalize();
			Vec3 offset = stats.start()
				.subtract(origin);
			Vec3 oppositeStart = origin.add(offset.add(normal.scale(-2 * normal.dot(offset))));
			renderChainStrand(ms, collector, tilePos, oppositeStart, (float) yaw, (float) pitch, -chainAnimation, length,
				light1, light2, far);
		}
	}

	private static boolean shouldRenderConnection(BlockPos pos, BlockPos connection) {
		BlockPos target = pos.offset(connection);
		if (pos.getX() != target.getX())
			return pos.getX() < target.getX();
		if (pos.getY() != target.getY())
			return pos.getY() < target.getY();
		return pos.getZ() <= target.getZ();
	}

	private static void renderChainStrand(PoseStack ms, SubmitNodeCollector collector, BlockPos tilePos,
		Vec3 start, float yaw, float pitch, float chainAnimation, float length, int light1, int light2, boolean far) {
		Vec3 startOffset = start.subtract(Vec3.atCenterOf(tilePos));

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.translate(startOffset.x, startOffset.y, startOffset.z);
		ms.mulPose(Axis.YP.rotationDegrees(yaw));
		ms.mulPose(Axis.XP.rotationDegrees(90 - pitch));
		ms.mulPose(Axis.YP.rotationDegrees(45));
		ms.translate(0, 8 / 16f, 0);
		ms.translate(-.5, -.5, -.5);
		ms.translate(.5, 0, .5);

		collector.submitCustomGeometry(ms, com.simibubi.create.foundation.render.RenderTypes.chain(CHAIN_LOCATION),
			(pose, consumer) -> renderChainGeometry(pose, consumer, chainAnimation, length, light1, light2, far));
		ms.popPose();
	}

	private void renderWheel(PoseStack ms, SubmitNodeCollector collector, int light) {
		List<BlockStateModelPart> wheel = getWheelModel();
		if (wheel.isEmpty())
			return;

		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), wheel,
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}

	private void renderConnectionGuard(PoseStack ms, SubmitNodeCollector collector, float yaw, int light) {
		renderConnectionGuard(ms, collector, BlockPos.ZERO, yaw, light);
	}

	private void renderConnectionGuard(PoseStack ms, SubmitNodeCollector collector, BlockPos offset, float yaw,
		int light) {
		List<BlockStateModelPart> guard = getGuardModel();
		if (guard.isEmpty())
			return;

		ms.pushPose();
		ms.translate(offset.getX(), offset.getY(), offset.getZ());
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(yaw));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), guard,
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private List<BlockStateModelPart> getShaftModel() {
		if (shaftModel != null)
			return shaftModel;
		BlockStateModelPart shaft = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.CHAIN_CONVEYOR_SHAFT);
		return shaftModel = shaft == null ? List.of() : List.of(shaft);
	}

	private List<BlockStateModelPart> getWheelModel() {
		if (wheelModel != null)
			return wheelModel;
		BlockStateModelPart wheel = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.CHAIN_CONVEYOR_WHEEL);
		return wheelModel = wheel == null ? List.of() : List.of(wheel);
	}

	private List<BlockStateModelPart> getGuardModel() {
		if (guardModel != null)
			return guardModel;
		BlockStateModelPart guard = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.CHAIN_CONVEYOR_GUARD);
		return guardModel = guard == null ? List.of() : List.of(guard);
	}

	public static void submitConnectionPreview(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		BlockPos source = ChainConveyorConnectionHandler.getPreviewSource();
		if (source == null)
			return;

		BlockPos target = ChainConveyorConnectionHandler.getPreviewTarget();
		int color = 0xFF000000 | ChainConveyorConnectionHandler.getPreviewColor();

		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			renderPreviewRings(pose, consumer, camera, source, color);

			if (target != null && !target.equals(source))
				renderPreviewRings(pose, consumer, camera, target, color);

			if (!ChainConveyorConnectionHandler.shouldShowPreviewLine())
				return;

			Vec3 aStart = ChainConveyorConnectionHandler.getPreviewChainAStart();
			Vec3 aEnd = ChainConveyorConnectionHandler.getPreviewChainAEnd();
			Vec3 bStart = ChainConveyorConnectionHandler.getPreviewChainBStart();
			Vec3 bEnd = ChainConveyorConnectionHandler.getPreviewChainBEnd();
			if (aStart != null && aEnd != null)
				renderPreviewStrip(pose, consumer, camera, aStart, aEnd, 1.25f / 16f, color);
			if (bStart != null && bEnd != null)
				renderPreviewStrip(pose, consumer, camera, bStart, bEnd, 1.25f / 16f, color);
		});
	}

	private static void renderPreviewRings(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera, BlockPos pos,
		int color) {
		for (int y = 0; y <= 1; y++) {
			Vec3 previous = ringPoint(pos, y, -22.5f);
			for (int i = 0; i < 8; i++) {
				Vec3 next = ringPoint(pos, y, 22.5f + i * 45f);
				renderPreviewStrip(pose, consumer, camera, previous, next, 1.25f / 16f, color);
				previous = next;
			}
		}
	}

	private static Vec3 ringPoint(BlockPos pos, int y, float angle) {
		double radians = Math.toRadians(angle);
		return Vec3.atBottomCenterOf(pos)
			.add(Math.sin(radians) * 1.25, .125 + y * .75, Math.cos(radians) * 1.25);
	}

	private static void renderPreviewStrip(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera, Vec3 worldStart,
		Vec3 worldEnd, float width, int color) {
		Vec3 start = worldStart.subtract(camera);
		Vec3 end = worldEnd.subtract(camera);
		Vec3 direction = end.subtract(start);
		if (direction.lengthSqr() < 1e-5)
			return;

		Vec3 normal = direction.cross(new Vec3(0, 1, 0));
		if (normal.lengthSqr() < 1e-5)
			normal = new Vec3(1, 0, 0);
		normal = normal.normalize()
			.scale(width);

		Vec3 a = start.add(normal);
		Vec3 b = end.add(normal);
		Vec3 c = end.subtract(normal);
		Vec3 d = start.subtract(normal);
		renderPreviewQuad(pose, consumer, a, b, c, d, color);

		Vec3 side = direction.cross(normal);
		if (side.lengthSqr() < 1e-5)
			return;
		side = side.normalize()
			.scale(width);

		a = start.add(side);
		b = end.add(side);
		c = end.subtract(side);
		d = start.subtract(side);
		renderPreviewQuad(pose, consumer, a, b, c, d, color);
	}

	private static void renderPreviewQuad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
		int color) {
		addPreviewVertex(pose, consumer, a, color);
		addPreviewVertex(pose, consumer, b, color);
		addPreviewVertex(pose, consumer, c, color);
		addPreviewVertex(pose, consumer, d, color);
		addPreviewVertex(pose, consumer, d, color);
		addPreviewVertex(pose, consumer, c, color);
		addPreviewVertex(pose, consumer, b, color);
		addPreviewVertex(pose, consumer, a, color);
	}

	private static void addPreviewVertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 vertex, int color) {
		consumer.addVertex(pose, (float) vertex.x, (float) vertex.y, (float) vertex.z)
			.setColor(color);
	}

	private static int getLight(Level level, BlockPos pos) {
		int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
		int skyLight = level.getBrightness(LightLayer.SKY, pos);
		return blockLight << 4 | skyLight << 20;
	}

	private static void renderChainGeometry(PoseStack.Pose pose, VertexConsumer consumer, float animation, float length,
		int light1, int light2, boolean far) {
		float radius = far ? 1f / 16f : 1.5f / 16f;
		float minV = far ? 0 : animation;
		float maxV = far ? 1 / 16f : length + minV;
		float minU = far ? 3 / 16f : 0;
		float maxU = far ? 4 / 16f : 3 / 16f;
		float uOffset = far ? 0f : 3 / 16f;

		renderPart(pose, consumer, length, 0.0F, radius, radius, 0.0F, -radius, 0.0F, 0.0F, -radius,
			minU, maxU, minV, maxV, uOffset, light1, light2);
	}

	private static void renderPart(PoseStack.Pose pose, VertexConsumer consumer, float maxY, float x0, float z0,
		float x1, float z1, float x2, float z2, float x3, float z3, float minU, float maxU, float minV, float maxV,
		float uOffset, int light1, int light2) {
		renderQuad(pose, consumer, 0, maxY, x0, z0, x3, z3, minU, maxU, minV, maxV, light1, light2);
		renderQuad(pose, consumer, 0, maxY, x3, z3, x0, z0, minU, maxU, minV, maxV, light1, light2);
		renderQuad(pose, consumer, 0, maxY, x1, z1, x2, z2, minU + uOffset, maxU + uOffset, minV, maxV, light1,
			light2);
		renderQuad(pose, consumer, 0, maxY, x2, z2, x1, z1, minU + uOffset, maxU + uOffset, minV, maxV, light1,
			light2);
	}

	private static void renderQuad(PoseStack.Pose pose, VertexConsumer consumer, float minY, float maxY, float minX,
		float minZ, float maxX, float maxZ, float minU, float maxU, float minV, float maxV, int light1, int light2) {
		addVertex(pose, consumer, maxY, minX, minZ, maxU, minV, light2);
		addVertex(pose, consumer, minY, minX, minZ, maxU, maxV, light1);
		addVertex(pose, consumer, minY, maxX, maxZ, minU, maxV, light1);
		addVertex(pose, consumer, maxY, maxX, maxZ, minU, minV, light2);
	}

	private static void addVertex(PoseStack.Pose pose, VertexConsumer consumer, float y, float x, float z, float u,
		float v, int light) {
		consumer.addVertex(pose, x, y, z)
			.setColor(1.0f, 1.0f, 1.0f, 1.0f)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}

	public int getViewDistance() {
		return 256;
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	protected SuperByteBuffer getRotatedModel(ChainConveyorBlockEntity be, BlockState state) {
		return null;
	}

	protected RenderType getRenderType(ChainConveyorBlockEntity be, BlockState state) {
		return null;
	}

}
