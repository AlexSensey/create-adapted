package net.createmod.catnip.api.client.render;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.client.level.EmptyVirtualBlockGetter;
import net.createmod.catnip.api.client.render.model.BakedModelBufferer;
import net.createmod.catnip.api.client.render.model.ShadeSeparatedResultConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class SuperBufferFactory {
	private static final ThreadLocal<ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(ThreadLocalObjects::new);

	private static SuperBufferFactory instance = new SuperBufferFactory();

	public static SuperBufferFactory getInstance() {
		return instance;
	}

	static void setInstance(SuperBufferFactory factory) {
		instance = factory;
	}

	public SuperByteBuffer create(MeshData data) {
		return new ShadeSeparatingSuperByteBuffer(new MutableTemplateMesh(data).toImmutable());
	}

	public SuperByteBuffer createForBlock(BlockState renderedState) {
		return createForBlock(Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(renderedState), renderedState);
	}

	public SuperByteBuffer createForBlock(BlockStateModel model, BlockState referenceState) {
		return createForBlock(model, referenceState, new PoseStack());
	}

	public SuperByteBuffer createForBlock(BlockStateModel model, BlockState state, @Nullable PoseStack poseStack) {
		ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();
		SbbBuilder sbbBuilder = objects.sbbBuilder;
		sbbBuilder.prepare();
		BakedModelBufferer.bufferModel(model, BlockPos.ZERO, EmptyVirtualBlockGetter.FULL_DARK, state, poseStack, sbbBuilder);
		return sbbBuilder.build();
	}

	private static class SbbBuilder extends SuperByteBufferBuilder implements ShadeSeparatedResultConsumer {
		@Override
		public void accept(RenderPipeline pipeline, boolean shaded, MeshData data) {
			add(data, shaded);
		}
	}

	private static class ThreadLocalObjects {
		public final SbbBuilder sbbBuilder = new SbbBuilder();
	}
}
