package com.simibubi.create.content.trains.entity;

import java.util.Collection;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class CarriageCouplingRenderer {

	public static void renderAll(PoseStack ms, MultiBufferSource buffer, Vec3 camera) {
	}

	public static void submitAll(PoseStack ms, SubmitNodeCollector collector, Vec3 camera) {
		Level level = Minecraft.getInstance().level;
		if (level == null)
			return;

		BlockStateModelPart head = model(CreateStandaloneModels.TRAIN_COUPLING_HEAD);
		BlockStateModelPart cable = model(CreateStandaloneModels.TRAIN_COUPLING_CABLE);
		if (head == null || cable == null)
			return;

		Collection<Train> trains = CreateClient.RAILWAYS.trains.values();
		float partialTicks = AnimationTickHolder.getPartialTicks();
		for (Train train : trains) {
			List<Carriage> carriages = train.carriages;
			for (int i = 0; i < carriages.size() - 1; i++) {
				Carriage carriage = carriages.get(i);
				Carriage carriage2 = carriages.get(i + 1);
				Carriage.DimensionalCarriageEntity dimensional = carriage.getDimensionalIfPresent(level.dimension());
				Carriage.DimensionalCarriageEntity dimensional2 = carriage2.getDimensionalIfPresent(level.dimension());
				if (dimensional == null || dimensional2 == null)
					continue;

				CarriageContraptionEntity entity = dimensional.entity.get();
				CarriageContraptionEntity entity2 = dimensional2.entity.get();
				if (entity == null || entity2 == null)
					continue;

				CarriageBogey bogey1 = carriage.trailingBogey();
				CarriageBogey bogey2 = carriage2.leadingBogey();
				Vec3 anchor = bogey1.couplingAnchors.getSecond();
				Vec3 anchor2 = bogey2.couplingAnchors.getFirst();
				if (anchor == null || anchor2 == null || !anchor.closerThan(camera, 64))
					continue;

				Vec3 difference = anchor2.subtract(anchor);
				if (difference.lengthSqr() < 1e-6)
					continue;

				float yRot = (float) (Mth.atan2(difference.z, difference.x) * Mth.RAD_TO_DEG) + 90;
				float xRot = (float) (Math.atan2(difference.y,
					Math.sqrt(difference.x * difference.x + difference.z * difference.z)) * Mth.RAD_TO_DEG);
				int light = getPackedLightCoords(entity, partialTicks);
				int light2 = getPackedLightCoords(entity2, partialTicks);

				submitHead(ms, collector, head, anchor.subtract(camera), -yRot, xRot, light);

				float margin = 3 / 16f;
				double couplingDistance = train.carriageSpacing.get(i) - 2 * margin
					- bogey1.type.getConnectorAnchorOffset(bogey1.isUpsideDown()).z
					- bogey2.type.getConnectorAnchorOffset(bogey2.isUpsideDown()).z;
				int couplingSegments = Math.max(1, (int) Math.round(couplingDistance * 4));
				float stretch = (float) (((anchor2.distanceTo(anchor) - 2 * margin) * 4) / couplingSegments);
				for (int segment = 0; segment < couplingSegments; segment++)
					submitCable(ms, collector, cable, anchor.subtract(camera), -yRot + 180, -xRot,
						margin, stretch, segment, light);

				submitHead(ms, collector, head, anchor2.subtract(camera), -yRot + 180, -xRot, light2);
			}
		}
	}

	private static void submitHead(PoseStack ms, SubmitNodeCollector collector, BlockStateModelPart model,
		Vec3 position, float yRot, float xRot, int light) {
		ms.pushPose();
		ms.translate(position.x, position.y, position.z);
		ms.mulPose(Axis.YP.rotationDegrees(yRot));
		ms.mulPose(Axis.XP.rotationDegrees(xRot));
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(model),
			BlockModelRenderState.EMPTY_TINTS, light, 0, 0);
		ms.popPose();
	}

	private static void submitCable(PoseStack ms, SubmitNodeCollector collector, BlockStateModelPart model,
		Vec3 position, float yRot, float xRot, float margin, float stretch, int segment, int light) {
		ms.pushPose();
		ms.translate(position.x, position.y, position.z);
		ms.mulPose(Axis.YP.rotationDegrees(yRot));
		ms.mulPose(Axis.XP.rotationDegrees(xRot));
		ms.translate(0, 0, margin + 2 / 16f);
		ms.scale(1, 1, stretch);
		ms.translate(0, 0, segment / 4f);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(model),
			BlockModelRenderState.EMPTY_TINTS, light, 0, 0);
		ms.popPose();
	}

	private static int getPackedLightCoords(Entity entity, float partialTicks) {
		return LightCoordsUtil.getLightCoords(entity.level(), BlockPos.containing(entity.getLightProbePosition(partialTicks)));
	}

	private static BlockStateModelPart model(StandaloneModelKey<BlockStateModelPart> key) {
		return Minecraft.getInstance().getModelManager().getStandaloneModel(key);
	}
}
