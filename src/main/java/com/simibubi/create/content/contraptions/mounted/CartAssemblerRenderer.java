package com.simibubi.create.content.contraptions.mounted;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CartAssemblerRenderer extends SmartBlockEntityRenderer<CartAssemblerBlockEntity> {

	private static final float ICON_SCALE = .34f;
	private static final float ICON_Z_OFFSET = 1 / 512f;
	private static final int ICON_COLOR = 0xFFDDDDDD;

	public CartAssemblerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(CartAssemblerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new CartAssemblerRenderState();
	}

	@Override
	public void extractRenderState(CartAssemblerBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof CartAssemblerRenderState cartState) {
			cartState.blockEntity = be;
			cartState.partialTicks = partialTicks;
			cartState.blockState = be.getBlockState();
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof CartAssemblerRenderState cartState))
			return;
		CartAssemblerBlockEntity be = cartState.blockEntity;
		if (be == null || isInvalid(be))
			return;
		submitBehaviours(be, cartState.partialTicks, ms, collector, state.lightCoords);
		if (be.movementMode == null)
			return;

		ValueBoxTransform transform = be.getMovementModeSlot();
		if (!(transform instanceof ValueBoxTransform.Sided sided))
			return;

		HitResult hitResult = Minecraft.getInstance().hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit))
			return;
		var pos = be.getBlockPos();
		if (!blockHit.getBlockPos()
			.equals(pos))
			return;

		Direction side = blockHit.getDirection();
		if (side.getAxis()
			.isVertical())
			return;

		sided.fromSide(side);
		BlockState blockState = cartState.blockState;
		if (!sided.shouldRender(be.getLevel(), pos, blockState))
			return;

		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(pos));
		if (!sided.testHit(be.getLevel(), pos, blockState, localHit))
			return;

		Vec3 offset = sided.getLocalOffset(be.getLevel(), pos, blockState);
		if (offset == null)
			return;

		Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
		CartAssemblerBlockEntity.CartMovementMode mode = be.movementMode.get();

		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d,
			offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateToFace(ms, side);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> renderCartModeIcon(pose, consumer, mode));
		ms.popPose();
	}

	private static void renderCartModeIcon(Pose pose, VertexConsumer consumer,
		CartAssemblerBlockEntity.CartMovementMode mode) {
		String[] pixels = switch (mode) {
			case ROTATE -> CART_ROTATE_ICON;
			case ROTATE_PAUSED -> CART_ROTATE_PAUSED_ICON;
			case ROTATION_LOCKED -> CART_ROTATE_LOCKED_ICON;
		};
		for (int y = 0; y < pixels.length; y++) {
			String row = pixels[y];
			for (int x = 0; x < row.length(); x++) {
				if (row.charAt(x) != '#')
					continue;
				flatScaledPixelXY(pose, consumer, x, 15 - y, ICON_SCALE, ICON_Z_OFFSET, ICON_COLOR);
			}
		}
	}

	private static void flatScaledPixelXY(Pose pose, VertexConsumer consumer, int x, int y, float scale, float z,
		int color) {
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, (x * pixel - .5f) * scale, (y * pixel - .5f) * scale,
			((x + 1) * pixel - .5f) * scale, ((y + 1) * pixel - .5f) * scale, z, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1,
		float z, int color) {
		consumer.addVertex(pose, x0, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x0, y0, z).setColor(color);
	}

	private static void rotateToFace(PoseStack ms, Direction face) {
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

	private static class CartAssemblerRenderState extends BlockEntityRenderState {
		private CartAssemblerBlockEntity blockEntity;
		private float partialTicks;
		private BlockState blockState;
	}

	private static final String[] CART_ROTATE_ICON = {
		"................",
		"................",
		"........#.......",
		"........#.......",
		"........#.###...",
		"......#.#.......",
		"...#.#..#..###..",
		"...##...#...##..",
		"...###..#..#.#..",
		"..........#.....",
		"....######......",
		"................",
		"........#.......",
		"........#.......",
		"................",
		"................"
	};

	private static final String[] CART_ROTATE_PAUSED_ICON = {
		"................",
		"................",
		"........#.......",
		"........#.......",
		"........#.#.#...",
		"......#.#.......",
		"...#.#..#..###..",
		"...##...#...##..",
		"...###..#..#.#..",
		"..........#.....",
		"....#.#.#.......",
		"................",
		"........#.......",
		"........#.......",
		"................",
		"................"
	};

	private static final String[] CART_ROTATE_LOCKED_ICON = {
		"................",
		"................",
		"........#.......",
		"........#.......",
		"........#.#.#...",
		"......#.#.......",
		"...#.#..#....#..",
		"...##...#.#.#...",
		"...###..#.##....",
		"..........###...",
		"....#.#.#.......",
		"................",
		"........#.......",
		"........#.......",
		"................",
		"................"
	};
}
