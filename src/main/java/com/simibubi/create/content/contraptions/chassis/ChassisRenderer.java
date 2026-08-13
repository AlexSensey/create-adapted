package com.simibubi.create.content.contraptions.chassis;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ChassisRenderer extends SmartBlockEntityRenderer<ChassisBlockEntity> {

	private static final long DIGIT_LINGER_MS = 1200;
	private static final Map<BlockPos, Long> LAST_SEEN = new HashMap<>();
	private static final float FRAME_WIDTH = 6 / 16f;
	private static final float FRAME_HEIGHT = 6 / 16f;
	private static final float CORNER_LENGTH = 2 / 16f;
	private static final float CORNER_THICKNESS = 1 / 16f;
	private static final float DIGIT_HEIGHT = .105f;
	private static final float DIGIT_WIDTH = .05f;
	private static final float DIGIT_GAP = .01f;
	private static final int COLOR = 0xFFFFFFFF;

	public ChassisRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ChassisBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new ChassisRenderState();
	}

	@Override
	public void extractRenderState(ChassisBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof ChassisRenderState chassisState) {
			chassisState.blockEntity = be;
			chassisState.partialTicks = partialTicks;
			chassisState.blockState = be.getBlockState();
			chassisState.range = be.currentlySelectedRange;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof ChassisRenderState chassisState))
			return;
		ChassisBlockEntity be = chassisState.blockEntity;
		if (be == null || isInvalid(be))
			return;
		submitBehaviours(be, chassisState.partialTicks, ms, collector, state.lightCoords);

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		HitResult hitResult = mc.hitResult;
		if (player == null)
			return;
		if (!AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem()))
			return;
		if (!(chassisState.blockState.getBlock() instanceof AbstractChassisBlock))
			return;

		BlockPos pos = be.getBlockPos();
		boolean active = hitResult instanceof BlockHitResult blockHit && blockHit.getBlockPos()
			.equals(pos);
		long now = System.currentTimeMillis();
		if (active)
			LAST_SEEN.put(pos, now);
		if (LAST_SEEN.size() > 64)
			LAST_SEEN.entrySet()
				.removeIf(entry -> now - entry.getValue() > DIGIT_LINGER_MS);

		Long lastSeen = LAST_SEEN.get(pos);
		if (lastSeen == null)
			return;
		long elapsed = now - lastSeen;
		if (!active && elapsed > DIGIT_LINGER_MS) {
			LAST_SEEN.remove(pos);
			return;
		}

		int alpha = active ? 255 : Math.max(0, 255 - (int) (elapsed * 255 / DIGIT_LINGER_MS));
		String text = Integer.toString(Math.max(1, chassisState.range));
		for (Direction side : Iterate.directions) {
			submitSideOverlay(ms, collector, side, text, active, alpha);
		}
	}

	private static void submitSideOverlay(PoseStack ms, SubmitNodeCollector collector, Direction side, String text,
		boolean active, int alpha) {
		Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
		ms.pushPose();
		ms.translate(.5 + normal.x * (.5 + 1 / 512d), .5 + normal.y * (.5 + 1 / 512d),
			.5 + normal.z * (.5 + 1 / 512d));
		rotateToFace(ms, side);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> renderOverlay(pose, consumer, text, active, alpha));
		ms.popPose();
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

	private static void renderOverlay(Pose pose, VertexConsumer consumer, String text, boolean active, int alpha) {
		if (active)
			renderCorners(pose, consumer);
		renderDigits(pose, consumer, text, alpha);
	}

	private static void renderCorners(Pose pose, VertexConsumer consumer) {
		float x0 = -FRAME_WIDTH / 2;
		float x1 = FRAME_WIDTH / 2;
		float y0 = -FRAME_HEIGHT / 2;
		float y1 = FRAME_HEIGHT / 2;
		float l = CORNER_LENGTH;
		float t = CORNER_THICKNESS;

		quad(pose, consumer, x0, y1 - t, x0 + l, y1, COLOR);
		quad(pose, consumer, x0, y1 - l, x0 + t, y1, COLOR);
		quad(pose, consumer, x1 - l, y1 - t, x1, y1, COLOR);
		quad(pose, consumer, x1 - t, y1 - l, x1, y1, COLOR);
		quad(pose, consumer, x0, y0, x0 + l, y0 + t, COLOR);
		quad(pose, consumer, x0, y0, x0 + t, y0 + l, COLOR);
		quad(pose, consumer, x1 - l, y0, x1, y0 + t, COLOR);
		quad(pose, consumer, x1 - t, y0, x1, y0 + l, COLOR);
	}

	private static void renderDigits(Pose pose, VertexConsumer consumer, String text, int alpha) {
		float scale = text.length() <= 1 ? 1.16f : 1;
		float digitHeight = DIGIT_HEIGHT * scale;
		float digitWidth = DIGIT_WIDTH * scale;
		float gap = DIGIT_GAP * scale;
		float totalWidth = text.length() * digitWidth + Math.max(0, text.length() - 1) * gap;
		float x = -totalWidth / 2;
		int color = (alpha << 24) | 0xFFFFFF;

		for (int i = 0; i < text.length(); i++) {
			int digit = text.charAt(i) - '0';
			if (digit >= 0 && digit <= 9)
				renderDigit(pose, consumer, digit, x, -digitHeight / 2, digitWidth, digitHeight, 1 / 512f, color);
			x += digitWidth + gap;
		}
	}

	private static void renderDigit(Pose pose, VertexConsumer consumer, int digit, float x, float y, float w, float h,
		float z, int color) {
		if (digit == 1) {
			float t = Math.min(w, h) * .28f;
			float midX = x + w / 2;
			float halfT = t / 2;
			float midY = y + h / 2;
			quad(pose, consumer, midX - halfT, midY + halfT, midX + halfT, y + h - t, z, color);
			quad(pose, consumer, midX - halfT, y + t, midX + halfT, midY - halfT, z, color);
			return;
		}

		boolean[] segments = switch (digit) {
			case 0 -> new boolean[] { true, true, true, true, true, true, false };
			case 2 -> new boolean[] { true, true, false, true, true, false, true };
			case 3 -> new boolean[] { true, true, true, true, false, false, true };
			case 4 -> new boolean[] { false, true, true, false, false, true, true };
			case 5 -> new boolean[] { true, false, true, true, false, true, true };
			case 6 -> new boolean[] { true, false, true, true, true, true, true };
			case 7 -> new boolean[] { true, true, true, false, false, false, false };
			case 8 -> new boolean[] { true, true, true, true, true, true, true };
			case 9 -> new boolean[] { true, true, true, true, false, true, true };
			default -> new boolean[7];
		};

		float t = Math.min(w, h) * .28f;
		float halfT = t / 2;
		float midY = y + h / 2;
		float right = x + w;
		float top = y + h;

		if (segments[0])
			quad(pose, consumer, x + t, top - t, right - t, top, z, color);
		if (segments[1])
			quad(pose, consumer, right - t, midY + halfT, right, top - t, z, color);
		if (segments[2])
			quad(pose, consumer, right - t, y + t, right, midY - halfT, z, color);
		if (segments[3])
			quad(pose, consumer, x + t, y, right - t, y + t, z, color);
		if (segments[4])
			quad(pose, consumer, x, y + t, x + t, midY - halfT, z, color);
		if (segments[5])
			quad(pose, consumer, x, midY + halfT, x + t, top - t, z, color);
		if (segments[6])
			quad(pose, consumer, x + t, midY - halfT, right - t, midY + halfT, z, color);
	}

	private static void quad(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1, int color) {
		quad(pose, consumer, x0, y0, x1, y1, 0, color);
	}

	private static void quad(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1, float z,
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

	private static class ChassisRenderState extends BlockEntityRenderState {
		private ChassisBlockEntity blockEntity;
		private float partialTicks;
		private BlockState blockState;
		private int range;
	}
}
