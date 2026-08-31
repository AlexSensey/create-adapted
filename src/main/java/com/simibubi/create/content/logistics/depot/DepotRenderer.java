package com.simibubi.create.content.logistics.depot;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.belt.BeltItemRenderHelper;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class DepotRenderer extends SafeBlockEntityRenderer<DepotBlockEntity> {

	public DepotRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(DepotBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new DepotRenderState();
	}

	@Override
	public void extractRenderState(DepotBlockEntity be, BlockEntityRenderState state, float partialTicks, Vec3 cameraPos,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (state instanceof DepotRenderState depotState) {
			depotState.blockEntity = be;
			depotState.partialTicks = partialTicks;
		}
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof DepotRenderState depotState))
			return;
		DepotBlockEntity be = depotState.blockEntity;
		if (be == null || isInvalid(be))
			return;

		renderItemsOf(be, depotState.partialTicks, ms, collector, state.lightCoords, be.depotBehaviour);
	}

	public static void renderItemsOf(SmartBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay, DepotBehaviour depotBehaviour) {}

	public static void renderItemsOf(SmartBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light, DepotBehaviour depotBehaviour) {
		TransportedItemStack transported = depotBehaviour.heldItem;
		Vec3 itemPosition = VecHelper.getCenterOf(be.getBlockPos());

		ms.pushPose();
		ms.translate(.5f, 15 / 16f, .5f);

		if (transported != null)
			renderTransportedItem(be, partialTicks, ms, collector, light, transported, itemPosition);

		for (TransportedItemStack incoming : depotBehaviour.incoming)
			renderTransportedItem(be, partialTicks, ms, collector, light, incoming, itemPosition);

		for (int i = 0; i < depotBehaviour.processingOutputBuffer.getSlots(); i++) {
			ItemStack stack = depotBehaviour.processingOutputBuffer.getStackInSlot(i);
			if (stack.isEmpty())
				continue;
			ms.pushPose();
			ItemStackRenderState itemState = BeltItemRenderHelper.createRenderState(stack);
			boolean upright = BeltHelper.isItemUpright(stack);
			boolean blockItem = BeltItemRenderHelper.isGui3d(itemState);
			ms.mulPose(Axis.YP.rotationDegrees(360 / 8f * i));
			ms.translate(.35f, 0, 0);
			if (upright)
				ms.mulPose(Axis.YP.rotationDegrees(-(360 / 8f * i)));
			int angle = (int) (360 * new Random(i + 1).nextFloat());
			renderItem(ms, collector, light, stack, itemState, upright ? angle + 90 : angle, upright, blockItem,
				itemPosition);
			ms.popPose();
		}

		ms.popPose();
	}

	private static void renderTransportedItem(SmartBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light, TransportedItemStack transported, Vec3 itemPosition) {
		ms.pushPose();
		float offset = Mth.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
		float sideOffset = Mth.lerp(partialTicks, transported.prevSideOffset, transported.sideOffset);

		if (transported.insertedFrom.getAxis()
			.isHorizontal()) {
			Direction opposite = transported.insertedFrom.getOpposite();
			Vec3 offsetVec = new Vec3(opposite.getStepX(), opposite.getStepY(), opposite.getStepZ())
				.scale(.5f - offset);
			ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);
			boolean alongX = transported.insertedFrom.getClockWise()
				.getAxis() == Direction.Axis.X;
			if (!alongX)
				sideOffset *= -1;
			ms.translate(alongX ? sideOffset : 0, 0, alongX ? 0 : sideOffset);
		}

		ItemStackRenderState itemState = BeltItemRenderHelper.createRenderState(transported.stack);
		boolean upright = BeltHelper.isItemUpright(transported.stack);
		boolean blockItem = BeltItemRenderHelper.isGui3d(itemState);
		renderItem(ms, collector, light, transported.stack, itemState, transported.angle, upright, blockItem,
			itemPosition);
		ms.popPose();
	}

	public static void renderItem(PoseStack ms, MultiBufferSource buffer, int light, int overlay,
		ItemStack itemStack, int angle, Random r, Vec3 itemPosition, boolean alwaysUpright) {}

	private static void renderItem(PoseStack ms, SubmitNodeCollector collector, int light, ItemStack itemStack,
		ItemStackRenderState itemState, int angle, boolean renderUpright, boolean blockItem, Vec3 itemPosition) {
		if (itemStack.isEmpty())
			return;

		int count = Mth.log2(itemStack.getCount()) / 2;
		Random random = new Random(0);

		ms.pushPose();
		ms.mulPose(Axis.YP.rotationDegrees(angle));
		boolean box = PackageItem.isPackage(itemStack);
		if (renderUpright) {
			Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.mainCamera()
				.position();
			Vec3 diff = itemPosition.subtract(cameraPosition);
			float yRot = (float) (Mth.atan2(diff.x, diff.z) + Math.PI);
			ms.mulPose(Axis.YP.rotation(yRot));
			ms.translate(0, 3 / 32d, -1 / 16f);
		}

		for (int i = 0; i <= count; i++) {
			ms.pushPose();
			if (blockItem && !box)
				ms.translate(random.nextFloat() * .0625f * i, 0, random.nextFloat() * .0625f * i);
			if (box) {
				ms.translate(0, 4 / 16f, 0);
				ms.scale(1.5f, 1.5f, 1.5f);
			} else {
				ms.scale(.5f, .5f, .5f);
			}
			if (!blockItem && !renderUpright) {
				ms.translate(0, -3 / 16f, 0);
				ms.mulPose(Axis.XP.rotationDegrees(90));
			}

			itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();

			if (!renderUpright) {
				if (!blockItem)
					ms.mulPose(Axis.YP.rotationDegrees(10));
				ms.translate(0, blockItem ? 1 / 64d : 1 / 16d, 0);
			} else
				ms.translate(0, 0, -1 / 16f);
		}

		ms.popPose();
	}

	private static class DepotRenderState extends BlockEntityRenderState {
		private DepotBlockEntity blockEntity;
		private float partialTicks;
	}
}
