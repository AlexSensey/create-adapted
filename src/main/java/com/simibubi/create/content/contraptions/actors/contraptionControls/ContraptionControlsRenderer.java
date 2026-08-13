package com.simibubi.create.content.contraptions.actors.contraptionControls;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement.ElevatorFloorSelection;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.FlatGuiItemRenderer;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class ContraptionControlsRenderer extends SmartBlockEntityRenderer<ContraptionControlsBlockEntity> {
	private static final int MAX_FLOOR_LABEL_LENGTH = 4;
	private static final float FLOOR_LABEL_MAX_SCALE = .22f;
	private static final float FLOOR_LABEL_FIT_WIDTH = 3.4f;

	private BlockStateModelPart buttonModel;
	private List<BlockStateModelPart> indicatorModels;

	public ContraptionControlsRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ContraptionControlsBlockEntity blockEntity, float pt, PoseStack ms,
							  MultiBufferSource buffer, int light, int overlay) {
		// Rendering is submitted through the 26.2 BlockEntityRenderer submit API.
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new ContraptionControlsRenderState();
	}

	@Override
	public void extractRenderState(ContraptionControlsBlockEntity be, BlockEntityRenderState state, float partialTicks,
		net.minecraft.world.phys.Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof ContraptionControlsRenderState controlsState) {
			controlsState.blockEntity = be;
			controlsState.blockState = be.getBlockState();
			controlsState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof ContraptionControlsRenderState controlsState))
			return;
		ContraptionControlsBlockEntity be = controlsState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		BlockState blockState = controlsState.blockState;
		Direction facing = blockState.getValue(ContraptionControlsBlock.FACING)
			.getOpposite();
		float pt = controlsState.partialTicks;

		Vec3 buttonMovementAxis = VecHelper.rotate(new Vec3(0, 1, -.325), AngleHelper.horizontalAngle(facing), Axis.Y);
		Vec3 buttonMovement = buttonMovementAxis.scale(-0.07f + -1 / 24f * be.button.getValue(pt));
		Vec3 buttonOffset = buttonMovementAxis.scale(0.07f);

		ms.pushPose();
		ms.translate(buttonMovement.x, buttonMovement.y, buttonMovement.z);
		ms.translate(buttonOffset.x, buttonOffset.y, buttonOffset.z);
		submitFacing(getButtonModel(), facing, ms, collector, state.lightCoords);
		ms.popPose();

		int i = (((int) be.indicator.getValue(pt) / 45) % 8) + 8;
		submitFacing(getIndicatorModels().get(i % 8), facing, ms, collector, state.lightCoords);
		submitFilterItem(be.filtering.getFilter(), blockState, ms, collector, state.lightCoords, be.button.getValue(pt));
	}

	public static void renderInContraption(MovementContext ctx, VirtualRenderWorld renderWorld,
										   ContraptionMatrices matrices, MultiBufferSource buffer) {
		if (!AllBlocks.CONTRAPTION_CONTROLS.has(ctx.state))
			return;
		if (!(ctx.contraption instanceof ElevatorContraption elevator))
			return;
		if (!(ctx.temporaryData instanceof ElevatorFloorSelection)) {
			ElevatorFloorSelection selection = new ElevatorFloorSelection();
			ContraptionControlsMovement.initFloorSelectionAtCurrentTarget(selection, elevator);
			ctx.temporaryData = selection;
		}

		ElevatorFloorSelection efs = (ElevatorFloorSelection) ctx.temporaryData;
		ContraptionControlsMovement.tickFloorSelection(efs, elevator);

		Entity cameraEntity = Minecraft.getInstance()
			.getCameraEntity();
		float playerDistance = (float) (ctx.position == null || cameraEntity == null ? 0
			: ctx.position.distanceToSqr(cameraEntity.getEyePosition()));

		float flicker = ThreadLocalRandom.current()
			.nextFloat();
		Couple<Integer> couple = DyeHelper.getDyeColors(efs.targetYEqualsSelection ? DyeColor.WHITE : DyeColor.ORANGE);
		int brightColor = couple.getFirst();
		int darkColor = couple.getSecond();
		int flickeringBrightColor = Color.mixColors(brightColor, darkColor, flicker / 4);
		Font fontRenderer = Minecraft.getInstance().font;
		float shadowOffset = .5f;

		String text = efs.currentShortName.isBlank() ? String.valueOf(efs.currentIndex + 1) : efs.currentShortName;
		String description = efs.currentLongName;
		PoseStack ms = matrices.getViewProjection();
		TransformStack<?> msr = TransformStack.of(ms);

		float buttonDepth = 0;
		if (ctx.contraption.getBlockEntityClientSide(ctx.localPos) instanceof ContraptionControlsBlockEntity cbe)
			buttonDepth = -1 / 24f * cbe.button.getValue(AnimationTickHolder.getPartialTicks());

		ms.pushPose();
		msr.translate(ctx.localPos);
		msr.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(ctx.state.getValue(ContraptionControlsBlock.FACING))),
			Direction.UP);
		ms.translate(0.275f + 0.125f, 1 + 2 / 16f, 0.5f);
		msr.rotate(AngleHelper.rad(67.5f), Direction.WEST);

		if (!text.isBlank() && playerDistance < 100) {
			int actualWidth = fontRenderer.width(text);
			int width = Math.max(actualWidth, 12);
			float scale = 1 / (5f * (width - .5f));
			float heightCentering = (width - 8f) / 2;

			ms.pushPose();
			ms.translate(0, .15f, buttonDepth - .25f);
			ms.scale(scale, -scale, scale);
			ms.translate((float) Math.max(0, width - actualWidth) / 2, heightCentering, 0);
			NixieTubeRenderer.drawInWorldString(ms, buffer, text, flickeringBrightColor);
			ms.translate(shadowOffset, shadowOffset, -1 / 16f);
			NixieTubeRenderer.drawInWorldString(ms, buffer, text, Color.mixColors(darkColor, 0, .35f));
			ms.popPose();
		}

		if (!description.isBlank() && playerDistance < 20) {
			int actualWidth = fontRenderer.width(description);
			int width = Math.max(actualWidth, 55);
			float scale = 1 / (3f * (width - .5f));
			float heightCentering = (width - 8f) / 2;

			ms.pushPose();
			ms.translate(-.0635f, 0.06f, buttonDepth - .25f);
			ms.scale(scale, -scale, scale);
			ms.translate((float) Math.max(0, width - actualWidth) / 2, heightCentering, 0);
			NixieTubeRenderer.drawInWorldString(ms, buffer, description, flickeringBrightColor);
			ms.popPose();
		}

		ms.popPose();
	}

	public static void submitInContraption(MovementContext ctx, PoseStack ms, SubmitNodeCollector collector, int light) {
		if (!AllBlocks.CONTRAPTION_CONTROLS.has(ctx.state))
			return;
		if (!(ctx.contraption instanceof ElevatorContraption elevator)) {
			ItemStack filter = ContraptionControlsMovement.getFilter(ctx);
			if (filter != null) {
				float buttonDepth = 0;
				if (ctx.contraption.getBlockEntityClientSide(ctx.localPos) instanceof ContraptionControlsBlockEntity cbe)
					buttonDepth = cbe.button.getValue(AnimationTickHolder.getPartialTicks());
				submitFilterItem(filter, ctx.state, ms, collector, light, buttonDepth);
			}
			return;
		}
		if (!(ctx.temporaryData instanceof ElevatorFloorSelection)) {
			ElevatorFloorSelection selection = new ElevatorFloorSelection();
			ContraptionControlsMovement.initFloorSelectionAtCurrentTarget(selection, elevator);
			ctx.temporaryData = selection;
		}

		ElevatorFloorSelection efs = (ElevatorFloorSelection) ctx.temporaryData;
		ContraptionControlsMovement.tickFloorSelection(efs, elevator);

		String text = efs.currentShortName.isBlank() ? String.valueOf(efs.currentIndex + 1) : efs.currentShortName;
		if (text.isBlank())
			text = "?";
		if (text.length() > MAX_FLOOR_LABEL_LENGTH)
			text = text.substring(0, MAX_FLOOR_LABEL_LENGTH);

		Couple<Integer> colors = DyeHelper.getDyeColors(efs.targetYEqualsSelection ? DyeColor.WHITE : DyeColor.ORANGE);
		int color = 0xFF000000 | colors.getFirst();

		float buttonDepth = 0;
		if (ctx.contraption.getBlockEntityClientSide(ctx.localPos) instanceof ContraptionControlsBlockEntity cbe)
			buttonDepth = -1 / 24f * cbe.button.getValue(AnimationTickHolder.getPartialTicks());

		Direction facing = ctx.state.getValue(ContraptionControlsBlock.FACING);
		ms.pushPose();
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.translate(-.5f, -.5f, -.5f);
		ms.translate(.5f, 1 + 2 / 16f, 0.5f);
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-67.5f));
		ms.translate(0, .02f, buttonDepth - .25f);
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));

		int pixelWidth = Math.max(1, textPixelWidth(text));
		float textScale = Math.min(FLOOR_LABEL_MAX_SCALE, FLOOR_LABEL_FIT_WIDTH / pixelWidth);
		ms.scale(-textScale, textScale, textScale);

		final String label = text;
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> renderMovingControlsOverlay(pose, consumer, label, color));
		ms.popPose();
	}

	private static void submitFilterItem(ItemStack filter, BlockState state, PoseStack ms, SubmitNodeCollector collector,
		int light, float buttonValue) {
		if (filter == null || filter.isEmpty())
			return;

		Direction facing = state.getValue(ContraptionControlsBlock.FACING)
			.getOpposite();
		Vec3 buttonMovementAxis = VecHelper.rotate(new Vec3(0, 1, -.325), AngleHelper.horizontalAngle(facing), Axis.Y);
		Vec3 buttonMovement = buttonMovementAxis.scale(-0.07f + -1 / 24f * buttonValue);
		Vec3 buttonOffset = buttonMovementAxis.scale(0.07f);

		ms.pushPose();
		ms.translate(buttonMovement.x + buttonOffset.x, buttonMovement.y + buttonOffset.y,
			buttonMovement.z + buttonOffset.z);
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.translate(-.5f, -.5f, -.5f);
		ms.translate(.5f, 14.45f / 16f, 10.31f / 16f);
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-112.5f));
		ms.scale(.38f, .38f, .38f);
		submitValueBoxFrame(ms, collector);
		renderButtonFilterStack(filter, ms, collector, light);
		ms.popPose();
	}

	private static void submitValueBoxFrame(PoseStack ms, SubmitNodeCollector collector) {
		ms.pushPose();
		ms.translate(0, 0, 1 / 512f);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> renderValueBoxCorners(pose, consumer));
		ms.popPose();
	}

	private static void renderButtonFilterStack(ItemStack stack, PoseStack ms, SubmitNodeCollector collector, int light) {
		if (stack.isEmpty())
			return;

		ms.pushPose();
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90));
		FlatGuiItemRenderer.submit(stack, ms, collector, light, .5f);
		ms.popPose();
	}

	private static void renderValueBoxCorners(Pose pose, VertexConsumer consumer) {
		int color = 0xFFFFFFFF;
		renderThreePixelCorner(pose, consumer, 4, 4, 1, 1, color);
		renderThreePixelCorner(pose, consumer, 11, 4, -1, 1, color);
		renderThreePixelCorner(pose, consumer, 4, 11, 1, -1, color);
		renderThreePixelCorner(pose, consumer, 11, 11, -1, -1, color);
	}

	private static void renderThreePixelCorner(Pose pose, VertexConsumer consumer, int x, int y, int xDir, int yDir,
		int color) {
		valueBoxPixelXY(pose, consumer, x, y, color);
		valueBoxPixelXY(pose, consumer, x + xDir, y, color);
		valueBoxPixelXY(pose, consumer, x, y + yDir, color);
	}

	private static void valueBoxPixelXY(Pose pose, VertexConsumer consumer, int x, int y, int color) {
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, x * pixel - .5f, y * pixel - .5f, (x + 1) * pixel - .5f,
			(y + 1) * pixel - .5f, 0, color);
	}

	private static void renderMovingControlsOverlay(Pose pose, VertexConsumer consumer, String text, int color) {
		renderSmallText(pose, consumer, text, color);
	}

	private static void renderSmallText(Pose pose, VertexConsumer consumer, String text, int color) {
		int width = textPixelWidth(text);
		float startX = -width / 2f;
		float startY = -7 / 2f;
		float cursor = startX;
		for (int i = 0; i < text.length(); i++) {
			String[] rows = getSmallCharRows(text.charAt(i));
			renderSmallChar(pose, consumer, rows, cursor, startY, color);
			cursor += glyphWidth(rows) + 1;
		}
	}

	private static void renderSmallChar(Pose pose, VertexConsumer consumer, String[] rows, float xOffset, float yOffset,
		int color) {
		int minX = glyphMinX(rows);
		for (int y = 0; y < rows.length; y++) {
			String row = rows[y];
			for (int x = 0; x < row.length(); x++) {
				if (row.charAt(x) == '#')
					flatPixelXY(pose, consumer, xOffset + x - minX, yOffset + y, color);
			}
		}
	}

	private static String[] getSmallCharRows(char c) {
		return switch (Character.toUpperCase(c)) {
			case '0' -> new String[] { ".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###." };
			case '1' -> new String[] { "..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###." };
			case '2' -> new String[] { ".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####" };
			case '3' -> new String[] { "####.", "....#", "...#.", "..##.", "....#", "#...#", ".###." };
			case '4' -> new String[] { "...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#." };
			case '5' -> new String[] { "#####", "#....", "####.", "....#", "....#", "#...#", ".###." };
			case '6' -> new String[] { "..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###." };
			case '7' -> new String[] { "#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..." };
			case '8' -> new String[] { ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###." };
			case '9' -> new String[] { ".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.." };
			case 'A' -> new String[] { ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#" };
			case 'B' -> new String[] { "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####." };
			case 'C' -> new String[] { ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###." };
			case 'D' -> new String[] { "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####." };
			case 'E' -> new String[] { "#####", "#....", "#....", "####.", "#....", "#....", "#####" };
			case 'F' -> new String[] { "#####", "#....", "#....", "####.", "#....", "#....", "#...." };
			case 'G' -> new String[] { ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".###." };
			case 'H' -> new String[] { "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#" };
			case 'I' -> new String[] { "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####" };
			case 'J' -> new String[] { "..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##.." };
			case 'K' -> new String[] { "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#" };
			case 'L' -> new String[] { "#....", "#....", "#....", "#....", "#....", "#....", "#####" };
			case 'M' -> new String[] { "#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#" };
			case 'N' -> new String[] { "#...#", "##..#", "##..#", "#.#.#", "#..##", "#..##", "#...#" };
			case 'O' -> new String[] { ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###." };
			case 'P' -> new String[] { "####.", "#...#", "#...#", "####.", "#....", "#....", "#...." };
			case 'Q' -> new String[] { ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#" };
			case 'R' -> new String[] { "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#" };
			case 'S' -> new String[] { ".####", "#....", "#....", ".###.", "....#", "....#", "####." };
			case 'T' -> new String[] { "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.." };
			case 'U' -> new String[] { "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###." };
			case 'V' -> new String[] { "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#.." };
			case 'W' -> new String[] { "#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#" };
			case 'X' -> new String[] { "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#" };
			case 'Y' -> new String[] { "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.." };
			case 'Z' -> new String[] { "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####" };
			case '-' -> new String[] { ".....", ".....", ".....", "#####", ".....", ".....", "....." };
			case '_' -> new String[] { ".....", ".....", ".....", ".....", ".....", ".....", "#####" };
			case ' ' -> new String[] { ".....", ".....", ".....", ".....", ".....", ".....", "....." };
			default -> new String[] { ".....", "..#..", ".#.#.", "...#.", "..#..", ".....", "..#.." };
		};
	}

	private static int textPixelWidth(String text) {
		int width = 0;
		for (int i = 0; i < text.length(); i++) {
			if (i > 0)
				width++;
			width += glyphWidth(getSmallCharRows(text.charAt(i)));
		}
		return width;
	}

	private static int glyphWidth(String[] rows) {
		int minX = glyphMinX(rows);
		int maxX = Integer.MIN_VALUE;
		for (String row : rows) {
			for (int x = 0; x < row.length(); x++) {
				if (row.charAt(x) != '#')
					continue;
				maxX = Math.max(maxX, x);
			}
		}
		if (maxX < minX)
			return 3;
		return maxX - minX + 1;
	}

	private static int glyphMinX(String[] rows) {
		int minX = Integer.MAX_VALUE;
		for (String row : rows) {
			for (int x = 0; x < row.length(); x++) {
				if (row.charAt(x) == '#')
					minX = Math.min(minX, x);
			}
		}
		return minX == Integer.MAX_VALUE ? 0 : minX;
	}

	private static void flatPixelXY(Pose pose, VertexConsumer consumer, float x, float y, int color) {
		float pixel = 1 / 16f;
		flatQuadXY(pose, consumer, x * pixel, y * pixel, (x + 1) * pixel,
			(y + 1) * pixel, 1 / 64f, color);
	}

	private static void flatQuadXY(Pose pose, VertexConsumer consumer, float x0, float y0, float x1, float y1,
		float z, int color) {
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

	private static void submitFacing(BlockStateModelPart model, Direction facing, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		if (model == null)
			return;
		ms.pushPose();
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)));
		ms.translate(-.5f, -.5f, -.5f);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(model), BlockModelRenderState.EMPTY_TINTS,
			light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private BlockStateModelPart getButtonModel() {
		if (buttonModel != null)
			return buttonModel;
		return buttonModel = getModel(CreateStandaloneModels.CONTRAPTION_CONTROLS_BUTTON);
	}

	private List<BlockStateModelPart> getIndicatorModels() {
		if (indicatorModels != null)
			return indicatorModels;
		return indicatorModels = CreateStandaloneModels.CONTRAPTION_CONTROLS_INDICATOR.stream()
			.map(ContraptionControlsRenderer::getModel)
			.toList();
	}

	private static BlockStateModelPart getModel(StandaloneModelKey<BlockStateModelPart> key) {
		return Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
	}

	private static class ContraptionControlsRenderState extends BlockEntityRenderState {
		private ContraptionControlsBlockEntity blockEntity;
		private BlockState blockState;
		private float partialTicks;
	}
}
