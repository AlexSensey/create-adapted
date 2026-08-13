package com.simibubi.create.content.logistics.tableCloth;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
import com.simibubi.create.foundation.render.FlatGuiItemRenderer;

import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class TableClothRenderer extends SmartBlockEntityRenderer<TableClothBlockEntity> {

	public TableClothRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(TableClothBlockEntity blockEntity, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new TableClothRenderState();
	}

	@Override
	public void extractRenderState(TableClothBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof TableClothRenderState clothState) {
			clothState.blockEntity = be;
			clothState.items = be.getItemsForRender().stream().map(ItemStack::copy).toList();
			clothState.facing = be.facing;
			clothState.sideOccluded = be.sideOccluded;
			clothState.shop = be.isShop();
			clothState.position = be.getBlockPos();
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof TableClothRenderState clothState) || clothState.facing == null)
			return;
		float rotation = 180 - clothState.facing.toYRot();
		submitPriceFilter(clothState.blockEntity, ms, collector, state.lightCoords);

		if (clothState.shop) {
			BlockStateModelPart priceTag = Minecraft.getInstance().getModelManager().getStandaloneModel(
				clothState.sideOccluded ? CreateStandaloneModels.TABLE_CLOTH_PRICE_TOP
					: CreateStandaloneModels.TABLE_CLOTH_PRICE_SIDE);
			if (priceTag != null) {
				ms.pushPose();
				rotateCenteredY(ms, rotation);
				collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(priceTag),
					BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
				ms.popPose();
			}
		}

		ms.pushPose();
		rotateCenteredY(ms, rotation);
		for (int i = 0; i < clothState.items.size(); i++) {
			ItemStack stack = clothState.items.get(i);
			if (stack.isEmpty())
				continue;
			ItemStackRenderState itemState = new ItemStackRenderState();
			Minecraft.getInstance().getItemModelResolver()
				.updateForTopItem(itemState, stack, ItemDisplayContext.FIXED, null, null, 0);
			boolean faceCamera = isFlatModel(itemState);
			ms.pushPose();
			ms.translate(.5f, 3 / 16f, .5f);
			if (clothState.items.size() > 1) {
				float itemRotation = i * (360f / clothState.items.size()) + 45;
				ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(itemRotation));
				ms.translate(0, i % 2 == 0 ? -.005f : 0, 5 / 16f);
				ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-itemRotation));
			}
			if (faceCamera)
				ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-rotation + 180));
			submitItem(ms, collector, state.lightCoords, itemState, faceCamera,
				Vec3.atCenterOf(clothState.position));
			ms.popPose();
		}
		ms.popPose();
	}

	private static void submitPriceFilter(TableClothBlockEntity be, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		if (be == null || be.priceTag == null || !be.priceTag.isActive() || be.priceTag.getFilter().isEmpty())
			return;
		if (!be.priceTag.getSlotPositioning().shouldRender(be.getLevel(), be.getBlockPos(), be.getBlockState()))
			return;

		ms.pushPose();
		applyPriceSurfaceTransform(be, ms, 1 / 32d);
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));
		ms.scale(.5f, .5f, .5f);
		renderPriceItemPass(be.priceTag.getFilter(), ms, collector, light);
		ms.pushPose();
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
		ms.translate(0, 0, 1 / 128f);
		renderPriceItemPass(be.priceTag.getFilter(), ms, collector, light);
		ms.popPose();
		ms.popPose();
	}

	private static void renderPriceItemPass(ItemStack filter, PoseStack ms, SubmitNodeCollector collector, int light) {
		FlatGuiItemRenderer.submit(filter, ms, collector, light, .5f);
	}

	public static void applyPriceSurfaceTransform(TableClothBlockEntity be, PoseStack ms, double outwardOffset) {
		ValueBoxTransform slot = be.priceTag.getSlotPositioning();
		Vec3 offset = slot.getLocalOffset(be.getLevel(), be.getBlockPos(), be.getBlockState());
		Direction side = be.sideOccluded ? Direction.UP : be.facing;
		Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
		ms.translate(offset.x + normal.x * outwardOffset, offset.y + normal.y * outwardOffset,
			offset.z + normal.z * outwardOffset);
		rotateToFace(ms, side);
		ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));
		if (side == Direction.UP) {
			float angle = switch (be.facing) {
				case NORTH -> 0;
				case SOUTH -> 180;
				case WEST -> 90;
				case EAST -> 270;
				default -> 0;
			};
			ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
		}
	}

	private static void rotateToFace(PoseStack ms, Direction face) {
		switch (face) {
			case SOUTH -> { }
			case NORTH -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
			case EAST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
			case WEST -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270));
			case UP -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(270));
			case DOWN -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
		}
	}

	private static void rotateCenteredY(PoseStack ms, float degrees) {
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(degrees));
		ms.translate(-.5f, -.5f, -.5f);
	}

	private static void submitItem(PoseStack ms, SubmitNodeCollector collector, int light,
		ItemStackRenderState itemState, boolean faceCamera, Vec3 itemPosition) {
		ms.pushPose();
		if (faceCamera) {
			Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
			Vec3 diff = itemPosition.subtract(camera);
			float yRot = (float) (Mth.atan2(diff.x, diff.z) + Math.PI);
			ms.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
			ms.translate(0, 3 / 32d, -1 / 16f);
		}
		ms.scale(.5f, .5f, .5f);
		itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static boolean isFlatModel(ItemStackRenderState itemState) {
		net.minecraft.world.phys.AABB bounds = itemState.getModelBoundingBox();
		double smallestDimension = Math.min(bounds.getXsize(), Math.min(bounds.getYsize(), bounds.getZsize()));
		return smallestDimension < .25;
	}

	private static class TableClothRenderState extends BlockEntityRenderState {
		private TableClothBlockEntity blockEntity;
		private List<ItemStack> items = List.of();
		private Direction facing;
		private boolean sideOccluded;
		private boolean shop;
		private BlockPos position = BlockPos.ZERO;
	}
}
