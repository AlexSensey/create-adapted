package com.simibubi.create.content.redstone.nixieTube;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.redstone.nixieTube.DoubleFaceAttachedBlock.DoubleAttachFace;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity.ComputerSignal.TubeDisplay;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.utility.DyeHelper;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.theme.Color;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class NixieTubeRenderer extends SafeBlockEntityRenderer<NixieTubeBlockEntity> {
	private static final Random RANDOM = new Random();
	private static final int GLOW_VIEW_DISTANCE = 96;
	private final Map<StandaloneModelKey<BlockStateModelPart>, BlockStateModelPart> tintedModels = new HashMap<>();

	public NixieTubeRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(NixieTubeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new NixieTubeRenderState();
	}

	@Override
	public void extractRenderState(NixieTubeBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (!(state instanceof NixieTubeRenderState nixieState))
			return;
		nixieState.blockState = be.getBlockState();
		Couple<String> displayed = be.getDisplayedStrings();
		nixieState.first = displayed.getFirst();
		nixieState.second = displayed.getSecond();
		nixieState.signalState = be.signalState;
		nixieState.computerFirst = be.computerSignal == null ? null : TubeData.copy(be.computerSignal.first);
		nixieState.computerSecond = be.computerSignal == null ? null : TubeData.copy(be.computerSignal.second);
		nixieState.renderTime = AnimationTickHolder.getRenderTime();
		nixieState.distanceToCameraSqr = Vec3.atCenterOf(be.getBlockPos()).distanceToSqr(cameraPos);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof NixieTubeRenderState nixieState) || nixieState.blockState == null)
			return;

		BlockState blockState = nixieState.blockState;
		DoubleAttachFace face = blockState.getValue(NixieTubeBlock.FACE);
		float yRot = AngleHelper.horizontalAngle(blockState.getValue(NixieTubeBlock.FACING)) - 90
			+ (face == DoubleAttachFace.WALL_REVERSED ? 180 : 0);
		float xRot = face == DoubleAttachFace.WALL ? -90 : face == DoubleAttachFace.WALL_REVERSED ? 90 : 0;

		ms.pushPose();
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(xRot));
		ms.translate(-.5f, -.5f, -.5f);

		if (nixieState.signalState != null || nixieState.computerFirst != null) {
			submitAsSignal(nixieState, ms, collector, state.lightCoords);
			ms.popPose();
			return;
		}
		ms.translate(.5f, .5f, .5f);

		float height = face == DoubleAttachFace.CEILING ? 5 : 3;
		float scale = 1 / 20f;
		DyeColor color = NixieTubeBlock.colorOf(blockState);

		ms.pushPose();
		ms.translate(-4 / 16f, 0, 0);
		ms.scale(scale, -scale, scale);
		submitTube(ms, collector, nixieState.first, height, color);
		ms.popPose();

		ms.pushPose();
		ms.translate(4 / 16f, 0, 0);
		ms.scale(scale, -scale, scale);
		submitTube(ms, collector, nixieState.second, height, color);
		ms.popPose();

		ms.popPose();
	}

	private void submitAsSignal(NixieTubeRenderState state, PoseStack ms, SubmitNodeCollector collector, int light) {
		Direction facing = NixieTubeBlock.getFacing(state.blockState);
		if (facing == Direction.DOWN) {
			ms.translate(.5f, .5f, .5f);
			ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));
			ms.translate(-.5f, -.5f, -.5f);
		}
		submitModel(ms, collector, CreateStandaloneModels.NIXIE_SIGNAL_PANEL, RenderTypes.solidMovingBlock(), light,
			null);

		ms.pushPose();
		ms.translate(.5f, 7.5f / 16f, .5f);
		boolean invert = facing == Direction.DOWN
			|| state.blockState.getValue(NixieTubeBlock.FACE) == DoubleAttachFace.WALL_REVERSED;
		if (state.signalState != null)
			submitTrackSignalLamps(state, facing, invert, ms, collector);
		else
			submitComputerLamps(state, facing, invert, ms, collector);
		ms.popPose();
	}

	private void submitTrackSignalLamps(NixieTubeRenderState state, Direction facing, boolean invert, PoseStack ms,
		SubmitNodeCollector collector) {
		for (int index = 0; index < 2; index++) {
			boolean first = index == 0;
			if (first && !state.signalState.isRedLight(state.renderTime))
				continue;
			if (!first && !state.signalState.isGreenLight(state.renderTime)
				&& !state.signalState.isYellowLight(state.renderTime))
				continue;
			boolean flip = first == invert;
			boolean yellow = state.signalState.isYellowLight(state.renderTime);
			ms.pushPose();
			ms.translate(flip ? 4 / 16f : -4 / 16f, 0, 0);

			boolean vertical = first ^ facing.getAxis().isHorizontal();
			if (state.distanceToCameraSqr < GLOW_VIEW_DISTANCE * GLOW_VIEW_DISTANCE) {
				float longSide = yellow ? 1 : 4;
				float glowSide = yellow ? 2 : 5.125f;
				ms.pushPose();
				ms.scale(vertical ? longSide : 1, vertical ? 1 : longSide, 1);
				submitModel(ms, collector, CreateStandaloneModels.NIXIE_SIGNAL_WHITE_CUBE,
					RenderTypes.translucentMovingBlock(), LightCoordsUtil.pack(15, 15), null);
				ms.popPose();
				ms.pushPose();
				ms.scale(vertical ? glowSide : 2, vertical ? 2 : glowSide, 2);
				submitModel(ms, collector,
					first ? CreateStandaloneModels.NIXIE_SIGNAL_RED_GLOW
						: yellow ? CreateStandaloneModels.NIXIE_SIGNAL_YELLOW_GLOW
							: CreateStandaloneModels.NIXIE_SIGNAL_WHITE_GLOW,
					RenderTypes.translucentMovingBlock(), LightCoordsUtil.pack(15, 15), null);
				ms.popPose();
			}
			ms.pushPose();
			ms.scale(1 + 1 / 16f, 1 + 1 / 16f, 1 + 1 / 16f);
			submitModel(ms, collector,
				first ? CreateStandaloneModels.NIXIE_SIGNAL_RED
					: yellow ? CreateStandaloneModels.NIXIE_SIGNAL_YELLOW : CreateStandaloneModels.NIXIE_SIGNAL_WHITE,
				RenderTypes.translucentMovingBlock(), LightCoordsUtil.pack(15, 15), null);
			ms.popPose();
			ms.popPose();
		}
	}

	private void submitComputerLamps(NixieTubeRenderState state, Direction facing, boolean invert, PoseStack ms,
		SubmitNodeCollector collector) {
		TubeData[] tubes = { state.computerFirst, state.computerSecond };
		for (int index = 0; index < 2; index++) {
			TubeData tube = tubes[index];
			if (tube == null)
				continue;
			int period = Byte.toUnsignedInt(tube.blinkPeriod);
			int offTime = Byte.toUnsignedInt(tube.blinkOffTime);
			if (period > 1 && state.renderTime % period < offTime)
				continue;
			boolean first = index == 0;
			boolean flip = first == invert;
			ms.pushPose();
			ms.translate(flip ? 4 / 16f : -4 / 16f, 0, 0);
			boolean horizontal = facing.getAxis().isHorizontal();
			float width = horizontal ? Byte.toUnsignedInt(tube.glowWidth) : Byte.toUnsignedInt(tube.glowHeight);
			float height = horizontal ? Byte.toUnsignedInt(tube.glowHeight) : Byte.toUnsignedInt(tube.glowWidth);
			int r = Byte.toUnsignedInt(tube.r);
			int g = Byte.toUnsignedInt(tube.g);
			int b = Byte.toUnsignedInt(tube.b);
			if (state.distanceToCameraSqr < GLOW_VIEW_DISTANCE * GLOW_VIEW_DISTANCE) {
				ms.pushPose();
				ms.scale(width, height, 1);
				submitModel(ms, collector, CreateStandaloneModels.NIXIE_COMPUTER_WHITE_CUBE,
					RenderTypes.translucentMovingBlock(), LightCoordsUtil.pack(15, 15), 0xFFFFFFFF);
				ms.popPose();
				int glow = 0xFF000000 | Math.min((r * 6 + 256) >> 3, 255) << 16
					| Math.min((g * 6 + 256) >> 3, 255) << 8 | Math.min((b * 6 + 256) >> 3, 255);
				ms.pushPose();
				ms.scale(width + 1.125f, height + 1.125f, 2);
				submitModel(ms, collector, CreateStandaloneModels.NIXIE_COMPUTER_WHITE_GLOW,
					RenderTypes.translucentMovingBlock(), LightCoordsUtil.pack(15, 15), glow);
				ms.popPose();
			}
			ms.pushPose();
			ms.scale(1 + 1.25f / 16f, 1 + 1.25f / 16f, 1 + 1.25f / 16f);
			submitModel(ms, collector, CreateStandaloneModels.NIXIE_COMPUTER_WHITE_BASE,
				RenderTypes.translucentMovingBlock(), LightCoordsUtil.pack(15, 15), 0xFF0C0C0C);
			ms.popPose();
			ms.pushPose();
			ms.scale(1 + 1 / 16f, 1 + 1 / 16f, 1 + 1 / 16f);
			submitModel(ms, collector, CreateStandaloneModels.NIXIE_COMPUTER_WHITE,
				RenderTypes.translucentMovingBlock(), LightCoordsUtil.pack(15, 15),
				0xFF000000 | r << 16 | g << 8 | b);
			ms.popPose();
			ms.popPose();
		}
	}

	private void submitModel(PoseStack ms, SubmitNodeCollector collector,
		StandaloneModelKey<BlockStateModelPart> key, RenderType renderType, int light, Integer color) {
		if (renderType == null)
			return;
		BlockStateModelPart basePart = Minecraft.getInstance().getModelManager().getStandaloneModel(key);
		if (basePart == null)
			return;
		BlockStateModelPart part = color == null ? basePart
			: tintedModels.computeIfAbsent(key, ignored -> new TintedBlockStateModelPart(basePart));
		collector.submitBlockModel(ms, renderType, List.of(part),
			color == null ? BlockModelRenderState.EMPTY_TINTS : new int[] { color }, light,
			OverlayTexture.NO_OVERLAY, 0);
	}

	private static void submitTube(PoseStack ms, SubmitNodeCollector collector, String text, float height,
		DyeColor color) {
		if (text == null || text.isEmpty() || text.isBlank())
			return;
		Font font = Minecraft.getInstance().font;
		float charWidth = font.width(text);
		float shadowOffset = .5f;
		Couple<Integer> colors = DyeHelper.getDyeColors(color);
		int brightColor = 0xFF000000 | colors.getFirst();
		int darkColor = 0xFF000000 | colors.getSecond();
		int flickeringBrightColor = 0xFF000000
			| Color.mixColors(colors.getFirst(), colors.getSecond(), RANDOM.nextFloat() / 4);
		int fullBright = LightCoordsUtil.pack(15, 15);

		ms.pushPose();
		ms.translate((charWidth - shadowOffset) / -2f, -height, 0);
		submitString(ms, collector, text, flickeringBrightColor, fullBright);
		ms.pushPose();
		ms.translate(shadowOffset, shadowOffset, -1 / 16f);
		submitString(ms, collector, text, darkColor, fullBright);
		ms.popPose();
		ms.popPose();

		ms.pushPose();
		ms.scale(-1, 1, 1);
		ms.translate((charWidth - shadowOffset) / -2f, -height, 0);
		submitString(ms, collector, text, darkColor, fullBright);
		ms.pushPose();
		ms.translate(-shadowOffset, shadowOffset, -1 / 16f);
		submitString(ms, collector, text, 0xFF000000 | Color.mixColors(colors.getSecond(), 0, .35f), fullBright);
		ms.popPose();
		ms.popPose();
	}

	private static void submitString(PoseStack ms, SubmitNodeCollector collector, String text, int color, int light) {
		Minecraft.getInstance().font.prepareText(Component.literal(text).getVisualOrderText(), 0, 0, color,
			false, false, 0).visit(new Font.GlyphVisitor() {
				@Override
				public void acceptRenderable(TextRenderable renderable) {
					collector.submitCustomGeometry(ms, renderable.renderType(Font.DisplayMode.NORMAL, true),
						(pose, consumer) -> renderable.render(pose.pose(), consumer, light, false));
				}
			});
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	public static void drawInWorldString(PoseStack ms, MultiBufferSource buffer, String text, int color) {
		Minecraft.getInstance().font.prepareText(Component.literal(text)
			.getVisualOrderText(), 0, 0, color, false, false, 0)
			.visit(new Font.GlyphVisitor() {
				@Override
				public void acceptRenderable(TextRenderable renderable) {
					renderable.render(ms.last()
						.pose(), buffer.getBuffer(renderable.renderType(Font.DisplayMode.NORMAL, true)),
						LightCoordsUtil.pack(15, 15), false);
				}
			});
	}

	private static class NixieTubeRenderState extends BlockEntityRenderState {
		private BlockState blockState;
		private String first = "";
		private String second = "";
		private SignalState signalState;
		private TubeData computerFirst;
		private TubeData computerSecond;
		private float renderTime;
		private double distanceToCameraSqr;
	}

	private record TubeData(byte r, byte g, byte b, byte blinkPeriod, byte blinkOffTime, byte glowWidth,
		byte glowHeight) {
		private static TubeData copy(TubeDisplay tube) {
			return new TubeData(tube.r, tube.g, tube.b, tube.blinkPeriod, tube.blinkOffTime, tube.glowWidth,
				tube.glowHeight);
		}
	}

	private static class TintedBlockStateModelPart implements BlockStateModelPart {
		private final BlockStateModelPart wrapped;
		private final Map<Direction, List<BakedQuad>> tintedQuads = new HashMap<>();
		private List<BakedQuad> tintedNullQuads;

		private TintedBlockStateModelPart(BlockStateModelPart wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public List<BakedQuad> getQuads(Direction direction) {
			if (direction == null) {
				if (tintedNullQuads == null)
					tintedNullQuads = tint(wrapped.getQuads(null));
				return tintedNullQuads;
			}
			return tintedQuads.computeIfAbsent(direction, side -> tint(wrapped.getQuads(side)));
		}

		private static List<BakedQuad> tint(List<BakedQuad> quads) {
			return quads.stream().map(TintedBlockStateModelPart::tint).toList();
		}

		private static BakedQuad tint(BakedQuad quad) {
			BakedQuad.MaterialInfo material = quad.materialInfo();
			BakedQuad.MaterialInfo tinted = new BakedQuad.MaterialInfo(material.sprite(), material.layer(),
				material.itemRenderType(), 0, material.shade(), material.lightEmission(), material.ambientOcclusion());
			return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
				quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), tinted,
				quad.bakedNormals(), quad.bakedColors());
		}

		@Override
		public boolean useAmbientOcclusion() {
			return wrapped.useAmbientOcclusion();
		}

		@Override
		public Material.Baked particleMaterial() {
			return wrapped.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return wrapped.materialFlags();
		}
	}

}
