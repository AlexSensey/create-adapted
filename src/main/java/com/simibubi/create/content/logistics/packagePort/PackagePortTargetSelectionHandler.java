package com.simibubi.create.content.logistics.packagePort;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

public class PackagePortTargetSelectionHandler {

	public static PackagePortTarget activePackageTarget;
	public static Vec3 exactPositionOfTarget;
	public static boolean isPostbox;
	private static BlockPos previewPos;
	private static int previewColor;

	public static void flushSettings(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (activePackageTarget == null) {
			if (mc.player != null)
				mc.player.sendSystemMessage(CreateLang.translateDirect("gui.package_port.not_targeting_anything"));
			return;
		}

		if (exactPositionOfTarget != null && validateDiff(exactPositionOfTarget, pos) == null) {
			activePackageTarget.relativePos = activePackageTarget.relativePos.subtract(pos);
			ClientNetworkHelper.INSTANCE.sendToServer(new PackagePortPlacementPacket(activePackageTarget, pos));
		}

		activePackageTarget = null;
		exactPositionOfTarget = null;
		isPostbox = false;
	}

	public static boolean onUse() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null)
			return false;
		HitResult hitResult = mc.hitResult;
		ItemStack mainHandItem = mc.player.getMainHandItem();

		if (!(hitResult instanceof BlockHitResult bhr))
			return false;

		BlockPos pos = bhr.getBlockPos();
		if (!(mc.level.getBlockEntity(pos) instanceof StationBlockEntity sbe))
			return false;
		if (sbe.edgePoint == null)
			return false;
		if (!AllItemTags.POSTBOXES.matches(mainHandItem))
			return false;

		exactPositionOfTarget = Vec3.atCenterOf(pos);
		activePackageTarget = new PackagePortTarget.TrainStationFrogportTarget(pos);
		isPostbox = true;
		return true;
	}

	public static void tick() {
		previewPos = null;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null)
			return;

		boolean holdingPostbox = AllItemTags.POSTBOXES.matches(player.getMainHandItem());
		boolean holdingWrench = player.getMainHandItem().is(Tags.Items.TOOLS_WRENCH);

		if (!holdingWrench) {
			if (activePackageTarget == null || exactPositionOfTarget == null)
				return;
			if (!AllBlocks.PACKAGE_FROGPORT.isIn(player.getMainHandItem()) && !holdingPostbox)
				return;
		}

		if (!(mc.hitResult instanceof BlockHitResult blockHit))
			return;

		if (holdingWrench) {
			BlockPos pos = blockHit.getBlockPos();
			if (!(mc.level.getBlockEntity(pos) instanceof PackagePortBlockEntity ppbe) || ppbe.target == null)
				return;
			Vec3 source = Vec3.atBottomCenterOf(pos);
			Vec3 target = ppbe.target.getExactTargetLocation(ppbe, mc.level, pos);
			if (target.equals(Vec3.ZERO))
				return;
			Color color = new Color(0x9ede73);
			animateConnection(mc, source, target, color);
			Outliner.getInstance()
				.chaseAABB("ChainPointSelected", new AABB(target, target))
				.colored(color)
				.lineWidth(1 / 5f)
				.disableLineNormals();
			return;
		}

		Vec3 target = exactPositionOfTarget;
		BlockPos pos = blockHit.getBlockPos();
		if (!mc.level.getBlockState(pos)
			.canBeReplaced())
			pos = pos.relative(blockHit.getDirection());

		String validation = validateDiff(target, pos);
		boolean valid = validation == null;
		Color color = new Color(valid ? 0x9ede73 : 0xff7171);
		Vec3 source = Vec3.atBottomCenterOf(pos);

		mc.gui.hud.setOverlayMessage(CreateLang.translateDirect(valid ? "package_port.valid" : validation)
			.withStyle(style -> style.withColor(color.getRGB())), false);

		Outliner.getInstance()
			.chaseAABB("ChainPointSelected", new AABB(target, target))
			.colored(color)
			.lineWidth(1 / 5f)
			.disableLineNormals();

		if (!mc.level.getBlockState(pos)
			.canBeReplaced())
			return;

		previewPos = pos.immutable();
		previewColor = color.getRGB();

		Outliner.getInstance()
			.chaseAABB("TargetedFrogPos", new AABB(
				pos.getX() + .125, pos.getY() + .01, pos.getZ() + .125,
				pos.getX() + .875, pos.getY() + .045, pos.getZ() + .875))
			.colored(color)
			.lineWidth(1 / 12f)
			.disableLineNormals();

		animateConnection(mc, source, target, color);
	}

	public static void submitPlacementPreview(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		if (previewPos == null)
			return;

		int color = 0xFF000000 | previewColor;
		BlockPos pos = previewPos;
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			double x0 = pos.getX() + .125;
			double x1 = pos.getX() + .875;
			double y0 = pos.getY() + .015;
			double z0 = pos.getZ() + .125;
			double z1 = pos.getZ() + .875;
			Vec3[] corners = { new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y0, z1),
				new Vec3(x0, y0, z1) };
			for (int i = 0; i < 4; i++)
				renderPreviewEdge(pose, consumer, camera, corners[i], corners[(i + 1) % 4], color);
		});
	}

	private static void renderPreviewEdge(PoseStack.Pose pose, VertexConsumer consumer, Vec3 camera, Vec3 worldStart,
		Vec3 worldEnd, int color) {
		Vec3 start = worldStart.subtract(camera);
		Vec3 end = worldEnd.subtract(camera);
		Vec3 direction = end.subtract(start);
		Vec3 normal = direction.cross(new Vec3(0, 1, 0));
		if (normal.lengthSqr() < 1e-5)
			normal = new Vec3(1, 0, 0);
		normal = normal.normalize()
			.scale(1 / 24f);
		Vec3 side = direction.cross(normal)
			.normalize()
			.scale(1 / 24f);
		addPreviewQuad(pose, consumer, start.add(normal), end.add(normal), end.subtract(normal), start.subtract(normal),
			color);
		addPreviewQuad(pose, consumer, start.add(side), end.add(side), end.subtract(side), start.subtract(side), color);
	}

	private static void addPreviewQuad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c,
		Vec3 d, int color) {
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

	public static void animateConnection(Minecraft mc, Vec3 source, Vec3 target, Color color) {
		ClientLevel world = mc.level;
		if (world == null)
			return;

		DustParticleOptions particle = new DustParticleOptions(color.getRGB(), 1);
		double totalFlyingTicks = 10;
		int segments = ((int) totalFlyingTicks / 3) + 1;
		double tickOffset = totalFlyingTicks / segments;

		for (int i = 0; i < segments; i++) {
			double ticks = (AnimationTickHolder.getRenderTime() / 3 % tickOffset) + i * tickOffset;
			Vec3 vec = source.lerp(target, ticks / totalFlyingTicks);
			world.addParticle(particle, vec.x, vec.y, vec.z, 0, 0, 0);
		}
	}

	public static String validateDiff(Vec3 target, BlockPos placedPos) {
		Vec3 source = Vec3.atBottomCenterOf(placedPos);
		Vec3 diff = target.subtract(source);
		if (diff.y < 0 && !isPostbox)
			return "package_port.cannot_reach_down";
		if (diff.length() > AllConfigs.server().logistics.packagePortRange.get())
			return "package_port.too_far";
		return null;
	}
}
