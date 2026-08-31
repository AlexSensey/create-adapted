package com.simibubi.create.content.kinetics.fan;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.infrastructure.assets.ExternalCreateAssets;
import com.simibubi.create.foundation.render.CreateVisualizationManager;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;

import net.neoforged.neoforge.client.model.quad.BakedColors;

public class EncasedFanRenderer extends KineticBlockEntityRenderer<EncasedFanBlockEntity> {

	private List<BlockStateModelPart> shaftHalfModel;
	private List<BlockStateModelPart> fanInnerModel;

	public EncasedFanRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof EncasedFanBlockEntity be))
			return;
		if (isInvalid(be))
			return;
		if (ExternalCreateAssets.shouldUseFlywheelVisuals()
			&& CreateVisualizationManager.supportsVisualization(be.getLevel()))
			return;

		Direction direction = be.getBlockState()
			.getValue(FACING);
		Direction shaftSide = direction.getOpposite();
		int shaftLight = LightCoordsUtil.getLightCoords(be.getLevel(), be.getBlockPos()
			.relative(shaftSide));
		int fanLight = LightCoordsUtil.getLightCoords(be.getLevel(), be.getBlockPos()
			.relative(direction));

		List<BlockStateModelPart> shaftHalf = getShaftHalfModel();
		List<BlockStateModelPart> fanInner = getFanInnerModel();
		if (shaftHalf == null || fanInner == null)
			return;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(shaftSide.getAxis(),
			getAngleForBe(be, be.getBlockPos(), shaftSide.getAxis(), kineticState.partialTicks)));
		rotateToFacing(ms, shaftSide);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), shaftHalf,
			BlockModelRenderState.EMPTY_TINTS, shaftLight, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(direction.getAxis(), getFanAngle(be, kineticState.partialTicks)));
		rotateToFacing(ms, shaftSide);
		ms.translate(-.5, -.5, -.5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), fanInner,
			BlockModelRenderState.EMPTY_TINTS, fanLight, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	@Override
	protected void renderSafe(EncasedFanBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	private List<BlockStateModelPart> getShaftHalfModel() {
		if (shaftHalfModel != null)
			return shaftHalfModel;
		BlockStateModelPart shaftHalf = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.SHAFT_HALF);
		if (shaftHalf == null)
			return null;
		return shaftHalfModel = List.of(shaftHalf);
	}

	private List<BlockStateModelPart> getFanInnerModel() {
		if (fanInnerModel != null)
			return fanInnerModel;
		BlockStateModelPart fanInner = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.ENCASED_FAN_INNER);
		if (fanInner == null)
			return null;
		return fanInnerModel = List.of(new FallbackFanShadingPart(fanInner));
	}

	/**
	 * The legacy block renderer applied the north-face diffuse factor (0.8) to the blade quad.
	 * The 26.2 submit fallback does not reproduce that factor for a transformed standalone model,
	 * making the blades look white. Bake the missing factor into only the blade quad and disable
	 * its additional directional shade so the result cannot be shaded twice.
	 */
	private static class FallbackFanShadingPart implements BlockStateModelPart {
		private static final int SHADE = 204;
		private final BlockStateModelPart delegate;
		private final List<BakedQuad> unculled;
		private final Map<Direction, List<BakedQuad>> directional = new EnumMap<>(Direction.class);

		private FallbackFanShadingPart(BlockStateModelPart delegate) {
			this.delegate = delegate;
			unculled = shade(delegate.getQuads(null));
			for (Direction direction : Direction.values())
				directional.put(direction, shade(delegate.getQuads(direction)));
		}

		private static List<BakedQuad> shade(List<BakedQuad> quads) {
			return quads.stream().map(FallbackFanShadingPart::shade).toList();
		}

		private static BakedQuad shade(BakedQuad quad) {
			if (!quad.materialInfo().sprite().contents().name().equals(Create.asResource("block/fan_blades")))
				return quad;

			BakedQuad.MaterialInfo material = quad.materialInfo();
			BakedQuad.MaterialInfo shadedMaterial = new BakedQuad.MaterialInfo(material.sprite(), material.layer(),
				material.itemRenderType(), material.tintIndex(), false, material.lightEmission(),
				material.ambientOcclusion());
			BakedColors colors = BakedColors.of(shadeColor(quad.bakedColors().color(0)),
				shadeColor(quad.bakedColors().color(1)), shadeColor(quad.bakedColors().color(2)),
				shadeColor(quad.bakedColors().color(3)));
			return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
				quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
				shadedMaterial, quad.bakedNormals(), colors);
		}

		private static int shadeColor(int color) {
			int alpha = color >>> 24;
			int red = (color >>> 16 & 0xff) * SHADE / 255;
			int green = (color >>> 8 & 0xff) * SHADE / 255;
			int blue = (color & 0xff) * SHADE / 255;
			return alpha << 24 | red << 16 | green << 8 | blue;
		}

		@Override
		public List<BakedQuad> getQuads(Direction side) {
			return side == null ? unculled : directional.get(side);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return delegate.useAmbientOcclusion();
		}

		@Override
		public Material.Baked particleMaterial() {
			return delegate.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return delegate.materialFlags();
		}
	}

	private static float getFanAngle(KineticBlockEntity be, float partialTicks) {
		float time = getRenderTime(be, partialTicks);
		float speed = be.getSpeed() * 5;
		if (speed > 0)
			speed = Mth.clamp(speed, 80, 64 * 20);
		if (speed < 0)
			speed = Mth.clamp(speed, -64 * 20, -80);
		return ((time * speed * 3 / 10f) % 360) / 180f * (float) Math.PI;
	}

	private static void rotateToFacing(PoseStack ms, Direction direction) {
		switch (direction) {
			case NORTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
			case EAST -> ms.mulPose(Axis.YP.rotationDegrees(90));
			case WEST -> ms.mulPose(Axis.YP.rotationDegrees(-90));
			case UP -> ms.mulPose(Axis.XP.rotationDegrees(-90));
			case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(90));
			case SOUTH -> {
			}
		}
	}
}
