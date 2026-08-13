package com.simibubi.create.content.contraptions.elevator;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.contraptions.pulley.PulleyRenderer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.client.render.SpriteShiftEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class ElevatorPulleyRenderer extends KineticBlockEntityRenderer<ElevatorPulleyBlockEntity> {

	public ElevatorPulleyRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof ElevatorPulleyBlockEntity be))
			return;
		if (isInvalid(be))
			return;

		// Keep the complete pulley in one render pipeline. Splitting the shaft and
		// extended belt between Flywheel and the block entity renderer causes the
		// two copies to depth-fight and be culled independently at some angles.
		BlockState shaftState = getRenderedBlockState(be);
		List<BlockStateModelPart> shaftParts = getRotatingModelParts(be, shaftState);
		if (!shaftParts.isEmpty()) {
			ms.pushPose();
			transformRotatingModel(be, ms, kineticState.partialTicks);
			collector.submitBlockModel(ms, getRotatingRenderType(shaftParts), shaftParts,
				BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		float offset = PulleyRenderer.getBlockEntityOffset(kineticState.partialTicks, be);
		boolean running = PulleyRenderer.isPulleyRunning(be);
		Direction facing = be.getBlockState()
			.getValue(ElevatorPulleyBlock.HORIZONTAL_FACING);
		float modelAngle = 180 + AngleHelper.horizontalAngle(facing);
		float coilAngle = AngleHelper.horizontalAngle(facing);

		if (running || offset == 0)
			submitAt(CreateStandaloneModels.ELEVATOR_MAGNET, offset, modelAngle, ms, collector, state.lightCoords);

		ms.pushPose();
		rotateCenteredY(ms, coilAngle);
		if (offset == 0)
			submitPart(CreateStandaloneModels.ELEVATOR_COIL, ms, collector, state.lightCoords);
		else
			submitScrollingPart(CreateStandaloneModels.ELEVATOR_COIL, AllSpriteShifts.ELEVATOR_COIL,
				getCoilScroll(AllSpriteShifts.ELEVATOR_COIL, offset, 2), ms, collector, state.lightCoords);
		ms.popPose();

		if (offset == 0)
			return;

		submitBelt(offset, running, modelAngle, ms, collector, state.lightCoords);
	}

	private static void submitAt(StandaloneModelKey<BlockStateModelPart> key, float offset, float yRot, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		ms.pushPose();
		ms.translate(0, -offset, 0);
		rotateCenteredY(ms, yRot);
		submitPart(key, ms, collector, light);
		ms.popPose();
	}

	private static void rotateCenteredY(PoseStack ms, float yRot) {
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
		ms.translate(-.5, -.5, -.5);
	}

	private static void submitBelt(float offset, boolean running, float yRot, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		if (offset <= 0)
			return;

		SpriteShiftEntry beltShift = AllSpriteShifts.ELEVATOR_BELT;
		float spriteSize = beltShift.getTarget()
			.getV1()
			- beltShift.getTarget()
				.getV0();
		double beltScroll = (-(offset + .5) - Math.floor(-(offset + .5))) / 2;
		float scroll = (float) beltScroll * spriteSize;
		float f = offset % 1;

		if (f < .25f || f > .75f)
			submitAt(CreateStandaloneModels.ELEVATOR_BELT_HALF, f > .75f ? f - 1 : f, yRot, beltShift, scroll, ms,
				collector, light);

		if (!running)
			return;

		for (int i = 0; i < offset - .25f; i++)
			submitAt(CreateStandaloneModels.ELEVATOR_BELT, offset - i, yRot, beltShift, scroll, ms, collector, light);
	}

	private static void submitPart(StandaloneModelKey<BlockStateModelPart> key, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		if (part == null)
			return;
		collector.submitBlockModel(ms, RenderTypes.solidMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}

	private static void submitScrollingPart(StandaloneModelKey<BlockStateModelPart> key, SpriteShiftEntry shift,
		float scrollV, PoseStack ms, SubmitNodeCollector collector, int light) {
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		if (part == null)
			return;
		collector.submitBlockModel(ms, RenderTypes.solidMovingBlock(),
			List.of(new ScrollingSpritePart(part, shift, scrollV)), BlockModelRenderState.EMPTY_TINTS, light,
			OverlayTexture.NO_OVERLAY, 0);
	}

	private static float getCoilScroll(SpriteShiftEntry coilShift, float offset, float speedModifier) {
		float spriteSize = coilShift.getTarget()
			.getV1()
			- coilShift.getTarget()
				.getV0();
		offset *= speedModifier / 2;
		double coilScroll = -(offset + 3 / 16f) - Math.floor((offset + 3 / 16f) * -2) / 2;
		return (float) coilScroll * spriteSize;
	}

	private static void submitAt(StandaloneModelKey<BlockStateModelPart> key, float offset, float yRot,
		SpriteShiftEntry shift, float scrollV, PoseStack ms, SubmitNodeCollector collector, int light) {
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		if (part == null)
			return;
		ms.pushPose();
		ms.translate(0, -offset, 0);
		rotateCenteredY(ms, yRot);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(),
			List.of(new ScrollingSpritePart(part, shift, scrollV)), BlockModelRenderState.EMPTY_TINTS, light,
			OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private record ScrollingSpritePart(BlockStateModelPart delegate, SpriteShiftEntry shift,
		float scrollV) implements BlockStateModelPart {

		@Override
		public List<BakedQuad> getQuads(Direction side) {
			List<BakedQuad> quads = delegate.getQuads(side);
			if (quads.isEmpty())
				return quads;
			return quads.stream()
				.map(this::shift)
				.toList();
		}

		private BakedQuad shift(BakedQuad quad) {
			if (quad.materialInfo()
				.sprite() != shift.getOriginal())
				return quad;
			return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
				shiftUv(quad.packedUV0()), shiftUv(quad.packedUV1()), shiftUv(quad.packedUV2()),
				shiftUv(quad.packedUV3()), quad.direction(), quad.materialInfo());
		}

		private long shiftUv(long packedUv) {
			float u = Float.intBitsToFloat((int) (packedUv >>> 32));
			float v = Float.intBitsToFloat((int) packedUv);
			float targetU = u - shift.getOriginal()
				.getU0()
				+ shift.getTarget()
					.getU0();
			float targetV = v - shift.getOriginal()
				.getV0()
				+ shift.getTarget()
					.getV0()
				+ scrollV;
			long shiftedU = Integer.toUnsignedLong(Float.floatToIntBits(targetU));
			long shiftedV = Integer.toUnsignedLong(Float.floatToIntBits(targetV));
			return (shiftedU << 32) | shiftedV;
		}

		@Override
		public boolean useAmbientOcclusion() {
			return delegate.useAmbientOcclusion();
		}

		@Override
		public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
			return delegate.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return delegate.materialFlags();
		}
	}

	@Override
	protected BlockState getRenderedBlockState(ElevatorPulleyBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	@Override
	public @NotNull AABB getRenderBoundingBox(@NotNull ElevatorPulleyBlockEntity be) {
		return new AABB(be.getBlockPos())
			.expandTowards(0, -AllConfigs.server().kinetics.maxRopeLength.get() - 2, 0)
			.inflate(1);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return AllConfigs.server().kinetics.maxRopeLength.get() + 16;
	}

	@Override
	public boolean shouldRender(ElevatorPulleyBlockEntity be, Vec3 cameraPos) {
		int viewDistance = getViewDistance();
		return Vec3.atCenterOf(be.getBlockPos())
			.distanceToSqr(cameraPos) <= viewDistance * viewDistance;
	}
}
