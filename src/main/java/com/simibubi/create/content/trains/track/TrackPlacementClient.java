package com.simibubi.create.content.trains.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.data.Pair;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Client-only track placement preview. Common placement math stays in {@link TrackPlacement}. */
@EventBusSubscriber(Dist.CLIENT)
public final class TrackPlacementClient {
	private TrackPlacementClient() {
	}

	private static final LerpedFloat animation = LerpedFloat.linear().startWithValue(0);
	private static int lastLineCount;
	private static BlockPos hintPos;
	private static int hintAngle;
	private static Couple<List<BlockPos>> hints;
	private static final List<BlockPos> previewValidHints = new ArrayList<>();
	private static final List<BlockPos> previewInvalidHints = new ArrayList<>();
	private static final List<PreviewLine> previewLines = new ArrayList<>();

	private record PreviewLine(Vec3 start, Vec3 end, float width, int color) {
	}

	@SubscribeEvent
	public static void sendExtenderPacket(PlayerInteractEvent.RightClickBlock event) {
		ItemStack stack = event.getItemStack();
		if (!AllTags.AllBlockTags.TRACKS.matches(stack))
			return;
		if (Minecraft.getInstance().options.keySprint.isDown())
			ClientNetworkHelper.INSTANCE.sendToServer(
				new PlaceExtendedCurvePacket(event.getHand() == InteractionHand.MAIN_HAND, true));
	}

	public static void clientTick() {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			clearPreview();
			return;
		}
		ItemStack stack = player.getMainHandItem();
		HitResult hitResult = minecraft.hitResult;
		int restoreWarmup = TrackPlacement.extraTipWarmup;
		TrackPlacement.extraTipWarmup = 0;

		if (hitResult == null || hitResult.getType() != Type.BLOCK) {
			clearPreview();
			return;
		}

		InteractionHand hand = InteractionHand.MAIN_HAND;
		if (!AllTags.AllBlockTags.TRACKS.matches(stack)) {
			stack = player.getOffhandItem();
			hand = InteractionHand.OFF_HAND;
			if (!AllTags.AllBlockTags.TRACKS.matches(stack)) {
				clearPreview();
				return;
			}
		}

		if (!stack.has(AllDataComponents.TRACK_CONNECTING_FROM)) {
			clearPreview();
			return;
		}

		TrackBlockItem blockItem = (TrackBlockItem) stack.getItem();
		Level level = player.level();
		BlockHitResult bhr = (BlockHitResult) hitResult;
		BlockPos pos = bhr.getBlockPos();
		BlockState hitState = level.getBlockState(pos);
		if (!(hitState.getBlock() instanceof TrackBlock) && !hitState.canBeReplaced()) {
			pos = pos.relative(bhr.getDirection());
			hitState = blockItem.getPlacementState(new UseOnContext(player, hand, bhr));
			if (hitState == null) {
				clearPreview();
				return;
			}
		}

		if (!(hitState.getBlock() instanceof TrackBlock)) {
			clearPreview();
			return;
		}

		TrackPlacement.extraTipWarmup = restoreWarmup;
		boolean maxTurns = minecraft.options.keySprint.isDown();
		PlacementInfo info = TrackPlacement.tryConnect(level, player, pos, hitState, stack, false, maxTurns);
		if (TrackPlacement.extraTipWarmup < 20)
			TrackPlacement.extraTipWarmup++;
		if (!info.valid || !TrackPlacement.hoveringMaxed && (info.end1Extent == 0 || info.end2Extent == 0))
			TrackPlacement.extraTipWarmup = 0;

		if (!player.isCreative() && (info.valid || !info.hasRequiredTracks || !info.hasRequiredPavement))
			BlueprintOverlayRenderer.displayTrackRequirements(info, player.getOffhandItem());

		if (info.valid)
			minecraft.gui.hud.setOverlayMessage(CreateLang.translateDirect("track.valid_connection")
				.withStyle(ChatFormatting.GREEN), false);
		else if (info.message != null)
			minecraft.gui.hud.setOverlayMessage(CreateLang.translateDirect(info.message)
				.withStyle(info.message.equals("track.second_point") ? ChatFormatting.WHITE : ChatFormatting.RED), false);

		if (bhr.getDirection() == Direction.UP) {
			Vec3 lookVec = player.getLookAngle();
			int lookAngle = (int) (22.5 + AngleHelper.deg(Mth.atan2(lookVec.z, lookVec.x)) % 360) / 8;
			if (!pos.equals(hintPos) || lookAngle != hintAngle) {
				hints = Couple.create(ArrayList::new);
				hintAngle = lookAngle;
				hintPos = pos;
				for (int xOffset = -2; xOffset <= 2; xOffset++) {
					for (int zOffset = -2; zOffset <= 2; zOffset++) {
						BlockPos offset = pos.offset(xOffset, 0, zOffset);
						PlacementInfo adjacent = TrackPlacement.tryConnect(level, player, offset, hitState, stack, false, maxTurns);
						hints.get(adjacent.valid).add(offset.below());
					}
				}
			}
			if (hints != null && !hints.either(Collection::isEmpty)) {
				previewValidHints.clear();
				previewValidHints.addAll(hints.getFirst());
				previewInvalidHints.clear();
				previewInvalidHints.addAll(hints.getSecond());
				Outliner.getInstance().showCluster("track_valid", hints.getFirst())
					.withFaceTexture(AllSpecialTextures.THIN_CHECKERED).colored(0x95CD41).lineWidth(0);
				Outliner.getInstance().showCluster("track_invalid", hints.getSecond())
					.withFaceTexture(AllSpecialTextures.THIN_CHECKERED).colored(0xEA5C2B).lineWidth(0);
			}
		} else {
			previewValidHints.clear();
			previewInvalidHints.clear();
		}

		animation.chase(info.valid ? 1 : 0, 0.25, Chaser.EXP);
		animation.tickChaser();
		previewLines.clear();
		if (!info.valid) {
			info.end1Extent = 0;
			info.end2Extent = 0;
		}

		int color = Color.mixColors(0xEA5C2B, 0x95CD41, animation.getValue());
		Vec3 up = new Vec3(0, 4 / 16f, 0);
		Vec3 v1 = info.end1;
		Vec3 a1 = info.axis1.normalize();
		Vec3 n1 = info.normal1.cross(a1).scale(15 / 16f);
		Vec3 o1 = a1.scale(0.125f);
		Vec3 ex1 = a1.scale((info.end1Extent - (info.curve == null && info.end1Extent > 0 ? 2 : 0)) * info.axis1.length());
		line(1, v1.add(n1).add(up), o1, ex1);
		line(2, v1.subtract(n1).add(up), o1, ex1);

		Vec3 v2 = info.end2;
		Vec3 a2 = info.axis2.normalize();
		Vec3 n2 = info.normal2.cross(a2).scale(15 / 16f);
		Vec3 o2 = a2.scale(0.125f);
		Vec3 ex2 = a2.scale(info.end2Extent * info.axis2.length());
		line(3, v2.add(n2).add(up), o2, ex2);
		line(4, v2.subtract(n2).add(up), o2, ex2);

		BezierConnection curve = info.curve;
		if (curve == null)
			return;

		Vec3 previous1 = null;
		Vec3 previous2 = null;
		int segmentCount = curve.getSegmentCount();
		float scale = animation.getValue() * 7 / 8f + 1 / 8f;
		float lineWidth = animation.getValue() * 1 / 16f + 1 / 16f;
		Vec3 end1 = curve.starts.getFirst();
		Vec3 end2 = curve.starts.getSecond();
		Vec3 finish1 = end1.add(curve.axes.getFirst().scale(curve.getHandleLength()));
		Vec3 finish2 = end2.add(curve.axes.getSecond().scale(curve.getHandleLength()));
		String key = "curve";

		for (int i = 0; i <= segmentCount; i++) {
			float t = i / (float) segmentCount;
			Vec3 result = VecHelper.bezier(end1, end2, finish1, finish2, t);
			Vec3 derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t).normalize();
			Vec3 normal = curve.getNormal(t).cross(derivative).scale(15 / 16f);
			Vec3 rail1 = result.add(normal).add(up);
			Vec3 rail2 = result.subtract(normal).add(up);
			if (previous1 != null) {
				Vec3 middle1 = rail1.add(previous1).scale(0.5f);
				Vec3 middle2 = rail2.add(previous2).scale(0.5f);
				Vec3 start1 = VecHelper.lerp(scale, middle1, previous1);
				Vec3 finishRail1 = VecHelper.lerp(scale, middle1, rail1);
				Vec3 start2 = VecHelper.lerp(scale, middle2, previous2);
				Vec3 finishRail2 = VecHelper.lerp(scale, middle2, rail2);
				addPreviewLine(start1, finishRail1, lineWidth, color);
				addPreviewLine(start2, finishRail2, lineWidth, color);
				Outliner.getInstance().showLine(Pair.of(key, i * 2), start1, finishRail1)
					.colored(color).disableLineNormals().lineWidth(lineWidth);
				Outliner.getInstance().showLine(Pair.of(key, i * 2 + 1), start2, finishRail2)
					.colored(color).disableLineNormals().lineWidth(lineWidth);
			}
			previous1 = rail1;
			previous2 = rail2;
		}
		for (int i = segmentCount + 1; i <= lastLineCount; i++) {
			Outliner.getInstance().remove(Pair.of(key, i * 2));
			Outliner.getInstance().remove(Pair.of(key, i * 2 + 1));
		}
		lastLineCount = segmentCount;
	}

	private static void line(int id, Vec3 vertex, Vec3 offset, Vec3 extension) {
		int color = Color.mixColors(0xEA5C2B, 0x95CD41, animation.getValue());
		Vec3 start = vertex.subtract(offset);
		Vec3 end = vertex.add(extension);
		addPreviewLine(start, end, 1 / 8f, color);
		Outliner.getInstance().showLine(Pair.of("start", id), start, end)
			.lineWidth(1 / 8f).disableLineNormals().colored(color);
	}

	private static void addPreviewLine(Vec3 start, Vec3 end, float width, int color) {
		previewLines.add(new PreviewLine(start, end, width, 0xFF000000 | color));
	}

	private static void clearPreview() {
		previewValidHints.clear();
		previewInvalidHints.clear();
		previewLines.clear();
	}

	public static void submit(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
		if (previewValidHints.isEmpty() && previewInvalidHints.isEmpty() && previewLines.isEmpty())
			return;
		Vec3 camera = cameraState.pos;
		collector.submitCustomGeometry(poseStack, net.minecraft.client.renderer.rendertype.RenderTypes.debugQuads(),
			(pose, consumer) -> {
				for (BlockPos pos : previewValidHints)
					renderGroundTile(pose, consumer, camera, pos, 0x2095CD41);
				for (BlockPos pos : previewInvalidHints)
					renderGroundTile(pose, consumer, camera, pos, 0x20EA5C2B);
				for (PreviewLine line : previewLines)
					renderPreviewStrip(pose, consumer, camera, line.start(), line.end(), line.width(), line.color());
			});
	}

	private static void renderGroundTile(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera, BlockPos pos, int color) {
		float x0 = (float) (pos.getX() + 1 / 32f - camera.x);
		float x1 = (float) (pos.getX() + 31 / 32f - camera.x);
		float y = (float) (pos.getY() + 1 + 1 / 128f - camera.y);
		float z0 = (float) (pos.getZ() + 1 / 32f - camera.z);
		float z1 = (float) (pos.getZ() + 31 / 32f - camera.z);
		renderQuad(pose, consumer, new Vec3(x0, y, z0), new Vec3(x1, y, z0), new Vec3(x1, y, z1), new Vec3(x0, y, z1), color);
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
		normal = normal.normalize().scale(width);
		renderQuad(pose, consumer, start.add(normal), end.add(normal), end.subtract(normal), start.subtract(normal), color);
	}

	private static void renderQuad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
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
		consumer.addVertex(pose, (float) vertex.x, (float) vertex.y, (float) vertex.z).setColor(color);
	}
}
