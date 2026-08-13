package com.simibubi.create.content.processing.burner;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class BlazeBurnerRenderer extends SafeBlockEntityRenderer<BlazeBurnerBlockEntity> {

	private static final Identifier BURNER_FLAME_TEXTURE =
		Create.asResource("textures/block/blaze_burner_flame_scroll.png");
	private static final Identifier SUPER_BURNER_FLAME_TEXTURE =
		Create.asResource("textures/block/blaze_burner_flame_superheated_scroll.png");

	public BlazeBurnerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(BlazeBurnerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource,
		int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new BlazeBurnerRenderState();
	}

	@Override
	public void extractRenderState(BlazeBurnerBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof BlazeBurnerRenderState burnerState) {
			burnerState.blockEntity = be;
			burnerState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof BlazeBurnerRenderState burnerState))
			return;
		BlazeBurnerBlockEntity be = burnerState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		HeatLevel heatLevel = be.getHeatLevelForRender();
		if (heatLevel == HeatLevel.NONE)
			return;

		float animation = be.headAnimation.getValue(burnerState.partialTicks) * .175f;
		float horizontalAngle = AngleHelper.rad(be.headAngle.getValue(burnerState.partialTicks));
		boolean blockAbove = animation > 0.125f;
		float time = AnimationTickHolder.getRenderTime();
		float renderTick = time + (be.hashCode() % 13) * 16f;
		float offsetMult = heatLevel.isAtLeast(HeatLevel.FADING) ? 64 : 16;
		float offset = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMult;
		float offset1 = Mth.sin((float) ((renderTick / 16f + Math.PI) % (2 * Math.PI))) / offsetMult;
		float offset2 = Mth.sin((float) ((renderTick / 16f + Math.PI / 2) % (2 * Math.PI))) / offsetMult;
		float headY = offset - (animation * .75f);
		int light = LightCoordsUtil.pack(15, 15);

		BlockStateModelPart blaze = getModel(getBlazeModelKey(heatLevel, blockAbove));
		if (blaze != null)
			submitRotatingPart(ms, collector, blaze, horizontalAngle, headY, light);

		if (be.goggles) {
			BlockStateModelPart goggles = getModel(blaze == getModel(CreateStandaloneModels.BLAZE_INERT)
				? CreateStandaloneModels.BLAZE_GOGGLES_SMALL
				: CreateStandaloneModels.BLAZE_GOGGLES);
			if (goggles != null)
				submitRotatingPart(ms, collector, goggles, horizontalAngle, headY + 8 / 16f, light);
		}

		if (heatLevel.isAtLeast(HeatLevel.FADING)) {
			BlockStateModelPart rods = getModel(heatLevel == HeatLevel.SEETHING
				? CreateStandaloneModels.BLAZE_BURNER_SUPER_RODS
				: CreateStandaloneModels.BLAZE_BURNER_RODS);
			BlockStateModelPart rods2 = getModel(heatLevel == HeatLevel.SEETHING
				? CreateStandaloneModels.BLAZE_BURNER_SUPER_RODS_2
				: CreateStandaloneModels.BLAZE_BURNER_RODS_2);
			if (rods != null)
				submitTranslatedPart(ms, collector, rods, offset1 + animation + .125f, light);
			if (rods2 != null)
				submitTranslatedPart(ms, collector, rods2, offset2 + animation - 3 / 16f, light);
		}

		if (heatLevel.isAtLeast(HeatLevel.FADING) && blockAbove) {
			submitScrollingFlame(ms, collector, heatLevel, horizontalAngle, time, light);
		}
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource bufferSource, LerpedFloat headAngle, boolean conductor) {
	}

	public static void submitInContraption(MovementContext context, PoseStack ms, SubmitNodeCollector collector,
		int light, LerpedFloat headAngle, boolean conductor) {
		HeatLevel heatLevel = BlazeBurnerBlock.getHeatLevelOf(context.state);
		if (heatLevel == HeatLevel.NONE)
			return;

		// A captured burner is always shown lit, matching the pre-26.2 contraption renderer.
		if (!heatLevel.isAtLeast(HeatLevel.FADING))
			heatLevel = HeatLevel.FADING;

		float partialTicks = AnimationTickHolder.getPartialTicks();
		float horizontalAngle = AngleHelper.rad(headAngle.getValue(partialTicks));
		float time = AnimationTickHolder.getRenderTime();
		float renderTick = time + (context.hashCode() % 13) * 16f;
		float offsetMult = heatLevel.isAtLeast(HeatLevel.FADING) ? 64 : 16;
		float headY = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMult;
		float rodsY = Mth.sin((float) ((renderTick / 16f + Math.PI) % (2 * Math.PI))) / offsetMult + .125f;
		float rods2Y = Mth.sin((float) ((renderTick / 16f + Math.PI / 2) % (2 * Math.PI))) / offsetMult - 3 / 16f;
		int fullLight = LightCoordsUtil.pack(15, 15);

		BlockStateModelPart blaze = getModel(getBlazeModelKey(heatLevel, false));
		if (blaze != null)
			submitRotatingPart(ms, collector, blaze, horizontalAngle, headY, fullLight);

		if (context.blockEntityData.contains("Goggles")) {
			BlockStateModelPart goggles = getModel(CreateStandaloneModels.BLAZE_GOGGLES);
			if (goggles != null)
				submitRotatingPart(ms, collector, goggles, horizontalAngle, headY + 8 / 16f, fullLight);
		}

		if (conductor || context.blockEntityData.contains("TrainHat")) {
			BlockStateModelPart hat = getModel(CreateStandaloneModels.TRAIN_HAT);
			if (hat != null) {
				ms.pushPose();
				ms.translate(0, headY + .75f, 0);
				ms.translate(.5, .5, .5);
				ms.mulPose(Axis.YP.rotation(horizontalAngle + Mth.PI));
				ms.translate(-.5, -.5, -.5);
				submitPart(ms, collector, hat, fullLight);
				ms.popPose();
			}
		}

		BlockStateModelPart rods = getModel(heatLevel == HeatLevel.SEETHING
			? CreateStandaloneModels.BLAZE_BURNER_SUPER_RODS
			: CreateStandaloneModels.BLAZE_BURNER_RODS);
		BlockStateModelPart rods2 = getModel(heatLevel == HeatLevel.SEETHING
			? CreateStandaloneModels.BLAZE_BURNER_SUPER_RODS_2
			: CreateStandaloneModels.BLAZE_BURNER_RODS_2);
		if (rods != null)
			submitTranslatedPart(ms, collector, rods, rodsY, fullLight);
		if (rods2 != null)
			submitTranslatedPart(ms, collector, rods2, rods2Y, fullLight);
	}

	public static void renderShared(PoseStack ms, @Nullable PoseStack modelTransform, MultiBufferSource bufferSource,
		Level level, BlockState blockState, HeatLevel heatLevel, float animation, float horizontalAngle,
		boolean canDrawFlame, boolean drawGoggles, PartialModel drawHat, int hashCode) {
	}

	public static PartialModel getBlazeModel(HeatLevel heatLevel, boolean blockAbove) {
		if (heatLevel.isAtLeast(HeatLevel.SEETHING))
			return blockAbove ? AllPartialModels.BLAZE_SUPER_ACTIVE : AllPartialModels.BLAZE_SUPER;
		if (heatLevel.isAtLeast(HeatLevel.FADING))
			return blockAbove && heatLevel.isAtLeast(HeatLevel.KINDLED) ? AllPartialModels.BLAZE_ACTIVE
				: AllPartialModels.BLAZE_IDLE;
		return AllPartialModels.BLAZE_INERT;
	}

	private static StandaloneModelKey<BlockStateModelPart> getBlazeModelKey(HeatLevel heatLevel, boolean blockAbove) {
		if (heatLevel.isAtLeast(HeatLevel.SEETHING))
			return blockAbove ? CreateStandaloneModels.BLAZE_SUPER_ACTIVE : CreateStandaloneModels.BLAZE_SUPER;
		if (heatLevel.isAtLeast(HeatLevel.FADING))
			return blockAbove && heatLevel.isAtLeast(HeatLevel.KINDLED) ? CreateStandaloneModels.BLAZE_ACTIVE
				: CreateStandaloneModels.BLAZE_IDLE;
		return CreateStandaloneModels.BLAZE_INERT;
	}

	private static BlockStateModelPart getModel(StandaloneModelKey<BlockStateModelPart> key) {
		return Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
	}

	private static void submitRotatingPart(PoseStack ms, SubmitNodeCollector collector, BlockStateModelPart part,
		float horizontalAngle, float y, int light) {
		ms.pushPose();
		ms.translate(0, y, 0);
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotation(horizontalAngle));
		ms.translate(-.5, -.5, -.5);
		submitPart(ms, collector, part, light);
		ms.popPose();
	}

	private static void submitTranslatedPart(PoseStack ms, SubmitNodeCollector collector, BlockStateModelPart part,
		float y, int light) {
		ms.pushPose();
		ms.translate(0, y, 0);
		submitPart(ms, collector, part, light);
		ms.popPose();
	}

	public static void submitScrollingFlame(PoseStack ms, SubmitNodeCollector collector, HeatLevel heatLevel,
		float horizontalAngle, float time, int light) {
		float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();
		float vScroll = ((speed * time) % 1) * .5f;
		float uScroll = ((speed * time / 2) % 1) * .5f;
		Identifier texture = heatLevel == HeatLevel.SEETHING ? SUPER_BURNER_FLAME_TEXTURE : BURNER_FLAME_TEXTURE;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotation(horizontalAngle));
		ms.translate(-.5, -.5, -.5);
		collector.submitCustomGeometry(ms, com.simibubi.create.foundation.render.RenderTypes.scrollingCutout(texture),
			(pose, consumer) -> renderScrollingFlame(pose, consumer, uScroll, vScroll, light));
		ms.popPose();
	}

	private static void renderScrollingFlame(PoseStack.Pose pose, VertexConsumer consumer, float uScroll, float vScroll,
		int light) {
		renderFlameSide(pose, consumer, .875f, .8125f, 1, light, uScroll, vScroll);
		renderFlameSide(pose, consumer, .125f, .1875f, -1, light, uScroll, vScroll);
		renderFlameSideZ(pose, consumer, .875f, .8125f, 1, light, uScroll, vScroll);
		renderFlameSideZ(pose, consumer, .125f, .1875f, -1, light, uScroll, vScroll);
	}

	private static void renderFlameSide(PoseStack.Pose pose, VertexConsumer consumer, float z0, float z1, float normalZ,
		int light, float uScroll, float vScroll) {
		renderFlameQuad(pose, consumer, .125f, 1f, z0, .875f, 1f, z0, .8125f, .9375f, z1, .1875f, .9375f, z1,
			normalZ, 0, light, uScroll, vScroll, .0625f, .9375f, 0, .0625f);
		renderFlameQuad(pose, consumer, .1875f, .9375f, z1, .8125f, .9375f, z1, .8125f, .375f, z1, .1875f, .375f,
			z1, normalZ, 0, light, uScroll, vScroll, .125f, .875f, .0625f, .75f);
		renderFlameQuad(pose, consumer, .1875f, .375f, z1, .8125f, .375f, z1, .6875f, .25f, .5f + normalZ * .1875f,
			.3125f, .25f, .5f + normalZ * .1875f, normalZ, 0, light, uScroll, vScroll, .25f, .75f, .75f, 1f);
	}

	private static void renderFlameSideZ(PoseStack.Pose pose, VertexConsumer consumer, float x0, float x1,
		float normalX, int light, float uScroll, float vScroll) {
		renderFlameQuad(pose, consumer, x0, 1f, .875f, x0, 1f, .125f, x1, .9375f, .1875f, x1, .9375f, .8125f,
			0, normalX, light, uScroll, vScroll, .0625f, .9375f, 0, .0625f);
		renderFlameQuad(pose, consumer, x1, .9375f, .8125f, x1, .9375f, .1875f, x1, .375f, .1875f, x1, .375f,
			.8125f, 0, normalX, light, uScroll, vScroll, .125f, .875f, .0625f, .75f);
		renderFlameQuad(pose, consumer, x1, .375f, .8125f, x1, .375f, .1875f, .5f + normalX * .1875f, .25f, .3125f,
			.5f + normalX * .1875f, .25f, .6875f, 0, normalX, light, uScroll, vScroll, .25f, .75f, .75f, 1f);
	}

	private static void renderFlameQuad(PoseStack.Pose pose, VertexConsumer consumer,
		float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
		float x3, float y3, float z3, float normalZ, float normalX, int light, float uScroll, float vScroll,
		float minU, float maxU, float minV, float maxV) {
		addFlameVertex(pose, consumer, x0, y0, z0, maxU, minV, uScroll, vScroll, normalX, normalZ, light);
		addFlameVertex(pose, consumer, x1, y1, z1, minU, minV, uScroll, vScroll, normalX, normalZ, light);
		addFlameVertex(pose, consumer, x2, y2, z2, minU, maxV, uScroll, vScroll, normalX, normalZ, light);
		addFlameVertex(pose, consumer, x3, y3, z3, maxU, maxV, uScroll, vScroll, normalX, normalZ, light);
	}

	private static void addFlameVertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z,
		float u, float v, float uScroll, float vScroll, float normalX, float normalZ, int light) {
		consumer.addVertex(pose, x, y, z)
			.setColor(1.0f, 1.0f, 1.0f, 1.0f)
			.setUv(u * .5f + uScroll, v * .5f + vScroll)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, normalX, 0.0F, normalZ);
	}

	private static void submitPart(PoseStack ms, SubmitNodeCollector collector, BlockStateModelPart part, int light) {
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}

	private static class BlazeBurnerRenderState extends BlockEntityRenderState {
		private BlazeBurnerBlockEntity blockEntity;
		private float partialTicks;
	}

}
