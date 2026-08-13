package com.simibubi.create.content.kinetics.belt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.Create;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.render.SpriteShiftEntry;
import net.createmod.ponder.api.client.level.PonderLevel;
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
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class BeltRenderer extends KineticBlockEntityRenderer<BeltBlockEntity> {
	private static final Identifier BELT_SCROLL_TEXTURE = Create.asResource("textures/block/belt_scroll.png");
	private static final Map<Integer, List<CachedBeltQuad>> HORIZONTAL_BELT_QUAD_CACHE = new ConcurrentHashMap<>();
	private static final ThreadLocal<List<CachedBeltQuad>> BUILDING_BELT_QUADS = new ThreadLocal<>();
	private List<BlockStateModelPart> pulleyModel;

	public BeltRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	public boolean shouldRenderOffScreen(BeltBlockEntity be) {
		return be.isController();
	}

	@Override
	protected BlockState getRenderedBlockState(BeltBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	@Override
	protected List<BlockStateModelPart> getRotatingModelParts(BeltBlockEntity be, BlockState renderedState) {
		return List.of();
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof BeltBlockEntity be))
			return;
		if (isInvalid(be))
			return;

		// The 26.2 Flywheel scrolling instance is not yet a reliable replacement for
		// the submitted belt surface. Keep the renderer-owned surface visible.
		renderPulley(be, kineticState.partialTicks, state.lightCoords, ms, collector);
		renderAnimatedBelt(be, kineticState.partialTicks, state.lightCoords, ms, collector);
		renderItems(be, kineticState.partialTicks, state.lightCoords, ms, collector);
	}

	private void renderPulley(BeltBlockEntity be, float partialTicks, int light, PoseStack ms,
		SubmitNodeCollector collector) {
		if (!be.hasPulley())
			return;

		List<BlockStateModelPart> pulley = getPulleyModel();
		if (pulley.isEmpty())
			return;

		Axis axis = getRotationAxisOf(be);
		ms.pushPose();
		ms.translate(.5, .5, .5);
		orientPulleyToAxis(ms, axis);
		ms.mulPose(com.mojang.math.Axis.YP.rotation(getAngleForBelt(be, axis, partialTicks)));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), pulley,
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private List<BlockStateModelPart> getPulleyModel() {
		if (pulleyModel != null)
			return pulleyModel;
		BlockStateModelPart pulley = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.BELT_PULLEY);
		return pulleyModel = pulley == null ? List.of() : List.of(pulley);
	}

	private static void orientPulleyToAxis(PoseStack ms, Axis axis) {
		switch (axis) {
			case X -> {
				ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
				ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
			}
			case Z -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
			case Y -> {
			}
		}
	}

	private static void renderAnimatedBelt(BeltBlockEntity be, float partialTicks, int light, PoseStack ms,
		SubmitNodeCollector collector) {
		if (!be.isController() || be.beltLength == 0)
			return;
		BlockState blockState = be.getBlockState();
		if (blockState.getValue(BeltBlock.SLOPE) != BeltSlope.HORIZONTAL)
			return;

		Direction facing = blockState.getValue(BeltBlock.HORIZONTAL_FACING);
		float speed = getBeltSpeed(be);
		float time = getRenderTime(be, partialTicks) * facing.getAxisDirection()
			.getStep();
		boolean alongX = facing.getAxis() == Axis.X;
		if (alongX)
			speed = -speed;
		float scroll = speed == 0 ? 0 : -time * speed / 480f;

		float topScroll = scroll;
		float bottomScroll = scroll + .5f;
		collector.submitCustomGeometry(ms, com.simibubi.create.foundation.render.RenderTypes.belt(BELT_SCROLL_TEXTURE),
			(pose, consumer) -> renderBeltQuads(pose, consumer, be.beltLength, facing, topScroll, bottomScroll, light));
	}

	private static float getBeltSpeed(BeltBlockEntity be) {
		BeltBlockEntity controller = be.getControllerBE();
		BeltBlockEntity source = controller != null ? controller : be;
		// Ponder assigns speeds directly and does not construct a live kinetic network.
		if (source.getLevel() instanceof PonderLevel)
			return source.getSpeed();
		if (!hasLiveSpeedSource(source))
			return 0;
		return source.getSpeed();
	}

	private static boolean hasLiveSpeedSource(BeltBlockEntity be) {
		if (be.isSource())
			return true;
		if (!be.hasSource())
			return false;
		if (be.getLevel() == null || be.source == null)
			return false;
		BlockEntity sourceBE = be.getLevel()
			.getBlockEntity(be.source);
		return sourceBE instanceof com.simibubi.create.content.kinetics.base.KineticBlockEntity kinetic
			&& kinetic.getTheoreticalSpeed() != 0;
	}

	private static float getAngleForBelt(BeltBlockEntity be, Axis axis, float partialTicks) {
		float time = getRenderTime(be, partialTicks);
		float offset = getRotationOffsetForPosition(be, be.getBlockPos(), axis);
		return ((time * getBeltSpeed(be) * 3f / 10 + offset) % 360) / 180 * (float) Math.PI;
	}

	private static void renderBeltQuads(PoseStack.Pose pose, VertexConsumer consumer, int beltLength, Direction facing,
		float topScroll, float bottomScroll, int light) {
		for (CachedBeltQuad quad : HORIZONTAL_BELT_QUAD_CACHE.computeIfAbsent(beltLength,
			BeltRenderer::buildHorizontalBeltQuads))
			renderCachedBeltQuad(pose, consumer, quad, facing, quad.bottom() ? bottomScroll : topScroll, light);
	}

	private static List<CachedBeltQuad> buildHorizontalBeltQuads(int beltLength) {
		List<CachedBeltQuad> quads = new ArrayList<>();
		BUILDING_BELT_QUADS.set(quads);
		try {
		for (int segment = 0; segment < beltLength; segment++) {
			BeltPart part = segment == 0 ? BeltPart.START : segment == beltLength - 1 ? BeltPart.END : BeltPart.MIDDLE;
				renderHorizontalBeltModel(null, null, part, false, 0, 0, segment);
				renderHorizontalBeltModel(null, null, part, true, 0, 0, segment);
			}
		} finally {
			BUILDING_BELT_QUADS.remove();
		}
		return List.copyOf(quads);
	}

	private static void renderCachedBeltQuad(PoseStack.Pose pose, VertexConsumer consumer, CachedBeltQuad quad,
		Direction facing, float scroll, int light) {
		float[] vertices = quad.vertices();
		float[] u = quad.u();
		float[] v = quad.v();
		float normalX = transformNormalX(facing, quad.normalX(), quad.normalZ());
		float normalZ = transformNormalZ(facing, quad.normalX(), quad.normalZ());
		for (int i = 0; i < 4; i++) {
			int vertexOffset = i * 3;
			float x = vertices[vertexOffset];
			float y = vertices[vertexOffset + 1];
			float z = vertices[vertexOffset + 2];
			addVertex(pose, consumer, transformX(facing, x, z), y, transformZ(facing, x, z),
				u[i], v[i] + scroll, light, normalX, quad.normalY(), normalZ);
		}
	}

	private static float transformX(Direction facing, float x, float z) {
		return switch (facing) {
			case NORTH -> 1 - x;
			case EAST -> z;
			case WEST -> 1 - z;
			default -> x;
		};
	}

	private static float transformZ(Direction facing, float x, float z) {
		return switch (facing) {
			case NORTH -> 1 - z;
			case EAST -> 1 - x;
			case WEST -> x;
			default -> z;
		};
	}

	private static float transformNormalX(Direction facing, float x, float z) {
		return switch (facing) {
			case NORTH -> -x;
			case EAST -> z;
			case WEST -> -z;
			default -> x;
		};
	}

	private static float transformNormalZ(Direction facing, float x, float z) {
		return switch (facing) {
			case NORTH -> -z;
			case EAST -> -x;
			case WEST -> x;
			default -> z;
		};
	}

	private static void renderHorizontalBeltModel(PoseStack.Pose pose, VertexConsumer consumer, BeltPart part,
		boolean bottom, float scroll, int light, int segment) {
		float uvOffset = segment;
		if (bottom) {
			switch (part) {
				case START -> {
					renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 1, 3, 1, 15, 5, 16,
						face(Direction.NORTH, 1, 0, 15, 1, 0),
						face(Direction.EAST, 0, 1, 2, 16, 90),
						face(Direction.WEST, 14, 1, 16, 16, 270),
						face(Direction.UP, 1, 1, 15, 16, 0),
						face(Direction.DOWN, 1, 1, 15, 16, 180));
					renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 3, 5, 2, 13, 6, 16,
						face(Direction.EAST, 12, 2, 13, 16, 90),
						face(Direction.WEST, 3, 2, 4, 16, 270),
						face(Direction.UP, 3, 2, 13, 16, 0));
				}
				case MIDDLE -> {
					renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 1, 3, 0, 15, 5, 16,
						face(Direction.EAST, 0, 0, 2, 16, 90),
						face(Direction.WEST, 14, 0, 16, 16, 270),
						face(Direction.UP, 1, 0, 15, 16, 0),
						face(Direction.DOWN, 1, 0, 15, 16, 180));
					renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 3, 5, 0, 13, 6, 16,
						face(Direction.EAST, 12, 0, 13, 16, 90),
						face(Direction.WEST, 3, 0, 4, 16, 270),
						face(Direction.UP, 3, 0, 13, 16, 0));
				}
				case END -> {
					renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 1, 3, 0, 15, 5, 15,
						face(Direction.EAST, 0, 0, 2, 15, 90),
						face(Direction.SOUTH, 1, 15, 15, 16, 180),
						face(Direction.WEST, 14, 0, 16, 15, 270),
						face(Direction.UP, 1, 0, 15, 15, 0),
						face(Direction.DOWN, 1, 0, 15, 15, 180));
					renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 3, 5, 0, 13, 6, 14,
						face(Direction.EAST, 3, 0, 4, 14, 90),
						face(Direction.WEST, 12, 0, 13, 14, 270),
						face(Direction.UP, 3, 0, 13, 14, 0));
				}
			}
			return;
		}

		switch (part) {
			case START -> {
				renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 3, 10, 2, 13, 11, 16,
					face(Direction.EAST, 3, 0, 4, 14, 270),
					face(Direction.WEST, 12, 0, 13, 14, 90),
					face(Direction.DOWN, 3, 0, 13, 14, 0));
				renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 1, 11, 1, 15, 13, 16,
					face(Direction.NORTH, 1, 15, 15, 16, 0),
					face(Direction.EAST, 0, 0, 2, 15, 270),
					face(Direction.WEST, 14, 0, 16, 15, 90),
					face(Direction.UP, 1, 0, 15, 15, 180),
					face(Direction.DOWN, 1, 0, 15, 15, 0));
				renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 1.1f, 4, -.05f, 14.9f, 12, 1.95f,
					face(Direction.NORTH, 1, 0, 15, 8, 0),
					face(Direction.EAST, 0, 0, 2, 8, 0),
					face(Direction.SOUTH, 1, 0, 15, 8, 0),
					face(Direction.WEST, 14, 0, 16, 8, 0),
					face(Direction.UP, 1, 15, 15, 16, 180),
					face(Direction.DOWN, 1, 8, 15, 9, 180));
				renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 3, 6, 2, 13, 10, 3,
					face(Direction.EAST, 3, 6, 4, 10, 0),
					face(Direction.SOUTH, 3, 2, 13, 6, 0),
					face(Direction.WEST, 12, 6, 13, 10, 0));
			}
			case MIDDLE -> {
				renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 1, 11, 0, 15, 13, 16,
					face(Direction.EAST, 0, 0, 2, 16, 270),
					face(Direction.WEST, 14, 0, 16, 16, 90),
					face(Direction.UP, 1, 0, 15, 16, 180),
					face(Direction.DOWN, 1, 0, 15, 16, 0));
				renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 3, 10, 0, 13, 11, 16,
					face(Direction.EAST, 12, 0, 13, 16, 270),
					face(Direction.WEST, 3, 0, 4, 16, 90),
					face(Direction.DOWN, 3, 0, 13, 16, 0));
			}
			case END -> {
				renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 1, 11, 0, 15, 13, 15,
					face(Direction.EAST, 0, 1, 2, 16, 270),
					face(Direction.SOUTH, 1, 0, 15, 1, 180),
					face(Direction.WEST, 14, 1, 16, 16, 90),
					face(Direction.UP, 1, 1, 15, 16, 180),
					face(Direction.DOWN, 1, 1, 15, 16, 0));
				renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 3, 10, 0, 13, 11, 14,
					face(Direction.EAST, 3, 2, 4, 16, 270),
					face(Direction.WEST, 12, 2, 13, 16, 90),
					face(Direction.DOWN, 3, 2, 13, 16, 0));
				renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 1.1f, 4, 14.05f, 14.9f, 12, 16.05f,
					face(Direction.NORTH, 1, 8, 15, 16, 180),
					face(Direction.EAST, 0, 8, 2, 16, 180),
					face(Direction.SOUTH, 1, 8, 15, 16, 180),
					face(Direction.WEST, 14, 8, 16, 16, 180),
					face(Direction.UP, 1, 0, 15, 1, 180),
					face(Direction.DOWN, 1, 7, 15, 8, 180));
				renderBox(pose, consumer, light, scroll, uvOffset, segment, bottom, 3, 6, 13, 13, 10, 14,
					face(Direction.NORTH, 3, 10, 13, 14, 180),
					face(Direction.EAST, 12, 10, 13, 14, 180),
					face(Direction.WEST, 3, 10, 4, 14, 180));
			}
		}
	}

	private static FaceSpec face(Direction direction, float u1, float v1, float u2, float v2, int rotation) {
		return new FaceSpec(direction, u1 / 16f, v1 / 16f, u2 / 16f, v2 / 16f, rotation);
	}

	private static void renderBox(PoseStack.Pose pose, VertexConsumer consumer, int light, float scroll, float uvOffset,
		float zOffset, boolean bottom, float x1, float y1, float z1, float x2, float y2, float z2, FaceSpec... faces) {
		x1 /= 16f;
		y1 /= 16f;
		z1 = z1 / 16f + zOffset;
		x2 /= 16f;
		y2 /= 16f;
		z2 = z2 / 16f + zOffset;
		for (FaceSpec face : faces)
			renderBoxFace(pose, consumer, light, scroll + uvOffset, x1, y1, z1, x2, y2, z2, face, bottom);
	}

	private static void renderBoxFace(PoseStack.Pose pose, VertexConsumer consumer, int light, float scroll, float x1,
		float y1, float z1, float x2, float y2, float z2, FaceSpec face, boolean bottom) {
		float[][] vertices = switch (face.direction()) {
			case UP -> new float[][]{{x1, y2, z2}, {x2, y2, z2}, {x2, y2, z1}, {x1, y2, z1}};
			case DOWN -> new float[][]{{x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}, {x1, y1, z2}};
			case NORTH -> new float[][]{{x1, y1, z1}, {x2, y1, z1}, {x2, y2, z1}, {x1, y2, z1}};
			case SOUTH -> new float[][]{{x2, y1, z2}, {x1, y1, z2}, {x1, y2, z2}, {x2, y2, z2}};
			case WEST -> new float[][]{{x1, y1, z2}, {x1, y1, z1}, {x1, y2, z1}, {x1, y2, z2}};
			case EAST -> new float[][]{{x2, y1, z1}, {x2, y1, z2}, {x2, y2, z2}, {x2, y2, z1}};
		};
		float[][] uvs = rotatedUvs(face, scroll, vertices);
		float normalX = face.direction()
			.getStepX();
		float normalY = face.direction()
			.getStepY();
		float normalZ = face.direction()
			.getStepZ();
		List<CachedBeltQuad> buildingQuads = BUILDING_BELT_QUADS.get();
		if (buildingQuads != null) {
			float[] cachedVertices = new float[12];
			float[] cachedU = new float[4];
			float[] cachedV = new float[4];
			for (int i = 0; i < 4; i++) {
				int vertexOffset = i * 3;
				cachedVertices[vertexOffset] = vertices[i][0];
				cachedVertices[vertexOffset + 1] = vertices[i][1];
				cachedVertices[vertexOffset + 2] = vertices[i][2];
				cachedU[i] = uvs[i][0];
				cachedV[i] = uvs[i][1];
			}
			buildingQuads.add(new CachedBeltQuad(cachedVertices, cachedU, cachedV, normalX, normalY, normalZ, bottom));
			return;
		}
		for (int i = 0; i < 4; i++)
			addVertex(pose, consumer, vertices[i][0], vertices[i][1], vertices[i][2], uvs[i][0], uvs[i][1], light,
				normalX, normalY, normalZ);
	}

	private static float[][] rotatedUvs(FaceSpec face, float scroll, float[][] vertices) {
		float[][] uvs = new float[][]{
			{face.u1(), face.v1()},
			{face.u2(), face.v1()},
			{face.u2(), face.v2()},
			{face.u1(), face.v2()}
		};
		int steps = Math.floorMod(face.rotation() / 90, 4);
		float[][] rotated = new float[4][2];
		for (int i = 0; i < 4; i++) {
			float[] uv = uvs[Math.floorMod(i - steps, 4)];
			rotated[i][0] = uv[0];
			rotated[i][1] = uv[1];
		}

		float minY = Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE;
		float maxZ = -Float.MAX_VALUE;
		for (float[] vertex : vertices) {
			minY = Math.min(minY, vertex[1]);
			maxY = Math.max(maxY, vertex[1]);
			minZ = Math.min(minZ, vertex[2]);
			maxZ = Math.max(maxZ, vertex[2]);
		}

		boolean useY = maxY - minY > maxZ - minZ;
		int axisIndex = useY ? 1 : 2;
		int first = -1;
		int second = -1;
		for (int i = 0; i < 4 && first == -1; i++) {
			for (int j = i + 1; j < 4; j++) {
				if (Math.abs(vertices[i][axisIndex] - vertices[j][axisIndex]) > 1 / 1024f) {
					first = i;
					second = j;
					break;
				}
			}
		}

		if (first != -1) {
			float coordDelta = vertices[second][axisIndex] - vertices[first][axisIndex];
			float vDelta = rotated[second][1] - rotated[first][1];
			float direction = coordDelta == 0 || vDelta == 0 ? 1 : Math.signum(vDelta / coordDelta);
			for (int i = 0; i < 4; i++)
				rotated[i][1] = scroll + vertices[i][axisIndex] * direction;
			return rotated;
		}

		for (int i = 0; i < 4; i++)
			rotated[i][1] += scroll;
		return rotated;
	}

	private record FaceSpec(Direction direction, float u1, float v1, float u2, float v2, int rotation) {
	}

	private record CachedBeltQuad(float[] vertices, float[] u, float[] v, float normalX, float normalY, float normalZ,
								  boolean bottom) {
	}

	private static void renderItems(BeltBlockEntity be, float partialTicks, int light, PoseStack ms,
		SubmitNodeCollector collector) {
		if (!be.isController() || be.beltLength == 0)
			return;
		BeltInventory inventory = be.getInventory();
		if (inventory == null)
			return;

		ms.pushPose();
		Direction beltFacing = be.getBeltFacing();
		Vec3i directionVec = beltFacing.getUnitVec3i();
		Vec3 beltStartOffset = Vec3.atLowerCornerOf(directionVec)
			.scale(-.5)
			.add(.5, 15 / 16f, .5);
		ms.translate(beltStartOffset.x, beltStartOffset.y, beltStartOffset.z);

		for (TransportedItemStack transported : inventory.getTransportedItems())
			renderTransportedItem(be, partialTicks, light, ms, collector, beltFacing, directionVec, transported);
		if (inventory.getLazyClientItem() != null)
			renderTransportedItem(be, partialTicks, light, ms, collector, beltFacing, directionVec,
				inventory.getLazyClientItem());

		ms.popPose();
	}

	private static void renderTransportedItem(BeltBlockEntity be, float partialTicks, int light, PoseStack ms,
		SubmitNodeCollector collector, Direction beltFacing, Vec3i directionVec, TransportedItemStack transported) {
		if (transported.stack.isEmpty())
			return;

		float offset = Mth.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
		float sideOffset = Mth.lerp(partialTicks, transported.prevSideOffset, transported.sideOffset);
		if (be.getSpeed() == 0) {
			offset = transported.beltPosition;
			sideOffset = transported.sideOffset;
		}

		Vec3 offsetVec = Vec3.atLowerCornerOf(directionVec)
			.scale(offset);
		ms.pushPose();
		ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);
		ms.translate(0, 3 / 32f, 0);

		boolean alongX = beltFacing.getClockWise()
			.getAxis() == Axis.X;
		if (!alongX)
			sideOffset *= -1;
		ms.translate(alongX ? sideOffset : 0, 0, alongX ? 0 : sideOffset);

		boolean renderUpright = BeltHelper.isItemUpright(transported.stack);
		if (renderUpright) {
			Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.mainCamera()
				.position();
			Vec3 vectorForOffset = BeltHelper.getVectorForOffset(be, offset);
			Vec3 diff = vectorForOffset.subtract(cameraPosition);
			float yRot = (float) (Mth.atan2(diff.x, diff.z) + Math.PI);
			ms.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
			ms.translate(0, 0, 1 / 16f);
		}

		renderItem(ms, collector, light, transported.stack, transported.angle, renderUpright);
		ms.popPose();
	}

	private static void renderItem(PoseStack ms, SubmitNodeCollector collector, int light, ItemStack itemStack,
		int angle, boolean renderUpright) {
		if (itemStack.isEmpty())
			return;

		int count = Mth.log2(itemStack.getCount()) / 2;
		Random random = new Random(0);

		ms.pushPose();
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));
		boolean box = PackageItem.isPackage(itemStack);

		for (int i = 0; i <= count; i++) {
			ms.pushPose();
			if (i > 0)
				ms.translate(random.nextFloat() * .0625f * i, 0, random.nextFloat() * .0625f * i);

			if (box) {
				// The belt renderer already raises all transported items by 3/32.
				// Add the remaining 5/32 to match the original package height of 4/16.
				ms.translate(0, 5 / 32f, 0);
				ms.scale(1.5f, 1.5f, 1.5f);
			} else {
				ms.scale(.5f, .5f, .5f);
			}

			if (!box && !renderUpright) {
				ms.translate(0, -3 / 16f, 0);
				ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
			}

			ItemStackRenderState itemState = new ItemStackRenderState();
			Minecraft.getInstance()
				.getItemModelResolver()
				.updateForTopItem(itemState, itemStack, ItemDisplayContext.FIXED, null, null, 0);
			itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();

			if (!renderUpright)
				ms.translate(0, 1 / 16d, 0);
			else
				ms.translate(0, 0, -1 / 16f);
		}

		ms.popPose();
	}

	private static void renderQuad(PoseStack.Pose pose, VertexConsumer consumer,
		float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4,
		float y4, float z4, float minU, float minV, float maxU, float maxV, int light, float normalX, float normalY,
		float normalZ) {
		addVertex(pose, consumer, x1, y1, z1, minU, minV, light, normalX, normalY, normalZ);
		addVertex(pose, consumer, x2, y2, z2, maxU, minV, light, normalX, normalY, normalZ);
		addVertex(pose, consumer, x3, y3, z3, maxU, maxV, light, normalX, normalY, normalZ);
		addVertex(pose, consumer, x4, y4, z4, minU, maxV, light, normalX, normalY, normalZ);
	}

	private static void addVertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u,
		float v, int light, float normalX, float normalY, float normalZ) {
		consumer.addVertex(pose, x, y, z)
			.setColor(1.0f, 1.0f, 1.0f, 1.0f)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, normalX, normalY, normalZ);
	}

	public static PartialModel getBeltPartial(boolean diagonal, boolean start, boolean end, boolean bottom) {
		if (diagonal) {
			if (start)
				return AllPartialModels.BELT_DIAGONAL_START;
			if (end)
				return AllPartialModels.BELT_DIAGONAL_END;
			return AllPartialModels.BELT_DIAGONAL_MIDDLE;
		} else if (bottom) {
			if (start)
				return AllPartialModels.BELT_START_BOTTOM;
			if (end)
				return AllPartialModels.BELT_END_BOTTOM;
			return AllPartialModels.BELT_MIDDLE_BOTTOM;
		} else {
			if (start)
				return AllPartialModels.BELT_START;
			if (end)
				return AllPartialModels.BELT_END;
			return AllPartialModels.BELT_MIDDLE;
		}
	}

	public static SpriteShiftEntry getSpriteShiftEntry(DyeColor color, boolean diagonal, boolean bottom) {
		if (color != null) {
			return (diagonal ? AllSpriteShifts.DYED_DIAGONAL_BELTS
				: bottom ? AllSpriteShifts.DYED_OFFSET_BELTS : AllSpriteShifts.DYED_BELTS).get(color);
		} else
			return diagonal ? AllSpriteShifts.BELT_DIAGONAL
				: bottom ? AllSpriteShifts.BELT_OFFSET : AllSpriteShifts.BELT;
	}

}
