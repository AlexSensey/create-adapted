package net.createmod.catnip.api.client.render.model;

import java.util.Iterator;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.platform.ModClientHooksHelper;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class BakedModelBufferer {
	private BakedModelBufferer() {
	}

	public static void submitModel(BlockStateModel model, BlockPos pos, BlockState state, @Nullable PoseStack poseStack, ShadeSeparatedBufferSource bufferSource, OrderedSubmitNodeCollector submitNodeCollector) {
		ModClientHooksHelper.INSTANCE.submitModel(model, pos, state, poseStack, bufferSource, submitNodeCollector);
	}

	public static void bufferModel(BlockStateModel model, BlockPos pos, BlockAndTintGetter level, BlockState state, @Nullable PoseStack poseStack, ShadeSeparatedBufferSource bufferSource) {
		ModClientHooksHelper.INSTANCE.bufferModel(model, pos, level, state, poseStack, bufferSource);
	}

	public static void bufferModel(BlockStateModel model, BlockPos pos, BlockAndTintGetter level, BlockState state, @Nullable PoseStack poseStack, ShadeSeparatedResultConsumer resultConsumer) {
		ModClientHooksHelper.INSTANCE.bufferModel(model, pos, level, state, poseStack, resultConsumer);
	}

	public static void bufferBlocks(Iterator<BlockPos> posIterator, BlockAndTintGetter level, @Nullable PoseStack poseStack, boolean renderFluids, ShadeSeparatedBufferSource bufferSource) {
		ModClientHooksHelper.INSTANCE.bufferBlocks(posIterator, level, poseStack, renderFluids, bufferSource);
	}

	public static void bufferBlocks(Iterator<BlockPos> posIterator, BlockAndTintGetter level, @Nullable PoseStack poseStack, boolean renderFluids, ShadeSeparatedResultConsumer resultConsumer) {
		ModClientHooksHelper.INSTANCE.bufferBlocks(posIterator, level, poseStack, renderFluids, resultConsumer);
	}
}
