package net.createmod.catnip.api.client.ghostblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.client.render.SuperRenderTypeBuffer;
import net.createmod.catnip.api.client.level.EmptyVirtualBlockGetter;
import net.createmod.catnip.api.client.render.model.BakedModelBufferer;
import net.createmod.catnip.impl.client.placement.PlacementClient;
import net.createmod.catnip.impl.client.render.ColoringVertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class GhostBlockRenderer {
	private static final GhostBlockRenderer STANDARD = new DefaultGhostBlockRenderer();
	private static final GhostBlockRenderer TRANSPARENT = new TransparentGhostBlockRenderer();

	public static GhostBlockRenderer standard() {
		return STANDARD;
	}

	public static GhostBlockRenderer transparent() {
		return TRANSPARENT;
	}

	public abstract void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, GhostBlockParams params);

	private static float getTransparentAlpha(GhostBlockParams params) {
		return params.alphaSupplier.get() * .75f * PlacementClient.getCurrentAlpha();
	}

	/** Buffer placement ghosts in the post-water pass. */
	public void bufferAfterTranslucentBlocks(PoseStack ms, VertexConsumer consumer, Vec3 camera,
		GhostBlockParams params) {
	}

	private static class DefaultGhostBlockRenderer extends GhostBlockRenderer {
		@Override
		public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, GhostBlockParams params) {
			BlockState state = params.state;
			BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
			BlockPos pos = params.pos;

			ms.pushPose();
			ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
			BakedModelBufferer.bufferModel(model, pos, BlockAndTintGetter.EMPTY, state, ms, (layer, shade) -> buffer.getEarlyBuffer(layer));
			ms.popPose();
		}
	}

	private static class TransparentGhostBlockRenderer extends GhostBlockRenderer {
		@Override
		public void bufferAfterTranslucentBlocks(PoseStack ms, VertexConsumer consumer, Vec3 camera,
			GhostBlockParams params) {
			float alpha = getTransparentAlpha(params);
			if (alpha <= 0)
				return;

			BlockState state = params.state;
			BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
			BlockPos pos = params.pos;

			ms.pushPose();
			ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
			ms.translate(.5, .5, .5);
			ms.scale(.85f, .85f, .85f);
			ms.translate(-.5, -.5, -.5);

			VertexConsumer tinted = new ColoringVertexConsumer(consumer, 1, 1, 1, alpha);
			BakedModelBufferer.bufferModel(model, pos, EmptyVirtualBlockGetter.FULL_BRIGHT, state, ms,
				(_, _) -> tinted);
			ms.popPose();
		}

		@Override
		public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, GhostBlockParams params) {
			BlockState state = params.state;
			BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
			BlockPos pos = params.pos;
			float alpha = getTransparentAlpha(params);
			VertexConsumer vb = new ColoringVertexConsumer(buffer.getEarlyBuffer(ChunkSectionLayer.TRANSLUCENT), 1, 1, 1, alpha);

			ms.pushPose();
			ms.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

			ms.translate(.5, .5, .5);
			ms.scale(.85f, .85f, .85f);
			ms.translate(-.5, -.5, -.5);
			BakedModelBufferer.bufferModel(model, pos, BlockAndTintGetter.EMPTY, state, ms, (_, _) -> vb);
			ms.popPose();
		}
	}
}
