package net.createmod.catnip.impl.fabric.client.render;

import java.util.function.Predicate;

import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.client.render.model.ShadeSeparatedBufferSource;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

// Modified from https://github.com/Engine-Room/Flywheel/blob/2f67f54c8898d91a48126c3c753eefa6cd224f84/fabric/src/lib/java/dev/engine_room/flywheel/lib/model/baked/UniversalMeshEmitter.java
class UniversalMeshEmitter {//implements BlockMultiBufferSource {
	private final WrapperModel wrapperModel = new WrapperModel();

	@UnknownNullability
	private ShadeSeparatedBufferSource bufferSource;
	@UnknownNullability
	private ChunkSectionLayer defaultLayer;
	@UnknownNullability
	private boolean currentShade;

	public void prepare(ShadeSeparatedBufferSource bufferSource, ChunkSectionLayer defaultLayer) {
		this.bufferSource = bufferSource;
		this.defaultLayer = defaultLayer;
	}

	public void clear() {
		bufferSource = null;
		wrapperModel.setWrapped(null);
	}

	public BlockStateModel wrapModel(BlockStateModel model) {
		wrapperModel.setWrapped(model);
		return wrapperModel;
	}

	private void prepareForGeometry(MutableQuadView quad) {
		currentShade = quad.diffuseShade();
	}

	/*@Override
	public VertexConsumer getBuffer(ChunkSectionLayer layer) {
		return bufferSource.getBuffer(layer, currentShade);
	}*/

	private class WrapperModel extends WrapperBlockStateModel {
		private final QuadTransform quadTransform = quad -> {
			UniversalMeshEmitter.this.prepareForGeometry(quad);
			return true;
		};

		public void setWrapped(@Nullable BlockStateModel wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
			emitter.pushTransform(quadTransform);
			super.emitQuads(emitter, level, pos, state, random, cullTest);
			emitter.popTransform();
		}
	}
}
