package com.simibubi.create.content.logistics.factoryBoard;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.FlatGuiItemRenderer;

import net.createmod.catnip.api.theme.Color;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class FactoryPanelRenderer extends SmartBlockEntityRenderer<FactoryPanelBlockEntity> {

	private final Map<StandaloneModelKey<BlockStateModelPart>, BlockStateModelPart> models = new java.util.HashMap<>();
	private final Map<BlockStateModelPart, BlockStateModelPart> tintedModels = new IdentityHashMap<>();
	private final Map<BlockStateModelPart, BlockStateModelPart> animatedModels = new IdentityHashMap<>();

	public FactoryPanelRenderer(Context context) {
		super(context);
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new FactoryPanelRenderState();
	}

	@Override
	public void extractRenderState(FactoryPanelBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (!(state instanceof FactoryPanelRenderState panelState))
			return;
		panelState.blockEntity = be;
		panelState.partialTicks = partialTicks;
		panelState.active.clear();
		for (var entry : be.panels.entrySet())
			if (entry.getValue().active)
				panelState.active.put(entry.getKey(), entry.getValue());
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof FactoryPanelRenderState panelState) || panelState.blockEntity == null)
			return;
		FactoryPanelBlockEntity be = panelState.blockEntity;
		BlockState blockState = be.getBlockState();
		for (var entry : panelState.active.entrySet()) {
			FactoryPanelBehaviour behaviour = entry.getValue();
			submitPanel(be, blockState, entry.getKey(), behaviour, ms, collector, state.lightCoords);
			submitFilter(blockState, behaviour, ms, collector, state.lightCoords);
			if (behaviour.getAmount() > 0)
				submitBulb(behaviour, panelState.partialTicks, ms, collector, state.lightCoords);
			for (FactoryPanelConnection connection : behaviour.targetedBy.values())
				submitPath(behaviour, connection, panelState.partialTicks, ms, collector, state.lightCoords);
			for (FactoryPanelConnection connection : behaviour.targetedByLinks.values())
				submitPath(behaviour, connection, panelState.partialTicks, ms, collector, state.lightCoords);
		}
	}

	private void submitBulb(FactoryPanelBehaviour behaviour, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		float glow = behaviour.bulb.getValue(partialTicks);
		StandaloneModelKey<BlockStateModelPart> key = behaviour.redstonePowered || behaviour.isMissingAddress()
			? CreateStandaloneModels.FACTORY_PANEL_RED_LIGHT
			: CreateStandaloneModels.FACTORY_PANEL_LIGHT;
		BlockStateModelPart part = model(key);
		if (part == null)
			return;

		pushPanelTransform(ms, behaviour.blockEntity.getBlockState(), behaviour.slot);
		collector.submitBlockModel(ms, RenderTypes.translucentMovingBlock(), List.of(part),
			BlockModelRenderState.EMPTY_TINTS, glow > .125f ? LightCoordsUtil.FULL_BRIGHT : light,
			OverlayTexture.NO_OVERLAY, 0);
		if (glow >= .125f) {
			float pulse = Mth.clamp((float) (1 - 2 * Math.pow(glow - .75f, 2)), -1, 1);
			int shade = Mth.clamp((int) (200 * pulse), 0, 255);
			int color = 0xff000000 | shade << 16 | shade << 8 | shade;
			collector.submitBlockModel(ms, RenderTypes.translucentMovingBlock(),
				List.of(tintedModels.computeIfAbsent(part, TintedBlockStateModelPart::new)), new int[] { color },
				LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
		}
		ms.popPose();
	}

	private void submitPath(FactoryPanelBehaviour behaviour, FactoryPanelConnection connection, float partialTicks,
		PoseStack ms, SubmitNodeCollector collector, int light) {
		BlockState blockState = behaviour.blockEntity.getBlockState();
		List<Direction> path = connection.getPath(behaviour.getWorld(), blockState, behaviour.getPanelPosition());
		FactoryPanelSupportBehaviour support = FactoryPanelBehaviour.linkAt(behaviour.getWorld(), connection);
		boolean displayLink = support != null && support.blockEntity instanceof DisplayLinkBlockEntity;
		boolean redstoneLink = support != null && support.blockEntity instanceof RedstoneLinkBlockEntity;
		boolean reversed = support != null && !support.isOutput();

		int color;
		float yOffset = 0;
		boolean dots = false;
		if (displayLink) {
			color = 0x3c9852;
			dots = true;
		} else if (redstoneLink) {
			color = reversed ? (behaviour.count == 0 ? 0x888898 : behaviour.satisfied ? 0xef0000 : 0x580101)
				: (behaviour.redstonePowered ? 0xef0000 : 0x580101);
			yOffset = .5f;
		} else {
			color = behaviour.getIngredientStatusColor();
			yOffset = 1 + (behaviour.promisedSatisfied ? 1 : behaviour.satisfied ? 0 : 2);
			float glow = behaviour.bulb.getValue(partialTicks);
			if (!behaviour.redstonePowered && !behaviour.waitingForNetwork && glow > 0 && !behaviour.satisfied) {
				float progress = 1 - (1 - glow) * (1 - glow);
				color = Color.mixColors(color, connection.success ? 0xeaf2ec : 0xe5654b, progress);
				if (!behaviour.promisedSatisfied)
					yOffset += (connection.success ? 1 : 2) * progress;
			}
		}

		float currentX = 0;
		float currentZ = 0;
		for (int i = 0; i < path.size(); i++) {
			Direction direction = path.get(i);
			if (!reversed) {
				currentX += direction.getStepX() * .5f;
				currentZ += direction.getStepZ() * .5f;
			}
			boolean arrow = reversed ? i == path.size() - 1 : i == 0;
			Map<Direction, StandaloneModelKey<BlockStateModelPart>> modelsByDirection = dots
				? CreateStandaloneModels.FACTORY_PANEL_DOTTED
				: arrow ? CreateStandaloneModels.FACTORY_PANEL_ARROWS : CreateStandaloneModels.FACTORY_PANEL_LINES;
			BlockStateModelPart part = model(modelsByDirection.get(reversed ? direction : direction.getOpposite()));
			if (part != null) {
				boolean animate = !displayLink && !redstoneLink && !behaviour.isMissingAddress()
					&& !behaviour.waitingForNetwork && !behaviour.satisfied && !behaviour.redstonePowered;
				if (animate)
					part = animatedModels.computeIfAbsent(part, AnimatedConnectionPart::new);
				part = tintedModels.computeIfAbsent(part, TintedBlockStateModelPart::new);
				pushPanelTransform(ms, blockState, behaviour.slot);
				ms.translate(.25f + currentX,
					(yOffset + direction.get2DDataValue() % 2 * .125f) / 512f, .25f + currentZ);
				collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
					new int[] { 0xff000000 | color }, light, OverlayTexture.NO_OVERLAY, 0);
				ms.popPose();
			}
			if (reversed) {
				currentX += direction.getStepX() * .5f;
				currentZ += direction.getStepZ() * .5f;
			}
		}
	}

	private BlockStateModelPart model(StandaloneModelKey<BlockStateModelPart> key) {
		return key == null ? null : models.computeIfAbsent(key,
			k -> Minecraft.getInstance().getModelManager().getStandaloneModel(k));
	}

	private static void pushPanelTransform(PoseStack ms, BlockState state, PanelSlot slot) {
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(Mth.RAD_TO_DEG * FactoryPanelBlock.getYRot(state)));
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(Mth.RAD_TO_DEG * FactoryPanelBlock.getXRot(state) + 90));
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
		ms.translate(-.5, -.5, -.5);
		ms.translate(slot.xOffset * .5, 0, slot.yOffset * .5);
	}

	private void submitFilter(BlockState state, FactoryPanelBehaviour behaviour, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		ItemStack filter = behaviour.getFilter();
		if (filter.isEmpty())
			return;

		float xRot = Mth.RAD_TO_DEG * FactoryPanelBlock.getXRot(state);
		float yRot = Mth.RAD_TO_DEG * FactoryPanelBlock.getYRot(state);

		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRot + 90));
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
		ms.translate(-.5, -.5, -.5);
		ms.translate(behaviour.slot.xOffset * .5, 0, behaviour.slot.yOffset * .5);
		ms.translate(.25f, 2.05f / 16f, .25f);
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .24f, 180);
		ms.popPose();
	}

	private void submitPanel(FactoryPanelBlockEntity be, BlockState state, PanelSlot slot,
		FactoryPanelBehaviour behaviour, PoseStack ms, SubmitNodeCollector collector, int light) {
		StandaloneModelKey<BlockStateModelPart> key = behaviour.count == 0
			? be.restocker ? CreateStandaloneModels.FACTORY_PANEL_RESTOCKER : CreateStandaloneModels.FACTORY_PANEL
			: be.restocker ? CreateStandaloneModels.FACTORY_PANEL_RESTOCKER_WITH_BULB
				: CreateStandaloneModels.FACTORY_PANEL_WITH_BULB;
		BlockStateModelPart model = models.computeIfAbsent(key,
			k -> Minecraft.getInstance().getModelManager().getStandaloneModel(k));
		if (model == null)
			return;

		float xRot = Mth.RAD_TO_DEG * FactoryPanelBlock.getXRot(state);
		float yRot = Mth.RAD_TO_DEG * FactoryPanelBlock.getYRot(state);
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRot + 90));
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
		ms.translate(-.5, -.5, -.5);
		ms.translate(slot.xOffset * .5, 0, slot.yOffset * .5);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(model),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	@Override
	protected void renderSafe(FactoryPanelBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		// Rendering is submitted through the 26.2 renderer API.
	}

	public static void renderBulb(FactoryPanelBehaviour behaviour, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
	}

	public static void renderPath(FactoryPanelBehaviour behaviour, FactoryPanelConnection connection,
		float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
	}

	private static class FactoryPanelRenderState extends BlockEntityRenderState {
		private FactoryPanelBlockEntity blockEntity;
		private float partialTicks;
		private final EnumMap<PanelSlot, FactoryPanelBehaviour> active = new EnumMap<>(PanelSlot.class);
	}

	private static class TintedBlockStateModelPart implements BlockStateModelPart {
		private final BlockStateModelPart wrapped;
		private final Map<Direction, List<BakedQuad>> cache = new HashMap<>();

		private TintedBlockStateModelPart(BlockStateModelPart wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public List<BakedQuad> getQuads(Direction direction) {
			return cache.computeIfAbsent(direction, side -> wrapped.getQuads(side).stream().map(quad -> {
				BakedQuad.MaterialInfo material = quad.materialInfo();
				BakedQuad.MaterialInfo tinted = new BakedQuad.MaterialInfo(material.sprite(), material.layer(),
					material.itemRenderType(), 0, material.shade(), material.lightEmission(), material.ambientOcclusion());
				return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
					quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), tinted,
					quad.bakedNormals(), quad.bakedColors());
			}).toList());
		}

		@Override public boolean useAmbientOcclusion() { return wrapped.useAmbientOcclusion(); }
		@Override public Material.Baked particleMaterial() { return wrapped.particleMaterial(); }
		@Override public int materialFlags() { return wrapped.materialFlags(); }
	}

	private static class AnimatedConnectionPart implements BlockStateModelPart {
		private final BlockStateModelPart wrapped;
		private final Map<Direction, List<BakedQuad>> cache = new HashMap<>();

		private AnimatedConnectionPart(BlockStateModelPart wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public List<BakedQuad> getQuads(Direction direction) {
			return cache.computeIfAbsent(direction,
				side -> wrapped.getQuads(side).stream().map(AnimatedConnectionPart::shift).toList());
		}

		private static BakedQuad shift(BakedQuad quad) {
			TextureAtlasSprite source = quad.materialInfo().sprite();
			TextureAtlasSprite target = AllSpriteShifts.FACTORY_PANEL_CONNECTIONS.getTarget();
			BakedQuad.MaterialInfo material = quad.materialInfo();
			BakedQuad.MaterialInfo shifted = new BakedQuad.MaterialInfo(target, material.layer(),
				material.itemRenderType(), material.tintIndex(), material.shade(), material.lightEmission(),
				material.ambientOcclusion());
			return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
				remap(quad.packedUV0(), source, target), remap(quad.packedUV1(), source, target),
				remap(quad.packedUV2(), source, target), remap(quad.packedUV3(), source, target), quad.direction(),
				shifted, quad.bakedNormals(), quad.bakedColors());
		}

		private static long remap(long packedUv, TextureAtlasSprite source, TextureAtlasSprite target) {
			float u = Float.intBitsToFloat((int) (packedUv >>> 32));
			float v = Float.intBitsToFloat((int) packedUv);
			float localU = (u - source.getU0()) / (source.getU1() - source.getU0());
			float localV = (v - source.getV0()) / (source.getV1() - source.getV0());
			return Integer.toUnsignedLong(Float.floatToIntBits(target.getU(localU))) << 32
				| Integer.toUnsignedLong(Float.floatToIntBits(target.getV(localV)));
		}

		@Override public boolean useAmbientOcclusion() { return wrapped.useAmbientOcclusion(); }
		@Override public Material.Baked particleMaterial() { return wrapped.particleMaterial(); }
		@Override public int materialFlags() { return wrapped.materialFlags(); }
	}
}
