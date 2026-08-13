package com.simibubi.create.content.schematics.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.Create;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicExport.SchematicExportResult;
import com.simibubi.create.content.schematics.packet.InstantSchematicPacket;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.CreatePaths;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.RaycastHelper.PredicateTraceResult;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public class SchematicAndQuillHandler {

	private static final int SELECTION_COLOR = 0xFF6886C5;
	private static final double SELECTION_LINE_WIDTH = 1 / 32d;

	public BlockPos firstPos;
	public BlockPos secondPos;
	private BlockPos selectedPos;
	private Direction selectedFace;
	private int range = 10;

	public boolean mouseScrolled(double delta) {
		if (!isActive() || !AllKeys.ctrlDown())
			return false;
		if (secondPos == null)
			range = (int) Mth.clamp(range + delta, 1, 100);
		if (selectedFace == null || firstPos == null || secondPos == null)
			return true;

		AABB bounds = new AABB(Vec3.atLowerCornerOf(firstPos), Vec3.atLowerCornerOf(secondPos));
		Vec3i normal = new Vec3i(selectedFace.getStepX(), selectedFace.getStepY(), selectedFace.getStepZ());
		Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera()
			.position();
		if (bounds.contains(camera))
			delta *= -1;

		int amount = (int) (delta > 0 ? Math.ceil(delta) : Math.floor(delta));
		int x = normal.getX() * amount;
		int y = normal.getY() * amount;
		int z = normal.getZ() * amount;
		AxisDirection axisDirection = selectedFace.getAxisDirection();
		if (axisDirection == AxisDirection.NEGATIVE)
			bounds = bounds.move(-x, -y, -z);

		double maxX = Math.max(bounds.maxX - x * axisDirection.getStep(), bounds.minX);
		double maxY = Math.max(bounds.maxY - y * axisDirection.getStep(), bounds.minY);
		double maxZ = Math.max(bounds.maxZ - z * axisDirection.getStep(), bounds.minZ);
		bounds = new AABB(bounds.minX, bounds.minY, bounds.minZ, maxX, maxY, maxZ);

		firstPos = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
		secondPos = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
		sendStatus(CreateLang.translate("schematicAndQuill.dimensions", (int) bounds.getXsize() + 1,
			(int) bounds.getYsize() + 1, (int) bounds.getZsize() + 1)
			.component());
		return true;
	}

	public boolean onMouseInput(int button, boolean pressed) {
		if (!pressed || button != 1 || !isActive())
			return false;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return false;

		if (player.isShiftKeyDown()) {
			discard();
			return true;
		}
		if (secondPos != null) {
			ScreenOpener.open(new SchematicPromptScreen());
			return true;
		}
		if (selectedPos == null) {
			sendStatus(CreateLang.translate("schematicAndQuill.noTarget")
				.component());
			return true;
		}
		if (firstPos != null) {
			secondPos = selectedPos;
			sendStatus(CreateLang.translate("schematicAndQuill.secondPos")
				.component());
			return true;
		}

		firstPos = selectedPos;
		sendStatus(CreateLang.translate("schematicAndQuill.firstPos")
			.component());
		return true;
	}

	public void discard() {
		LocalPlayer player = Minecraft.getInstance().player;
		firstPos = null;
		secondPos = null;
		selectedFace = null;
		sendStatus(CreateLang.translate("schematicAndQuill.abort")
			.component());
	}

	public void tick() {
		if (!isActive())
			return;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return;

		if (AllKeys.ACTIVATE_TOOL.isPressed()) {
			float partialTicks = AnimationTickHolder.getPartialTicks();
			Vec3 target = player.getEyePosition(partialTicks)
				.add(player.getLookAngle()
					.scale(range));
			selectedPos = BlockPos.containing(target);
		} else {
			BlockHitResult trace = RaycastHelper.rayTraceRange(player.level(), player, 75);
			if (trace != null && trace.getType() == Type.BLOCK) {
				BlockPos hit = trace.getBlockPos();
				boolean replaceable = player.level()
					.getBlockState(hit)
					.canBeReplaced(new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, trace)));
				if (trace.getDirection()
					.getAxis()
					.isVertical() && !replaceable)
					hit = hit.relative(trace.getDirection());
				selectedPos = hit;
			} else {
				selectedPos = null;
			}
		}

		selectedFace = null;
		if (secondPos != null && firstPos != null) {
			AABB bounds = new AABB(Vec3.atLowerCornerOf(firstPos), Vec3.atLowerCornerOf(secondPos))
				.expandTowards(1, 1, 1)
				.inflate(.45f);
			Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera()
				.position();
			boolean inside = bounds.contains(camera);
			PredicateTraceResult result =
				RaycastHelper.rayTraceUntil(player, 70, pos -> inside ^ bounds.contains(VecHelper.getCenterOf(pos)));
			if (result != null && !result.missed())
				selectedFace = inside ? result.getFacing()
					.getOpposite() : result.getFacing();
		}

	}

	public void submit(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		if (!isActive())
			return;
		AABB selection = getCurrentSelectionBox();
		if (selection == null)
			return;

		Vec3 camera = cameraRenderState.pos;
		AABB relative = selection.inflate(1 / 128d)
			.move(-camera.x, -camera.y, -camera.z);
		collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
			(pose, consumer) -> renderSelectionBox(pose, consumer, relative));
	}

	private static void renderSelectionBox(PoseStack.Pose pose, VertexConsumer consumer, AABB box) {
		double w = SELECTION_LINE_WIDTH / 2;
		for (double y : new double[] {box.minY, box.maxY})
			for (double z : new double[] {box.minZ, box.maxZ})
				renderCuboid(pose, consumer, box.minX - w, y - w, z - w, box.maxX + w, y + w, z + w);
		for (double x : new double[] {box.minX, box.maxX})
			for (double z : new double[] {box.minZ, box.maxZ})
				renderCuboid(pose, consumer, x - w, box.minY - w, z - w, x + w, box.maxY + w, z + w);
		for (double x : new double[] {box.minX, box.maxX})
			for (double y : new double[] {box.minY, box.maxY})
				renderCuboid(pose, consumer, x - w, y - w, box.minZ - w, x + w, y + w, box.maxZ + w);
	}

	private static void renderCuboid(PoseStack.Pose pose, VertexConsumer consumer, double minX, double minY,
		double minZ, double maxX, double maxY, double maxZ) {
		renderQuad(pose, consumer, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
		renderQuad(pose, consumer, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ);
		renderQuad(pose, consumer, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
		renderQuad(pose, consumer, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, minX, minY, minZ);
		renderQuad(pose, consumer, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
		renderQuad(pose, consumer, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ);
	}

	private static void renderQuad(PoseStack.Pose pose, VertexConsumer consumer,
		double ax, double ay, double az, double bx, double by, double bz,
		double cx, double cy, double cz, double dx, double dy, double dz) {
		addVertex(pose, consumer, ax, ay, az);
		addVertex(pose, consumer, bx, by, bz);
		addVertex(pose, consumer, cx, cy, cz);
		addVertex(pose, consumer, dx, dy, dz);
		addVertex(pose, consumer, dx, dy, dz);
		addVertex(pose, consumer, cx, cy, cz);
		addVertex(pose, consumer, bx, by, bz);
		addVertex(pose, consumer, ax, ay, az);
	}

	private static void addVertex(PoseStack.Pose pose, VertexConsumer consumer, double x, double y, double z) {
		consumer.addVertex(pose, (float) x, (float) y, (float) z)
			.setColor(SELECTION_COLOR);
	}

	private AABB getCurrentSelectionBox() {
		if (secondPos == null) {
			if (firstPos == null)
				return selectedPos == null ? null : new AABB(selectedPos);
			return selectedPos == null ? new AABB(firstPos)
				: new AABB(Vec3.atLowerCornerOf(firstPos), Vec3.atLowerCornerOf(selectedPos))
					.expandTowards(1, 1, 1);
		}
		return new AABB(Vec3.atLowerCornerOf(firstPos), Vec3.atLowerCornerOf(secondPos))
			.expandTowards(1, 1, 1);
	}

	private boolean isActive() {
		Minecraft mc = Minecraft.getInstance();
		return isPresent() && mc.player != null && AllItems.SCHEMATIC_AND_QUILL.isIn(mc.player.getMainHandItem());
	}

	private boolean isPresent() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.gui.screen() == null;
	}

	public void saveSchematic(String name, boolean convertImmediately) {
		if (firstPos == null || secondPos == null || Minecraft.getInstance().level == null)
			return;
		SchematicExportResult result = SchematicExport.saveSchematic(CreatePaths.SCHEMATICS_DIR, name, false,
			Minecraft.getInstance().level, firstPos, secondPos);
		LocalPlayer player = Minecraft.getInstance().player;
		if (result == null) {
			sendStatus(CreateLang.translate("schematicAndQuill.failed")
				.style(ChatFormatting.RED)
				.component());
			return;
		}

		Path file = result.file();
		sendStatus(CreateLang.translate("schematicAndQuill.saved", file.getFileName()
			.toString())
			.component());
		firstPos = null;
		secondPos = null;
		if (!convertImmediately)
			return;
		try {
			if (!ClientSchematicLoader.validateSizeLimitation(Files.size(file)))
				return;
			ClientNetworkHelper.INSTANCE.sendToServer(
				new InstantSchematicPacket(result.fileName(), result.origin(), result.bounds()));
		} catch (IOException e) {
			Create.LOGGER.error("Error instantly uploading schematic file: {}", file, e);
		}
	}

	private static void sendStatus(Component message) {
		Minecraft.getInstance().gui.hud.setOverlayMessage(message, false);
	}
}
