package com.simibubi.create.content.trains.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class CarriageContraptionEntityRenderer extends ContraptionEntityRenderer<CarriageContraptionEntity> {

	public CarriageContraptionEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(CarriageContraptionEntity entity, Frustum clippingHelper, double cameraX,
		double cameraY, double cameraZ) {
		Carriage carriage = entity.getCarriage();
		if (carriage != null)
			for (CarriageBogey bogey : carriage.bogeys)
				if (bogey != null)
					bogey.couplingAnchors.replace(v -> null);
		return super.shouldRender(entity, clippingHelper, cameraX, cameraY, cameraZ);
	}

	@Override
	public void render(CarriageContraptionEntity entity, float yaw, float partialTicks, PoseStack ms,
		MultiBufferSource buffers, int overlay) {
		if (!entity.validForRender || entity.firstPositionUpdate)
			return;

		super.render(entity, yaw, partialTicks, ms, buffers, overlay);

		Carriage carriage = entity.getCarriage();
		if (carriage == null)
			return;

		Vec3 position = entity.getPosition(partialTicks);

		float viewYRot = entity.getViewYRot(partialTicks);
		float viewXRot = entity.getViewXRot(partialTicks);
		int bogeySpacing = carriage.bogeySpacing;

		carriage.bogeys.forEach(bogey -> {
			if (bogey == null)
				return;

			BlockPos bogeyPos = bogey.isLeading ? BlockPos.ZERO
				: BlockPos.ZERO.relative(entity.getInitialOrientation()
					.getCounterClockWise(), bogeySpacing);

			if (!supportsVisualization(entity.level()) && !entity.getContraption()
				.isHiddenInPortal(bogeyPos)) {

				ms.pushPose();
				translateBogey(ms, bogey, bogeySpacing, viewYRot, viewXRot, partialTicks);

				int light = getBogeyLightCoords(entity, bogey, partialTicks);

				bogey.getStyle().render(bogey.getSize(), partialTicks, ms, buffers, light,
					overlay, bogey.wheelAngle.getValue(partialTicks), bogey.bogeyData, true);

				ms.popPose();
			}

			bogey.updateCouplingAnchor(position, viewXRot, viewYRot, bogeySpacing, partialTicks, bogey.isLeading);
			if (!carriage.isOnTwoBogeys())
				bogey.updateCouplingAnchor(position, viewXRot, viewYRot, bogeySpacing, partialTicks, !bogey.isLeading);
		});
	}

	@Override
	public void submit(ContraptionRenderState<CarriageContraptionEntity> state, PoseStack ms,
		SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		CarriageContraptionEntity entity = state.getEntity();
		if (entity == null || !entity.validForRender || entity.firstPositionUpdate)
			return;

		super.submit(state, ms, collector, cameraRenderState);

		Carriage carriage = entity.getCarriage();
		if (carriage == null)
			return;

		float partialTicks = state.getPartialTicks();
		Vec3 position = entity.getPosition(partialTicks);
		float viewYRot = entity.getViewYRot(partialTicks);
		float viewXRot = entity.getViewXRot(partialTicks);
		int bogeySpacing = carriage.bogeySpacing;

		carriage.bogeys.forEach(bogey -> {
			if (bogey == null)
				return;

			BlockPos bogeyPos = bogey.isLeading ? BlockPos.ZERO
				: BlockPos.ZERO.relative(entity.getInitialOrientation()
					.getCounterClockWise(), bogeySpacing);

			if (!entity.getContraption()
				.isHiddenInPortal(bogeyPos)) {
				ms.pushPose();
				translateBogey(ms, bogey, bogeySpacing, viewYRot, viewXRot, partialTicks);
				int light = getBogeyLightCoords(entity, bogey, partialTicks);
				bogey.getStyle().submit(bogey.getSize(), partialTicks, ms, collector, light, 0,
					bogey.wheelAngle.getValue(partialTicks), bogey.bogeyData, true);
				ms.popPose();
			}

			bogey.updateCouplingAnchor(position, viewXRot, viewYRot, bogeySpacing, partialTicks, bogey.isLeading);
			if (!carriage.isOnTwoBogeys())
				bogey.updateCouplingAnchor(position, viewXRot, viewYRot, bogeySpacing, partialTicks, !bogey.isLeading);
		});
	}

	public static void translateBogey(PoseStack ms, CarriageBogey bogey, int bogeySpacing, float viewYRot,
		float viewXRot, float partialTicks) {
		boolean selfUpsideDown = bogey.isUpsideDown();
		boolean leadingUpsideDown = bogey.carriage.leadingBogey().isUpsideDown();
		ms.mulPose(Axis.YP.rotationDegrees(viewYRot + 90));
		ms.mulPose(Axis.XP.rotationDegrees(-viewXRot));
		ms.mulPose(Axis.YP.rotationDegrees(180));
		ms.translate(0, 0, bogey.isLeading ? 0 : -bogeySpacing);
		ms.mulPose(Axis.YP.rotationDegrees(-180));
		ms.mulPose(Axis.XP.rotationDegrees(viewXRot));
		ms.mulPose(Axis.YP.rotationDegrees(-viewYRot - 90));
		ms.mulPose(Axis.YP.rotationDegrees(bogey.yaw.getValue(partialTicks)));
		ms.mulPose(Axis.XP.rotationDegrees(bogey.pitch.getValue(partialTicks)));
		ms.translate(0, .5f, 0);
		if (selfUpsideDown)
			ms.mulPose(Axis.ZP.rotationDegrees(180));
		if (selfUpsideDown != leadingUpsideDown)
			ms.translate(0, 2, 0);
	}

	public static int getBogeyLightCoords(CarriageContraptionEntity entity, CarriageBogey bogey, float partialTicks) {
		var anchorPosition = bogey.getAnchorPosition();

		var lightPos = BlockPos.containing(anchorPosition == null ? entity.getLightProbePosition(partialTicks) : anchorPosition);

		return LightCoordsUtil.pack(entity.level().getBrightness(LightLayer.BLOCK, lightPos),
			entity.level().getBrightness(LightLayer.SKY, lightPos));
	}

	private static boolean supportsVisualization(Object level) {
		try {
			Class<?> manager = Class.forName("dev.engine_room.flywheel.api.visualization.VisualizationManager");
			for (java.lang.reflect.Method method : manager.getMethods()) {
				if (!method.getName().equals("supportsVisualization") || method.getParameterCount() != 1)
					continue;
				Object result = method.invoke(null, level);
				return result instanceof Boolean supported && supported;
			}
		} catch (ReflectiveOperationException | LinkageError ignored) {
		}
		return false;
	}

}
