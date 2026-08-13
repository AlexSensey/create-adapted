package dev.engine_room.flywheel.lib.model.baked;

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.lib.model.SimpleModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

final class BakedModelBufferer {
	private static final ThreadLocal<ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(ThreadLocalObjects::new);

	private BakedModelBufferer() {
	}

	public static SimpleModel bufferModel(BlockStateModelPart part, BlockPos pos, BlockAndTintGetter level,
		BlockState state, @Nullable PoseStack poseStack, BlockMaterialFunction materialFunction) {
		return bufferModel(asBlockStateModel(part), pos, level, state, poseStack, materialFunction);
	}

	public static SimpleModel bufferModel(BlockStateModel model, BlockPos pos, BlockAndTintGetter level,
		BlockState state, @Nullable PoseStack poseStack, BlockMaterialFunction materialFunction) {
		ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();
		PoseStack poses = poseStack == null ? objects.identityPoseStack : poseStack;
		MeshEmitterManager<NeoforgeMeshEmitter> emitters = objects.emitters;
		emitters.prepare(materialFunction);

		long seed = state.getSeed(pos);
		objects.blockRenderer.tesselateBlock((x, y, z, quad, instance) -> {
			NeoforgeMeshEmitter emitter = emitters.getEmitter(renderType(quad.materialInfo().layer()));
			poses.pushPose();
			poses.translate(x, y, z);
			emitter.put(poses.last(), quad, instance, level, pos, state);
			poses.popPose();
		}, 0, 0, 0, level, pos, state, model, seed);

		return emitters.end();
	}

	public static SimpleModel bufferBlocks(Iterator<BlockPos> positions, BlockAndTintGetter level,
		@Nullable PoseStack poseStack, boolean renderFluids, BlockMaterialFunction materialFunction) {
		ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();
		PoseStack poses = poseStack == null ? objects.identityPoseStack : poseStack;
		MeshEmitterManager<NeoforgeMeshEmitter> emitters = objects.emitters;
		TransformingVertexConsumer transforming = objects.transformingWrapper;
		Minecraft minecraft = Minecraft.getInstance();
		emitters.prepare(materialFunction);

		FluidRenderer fluidRenderer = renderFluids
			? new FluidRenderer(minecraft.getModelManager().getFluidStateModelSet())
			: null;

		while (positions.hasNext()) {
			BlockPos pos = positions.next();
			BlockState state = level.getBlockState(pos);
			emitters.prepareForBlock();

			if (fluidRenderer != null) {
				FluidState fluidState = state.getFluidState();
				if (!fluidState.isEmpty()) {
					poses.pushPose();
					poses.translate(pos.getX(), pos.getY(), pos.getZ());
					fluidRenderer.tesselate(level, pos, layer -> {
						RenderType type = renderType(layer);
						BufferBuilder buffer = emitters.getBuffer(type, true, false);
						if (buffer == null) {
							return objects.discardingConsumer;
						}
						transforming.prepare(buffer, poses);
						return transforming;
					}, state, fluidState);
					poses.popPose();
				}
			}

			if (state.getRenderShape() == RenderShape.MODEL) {
				BlockStateModel model = minecraft.getModelManager().getBlockStateModelSet().get(state);
				objects.blockRenderer.tesselateBlock((x, y, z, quad, instance) -> {
					NeoforgeMeshEmitter emitter = emitters.getEmitter(renderType(quad.materialInfo().layer()));
					poses.pushPose();
					poses.translate(x, y, z);
					emitter.put(poses.last(), quad, instance, level, pos, state);
					poses.popPose();
				}, pos.getX(), pos.getY(), pos.getZ(), level, pos, state, model, state.getSeed(pos));
			}
		}

		transforming.clear();
		return emitters.end();
	}

	private static RenderType renderType(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> RenderTypes.solidMovingBlock();
			case CUTOUT -> RenderTypes.cutoutMovingBlock();
			case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
		};
	}

	private static BlockStateModel asBlockStateModel(BlockStateModelPart part) {
		return new BlockStateModel() {
			@Override
			public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
				output.add(part);
			}

			@Override
			public Material.Baked particleMaterial() {
				return part.particleMaterial();
			}

			@Override
			public int materialFlags() {
				return part.materialFlags();
			}
		};
	}

	private static class ThreadLocalObjects {
		final PoseStack identityPoseStack = new PoseStack();
		final MeshEmitterManager<NeoforgeMeshEmitter> emitters = new MeshEmitterManager<>(NeoforgeMeshEmitter::new);
		final TransformingVertexConsumer transformingWrapper = new TransformingVertexConsumer();
		final DiscardingVertexConsumer discardingConsumer = new DiscardingVertexConsumer();
		final ModelBlockRenderer blockRenderer = new ModelBlockRenderer(true, true, Minecraft.getInstance().getBlockColors());
	}
}
