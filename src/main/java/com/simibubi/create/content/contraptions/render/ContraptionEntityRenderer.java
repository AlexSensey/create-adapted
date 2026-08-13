package com.simibubi.create.content.contraptions.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsRenderer;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterRenderer;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceRenderer;
import com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour;
import com.simibubi.create.content.contraptions.actors.roller.RollerRenderer;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlockEntity;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import com.simibubi.create.content.contraptions.render.ClientContraption.RenderedBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour;
import com.simibubi.create.content.kinetics.deployer.DeployerRenderer;
import com.simibubi.create.content.processing.burner.BlazeBurnerMovementBehaviour;
import com.simibubi.create.content.contraptions.bearing.StabilizedBearingMovementBehaviour;
import com.simibubi.create.content.kinetics.drill.DrillMovementBehaviour;
import com.simibubi.create.content.kinetics.drill.DrillRenderer;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import com.simibubi.create.content.kinetics.saw.SawRenderer;
import com.simibubi.create.foundation.render.BlockEntityRenderHelper;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class ContraptionEntityRenderer<C extends AbstractContraptionEntity>
	extends EntityRenderer<C, ContraptionEntityRenderer.ContraptionRenderState<C>> {

	private static final Matrix4f IDENTITY_TRANSFORM = new Matrix4f();

	public ContraptionEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public Identifier getTextureLocation(C entity) {
		return null;
	}

	@Override
	public boolean shouldRender(C entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
		return entity.getContraption() != null && entity.isAliveOrStale() && entity.isReadyForRender()
			&& super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
	}

	public void render(C entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers,
		int overlay) {
		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return;

		Level level = entity.level();
		ClientContraption clientContraption = contraption.getOrCreateClientContraptionLazy();
		VirtualRenderWorld renderWorld = clientContraption.getRenderLevel();
		ContraptionMatrices matrices = clientContraption.getMatrices();
		matrices.setup(poseStack, entity);

		renderStructure(entity, renderWorld, clientContraption.getRenderedBlocks(), matrices, buffers, partialTicks);
		renderBlockEntities(level, renderWorld, clientContraption, matrices, buffers);
		renderActors(level, renderWorld, contraption, matrices, buffers);

		matrices.clear();
	}

	@Override
	public ContraptionRenderState<C> createRenderState() {
		return new ContraptionRenderState<>();
	}

	@Override
	public void extractRenderState(C entity, ContraptionRenderState<C> state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.entity = entity;
		state.partialTicks = partialTicks;
	}

	@Override
	public void submit(ContraptionRenderState<C> state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		C entity = state.entity;
		if (entity == null)
			return;

		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return;

		ClientContraption clientContraption = contraption.getOrCreateClientContraptionLazy();
		VirtualRenderWorld renderWorld = clientContraption.getRenderLevel();
		ms.pushPose();
		entity.applyLocalTransforms(ms, state.partialTicks);
		submitStructure(entity, renderWorld, clientContraption.getRenderedBlocks(), ms, collector, state.partialTicks);
		submitBlockEntities(entity, renderWorld, clientContraption, ms, collector, cameraRenderState,
			state.partialTicks);
		submitActors(entity, renderWorld, contraption, ms, collector, state.partialTicks);
		ms.popPose();
	}

	private static void renderStructure(AbstractContraptionEntity entity, VirtualRenderWorld renderWorld,
		RenderedBlocks blocks, ContraptionMatrices matrices, MultiBufferSource buffer, float partialTicks) {
		PoseStack ms = matrices.getModel();

		for (BlockPos pos : blocks.positions()) {
			BlockState state = blocks.lookup().apply(pos);
			if (MovementBehaviour.REGISTRY.get(state) instanceof ControlsMovementBehaviour)
				state = renderStateForControls(state);
			if (state.getRenderShape() != RenderShape.MODEL)
				continue;

			SuperByteBuffer block = CachedBuffers.block(state);
			if (block.isEmpty())
				continue;

			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());
			block.reset()
				.light(getContraptionBlockLight(entity, renderWorld, pos, partialTicks))
				.renderInto(ms, buffer.getBuffer(RenderTypes.cutoutMovingBlock()));
			ms.popPose();
		}
	}

	private static void submitStructure(AbstractContraptionEntity entity, VirtualRenderWorld renderWorld,
		RenderedBlocks blocks, PoseStack ms, SubmitNodeCollector collector, float partialTicks) {
		for (BlockPos pos : blocks.positions()) {
			BlockState state = blocks.lookup().apply(pos);
			if (MovementBehaviour.REGISTRY.get(state) instanceof ControlsMovementBehaviour)
				state = renderStateForControls(state);
			if (state.getRenderShape() != RenderShape.MODEL)
				continue;

			List<BlockStateModelPart> parts = getModelParts(state, pos);
			if (parts.isEmpty()) {
				continue;
			}

			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());
			int light = getContraptionBlockLight(entity, renderWorld, pos, partialTicks);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), parts, BlockModelRenderState.EMPTY_TINTS,
				light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}
	}

	private static int getContraptionBlockLight(AbstractContraptionEntity entity, VirtualRenderWorld renderWorld,
		BlockPos localPos, float partialTicks) {
		int virtualLight = LightCoordsUtil.getLightCoords(renderWorld, localPos);
		int realLight = getRealLevelLight(entity, localPos, partialTicks);
		return maxLight(virtualLight, realLight);
	}

	private static BlockState renderStateForControls(BlockState state) {
		if (state.hasProperty(ControlsBlock.VIRTUAL))
			state = state.setValue(ControlsBlock.VIRTUAL, false);
		if (state.hasProperty(ControlsBlock.OPEN))
			state = state.setValue(ControlsBlock.OPEN, true);
		return state;
	}

	private static int getRealLevelLight(AbstractContraptionEntity entity, BlockPos localPos, float partialTicks) {
		BlockPos lightPos = BlockPos.containing(entity.toGlobalVector(Vec3.atCenterOf(localPos), partialTicks));
		Level level = entity.level();
		return LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, lightPos),
			level.getBrightness(LightLayer.SKY, lightPos));
	}

	private static void submitBlockEntities(AbstractContraptionEntity entity, VirtualRenderWorld renderWorld,
		ClientContraption clientContraption, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState, float partialTicks) {
		var shouldRenderBlockEntities = clientContraption.getAndAdjustShouldRenderBlockEntities();
		clientContraption.scratchErroredBlockEntities.clear();

		for (int i = shouldRenderBlockEntities.nextSetBit(0);
			 i >= 0 && i < clientContraption.renderedBlockEntityView.size();
			 i = shouldRenderBlockEntities.nextSetBit(i + 1)) {
			BlockEntity blockEntity = clientContraption.renderedBlockEntityView.get(i);
			BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = Minecraft.getInstance()
				.getBlockEntityRenderDispatcher()
				.getRenderer(blockEntity);
			if (renderer == null) {
				clientContraption.scratchErroredBlockEntities.set(i);
				continue;
			}

			BlockPos pos = blockEntity.getBlockPos();
			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());

			try {
				prepareContraptionBlockEntity(entity, blockEntity);
				BlockEntityRenderState beState = renderer.createRenderState();
				renderer.extractRenderState(blockEntity, beState, partialTicks, cameraRenderState.pos,
					(ModelFeatureRenderer.CrumblingOverlay) null);
				beState.lightCoords = getContraptionBlockLight(entity, renderWorld, pos, partialTicks);
				renderer.submit(beState, ms, collector, cameraRenderState);
			} catch (Exception e) {
				clientContraption.scratchErroredBlockEntities.set(i);
			}

			ms.popPose();
		}

		clientContraption.shouldRenderBlockEntities.andNot(clientContraption.scratchErroredBlockEntities);
	}

	private static int maxLight(int a, int b) {
		int block = Math.max(a >> 4 & 0xF, b >> 4 & 0xF);
		int sky = Math.max(a >> 20 & 0xF, b >> 20 & 0xF);
		return LightCoordsUtil.pack(block, sky);
	}

	private static void submitActors(AbstractContraptionEntity entity, VirtualRenderWorld renderWorld,
		Contraption contraption, PoseStack ms, SubmitNodeCollector collector, float partialTicks) {
		Level level = entity.level();
		for (Pair<StructureTemplate.StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
			MovementContext context = actor.getRight();
			if (context == null)
				continue;
			if (context.world == null)
				context.world = level;

			StructureTemplate.StructureBlockInfo blockInfo = actor.getLeft();
			if (contraption.isHiddenInPortal(blockInfo.pos()))
				continue;

			MovementBehaviour movementBehaviour = MovementBehaviour.REGISTRY.get(blockInfo.state());
			if (!(movementBehaviour instanceof ContraptionControlsMovement)
				&& !(movementBehaviour instanceof ControlsMovementBehaviour)
				&& !(movementBehaviour instanceof PortableStorageInterfaceMovement)
				&& !(movementBehaviour instanceof BlazeBurnerMovementBehaviour)
				&& !(movementBehaviour instanceof StabilizedBearingMovementBehaviour)
				&& !(movementBehaviour instanceof DeployerMovementBehaviour)
				&& !(movementBehaviour instanceof DrillMovementBehaviour)
				&& !(movementBehaviour instanceof HarvesterMovementBehaviour)
				&& !(movementBehaviour instanceof RollerMovementBehaviour)
				&& !(movementBehaviour instanceof SawMovementBehaviour))
				continue;

			ms.pushPose();
			ms.translate(blockInfo.pos().getX(), blockInfo.pos().getY(), blockInfo.pos().getZ());
			int light = getContraptionBlockLight(entity, renderWorld, blockInfo.pos(), partialTicks);
			if (movementBehaviour instanceof ContraptionControlsMovement)
				ContraptionControlsRenderer.submitInContraption(context, ms, collector, light);
			if (movementBehaviour instanceof ControlsMovementBehaviour controls)
				controls.submitInContraption(context, ms, collector, light);
			if (movementBehaviour instanceof PortableStorageInterfaceMovement)
				PortableStorageInterfaceRenderer.submitInContraption(context, ms, collector);
			if (movementBehaviour instanceof BlazeBurnerMovementBehaviour burner)
				burner.submitInContraption(context, ms, collector, light);
			if (movementBehaviour instanceof StabilizedBearingMovementBehaviour bearing)
				bearing.submitInContraption(context, ms, collector, light);
			if (movementBehaviour instanceof DeployerMovementBehaviour)
				DeployerRenderer.submitInContraption(context, ms, collector, light);
			if (movementBehaviour instanceof DrillMovementBehaviour)
				DrillRenderer.submitInContraption(context, ms, collector, light);
			if (movementBehaviour instanceof HarvesterMovementBehaviour)
				HarvesterRenderer.submitInContraption(context, ms, collector, light);
			if (movementBehaviour instanceof RollerMovementBehaviour)
				RollerRenderer.submitInContraption(context, ms, collector, light);
			if (movementBehaviour instanceof SawMovementBehaviour)
				SawRenderer.submitInContraption(context, ms, collector, light);
			ms.popPose();
		}
	}

	private static void prepareContraptionBlockEntity(AbstractContraptionEntity entity, BlockEntity blockEntity) {
		if (!(blockEntity instanceof KineticBlockEntity kineticBE))
			return;

		if (entity instanceof GantryContraptionEntity gantryEntity
			&& blockEntity instanceof GantryCarriageBlockEntity) {
			kineticBE.setSpeed((float) (-gantryEntity.getAxisMotion() * 512));
		}
	}

	private static List<BlockStateModelPart> getModelParts(BlockState state, BlockPos pos) {
		BlockStateModel model = Minecraft.getInstance()
			.getModelManager()
			.getBlockStateModelSet()
			.get(state);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(state.getSeed(pos)), parts);
		return parts;
	}


	private static void renderBlockEntities(Level level, VirtualRenderWorld renderWorld, ClientContraption clientContraption,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		var shouldRenderBlockEntities = clientContraption.getAndAdjustShouldRenderBlockEntities();
		clientContraption.scratchErroredBlockEntities.clear();
		BlockEntityRenderHelper.renderBlockEntities(clientContraption.renderedBlockEntityView, shouldRenderBlockEntities,
			clientContraption.scratchErroredBlockEntities, renderWorld, level, matrices.getModelViewProjection(),
			matrices.getLight(), buffer, AnimationTickHolder.getPartialTicks());
		clientContraption.shouldRenderBlockEntities.andNot(clientContraption.scratchErroredBlockEntities);
	}

	private static void renderActors(Level level, VirtualRenderWorld renderWorld, Contraption contraption,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		PoseStack ms = matrices.getModel();

		for (Pair<StructureTemplate.StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
			MovementContext context = actor.getRight();
			if (context == null)
				continue;
			if (context.world == null)
				context.world = level;

			StructureTemplate.StructureBlockInfo blockInfo = actor.getLeft();
			if (contraption.isHiddenInPortal(blockInfo.pos()))
				continue;

			MovementBehaviour movementBehaviour = MovementBehaviour.REGISTRY.get(blockInfo.state());
			if (movementBehaviour == null)
				continue;

			ms.pushPose();
			ms.translate(blockInfo.pos().getX(), blockInfo.pos().getY(), blockInfo.pos().getZ());
			movementBehaviour.renderInContraption(context, renderWorld, matrices, buffer);
			ms.popPose();
		}
	}

	public static class ContraptionRenderState<C extends AbstractContraptionEntity> extends EntityRenderState {
		C entity;
		float partialTicks;

		public C getEntity() {
			return entity;
		}

		public float getPartialTicks() {
			return partialTicks;
		}
	}
}
