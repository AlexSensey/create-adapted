package com.simibubi.create.content.trains.display;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;

public class FlapDisplayRenderer extends KineticBlockEntityRenderer<FlapDisplayBlockEntity> {

	private BlockStateModelPart cogwheel;

	public FlapDisplayRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(FlapDisplayBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		// Rendering is submitted through the 26.2 renderer API.
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof FlapDisplayBlockEntity be) || isInvalid(be))
			return;

		if (!CreateVisualizationManager.supportsVisualization(be.getLevel()))
			submitCogwheel(be, kineticState.partialTicks, ms, collector, state.lightCoords);
		if (be.isController)
			submitText(be, ms, collector, state.lightCoords);
	}

	private void submitCogwheel(FlapDisplayBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		if (cogwheel == null)
			cogwheel = Minecraft.getInstance().getModelManager()
				.getStandaloneModel(CreateStandaloneModels.SHAFTLESS_COGWHEEL);
		if (cogwheel == null)
			return;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(be.getBlockState()
			.getValue(FlapDisplayBlock.HORIZONTAL_FACING))));
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
		ms.mulPose(com.mojang.math.Axis.YP.rotation(
			getAngleForBe(be, be.getBlockPos(), getRotationAxisOf(be), partialTicks)));
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(cogwheel),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private void submitText(FlapDisplayBlockEntity be, PoseStack ms, SubmitNodeCollector collector, int light) {
		float scale = 1 / 32f;
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(be.getBlockState()
			.getValue(FlapDisplayBlock.HORIZONTAL_FACING))));
		ms.translate(-.5, -.5, -.5);
		ms.translate(0, 1, 13 / 16f);
		ms.scale(scale, -scale, scale);
		ms.translate(0, 0, .5f);

		List<FlapDisplayLayout> lines = be.getLines();
		for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
			List<FlapDisplaySection> sections = lines.get(lineIndex).getSections();
			float width = 0;
			for (FlapDisplaySection section : sections)
				width += section.getSize() + (section.hasGap ? 8 : 1);

			ms.pushPose();
			ms.translate(be.xSize * 16 - width / 2 + 1, 4.5f + lineIndex * 16, 0);
			int color = 0xff000000 | be.getLineColor(lineIndex);
			int lineLight = be.isLineGlowing(lineIndex) ? LightCoordsUtil.pack(15, 15) : light;

			for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
				FlapDisplaySection section = sections.get(sectionIndex);
				submitString(getAnimatedText(section, lineIndex, sectionIndex,
					!be.isSpeedRequirementFulfilled()), color, lineLight, ms, collector);
				ms.translate(section.getSize() + (section.hasGap ? 8 : 1), 0, 0);
			}
			ms.popPose();
		}
		ms.popPose();
	}

	private static String getAnimatedText(FlapDisplaySection section, int lineIndex, int sectionIndex,
		boolean paused) {
		if (section.text == null || section.cyclingOptions == null || section.cyclingOptions.length == 0
			|| section.spinning.length == 0)
			return section.text;

		if (!section.renderCharsIndividually()) {
			if (!section.spinning[0])
				return section.text;
			int ticks = paused ? 0 : AnimationTickHolder.getTicks();
			return section.cyclingOptions[Math.floorMod(ticks / 3 + sectionIndex * 13,
				section.cyclingOptions.length)];
		}

		char[] animated = section.text.toCharArray();
		float time = paused ? 0 : AnimationTickHolder.getRenderTime();
		for (int charIndex = 0; charIndex < animated.length && charIndex < section.spinning.length; charIndex++) {
			if (!section.spinning[charIndex])
				continue;

			float speed = section.spinningTicks > 5 && section.spinningTicks < 20 ? 1.75f : 2.5f;
			float cycle = time / speed + charIndex * 16.83f + lineIndex * .75f;
			int cycleIndex = Math.floorMod((int) Math.floor(cycle), section.cyclingOptions.length);
			String option = section.cyclingOptions[cycleIndex];
			char cyclingCharacter = option.isEmpty() ? ' ' : option.charAt(0);
			float partial = cycle - (float) Math.floor(cycle);
			animated[charIndex] = paused ? cyclingCharacter
				: partial > .75f ? '_'
				: partial > .5f ? '-'
				: cyclingCharacter;
		}
		return new String(animated);
	}

	private static void submitString(String text, int color, int light, PoseStack ms,
		SubmitNodeCollector collector) {
		if (text == null || text.isBlank())
			return;
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
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
