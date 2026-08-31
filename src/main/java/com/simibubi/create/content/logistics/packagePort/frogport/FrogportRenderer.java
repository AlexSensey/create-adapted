package com.simibubi.create.content.logistics.packagePort.frogport;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;
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
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class FrogportRenderer extends SmartBlockEntityRenderer<FrogportBlockEntity> {

	public FrogportRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(FrogportBlockEntity blockEntity, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new FrogportRenderState();
	}

	@Override
	public void extractRenderState(FrogportBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (!(state instanceof FrogportRenderState frogState))
			return;

		frogState.blockEntity = be;
		frogState.partialTicks = partialTicks;
		frogState.yaw = be.getYaw();
		frogState.goggles = be.goggles;
		frogState.packageStack = ItemStack.EMPTY;
		frogState.packageOffset = Vec3.ZERO;
		frogState.packageScale = 0;

		float headPitch = 80;
		float tonguePitch = 0;
		float tongueLength = 0;
		float headPitchModifier = 1;
		boolean animating = be.isAnimationInProgress();
		boolean depositing = be.currentlyDepositing;
		Vec3 diff = Vec3.ZERO;

		if (be.target != null) {
			diff = be.target.getExactTargetLocation(be, be.getLevel(), be.getBlockPos())
				.subtract(0, animating && depositing ? 0 : .75, 0)
				.subtract(Vec3.atCenterOf(be.getBlockPos()));
			tonguePitch = (float) Mth.atan2(diff.y, diff.multiply(1, 0, 1)
				.length() + 3 / 16f) * Mth.RAD_TO_DEG;
			tongueLength = Math.max((float) diff.length(), 1);
			headPitch = Mth.clamp(tonguePitch * 2, 60, 100);
		}

		if (animating) {
			float progress = be.animationProgress.getValue(partialTicks);
			float scale;
			float itemDistance;
			if (depositing) {
				double modifier = Math.max(0, 1 - Math.pow((progress - .25) * 4 - 1, 4));
				itemDistance =
					(float) Math.max(tongueLength * Math.min(1, (progress - .25) * 3), tongueLength * modifier);
				tongueLength *= Math.max(0, 1 - Math.pow((progress * 1.25 - .25) * 4 - 1, 4));
				headPitchModifier = (float) Math.max(0, 1 - Math.pow(progress * 1.25 * 2 - 1, 4));
				scale = .25f + progress * .75f;
			} else {
				tongueLength *= Math.pow(Math.max(0, 1 - progress * 1.25), 5);
				headPitchModifier =
					1 - (float) Math.min(1, Math.max(0, (Math.pow(progress * 1.5, 2) - .5) * 2));
				scale = (float) Math.max(.5, 1 - progress * 1.25);
				itemDistance = tongueLength;
			}
			if (be.animatedPackage != null && scale >= .45f) {
				frogState.packageStack = be.animatedPackage.copy();
				Vec3 direction = diff.lengthSqr() == 0 ? Vec3.ZERO : diff.normalize();
				frogState.packageOffset = direction.scale(itemDistance)
					.subtract(0, animating && depositing ? .75 : 0, 0);
				frogState.packageScale = scale;
			}
		} else {
			tongueLength = 0;
			float anticipation = be.anticipationProgress.getValue(partialTicks);
			headPitchModifier =
				anticipation > 0 ? (float) Math.max(0, 1 - Math.pow(anticipation * 1.25 * 2 - 1, 4)) : 0;
		}

		headPitch *= headPitchModifier;
		headPitch = Math.max(headPitch, be.manualOpenAnimationProgress.getValue(partialTicks) * 60);
		tongueLength =
			Math.max(tongueLength, be.manualOpenAnimationProgress.getValue(partialTicks) * .25f);
		frogState.headPitch = headPitch;
		frogState.tonguePitch = tonguePitch;
		frogState.tongueScale = tongueLength / (7 / 16f);
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof FrogportRenderState frogState) || frogState.blockEntity == null)
			return;
		submitBehaviours(frogState.blockEntity, frogState.partialTicks, ms, collector, state.lightCoords);

		if (frogState.blockEntity.addressFilter != null && !frogState.blockEntity.addressFilter.isBlank())
			submitNameplateOnHover(frogState.blockEntity,
				Component.literal(frogState.blockEntity.addressFilter), 1, ms, collector,
				cameraRenderState, state.lightCoords);

		ms.pushPose();
		rotateCenteredY(ms, frogState.yaw);
		submitPart(CreateStandaloneModels.FROGPORT_BODY, ms, collector, state.lightCoords);
		ms.popPose();

		StandaloneModelKey<BlockStateModelPart> head =
			frogState.goggles ? CreateStandaloneModels.FROGPORT_HEAD_GOGGLES : CreateStandaloneModels.FROGPORT_HEAD;
		ms.pushPose();
		rotateCenteredY(ms, frogState.yaw);
		transformAtHeadPivot(ms, frogState.headPitch, 1);
		submitPart(head, ms, collector, state.lightCoords);
		ms.popPose();

		if (frogState.tongueScale > 0) {
			ms.pushPose();
			rotateCenteredY(ms, frogState.yaw);
			transformAtHeadPivot(ms, frogState.tonguePitch, frogState.tongueScale);
			submitPart(CreateStandaloneModels.FROGPORT_TONGUE, ms, collector, state.lightCoords);
			ms.popPose();
		}

		submitPackage(frogState, ms, collector, state.lightCoords);
	}

	private static void submitPackage(FrogportRenderState state, PoseStack ms, SubmitNodeCollector collector,
		int light) {
		if (state.packageStack.isEmpty() || state.packageScale <= 0)
			return;
		ms.pushPose();
		ms.translate(.5, 3 / 16f + .5, .5);
		ms.translate(state.packageOffset.x, state.packageOffset.y, state.packageOffset.z);
		ms.scale(state.packageScale, state.packageScale, state.packageScale);
		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, state.packageStack, ItemDisplayContext.FIXED,
				state.blockEntity.getLevel(), null, 0);
		itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	private static void transformAtHeadPivot(PoseStack ms, float pitch, float tongueScale) {
		ms.translate(8 / 16f, 10 / 16f, 11 / 16f);
		ms.mulPose(Axis.XP.rotationDegrees(pitch));
		ms.scale(1, 1, tongueScale);
		ms.translate(-8 / 16f, -10 / 16f, -11 / 16f);
	}

	private static void rotateCenteredY(PoseStack ms, float yaw) {
		ms.translate(.5, .5, .5);
		ms.mulPose(Axis.YP.rotationDegrees(yaw));
		ms.translate(-.5, -.5, -.5);
	}

	private static void submitPart(StandaloneModelKey<BlockStateModelPart> key, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		if (part != null)
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
				BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}

	private static class FrogportRenderState extends BlockEntityRenderState {
		private FrogportBlockEntity blockEntity;
		private float partialTicks;
		private float yaw;
		private float headPitch;
		private float tonguePitch;
		private float tongueScale;
		private boolean goggles;
		private ItemStack packageStack = ItemStack.EMPTY;
		private Vec3 packageOffset = Vec3.ZERO;
		private float packageScale;
	}
}
