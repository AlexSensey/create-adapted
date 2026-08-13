package com.simibubi.create.content.equipment.hats;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.trains.schedule.hat.TrainHatInfo;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class CreateHatArmorLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
	extends RenderLayer<S, M> {

	public CreateHatArmorLayer(RenderLayerParent<S, M> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack ms, SubmitNodeCollector collector, int light, S renderState,
		float yRot, float xRot) {
		if (!(renderState instanceof CreateHatRenderState hatState) || hatState.create$getHatType() == 0)
			return;

		TrainHatInfo info = hatState.create$getHatInfo();
		if (info == null)
			return;
		List<ModelPart> partsToHead = TrainHatInfo.getAdjustedPart(info, getParentModel().root(), "head");
		if (partsToHead.isEmpty())
			return;

		BlockStateModelPart hat = Minecraft.getInstance().getModelManager().getStandaloneModel(
			hatState.create$getHatType() == 1 ? CreateStandaloneModels.TRAIN_HAT : CreateStandaloneModels.LOGISTICS_HAT);
		if (hat == null)
			return;

		ms.pushPose();
		partsToHead.forEach(part -> part.translateAndRotate(ms));
		ModelPart lastChild = partsToHead.get(partsToHead.size() - 1);
		if (!lastChild.isEmpty() && !lastChild.cubes.isEmpty()) {
			Cube cube = lastChild.cubes.get(Mth.clamp(info.cubeIndex(), 0, lastChild.cubes.size() - 1));
			ms.translate(info.offset().x() / 16f, (cube.minY - cube.maxY + info.offset().y()) / 16f,
				info.offset().z() / 16f);
			float scale = Math.max(cube.maxX - cube.minX, cube.maxZ - cube.minZ) / 8f * info.scale();
			ms.scale(scale, scale, scale);
		}

		ms.scale(1, -1, -1);
		ms.translate(0, -2.25f / 16f, 0);
		ms.mulPose(Axis.XP.rotationDegrees(-8.5f));
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(hat),
			BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	public static void register(EntityRenderersEvent.AddLayers event) {
		for (var skin : event.getSkins())
			registerOn(event.getPlayerRenderer(skin));
		for (var type : event.getEntityTypes())
			registerOn(event.getRenderer(type));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void registerOn(EntityRenderer<?, ?> renderer) {
		if (renderer instanceof LivingEntityRenderer livingRenderer)
			livingRenderer.addLayer(new CreateHatArmorLayer(livingRenderer));
	}
}
