package com.simibubi.create.content.contraptions.actors.seat;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class SeatEntityRenderer extends EntityRenderer<SeatEntity, EntityRenderState> {
	public SeatEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(SeatEntity entity, Frustum frustum, double x, double y, double z) {
		return false;
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}
}
