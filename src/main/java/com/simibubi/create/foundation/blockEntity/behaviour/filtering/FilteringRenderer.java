package com.simibubi.create.foundation.blockEntity.behaviour.filtering;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.chute.SmartChuteBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.TableClothRenderer;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import com.simibubi.create.content.trains.observer.TrackObserverBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.ItemValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.render.FlatGuiItemRenderer;

import net.createmod.catnip.api.client.outliner.Outliner;
import net.createmod.catnip.api.client.render.SuperRenderTypeBuffer;
import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FilteringRenderer {
	public static void submitSmartChuteValueBox(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.player.isShiftKeyDown()
			|| !(mc.hitResult instanceof BlockHitResult result))
			return;
		if (!(mc.level.getBlockEntity(result.getBlockPos()) instanceof SmartChuteBlockEntity be))
			return;

		FilteringBehaviour behaviour = be.getBehaviour(FilteringBehaviour.TYPE);
		if (behaviour == null || !behaviour.isActive() || !behaviour.mayInteract(mc.player))
			return;
		BlockPos pos = be.getBlockPos();
		BlockState state = be.getBlockState();
		ValueBoxTransform transform = behaviour.getSlotPositioning();
		if (!(transform instanceof Sided sided))
			return;
		Direction previousSide = sided.getSide();
		sided.fromSide(result.getDirection());
		boolean hovered = transform.shouldRender(mc.level, pos, state);
		if (hovered) {
			boolean hasFilter = !behaviour.getFilter(result.getDirection()).isEmpty();
			Vec3 offset = transform.getLocalOffset(mc.level, pos, state);
			Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getUnitVec3i());
			if (offset == null) {
				sided.fromSide(previousSide);
				return;
			}
			ms.pushPose();
			ms.translate(pos.getX() - camera.x + offset.x + normal.x / 32d + normal.x / 512d,
				pos.getY() - camera.y + offset.y + normal.y / 32d + normal.y / 512d,
				pos.getZ() - camera.z + offset.z + normal.z / 32d + normal.z / 512d);
			rotateToFace(ms, result.getDirection());
			collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
				if (hasFilter)
					renderFilterCorners(pose, consumer);
				else
					renderFilterDots(pose, consumer);
			});
			ms.popPose();
		}
		sided.fromSide(previousSide);
	}

	public static void submitSmartObserverValueBox(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.player.isShiftKeyDown()
			|| !(mc.hitResult instanceof BlockHitResult result))
			return;
		if (!(mc.level.getBlockEntity(result.getBlockPos()) instanceof SmartObserverBlockEntity be))
			return;

		FilteringBehaviour behaviour = be.getBehaviour(FilteringBehaviour.TYPE);
		if (behaviour == null || !behaviour.isActive() || !behaviour.mayInteract(mc.player))
			return;
		BlockPos pos = be.getBlockPos();
		BlockState state = be.getBlockState();
		ValueBoxTransform transform = behaviour.getSlotPositioning();
		Direction side = result.getDirection();
		if (transform instanceof Sided sided)
			sided.fromSide(result.getDirection());
		Vec3 localHit = result.getLocation().subtract(Vec3.atLowerCornerOf(pos));
		if (!transform.shouldRender(mc.level, pos, state) || !transform.testHit(mc.level, pos, state, localHit))
			return;
		Vec3 offset = transform.getLocalOffset(mc.level, pos, state);
		if (offset == null)
			return;
		Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());

		boolean hasFilter = !behaviour.getFilter().isEmpty();
		ms.pushPose();
		ms.translate(pos.getX() - camera.x + offset.x + normal.x / 32d + normal.x / 512d,
			pos.getY() - camera.y + offset.y + normal.y / 32d + normal.y / 512d,
			pos.getZ() - camera.z + offset.z + normal.z / 32d + normal.z / 512d);
		rotateThresholdFilterSlot(ms, state, side);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			if (hasFilter)
				renderFilterCorners(pose, consumer);
			else
				renderFilterDots(pose, consumer);
		});
		ms.popPose();
	}

	public static void submitTrackObserverValueBox(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.player.isShiftKeyDown()
			|| !(mc.hitResult instanceof BlockHitResult result))
			return;
		if (!(mc.level.getBlockEntity(result.getBlockPos()) instanceof TrackObserverBlockEntity be))
			return;

		FilteringBehaviour behaviour = be.getBehaviour(FilteringBehaviour.TYPE);
		if (behaviour == null || !behaviour.isActive() || !behaviour.mayInteract(mc.player))
			return;
		BlockPos pos = be.getBlockPos();
		BlockState state = be.getBlockState();
		ValueBoxTransform transform = behaviour.getSlotPositioning();
		Vec3 localHit = result.getLocation().subtract(Vec3.atLowerCornerOf(pos));
		if (!transform.shouldRender(mc.level, pos, state) || !transform.testHit(mc.level, pos, state, localHit))
			return;

		boolean hasFilter = !behaviour.getFilter().isEmpty();
		Vec3 offset = transform.getLocalOffset(mc.level, pos, state);
		if (offset == null)
			return;
		ms.pushPose();
		ms.translate(pos.getX() - camera.x + offset.x, pos.getY() - camera.y + offset.y + 1 / 32f + 1 / 512f,
			pos.getZ() - camera.z + offset.z);
		ms.mulPose(Axis.XP.rotationDegrees(270));
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			if (hasFilter)
				renderFilterCorners(pose, consumer);
			else
				renderFilterDots(pose, consumer);
		});
		ms.popPose();
	}

	public static void submitTableClothValueBox(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult result))
			return;
		if (!(mc.level.getBlockEntity(result.getBlockPos()) instanceof TableClothBlockEntity be))
			return;

		FilteringBehaviour behaviour = be.priceTag;
		if (behaviour == null || !behaviour.isActive() || !behaviour.mayInteract(mc.player))
			return;
		BlockPos pos = be.getBlockPos();
		BlockState state = be.getBlockState();
		ValueBoxTransform transform = behaviour.getSlotPositioning();
		if (!transform.shouldRender(mc.level, pos, state))
			return;
		Vec3 localHit = result.getLocation().subtract(Vec3.atLowerCornerOf(pos));
		if (!transform.testHit(mc.level, pos, state, localHit))
			return;
		boolean hasFilter = !behaviour.getFilter().isEmpty();

		ms.pushPose();
		ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
		TableClothRenderer.applyPriceSurfaceTransform(be, ms, 1 / 32d + 1 / 512d);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			if (hasFilter)
				renderFilterCorners(pose, consumer);
			else
				renderFilterDots(pose, consumer);
		});
		if (hasFilter) {
			ms.pushPose();
			ms.scale(.45f, .45f, .45f);
			submitFactoryPanelCount(ms, collector, behaviour.getCountLabelForValueBox());
			ms.popPose();
		}
		ms.popPose();
	}

	public static void submitFactoryPanelValueBox(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult result))
			return;
		if (!(mc.level.getBlockEntity(result.getBlockPos()) instanceof SmartBlockEntity sbe))
			return;

		BlockPos pos = result.getBlockPos();
		BlockState state = mc.level.getBlockState(pos);
		Vec3 localHit = result.getLocation().subtract(Vec3.atLowerCornerOf(pos));
		ItemStack heldItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

		for (BlockEntityBehaviour b : sbe.getAllBehaviours()) {
			if (!(b instanceof FactoryPanelBehaviour behaviour) || !behaviour.isActive())
				continue;
			ValueBoxTransform transform = behaviour.getSlotPositioning();
			if (!transform.shouldRender(mc.level, pos, state)
				|| !transform.testHit(mc.level, pos, state, localHit)
				|| !behaviour.mayInteract(mc.player))
				continue;

			ItemStack filter = behaviour.getFilter();
			ms.pushPose();
			ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
			applyFactoryPanelSurfaceTransform(ms, state, behaviour);

			if (!behaviour.bypassesInput(heldItem)) {
				ms.pushPose();
				ms.scale(2.01f, 2.01f, 1);
				collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
					if (filter.isEmpty())
						renderFilterDots(pose, consumer);
					else
						renderFilterCorners(pose, consumer);
				});
				ms.popPose();
			}

			submitFactoryPanelCount(ms, collector, behaviour.getCountLabelForValueBox());
			ms.popPose();
			return;
		}
	}

	private static void applyFactoryPanelSurfaceTransform(PoseStack ms, BlockState state,
		FactoryPanelBehaviour behaviour) {
		float xRot = net.minecraft.util.Mth.RAD_TO_DEG * com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.getXRot(state);
		float yRot = net.minecraft.util.Mth.RAD_TO_DEG * com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.getYRot(state);
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(yRot));
		ms.mulPose(Axis.XP.rotationDegrees(xRot + 90));
		ms.mulPose(Axis.YP.rotationDegrees(180));
		ms.translate(-.5, -.5, -.5);
		ms.translate(behaviour.slot.xOffset * .5, 0, behaviour.slot.yOffset * .5);
		ms.translate(.25f, 2.05f / 16f + 1 / 512f, .25f);
		ms.mulPose(Axis.XP.rotationDegrees(-90));
		ms.scale(.5f, .5f, .5f);
	}

	private static void renderFilterCorners(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		renderThreePixelCorner(pose, consumer, 5, 5, 1, 1, color);
		renderThreePixelCorner(pose, consumer, 10, 5, -1, 1, color);
		renderThreePixelCorner(pose, consumer, 5, 10, 1, -1, color);
		renderThreePixelCorner(pose, consumer, 10, 10, -1, -1, color);
	}

	private static void renderFilterDots(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		flatPixelXY(pose, consumer, 6, 6, color);
		flatPixelXY(pose, consumer, 9, 6, color);
		flatPixelXY(pose, consumer, 6, 9, color);
		flatPixelXY(pose, consumer, 9, 9, color);
	}

	private static void renderThreePixelCorner(Pose pose, VertexConsumer consumer, int x, int y,
		int xStep, int yStep, int color) {
		flatPixelXY(pose, consumer, x, y, color);
		flatPixelXY(pose, consumer, x + xStep, y, color);
		flatPixelXY(pose, consumer, x, y + yStep, color);
	}

	private static void flatPixelXY(Pose pose, VertexConsumer consumer, int x, int y, int color) {
		float pixel = 1 / 16f;
		float x0 = x * pixel - .5f;
		float y0 = y * pixel - .5f;
		float x1 = (x + 1) * pixel - .5f;
		float y1 = (y + 1) * pixel - .5f;
		float z = 1 / 512f;
		consumer.addVertex(pose, x0, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x0, y0, z).setColor(color);
	}

	private static void submitFactoryPanelCount(PoseStack ms, SubmitNodeCollector collector,
		MutableComponent count) {
		if (count == null || count.getString().isEmpty())
			return;

		StringBuilder visible = new StringBuilder();
		for (char c : count.getString().toCharArray())
			if (c >= '0' && c <= '9' || c == '/' || c == '?' || c == '\u221e'
				|| c == '\u25A4' || c == '\u23F6')
				visible.append(c);
		if (visible.isEmpty())
			return;

		int foreground = 0xFFEDEDED;
		if (count.getStyle().getColor() != null)
			foreground = 0xFF000000 | count.getStyle().getColor().getValue();
		String text = visible.toString();
		int finalForeground = foreground;
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> renderFactoryPanelAmount(pose, consumer, text, finalForeground));
	}

	private static void renderFactoryPanelAmount(Pose pose, VertexConsumer consumer, String text, int foreground) {
		float gap = .3f;
		float cell = Math.min(.4f, (9.25f - gap * Math.max(0, text.length() - 1)) / (5 * text.length()));
		float characterWidth = 5 * cell;
		float startX = 5.7f;
		long stackIcons = text.chars()
			.filter(c -> c == '\u25A4')
			.count();
		startX -= stackIcons * (characterWidth + gap);
		float startY = 10.45f;

		for (int i = 0; i < text.length(); i++) {
			String[] rows = factoryPanelGlyph(text.charAt(i));
			int mirroredIndex = text.length() - 1 - i;
			float digitStartX = startX + mirroredIndex * (characterWidth + gap);
			for (int row = 0; row < rows.length; row++) {
				String bits = rows[row];
				for (int column = 0; column < bits.length(); column++) {
					if (bits.charAt(column) != '1')
						continue;
					float x = digitStartX + (bits.length() - 1 - column) * cell;
					float y = startY + row * cell;
					flatSubPixelXY(pose, consumer, x, y, cell * 1.55f, 3 / 128f, 0xFF202020);
					flatSubPixelXY(pose, consumer, x, y, cell, 1 / 32f, foreground);
				}
			}
		}
	}

	private static String[] factoryPanelGlyph(char c) {
		return switch (c) {
			case '0' -> new String[] {"01110", "10001", "10011", "10101", "11001", "10001", "01110"};
			case '1' -> new String[] {"00100", "01100", "00100", "00100", "00100", "00100", "01110"};
			case '2' -> new String[] {"01110", "10001", "00001", "00010", "00100", "01000", "11111"};
			case '3' -> new String[] {"11110", "00001", "00010", "00110", "00001", "10001", "01110"};
			case '4' -> new String[] {"00010", "00110", "01010", "10010", "11111", "00010", "00010"};
			case '5' -> new String[] {"11111", "10000", "11110", "00001", "00001", "10001", "01110"};
			case '6' -> new String[] {"00110", "01000", "10000", "11110", "10001", "10001", "01110"};
			case '7' -> new String[] {"11111", "00001", "00010", "00100", "01000", "01000", "01000"};
			case '8' -> new String[] {"01110", "10001", "10001", "01110", "10001", "10001", "01110"};
			case '9' -> new String[] {"01110", "10001", "10001", "01111", "00001", "00010", "01100"};
			case '/' -> new String[] {"00001", "00010", "00010", "00100", "01000", "01000", "10000"};
			case '?' -> new String[] {"01110", "10001", "00001", "00010", "00100", "00000", "00100"};
			case '\u221e' -> new String[] {"00000", "00000", "01010", "10101", "01010", "00000", "00000"};
			case '\u25A4' -> new String[] {"11111", "00000", "11111", "00000", "11111", "00000", "11111"};
			case '\u23F6' -> new String[] {"00100", "01110", "11111", "00100", "00100", "00100", "00100"};
			default -> new String[] {"00000", "00000", "00000", "00000", "00000", "00000", "00000"};
		};
	}

	private static void flatSubPixelXY(Pose pose, VertexConsumer consumer, float x, float y, float sizeScale,
		float z, int color) {
		float pixel = 1 / 16f;
		float size = pixel * sizeScale;
		float centerX = x * pixel - .5f;
		float centerY = y * pixel - .5f;
		float x0 = centerX - size / 2;
		float y0 = centerY - size / 2;
		float x1 = centerX + size / 2;
		float y1 = centerY + size / 2;
		consumer.addVertex(pose, x0, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x0, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y1, z).setColor(color);
		consumer.addVertex(pose, x1, y0, z).setColor(color);
		consumer.addVertex(pose, x0, y0, z).setColor(color);
	}

	public static void renderFactoryPanelValueBox(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera,
		float partialTicks) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult result))
			return;
		if (!(mc.level.getBlockEntity(result.getBlockPos()) instanceof SmartBlockEntity sbe))
			return;

		BlockPos pos = result.getBlockPos();
		BlockState state = mc.level.getBlockState(pos);
		Vec3 localHit = result.getLocation().subtract(Vec3.atLowerCornerOf(pos));
		ItemStack mainhandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

		for (BlockEntityBehaviour b : sbe.getAllBehaviours()) {
			if (!(b instanceof FactoryPanelBehaviour behaviour) || !behaviour.isActive())
				continue;
			ValueBoxTransform slotPositioning = behaviour.getSlotPositioning();
			if (!slotPositioning.shouldRender(mc.level, pos, state)
				|| !slotPositioning.testHit(mc.level, pos, state, localHit)
				|| !behaviour.mayInteract(mc.player))
				continue;

			ItemStack filter = behaviour.getFilter();
			boolean isFilterSlotted = filter.getItem() instanceof FilterItem;
			AABB emptyBB = new AABB(Vec3.ZERO, Vec3.ZERO);
			AABB bb = isFilterSlotted ? emptyBB.inflate(.45f, .31f, .2f) : emptyBB.inflate(.25f);
			ValueBox box = new ItemValueBox(behaviour.getLabel(), bb, pos, filter,
				behaviour.getCountLabelForValueBox());
			box.passive(behaviour.bypassesInput(mainhandItem))
				.transform(slotPositioning)
				.render(ms, buffer, camera, partialTicks);
			return;
		}
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		HitResult target = mc.hitResult;
		if (!(target instanceof BlockHitResult result))
			return;
		if (mc.level == null || mc.player == null)
			return;
		if (mc.player.isShiftKeyDown())
			return;

		ClientLevel world = mc.level;
		BlockPos pos = result.getBlockPos();
		BlockState state = world.getBlockState(pos);

		if (!(world.getBlockEntity(pos) instanceof SmartBlockEntity sbe))
			return;

		ItemStack mainhandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

		for (BlockEntityBehaviour b : sbe.getAllBehaviours()) {
			if (!(b instanceof FilteringBehaviour behaviour))
				continue;

			if (behaviour instanceof SidedFilteringBehaviour sidedFilteringBehaviour) {
				behaviour = sidedFilteringBehaviour.get(result.getDirection());
				if (behaviour == null)
					continue;
			}

			if (!behaviour.isActive())
				continue;
			if (behaviour.slotPositioning instanceof ValueBoxTransform.Sided)
				((Sided) behaviour.slotPositioning).fromSide(result.getDirection());
			if (!behaviour.slotPositioning.shouldRender(world, pos, state))
				continue;
			if (!behaviour.mayInteract(mc.player))
				continue;

			ItemStack filter = behaviour.getFilter();
			boolean isFilterSlotted = filter.getItem() instanceof FilterItem;
			boolean showCount = behaviour.isCountVisible();
			Component label = behaviour.getLabel();
			boolean hit = behaviour.slotPositioning.testHit(world, pos, state, target.getLocation()
				.subtract(Vec3.atLowerCornerOf(pos)));

			AABB emptyBB = new AABB(Vec3.ZERO, Vec3.ZERO);
			AABB bb = isFilterSlotted ? emptyBB.inflate(.45f, .31f, .2f) : emptyBB.inflate(.25f);

			ValueBox box = new ItemValueBox(label, bb, pos, filter, behaviour.getCountLabelForValueBox());
			box.passive(!hit || behaviour.bypassesInput(mainhandItem));

			Outliner.getInstance()
				.showOutline(Pair.of("filter" + behaviour.netId(), pos), box.transform(behaviour.slotPositioning))
				.lineWidth(1 / 64f)
				.withFaceTexture(hit ? AllSpecialTextures.THIN_CHECKERED : null)
				.highlightFace(result.getDirection());

			if (!hit)
				continue;

			List<MutableComponent> tip = new ArrayList<>();
			tip.add(label.copy());
			tip.add(behaviour.getTip());
			if (showCount)
				tip.add(behaviour.getAmountTip());

			CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
		}
	}

	public static void renderOnBlockEntity(SmartBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		if (be == null || be.isRemoved())
			return;

		Level level = be.getLevel();
		BlockPos blockPos = be.getBlockPos();

		for (BlockEntityBehaviour b : be.getAllBehaviours()) {
			if (!(b instanceof FilteringBehaviour behaviour))
				continue;
			if (behaviour instanceof FactoryPanelBehaviour)
				continue;
			if (be instanceof TrackObserverBlockEntity)
				continue;
			if (be instanceof SmartObserverBlockEntity)
				continue;

			if (!be.isVirtual()) {
				Entity cameraEntity = Minecraft.getInstance()
					.getCameraEntity();
				if (cameraEntity != null && level == cameraEntity.level()) {
					float max = behaviour.getRenderDistance();
					if (cameraEntity.position()
						.distanceToSqr(VecHelper.getCenterOf(blockPos)) > (max * max))
						continue;
				}
			}

			if (!behaviour.isActive())
				continue;
			boolean thresholdSwitchFilter = be instanceof ThresholdSwitchBlockEntity;
			if (behaviour.getFilter().isEmpty() && !(behaviour instanceof SidedFilteringBehaviour)
				&& !thresholdSwitchFilter)
				continue;

			ValueBoxTransform slotPositioning = behaviour.slotPositioning;
			BlockState blockState = be.getBlockState();

			if (slotPositioning instanceof Sided sided) {
				Direction side = sided.getSide();
				for (Direction d : Iterate.directions) {
					ItemStack filter = behaviour.getFilter(d);
					if (filter.isEmpty())
						continue;

					sided.fromSide(d);
					if (!slotPositioning.shouldRender(level, blockPos, blockState))
						continue;

					ms.pushPose();
					slotPositioning.transform(level, blockPos, blockState, ms);
					if (AllBlocks.CONTRAPTION_CONTROLS.has(blockState))
						ValueBoxRenderer.renderFlatItemIntoValueBox(filter, ms, buffer, light, overlay);
					else
						ValueBoxRenderer.renderItemIntoValueBox(filter, ms, buffer, light, overlay);
					ms.popPose();
				}
				sided.fromSide(side);
			} else if (slotPositioning.shouldRender(level, blockPos, blockState)) {
				ms.pushPose();
				slotPositioning.transform(level, blockPos, blockState, ms);
				ValueBoxRenderer.renderItemIntoValueBox(behaviour.getFilter(), ms, buffer, light, overlay);
				ms.popPose();
			}
		}
	}

	public static void submitOnBlockEntity(SmartBlockEntity be, PoseStack ms, SubmitNodeCollector collector, int light) {
		if (be == null || be.isRemoved())
			return;

		Level level = be.getLevel();
		BlockPos blockPos = be.getBlockPos();

		for (BlockEntityBehaviour b : be.getAllBehaviours()) {
			if (!(b instanceof FilteringBehaviour behaviour))
				continue;
			if (behaviour instanceof FactoryPanelBehaviour)
				continue;
			if (be instanceof TrackObserverBlockEntity)
				continue;

			if (!be.isVirtual()) {
				Entity cameraEntity = Minecraft.getInstance()
					.getCameraEntity();
				if (cameraEntity != null && level == cameraEntity.level()) {
					float max = behaviour.getRenderDistance();
					if (cameraEntity.position()
						.distanceToSqr(VecHelper.getCenterOf(blockPos)) > max * max)
						continue;
				}
			}

			if (!behaviour.isActive())
				continue;
			boolean thresholdSwitchFilter = be instanceof ThresholdSwitchBlockEntity;
			boolean smartObserverFilter = be instanceof SmartObserverBlockEntity;
			boolean smartChuteFilter = be instanceof SmartChuteBlockEntity;
			if (!thresholdSwitchFilter && !smartObserverFilter && !(be instanceof SmartChuteBlockEntity))
				submitHoveredValueBox(be, behaviour, ms, collector);
			if (behaviour.getFilter().isEmpty() && !(behaviour instanceof SidedFilteringBehaviour)
				&& !thresholdSwitchFilter)
				continue;

			ValueBoxTransform slotPositioning = behaviour.slotPositioning;
			BlockState blockState = be.getBlockState();

			if (slotPositioning instanceof Sided sided) {
				Direction previousSide = sided.getSide();
				for (Direction direction : Iterate.directions) {
					ItemStack filter = behaviour.getFilter(direction);
					if (filter.isEmpty() && !thresholdSwitchFilter)
						continue;

					sided.fromSide(direction);
					if (!slotPositioning.shouldRender(level, blockPos, blockState))
						continue;

					if (thresholdSwitchFilter) {
						Vec3 offset = slotPositioning.getLocalOffset(level, blockPos, blockState);
						Vec3 normal = Vec3.atLowerCornerOf(direction.getUnitVec3i());
						if (shouldRenderThresholdFilterOverlay(blockPos, offset, direction))
							renderThresholdFilterOverlay(ms, collector, offset, normal, blockState, direction,
								!filter.isEmpty());
						if (!filter.isEmpty())
							renderThresholdFilterItem(filter, ms, collector, offset, normal, blockState, direction,
								light);
						continue;
					}
					if (smartObserverFilter) {
						Vec3 offset = slotPositioning.getLocalOffset(level, blockPos, blockState);
						if (offset != null) {
							Vec3 normal = Vec3.atLowerCornerOf(direction.getUnitVec3i());
							renderThresholdFilterItem(filter, ms, collector, offset, normal, blockState, direction,
								light);
						}
						continue;
					}
					if (smartChuteFilter) {
						Vec3 offset = slotPositioning.getLocalOffset(level, blockPos, blockState);
						if (offset != null) {
							Vec3 normal = Vec3.atLowerCornerOf(direction.getUnitVec3i());
							renderThresholdFilterItem(filter, ms, collector, offset, normal, blockState, direction,
								light);
						}
						continue;
					}

					ms.pushPose();
					slotPositioning.transform(level, blockPos, blockState, ms);
					if (AllBlocks.CONTRAPTION_CONTROLS.has(blockState))
						ValueBoxRenderer.submitFlatItemIntoValueBox(filter, ms, collector, light);
					else
						ValueBoxRenderer.submitItemIntoValueBox(filter, ms, collector, light);
					ms.popPose();
				}
				sided.fromSide(previousSide);
			} else if (slotPositioning.shouldRender(level, blockPos, blockState)) {
				ms.pushPose();
				slotPositioning.transform(level, blockPos, blockState, ms);
				if (thresholdSwitchFilter) {
					boolean hasFilter = !behaviour.getFilter().isEmpty();
					collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
						if (hasFilter)
							renderFilterCorners(pose, consumer);
						else
							renderFilterDots(pose, consumer);
					});
				}
				ValueBoxRenderer.submitItemIntoValueBox(behaviour.getFilter(), ms, collector, light);
				ms.popPose();
			}
		}
	}

	private static void submitHoveredValueBox(SmartBlockEntity be, FilteringBehaviour behaviour,
		PoseStack ms, SubmitNodeCollector collector) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.player.isShiftKeyDown()
			|| !(mc.hitResult instanceof BlockHitResult hit)
			|| !hit.getBlockPos().equals(be.getBlockPos()) || !behaviour.mayInteract(mc.player))
			return;

		Level level = be.getLevel();
		BlockPos pos = be.getBlockPos();
		BlockState state = be.getBlockState();
		ValueBoxTransform transform = behaviour.getSlotPositioning();
		Direction previousSide = null;
		if (transform instanceof Sided sided) {
			previousSide = sided.getSide();
			sided.fromSide(hit.getDirection());
		}

		Vec3 localHit = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
		boolean hovered = transform.shouldRender(level, pos, state)
			&& transform.testHit(level, pos, state, localHit);
		if (hovered) {
			boolean hasFilter = !behaviour.getFilter(hit.getDirection()).isEmpty();
			Vec3 offset = transform.getLocalOffset(level, pos, state);
			Direction face = hit.getDirection();
			Vec3 normal = Vec3.atLowerCornerOf(face.getUnitVec3i());
			if (offset == null) {
				if (transform instanceof Sided sided && previousSide != null)
					sided.fromSide(previousSide);
				return;
			}
			ms.pushPose();
			ms.translate(offset.x + normal.x / 512d, offset.y + normal.y / 512d,
				offset.z + normal.z / 512d);
			rotateToFace(ms, face);
			collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
				if (hasFilter)
					renderFilterCorners(pose, consumer);
				else
					renderFilterDots(pose, consumer);
			});
			ms.popPose();
		}

		if (transform instanceof Sided sided && previousSide != null)
			sided.fromSide(previousSide);
	}

	private static boolean shouldRenderThresholdFilterOverlay(BlockPos pos, Vec3 offset, Direction side) {
		HitResult hitResult = Minecraft.getInstance().hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(pos)
			|| blockHit.getDirection() != side)
			return false;

		Vec3 localHit = blockHit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
		double halfSize = 3 / 16d;
		return switch (side.getAxis()) {
			case X -> Math.abs(localHit.y - offset.y) <= halfSize && Math.abs(localHit.z - offset.z) <= halfSize;
			case Y -> Math.abs(localHit.x - offset.x) <= halfSize && Math.abs(localHit.z - offset.z) <= halfSize;
			case Z -> Math.abs(localHit.x - offset.x) <= halfSize && Math.abs(localHit.y - offset.y) <= halfSize;
		};
	}

	private static void renderThresholdFilterOverlay(PoseStack ms, SubmitNodeCollector collector, Vec3 offset,
		Vec3 normal, BlockState state, Direction side, boolean hasFilter) {
		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d + normal.x / 512d,
			offset.y + normal.y / 32d + normal.y / 512d,
			offset.z + normal.z / 32d + normal.z / 512d);
		rotateThresholdFilterSlot(ms, state, side);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			if (hasFilter)
				renderFilterCorners(pose, consumer);
			else
				renderFilterDots(pose, consumer);
		});
		ms.popPose();
	}

	private static void renderThresholdFilterItem(ItemStack filter, PoseStack ms, SubmitNodeCollector collector,
		Vec3 offset, Vec3 normal, BlockState state, Direction side, int light) {
		ms.pushPose();
		ms.translate(offset.x + normal.x / 32d, offset.y + normal.y / 32d, offset.z + normal.z / 32d);
		rotateThresholdFilterSlot(ms, state, side);
		ms.scale(.5f, .5f, .5f);
		renderThresholdFilterItemPass(filter, ms, collector, light);
		ms.pushPose();
		ms.mulPose(Axis.YP.rotationDegrees(180));
		ms.translate(0, 0, 1 / 128f);
		renderThresholdFilterItemPass(filter, ms, collector, light);
		ms.popPose();
		ms.popPose();
	}

	private static void renderThresholdFilterItemPass(ItemStack filter, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .5f);
	}

	private static void rotateThresholdFilterSlot(PoseStack ms, BlockState state, Direction side) {
		rotateToFace(ms, side);
		if (!side.getAxis().isVertical())
			return;

		Direction facing = state.getValue(DirectedDirectionalBlock.FACING);
		if (!facing.getAxis().isHorizontal())
			return;
		float angle = switch (facing) {
			case NORTH -> 0;
			case SOUTH -> 180;
			case WEST -> 90;
			case EAST -> 270;
			default -> 0;
		};
		ms.mulPose(Axis.ZP.rotationDegrees(angle));
	}

	private static void rotateToFace(PoseStack ms, Direction face) {
		switch (face) {
			case SOUTH -> { }
			case NORTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
			case EAST -> ms.mulPose(Axis.YP.rotationDegrees(90));
			case WEST -> ms.mulPose(Axis.YP.rotationDegrees(270));
			case UP -> ms.mulPose(Axis.XP.rotationDegrees(270));
			case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(90));
		}
	}
}
