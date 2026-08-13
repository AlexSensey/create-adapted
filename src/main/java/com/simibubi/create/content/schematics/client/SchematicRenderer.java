package com.simibubi.create.content.schematics.client;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.level.wrapper.SchematicLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class SchematicRenderer {

	private final SchematicLevel schematic;
	private final List<RenderedBlock> renderedBlocks = new ArrayList<>();
	private final List<BlockEntity> renderedBlockEntities = new ArrayList<>();
	private final BitSet shouldRenderBlockEntities = new BitSet();
	private final BitSet scratchErroredBlockEntities = new BitSet();
	private boolean changed = true;

	public SchematicRenderer(SchematicLevel level) {
		schematic = level;
		for (BlockEntity blockEntity : level.getRenderedBlockEntities())
			renderedBlockEntities.add(blockEntity);
		shouldRenderBlockEntities.set(0, renderedBlockEntities.size());
	}

	public void update() {
		changed = true;
	}

	public void submit(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		if (changed)
			rebuild();
		changed = false;

		for (RenderedBlock rendered : renderedBlocks) {
			poseStack.pushPose();
			poseStack.translate(rendered.pos.getX(), rendered.pos.getY(), rendered.pos.getZ());
			collector.submitMultiLayerBlockModel(poseStack, rendered.parts, rendered.hasTranslucency,
				rendered.tints, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}

		submitBlockEntities(poseStack, collector, cameraRenderState);
	}

	private void rebuild() {
		renderedBlocks.clear();
		BoundingBox bounds = schematic.getBounds();
		for (BlockPos localPos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(),
			bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
			BlockPos globalPos = localPos.offset(schematic.anchor);
			BlockState state = schematic.getBlockState(globalPos);
			if (state.getRenderShape() != RenderShape.MODEL)
				continue;

			BlockStateModel model = Minecraft.getInstance()
				.getModelManager()
				.getBlockStateModelSet()
				.get(state);
			List<BlockStateModelPart> parts = new ArrayList<>();
			model.collectParts(RandomSource.create(state.getSeed(globalPos)), parts);
			List<BlockTintSource> tintSources = Minecraft.getInstance()
				.getBlockColors()
				.getTintSources(state);
			int[] tints = tintSources.isEmpty() ? BlockModelRenderState.EMPTY_TINTS : new int[tintSources.size()];
			for (int i = 0; i < tintSources.size(); i++)
				tints[i] = Minecraft.getInstance().level == null ? tintSources.get(i).color(state)
					: tintSources.get(i).colorInWorld(state, Minecraft.getInstance().level, globalPos);
			boolean hasTranslucency = model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT);
			if (!parts.isEmpty())
				renderedBlocks.add(new RenderedBlock(localPos.immutable(), List.copyOf(parts), tints, hasTranslucency));
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void submitBlockEntities(PoseStack poseStack, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		scratchErroredBlockEntities.clear();
		for (int i = shouldRenderBlockEntities.nextSetBit(0);
			i >= 0 && i < renderedBlockEntities.size();
			i = shouldRenderBlockEntities.nextSetBit(i + 1)) {
			BlockEntity blockEntity = renderedBlockEntities.get(i);
			BlockEntityRenderer renderer = Minecraft.getInstance()
				.getBlockEntityRenderDispatcher()
				.getRenderer(blockEntity);
			if (renderer == null) {
				scratchErroredBlockEntities.set(i);
				continue;
			}

			BlockPos pos = blockEntity.getBlockPos()
				.subtract(schematic.anchor);
			poseStack.pushPose();
			poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
			try {
				BlockEntityRenderState state = (BlockEntityRenderState) renderer.createRenderState();
				renderer.extractRenderState(blockEntity, state, Minecraft.getInstance()
					.getDeltaTracker()
					.getGameTimeDeltaPartialTick(false), cameraRenderState.pos,
					(ModelFeatureRenderer.CrumblingOverlay) null);
				state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
				renderer.submit(state, poseStack, collector, cameraRenderState);
			} catch (Exception exception) {
				scratchErroredBlockEntities.set(i);
			}
			poseStack.popPose();
		}
		shouldRenderBlockEntities.andNot(scratchErroredBlockEntities);
	}

	private record RenderedBlock(BlockPos pos, List<BlockStateModelPart> parts, int[] tints, boolean hasTranslucency) {
	}
}
