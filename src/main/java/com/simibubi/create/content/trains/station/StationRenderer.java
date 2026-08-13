package com.simibubi.create.content.trains.station;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.Transform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class StationRenderer extends SafeBlockEntityRenderer<StationBlockEntity> {

	public StationRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(StationBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new StationRenderState();
	}

	@Override
	public void extractRenderState(StationBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof StationRenderState stationState) {
			stationState.blockEntity = be;
			stationState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof StationRenderState stationState))
			return;
		StationBlockEntity be = stationState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		DepotRenderer.renderItemsOf(be, stationState.partialTicks, ms, collector, state.lightCoords,
			be.depotBehaviour);

		GlobalStation station = be.getStation();
		boolean isAssembling = be.getBlockState()
			.getValue(StationBlock.ASSEMBLING);

		if (!isAssembling || (station == null || station.getPresentTrain() != null) && !be.isVirtual()) {
			submitFlag(be.flag.getValue(stationState.partialTicks) > 0.75f ? CreateStandaloneModels.STATION_FLAG_ON
				: CreateStandaloneModels.STATION_FLAG_OFF, be, stationState.partialTicks, ms, collector,
				state.lightCoords);
			renderTrackMarker(ms, collector, be, state.lightCoords);
			return;
		}

		submitFlag(CreateStandaloneModels.STATION_FLAG_ASSEMBLE, be, stationState.partialTicks, ms, collector,
			state.lightCoords);

		Direction direction = be.assemblyDirection;
		if (be.isVirtual() && be.bogeyLocations == null)
			be.refreshAssemblyInfo();

		if (direction == null || be.assemblyLength == 0 || be.bogeyLocations == null)
			return;

		BlockPos targetPosition = be.edgePoint.getGlobalPosition();
		BlockState trackState = be.getLevel()
			.getBlockState(targetPosition);
		if (!(trackState.getBlock() instanceof ITrackBlock track))
			return;

		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.TRACK_ASSEMBLING_OVERLAY);
		if (part == null)
			return;

		int colorWhenValid = 0xFF96B5FF;
		int colorWhenCarriage = 0xFFCAFF96;

		ms.pushPose();
		BlockPos offset = targetPosition.subtract(be.getBlockPos());
		ms.translate(offset.getX(), offset.getY(), offset.getZ());
		track.prepareAssemblyOverlay(be.getLevel(), targetPosition, trackState, direction, ms);
		rotateOrthogonalXOverlay(ms, direction.getAxis() == Direction.Axis.X);
		rotateOppositeSideOverlay(ms, direction.getAxisDirection() == AxisDirection.NEGATIVE);

		ms.translate(0, 0, 1);

		for (int i = 0; i < be.assemblyLength; i++) {
			boolean valid = be.isValidBogeyOffset(i);
			boolean carriage = false;

			for (int j : be.bogeyLocations)
				if (i == j) {
					carriage = true;
					valid = true;
					break;
			}

			if (valid)
				renderAssemblyMarker(ms, collector, part, carriage ? colorWhenCarriage : colorWhenValid,
					state.lightCoords);

			ms.translate(0, 0, 1);
		}

		ms.popPose();
	}

	private static void renderTrackMarker(PoseStack ms, SubmitNodeCollector collector, StationBlockEntity be,
		int light) {
		BlockPos targetPosition = be.edgePoint.getGlobalPosition();

		ms.pushPose();
		BlockPos offset = targetPosition.subtract(be.getBlockPos());
		ms.translate(offset.getX(), offset.getY(), offset.getZ());
		TrackTargetingBehaviour.submit(be.getLevel(), targetPosition, be.edgePoint.getTargetDirection(),
			be.edgePoint.getTargetBezier(), ms, collector, light, RenderedTrackOverlayType.STATION, 1);
		ms.popPose();
	}

	private static void rotateOrthogonalXOverlay(PoseStack ms, boolean rotate) {
		if (!rotate)
			return;
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotation(Mth.HALF_PI));
		ms.translate(-.5, -.5, -.5);
	}

	private static void rotateOppositeSideOverlay(PoseStack ms, boolean rotate) {
		if (!rotate)
			return;
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotation(Mth.PI));
		ms.translate(-.5, -.5, -.5);
	}

	private static void renderAssemblyMarker(PoseStack ms, SubmitNodeCollector collector, BlockStateModelPart part,
		int color, int light) {
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(new TintedBlockStateModelPart(part)),
			new int[] { color }, light, 0, 0);
	}

	private static void submitFlag(StandaloneModelKey<BlockStateModelPart> flag, StationBlockEntity be,
		float partialTicks, PoseStack ms, SubmitNodeCollector collector, int light) {
		if (!be.resolveFlagAngle())
			return;

		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(flag);
		if (part == null)
			return;

		ms.pushPose();
		transformFlag(ms, be, partialTicks, be.flagYRot, be.flagFlipped);
		ms.translate(0.5f / 16, 0, 0);
		ms.mulPose(Axis.YP.rotationDegrees(be.flagFlipped ? 0 : 180));
		ms.translate(-0.5f / 16, 0, 0);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, light, 0, 0);
		ms.popPose();
	}

	private static void transformFlag(PoseStack ms, StationBlockEntity be, float partialTicks, int yRot,
		boolean flipped) {
		float value = be.flag.getValue(partialTicks);
		float progress = (float) (Math.pow(Math.min(value * 5, 1), 2));
		if (be.flag.getChaseTarget() > 0 && !be.flag.settled() && progress == 1) {
			float wiggleProgress = (value - .2f) / .8f;
			progress += (Math.sin(wiggleProgress * (2 * Mth.PI) * 4) / 8f) / Math.max(1, 8f * wiggleProgress);
		}

		float nudge = 1 / 512f;
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(Axis.YP.rotationDegrees(yRot));
		ms.translate(nudge, 9.5f / 16f, flipped ? 14f / 16f - nudge : 2f / 16f + nudge);
		ms.translate(-.5f, -.5f, -.5f);
		ms.mulPose(Axis.XP.rotationDegrees((flipped ? 1 : -1) * (progress * 90 + 270)));
	}

	private static AABB flatTrackBox(BlockPos pos, Direction direction) {
		double y = pos.getY() + 1 / 16d;
		double minX = pos.getX() + 2 / 16d;
		double maxX = pos.getX() + 14 / 16d;
		double minZ = pos.getZ() + 2 / 16d;
		double maxZ = pos.getZ() + 14 / 16d;

		if (direction.getAxis() == Direction.Axis.X) {
			minZ = pos.getZ() + 4 / 16d;
			maxZ = pos.getZ() + 12 / 16d;
		}
		if (direction.getAxis() == Direction.Axis.Z) {
			minX = pos.getX() + 4 / 16d;
			maxX = pos.getX() + 12 / 16d;
		}

		return new AABB(minX, y, minZ, maxX, y + 1 / 64d, maxZ);
	}

	private static class StationRenderState extends BlockEntityRenderState {
		private StationBlockEntity blockEntity;
		private float partialTicks;
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
			return quads.stream()
				.map(TintedBlockStateModelPart::tint)
				.toList();
		}

		private static BakedQuad tint(BakedQuad quad) {
			BakedQuad.MaterialInfo material = quad.materialInfo();
			BakedQuad.MaterialInfo tintedMaterial = new BakedQuad.MaterialInfo(material.sprite(), material.layer(),
				material.itemRenderType(), 0, material.shade(), material.lightEmission(), material.ambientOcclusion());
			return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
				quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
				tintedMaterial, quad.bakedNormals(), quad.bakedColors());
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

	public static void renderFlag(PartialModel flag, StationBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
	}

	public static void transformFlag(Transform<?> flag, StationBlockEntity be, float partialTicks, int yRot,
		boolean flipped) {
	}

	public boolean shouldRenderOffScreen(StationBlockEntity be) {
		return true;
	}

	public int getViewDistance() {
		return 192;
	}
}
