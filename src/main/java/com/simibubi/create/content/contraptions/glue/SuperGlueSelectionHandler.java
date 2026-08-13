package com.simibubi.create.content.contraptions.glue;

import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Objects;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.RaycastHelper;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public class SuperGlueSelectionHandler {

	private static final int HIGHLIGHT = 0x68c586;
	private static final int FAIL = 0xc5b548;

	private final Object clusterOutlineSlot = new Object();
	private final Object bbOutlineSlot = new Object();
	private int clusterCooldown;

	private BlockPos firstPos;
	private BlockPos hoveredPos;
	private Set<BlockPos> currentCluster;
	private Set<BlockPos> successCluster;
	private int glueRequired;

	private SuperGlueEntity selected;
	private BlockPos soundSourceForRemoval;
	private AABB previewBox;
	private int previewColor = HIGHLIGHT;

	public boolean isSelected(SuperGlueEntity entity) {
		return entity != null && entity == selected;
	}

	public int getRenderColor(SuperGlueEntity entity, int fallback) {
		return fallback;
	}

	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null)
			return;

		BlockPos hovered = null;
		ItemStack stack = player.getMainHandItem();

		if (clusterCooldown > 0) {
			if (clusterCooldown == 25)
				Minecraft.getInstance().gui.hud.setOverlayMessage(CommonComponents.EMPTY, false);
			Outliner.getInstance().keep(clusterOutlineSlot);
			clusterCooldown--;
			if (clusterCooldown == 0)
				successCluster = null;
		}

		if (!isGlue(stack)) {
			if (firstPos != null)
				discard();
			selected = null;
			successCluster = null;
			currentCluster = null;
			clearPreview();
			clusterCooldown = 0;
			return;
		}

		selected = null;
		if (firstPos == null) {
			clearPreview();
			double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1;
			Vec3 traceOrigin = player.getEyePosition();
			Vec3 traceTarget = RaycastHelper.getTraceTarget(player, range, traceOrigin);
			AABB scanArea = new AABB(traceOrigin, traceTarget).inflate(1);
			List<SuperGlueEntity> glueNearby = mc.level.getEntitiesOfClass(SuperGlueEntity.class, scanArea);

			double bestDistance = Double.MAX_VALUE;
			for (SuperGlueEntity glueEntity : glueNearby) {
				Optional<Vec3> clip = glueEntity.getBoundingBox()
					.clip(traceOrigin, traceTarget);
				if (clip.isEmpty())
					continue;
				Vec3 vec3 = clip.get();
				double distanceToSqr = vec3.distanceToSqr(traceOrigin);
				if (distanceToSqr > bestDistance)
					continue;
				selected = glueEntity;
				soundSourceForRemoval = BlockPos.containing(vec3);
				bestDistance = distanceToSqr;
			}

			if (selected == null && mc.hitResult instanceof BlockHitResult blockHit) {
				BlockPos hitPos = blockHit.getBlockPos();
				for (SuperGlueEntity glueEntity : glueNearby) {
					if (!glueEntity.contains(hitPos))
						continue;
					selected = glueEntity;
					soundSourceForRemoval = hitPos;
					break;
				}
			}

		}

		HitResult hitResult = mc.hitResult;
		if (hitResult != null && hitResult.getType() == Type.BLOCK)
			hovered = ((BlockHitResult) hitResult).getBlockPos();

		if (hovered == null) {
			hoveredPos = null;
			clearPreview();
			return;
		}

		if (firstPos != null && !firstPos.closerThan(hovered, 24)) {
			sendStatus(player, "super_glue.too_far", FAIL);
			updatePreviewEntity(mc.level);
			return;
		}

		boolean cancel = player.isShiftKeyDown();
		if (cancel && firstPos == null)
			return;

		boolean unchanged = Objects.equal(hovered, hoveredPos);

		if (unchanged) {
			if (firstPos != null) {
				boolean canReach = currentCluster != null && currentCluster.contains(hovered);
				boolean canAfford = SuperGlueSelectionHelper.collectGlueFromInventory(player, glueRequired, true);
				int color = HIGHLIGHT;
				String key = "super_glue.click_to_confirm";

				if (currentCluster == null) {
					color = FAIL;
					key = "super_glue.cannot_reach";
				} else if (!canReach) {
					color = FAIL;
					key = "super_glue.cannot_reach";
				} else if (!canAfford) {
					color = FAIL;
					key = "super_glue.not_enough";
				} else if (cancel) {
					color = FAIL;
					key = "super_glue.click_to_discard";
				}

				sendStatus(player, key, color);

				showCurrentSelection(player, cancel);
			}

			updatePreviewEntity(mc.level);
			return;
		}

		hoveredPos = hovered;
		if (firstPos != null) {
			clearPreview();
			currentCluster = SuperGlueSelectionHelper.searchGlueGroup(mc.level, firstPos, hoveredPos, true);
			glueRequired = 1;
			showCurrentSelection(player, cancel);
		} else
			currentCluster = null;
		updatePreviewEntity(mc.level);
	}

	private boolean isGlue(ItemStack stack) {
		return stack.getItem() instanceof SuperGlueItem;
	}

	private AABB getCurrentSelectionBox() {
		return firstPos == null || hoveredPos == null ? null
			: new AABB(Vec3.atLowerCornerOf(firstPos), Vec3.atLowerCornerOf(hoveredPos)).expandTowards(1, 1, 1);
	}

	public boolean onMouseInput(boolean attack) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (player == null || level == null)
			return false;

		if (!isGlue(player.getMainHandItem()))
			return false;
		if (!player.mayBuild())
			return false;

		if (attack) {
			if (selected == null)
				return false;
			ClientNetworkHelper.INSTANCE.sendToServer(new SuperGlueRemovalPacket(selected.getId(), soundSourceForRemoval));
			selected = null;
			clusterCooldown = 0;
			return true;
		}

		if (player.isShiftKeyDown()) {
			if (firstPos != null) {
				discard();
				return true;
			}
			return false;
		}

		if (hoveredPos == null)
			return false;

		Direction face = null;
		if (mc.hitResult instanceof BlockHitResult bhr) {
			face = bhr.getDirection();
			BlockState blockState = level.getBlockState(hoveredPos);
			if (blockState.getBlock() instanceof AbstractChassisBlock cb)
				if (cb.getGlueableSide(blockState, bhr.getDirection()) != null)
					return false;
		}

		if (firstPos != null) {
			if (currentCluster == null)
				return true;

			boolean canReach = currentCluster.contains(hoveredPos);
			boolean canAfford = SuperGlueSelectionHelper.collectGlueFromInventory(player, glueRequired, true);

			if (!canReach || !canAfford)
				return true;

			confirm();
			return true;
		}

		firstPos = hoveredPos;
		currentCluster = null;
		glueRequired = 1;
		if (face != null)
			SuperGlueItem.spawnParticles(level, firstPos, face, true);
		sendStatus(player, "super_glue.first_pos", HIGHLIGHT);
		AllSoundEvents.SLIME_ADDED.playAt(level, firstPos, 0.5F, 0.85F, false);
		level.playSound(player, firstPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75f, 1);
		return true;
	}

	public void discard() {
		discard(true);
	}

	private void discard(boolean notify) {
		LocalPlayer player = Minecraft.getInstance().player;
		currentCluster = null;
		firstPos = null;
		clearPreview();
		if (notify && player != null)
			sendStatus(player, "super_glue.abort", FAIL);
		clusterCooldown = 0;
	}

	public void confirm() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return;

		ClientNetworkHelper.INSTANCE.sendToServer(new SuperGlueSelectionPacket(firstPos, hoveredPos));
		AllSoundEvents.SLIME_ADDED.playAt(player.level(), hoveredPos, 0.5F, 0.95F, false);
		player.level().playSound(player, hoveredPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75f, 1);

		Set<BlockPos> confirmedCluster = currentCluster == null ? null : new HashSet<>(currentCluster);

		discard(false);
		successCluster = confirmedCluster;
		if (successCluster != null)
			showSuccessCluster();
		clearPreview();
		sendStatus(player, "super_glue.success", HIGHLIGHT);
		clusterCooldown = 40;
	}

	private void updatePreviewEntity(ClientLevel level) {
		AABB box = getCurrentSelectionBox();
		if (level == null || box == null) {
			clearPreview();
			return;
		}

		int color = getSelectionColor(Minecraft.getInstance().player);
		previewBox = box;
		previewColor = color;
		Outliner.getInstance().showAABB(bbOutlineSlot, box)
			.colored(color)
			.withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.GLUE)
			.disableLineNormals()
			.lineWidth(1 / 16f);
	}

	private int getSelectionColor(LocalPlayer player) {
		if (player == null || firstPos == null || hoveredPos == null)
			return HIGHLIGHT;
		if (player.isShiftKeyDown())
			return FAIL;
		if (currentCluster == null)
			return FAIL;
		if (!currentCluster.contains(hoveredPos))
			return FAIL;
		return SuperGlueSelectionHelper.collectGlueFromInventory(player, glueRequired, true) ? HIGHLIGHT : FAIL;
	}

	private void showSuccessCluster() {
		Outliner.getInstance().showCluster(clusterOutlineSlot, successCluster)
			.colored(0xB5F2C6)
			.withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.HIGHLIGHT_CHECKERED)
			.disableLineNormals()
			.lineWidth(1 / 24f);
	}

	private void sendStatus(LocalPlayer player, String key, int color) {
		Minecraft.getInstance().gui.hud.setOverlayMessage(CreateLang.translateDirect(key)
			.withStyle(style -> style.withColor(color)), false);
	}

	private void showCurrentSelection(LocalPlayer player, boolean cancel) {
		boolean canReach = currentCluster != null && hoveredPos != null && currentCluster.contains(hoveredPos);
		boolean canAfford = SuperGlueSelectionHelper.collectGlueFromInventory(player, glueRequired, true);
		int color = canReach && canAfford && !cancel ? HIGHLIGHT : FAIL;
		AABB box = getCurrentSelectionBox();

		if (box != null)
			Outliner.getInstance().showAABB(bbOutlineSlot, box)
				.colored(color)
				.withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.GLUE)
				.disableLineNormals()
				.lineWidth(1 / 16f);

		if (currentCluster != null)
			Outliner.getInstance().showCluster(clusterOutlineSlot, currentCluster)
				.colored(color)
				.disableLineNormals()
				.lineWidth(canReach && canAfford && !cancel ? 1 / 48f : 1 / 32f);

		if (!canReach)
			sendStatus(player, "super_glue.cannot_reach", FAIL);
		else if (!canAfford)
			sendStatus(player, "super_glue.not_enough", FAIL);
		else if (cancel)
			sendStatus(player, "super_glue.click_to_discard", FAIL);
	}

	private void clearPreview() {
		previewBox = null;
	}

	public void submitPreview(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		AABB box = previewBox;
		if (box == null)
			return;

		AABB cameraRelative = box.move(-camera.x, -camera.y, -camera.z);
		int color = previewColor;
		collector.submitCustomGeometry(ms,
			com.simibubi.create.foundation.render.RenderTypes.glueOverlay(AllSpecialTextures.GLUE.getId()),
			(pose, consumer) -> SuperGlueRenderer.renderGlueTexture(pose, consumer, cameraRelative, color, 105));
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> SuperGlueRenderer.renderWireframe(pose, consumer, cameraRelative, color, 235, 1 / 16f));
	}

}
