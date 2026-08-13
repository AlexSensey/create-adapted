package com.simibubi.create.content.contraptions.pulley;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class PulleyRenderer extends AbstractPulleyRenderer<PulleyBlockEntity> {

	private static final String[][] MOVEMENT_MODE_PIXELS = {
		{
			"................",
			"................",
			"................",
			"....###.........",
			"...####.........",
			"...#.##.........",
			"...###..........",
			"........#.......",
			".......##.......",
			"..........###...",
			".........####...",
			".........#.##...",
			".........###....",
			"................",
			"................",
			"................",
		},
		{
			"................",
			"................",
			"................",
			"....###.........",
			"...####.........",
			"...#.##.........",
			"...###..........",
			"........#.......",
			".......##.......",
			"..........#.#...",
			".........#......",
			"............#...",
			".........#.#....",
			"................",
			"................",
			"................",
		},
		{
			"................",
			"................",
			"................",
			"....#.#.........",
			"...#............",
			"......#.........",
			"...#.#..........",
			"........#.......",
			".......##.......",
			"..........#.#...",
			".........#......",
			"............#...",
			".........#.#....",
			"................",
			"................",
			"................",
		},
	};

	public PulleyRenderer(BlockEntityRendererProvider.Context context) {
		super(context, CreateStandaloneModels.ROPE_HALF, CreateStandaloneModels.ROPE_HALF_MAGNET);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);

		if (!(state instanceof PulleyRenderState pulleyState))
			return;
		if (!(pulleyState.blockEntity instanceof PulleyBlockEntity pulley))
			return;
		submitMovementModeOverlay(pulley, ms, collector);
	}

	@Override
	protected Axis getShaftAxis(PulleyBlockEntity be) {
		return be.getBlockState()
			.getValue(PulleyBlock.HORIZONTAL_AXIS);
	}

	@Override
	protected StandaloneModelKey<BlockStateModelPart> getCoil() {
		return CreateStandaloneModels.ROPE_COIL;
	}

	@Override
	protected StandaloneModelKey<BlockStateModelPart> getRope() {
		return CreateStandaloneModels.ROPE;
	}

	@Override
	protected StandaloneModelKey<BlockStateModelPart> getMagnet() {
		return CreateStandaloneModels.ROPE_PULLEY_MAGNET;
	}

	@Override
	protected float getOffset(PulleyBlockEntity be, float partialTicks) {
		return getBlockEntityOffset(partialTicks, be);
	}

	@Override
	protected boolean isRunning(PulleyBlockEntity be) {
		return isPulleyRunning(be);
	}

	public static boolean isPulleyRunning(PulleyBlockEntity be) {
		return be.running || be.mirrorParent != null || be.isVirtual();
	}

	public static float getBlockEntityOffset(float partialTicks, PulleyBlockEntity blockEntity) {
		float offset = blockEntity.getInterpolatedOffset(partialTicks);

		AbstractContraptionEntity attachedContraption = blockEntity.getAttachedContraption();
		if (attachedContraption != null) {
			PulleyContraption c = (PulleyContraption) attachedContraption.getContraption();
			double entityPos = Mth.lerp(partialTicks, attachedContraption.yOld, attachedContraption.getY());
			offset = (float) -(entityPos - c.anchor.getY() - c.getInitialOffset());
		}

		return offset;
	}

	private static void submitMovementModeOverlay(PulleyBlockEntity pulley, PoseStack ms, SubmitNodeCollector collector) {
		BlockState state = pulley.getBlockState();
		ValueBoxTransform slot = pulley.getMovementModeSlot();
		if (!(slot instanceof ValueBoxTransform.Sided sided))
			return;
		sided.fromSide(Direction.UP);
		if (!slot.shouldRender(pulley.getLevel(), pulley.getBlockPos(), state))
			return;
		if (!shouldRenderMovementModeOverlay(pulley, state, slot))
			return;

		Vec3 offset = slot.getLocalOffset(pulley.getLevel(), pulley.getBlockPos(), state);
		if (offset == null)
			return;

		ms.pushPose();
		ms.translate(offset.x, offset.y + 1 / 32d + 1 / 512d, offset.z);
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(270));
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			renderModeFrame(pose, consumer);
			renderMovementModeIcon(pose, consumer, pulley.getMovementModeIconIndex());
		});
		ms.popPose();
	}

	private static boolean shouldRenderMovementModeOverlay(PulleyBlockEntity pulley, BlockState state,
		ValueBoxTransform slot) {
		HitResult hitResult = Minecraft.getInstance().hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit))
			return false;
		if (!blockHit.getBlockPos()
			.equals(pulley.getBlockPos()))
			return false;
		if (blockHit.getDirection() != Direction.UP)
			return false;

		Vec3 localHit = blockHit.getLocation()
			.subtract(Vec3.atLowerCornerOf(pulley.getBlockPos()));
		return slot.testHit(pulley.getLevel(), pulley.getBlockPos(), state, localHit);
	}

	private static void renderModeFrame(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		renderThreePixelCorner(pose, consumer, 5, 5, 1, 1, color);
		renderThreePixelCorner(pose, consumer, 10, 5, -1, 1, color);
		renderThreePixelCorner(pose, consumer, 5, 10, 1, -1, color);
		renderThreePixelCorner(pose, consumer, 10, 10, -1, -1, color);
	}

	private static void renderMovementModeIcon(Pose pose, VertexConsumer consumer, int mode) {
		int color = 0xFFFFFFFF;
		String[] pixels = MOVEMENT_MODE_PIXELS[Mth.clamp(mode, 0, MOVEMENT_MODE_PIXELS.length - 1)];
		for (int y = 0; y < pixels.length; y++) {
			String row = pixels[y];
			for (int x = 0; x < row.length(); x++) {
				if (row.charAt(x) == '#')
					flatScaledPixelXY(pose, consumer, x, 15 - y, .16f, 1 / 512f, color);
			}
		}
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

	private static void flatScaledPixelXY(Pose pose, VertexConsumer consumer, int x, int y, float scale, float z,
		int color) {
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, (x * pixel - .5f) * scale, (y * pixel - .5f) * scale,
			((x + 1) * pixel - .5f) * scale, ((y + 1) * pixel - .5f) * scale, z, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1, float z,
		int color) {
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

}
