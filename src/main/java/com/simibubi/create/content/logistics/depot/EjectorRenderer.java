package com.simibubi.create.content.logistics.depot;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueLabelRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.transform.Rotate;
import dev.engine_room.flywheel.lib.transform.Translate;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.createmod.catnip.api.data.IntAttached;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class EjectorRenderer extends ShaftRenderer<EjectorBlockEntity> {

	static final Vec3 pivot = VecHelper.voxelSpace(0, 11.25, 0.75);
	private List<BlockStateModelPart> topModel;

	public EjectorRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	public boolean shouldRenderOffScreen(EjectorBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(EjectorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		super.submit(state, ms, collector, cameraRenderState);

		if (!(state instanceof KineticRenderState kineticState))
			return;
		if (!(kineticState.blockEntity instanceof EjectorBlockEntity be))
			return;
		if (isInvalid(be))
			return;

		float partialTicks = kineticState.partialTicks;
		float lidProgress = be.getLidProgress(partialTicks);
		float angle = lidProgress * 70;

		List<BlockStateModelPart> top = getTopModel();
		if (!VisualizationManager.supportsVisualization(be.getLevel()) && !top.isEmpty()) {
			ms.pushPose();
			applyLidAngle(be, angle, ms);
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), top, BlockModelRenderState.EMPTY_TINTS,
				state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}

		renderLaunchedItems(be, partialTicks, ms, collector, state.lightCoords);
		ScrollValueLabelRenderer.submitEjector(be.maxStackSize, state, ms, collector, cameraRenderState);

		DepotBehaviour behaviour = be.getBehaviour(DepotBehaviour.TYPE);
		if (behaviour == null || behaviour.isEmpty())
			return;

		ms.pushPose();
		var msr = TransformStack.of(ms);
		applyLidAngle(be, angle, ms);
		msr.center()
			.rotateYDegrees(-180 - AngleHelper.horizontalAngle(be.getBlockState()
				.getValue(EjectorBlock.HORIZONTAL_FACING)))
			.uncenter();
		DepotRenderer.renderItemsOf(be, partialTicks, ms, collector, state.lightCoords, behaviour);
		ms.popPose();
	}

	private void renderLaunchedItems(EjectorBlockEntity be, float partialTicks, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		float maxTime = (float) (be.earlyTarget != null ? be.earlyTargetTime : be.launcher.getTotalFlyingTicks());

		for (IntAttached<ItemStack> intAttached : be.launchedItems) {
			float time = intAttached.getFirst() + partialTicks;
			if (time > maxTime)
				continue;

			ms.pushPose();
			Vec3 launchedItemLocation = be.getLaunchedItemLocation(time);
			Vec3 local = launchedItemLocation.subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
			ms.translate(local.x, local.y, local.z);
			Vec3 itemRotOffset = VecHelper.voxelSpace(0, 2, -1);
			ms.translate(itemRotOffset.x, itemRotOffset.y, itemRotOffset.z);

			if (PackageItem.isPackage(intAttached.getValue())) {
				ms.translate(0, 4 / 16f, 0);
				ms.scale(1.5f, 1.5f, 1.5f);
				ms.mulPose(Axis.YP.rotationDegrees(time * 20));
			} else {
				ms.scale(.5f, .5f, .5f);
				ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(be.getFacing())));
				ms.mulPose(Axis.XP.rotationDegrees(time * 40));
			}
			ms.translate(-itemRotOffset.x, -itemRotOffset.y, -itemRotOffset.z);
			renderItem(ms, collector, light, intAttached.getValue());
			ms.popPose();
		}
	}

	private static void renderItem(PoseStack ms, SubmitNodeCollector collector, int light, ItemStack stack) {
		if (stack.isEmpty())
			return;
		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, stack, ItemDisplayContext.FIXED, null, null, 0);
		itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
	}

	private List<BlockStateModelPart> getTopModel() {
		if (topModel != null)
			return topModel;
		BlockStateModelPart top = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.EJECTOR_TOP);
		return topModel = top == null ? List.of() : List.of(top);
	}

	static <T extends Translate<T> & Rotate<T>> void applyLidAngle(KineticBlockEntity be, float angle, T tr) {
		applyLidAngle(be, pivot, angle, tr);
	}

	static <T extends Translate<T> & Rotate<T>> void applyLidAngle(KineticBlockEntity be, Vec3 rotationOffset,
		float angle, T tr) {
		tr.center()
			.rotateYDegrees(180 + AngleHelper.horizontalAngle(be.getBlockState()
				.getValue(EjectorBlock.HORIZONTAL_FACING)))
			.uncenter()
			.translate(rotationOffset)
			.rotateXDegrees(-angle)
			.translateBack(rotationOffset);
	}

	static void applyLidAngle(KineticBlockEntity be, float angle, PoseStack ms) {
		applyLidAngle(be, pivot, angle, ms);
	}

	static void applyLidAngle(KineticBlockEntity be, Vec3 rotationOffset, float angle, PoseStack ms) {
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(180 + AngleHelper.horizontalAngle(be.getBlockState()
			.getValue(EjectorBlock.HORIZONTAL_FACING))));
		ms.translate(-.5, -.5, -.5);
		ms.translate(rotationOffset.x, rotationOffset.y, rotationOffset.z);
		ms.mulPose(Axis.XP.rotationDegrees(-angle));
		ms.translate(-rotationOffset.x, -rotationOffset.y, -rotationOffset.z);
	}
}
