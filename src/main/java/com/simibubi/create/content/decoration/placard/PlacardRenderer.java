package com.simibubi.create.content.decoration.placard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;

public class PlacardRenderer extends SafeBlockEntityRenderer<PlacardBlockEntity> {

	public PlacardRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new PlacardRenderState();
	}

	@Override
	public void extractRenderState(PlacardBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof PlacardRenderState placardState) {
			placardState.blockState = be.getBlockState();
			placardState.heldItem = be.getHeldItem()
				.copy();
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof PlacardRenderState placardState))
			return;
		ItemStack heldItem = placardState.heldItem;
		if (heldItem.isEmpty())
			return;

		BlockState blockState = placardState.blockState;
		if (blockState == null)
			return;

		Direction facing = blockState.getValue(PlacardBlock.FACING);
		AttachFace face = blockState.getValue(PlacardBlock.FACE);
		boolean blockItem = heldItem.getItem() instanceof BlockItem;

		ms.pushPose();
		ms.translate(.5, .5, .5);
		if (face == AttachFace.CEILING)
			ms.mulPose(Axis.YP.rotationDegrees(180));
		ms.mulPose(Axis.YP.rotationDegrees(180 + AngleHelper.horizontalAngle(facing)));
		if (face == AttachFace.CEILING)
			ms.mulPose(Axis.XP.rotationDegrees(-90));
		else if (face == AttachFace.FLOOR)
			ms.mulPose(Axis.XP.rotationDegrees(90));
		ms.translate(0, 0, 4.5 / 16f);
		float scale = blockItem ? .5f : .375f;
		ms.scale(scale, scale, scale);

		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, heldItem, ItemDisplayContext.FIXED, null, null, 0);
		itemState.submit(ms, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	@Override
	protected void renderSafe(PlacardBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
	}

	private static class PlacardRenderState extends BlockEntityRenderState {
		private BlockState blockState;
		private ItemStack heldItem = ItemStack.EMPTY;
	}

}
