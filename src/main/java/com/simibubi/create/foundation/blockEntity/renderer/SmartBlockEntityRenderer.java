package com.simibubi.create.foundation.blockEntity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.redstone.link.LinkRenderer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public class SmartBlockEntityRenderer<T extends SmartBlockEntity> extends SafeBlockEntityRenderer<T> {

	protected static class SmartRenderState extends BlockEntityRenderState {
		protected SmartBlockEntity blockEntity;
		protected float partialTicks;
	}

	public SmartBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new SmartRenderState();
	}

	@Override
	public void extractRenderState(T blockEntity, BlockEntityRenderState state, float partialTicks, Vec3 cameraPos,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay);
		if (state instanceof SmartRenderState smartState) {
			smartState.blockEntity = blockEntity;
			smartState.partialTicks = partialTicks;
		}
	}

	@Override
	protected void renderSafe(T blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
			int overlay) {
		FilteringRenderer.renderOnBlockEntity(blockEntity, partialTicks, ms, buffer, light, overlay);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof SmartRenderState smartState))
			return;
		if (!(smartState.blockEntity instanceof SmartBlockEntity blockEntity))
			return;
		if (!blockEntity.hasLevel() || blockEntity.getBlockState()
			.getBlock() == Blocks.AIR)
			return;

		submitBehaviours(blockEntity, smartState.partialTicks, ms, collector, state.lightCoords);
	}

	protected void submitBehaviours(SmartBlockEntity blockEntity, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		FilteringRenderer.submitOnBlockEntity(blockEntity, ms, collector, light);
		LinkRenderer.submitOnBlockEntity(blockEntity, partialTicks, ms, collector, light);
	}

	protected void submitNameplateOnHover(T blockEntity, Component tag, float yOffset, PoseStack ms,
		SubmitNodeCollector collector, CameraRenderState cameraRenderState, int light) {
		Minecraft mc = Minecraft.getInstance();
		if (blockEntity.isVirtual() || mc.player == null)
			return;
		if (mc.player.distanceToSqr(Vec3.atCenterOf(blockEntity.getBlockPos())) > 4096.0)
			return;
		HitResult hitResult = mc.hitResult;
		if (!(hitResult instanceof BlockHitResult bhr) || bhr.getType() == Type.MISS
			|| !bhr.getBlockPos().equals(blockEntity.getBlockPos()))
			return;

		Font font = mc.font;
		float x = -font.width(tag) / 2f;
		int background = (int) (mc.options.getBackgroundOpacity(.25f) * 255) << 24;
		ms.pushPose();
		ms.translate(.5, yOffset + .25f, .5);
		ms.mulPose(cameraRenderState.orientation);
		ms.scale(.025f, -.025f, .025f);
		submitText(tag, x, 553648127, background, Font.DisplayMode.SEE_THROUGH, ms, collector, light);
		submitText(tag, x, -1, 0, Font.DisplayMode.NORMAL, ms, collector, light);
		ms.popPose();
	}

	private static void submitText(Component text, float x, int color, int background,
		Font.DisplayMode mode, PoseStack ms, SubmitNodeCollector collector, int light) {
		Minecraft.getInstance().font.prepareText(text.getVisualOrderText(), x, 0, color,
			false, false, background).visit(new Font.GlyphVisitor() {
			@Override
			public void acceptRenderable(TextRenderable renderable) {
				collector.submitCustomGeometry(ms, renderable.renderType(mode, true),
					(pose, consumer) -> renderable.render(pose.pose(), consumer, light, false));
			}
		});
	}

}
