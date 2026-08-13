package com.simibubi.create.content.contraptions.gantry;

import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GantryContraptionEntityRenderer extends ContraptionEntityRenderer<GantryContraptionEntity> {

	public GantryContraptionEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(GantryContraptionEntity entity, Frustum frustum, double cameraX, double cameraY,
		double cameraZ) {
		return entity.getContraption() != null && entity.isAliveOrStale() && entity.isReadyForRender();
	}
}
