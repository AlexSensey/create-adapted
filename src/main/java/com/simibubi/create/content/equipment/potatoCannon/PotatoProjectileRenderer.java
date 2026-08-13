package com.simibubi.create.content.equipment.potatoCannon;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PotatoProjectileRenderer
	extends EntityRenderer<PotatoProjectileEntity, PotatoProjectileRenderer.PotatoProjectileRenderState> {

	public PotatoProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void extractRenderState(PotatoProjectileEntity entity, PotatoProjectileRenderState state,
		float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.entity = entity;
		state.item = entity.getItem().copy();
		state.partialTicks = partialTicks;
		state.height = entity.getBoundingBox().getYsize();
	}

	@Override
	public void submit(PotatoProjectileRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		PotatoProjectileEntity entity = state.entity;
		if (entity == null || state.item.isEmpty())
			return;

		ms.pushPose();
		ms.translate(0, state.height / 2 - 1 / 8f, 0);
		entity.getRenderMode()
			.transform(ms, entity, state.partialTicks);

		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, state.item, ItemDisplayContext.GROUND, entity.level(), entity, entity.getId());
		itemState.submit(ms, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	public Identifier getTextureLocation(PotatoProjectileEntity entity) {
		return null;
	}

	@Override
	public PotatoProjectileRenderState createRenderState() {
		return new PotatoProjectileRenderState();
	}

	public static class PotatoProjectileRenderState extends EntityRenderState {
		private PotatoProjectileEntity entity;
		private ItemStack item = ItemStack.EMPTY;
		private float partialTicks;
		private double height;
	}
}
