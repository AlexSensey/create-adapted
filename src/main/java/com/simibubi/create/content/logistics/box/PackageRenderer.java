package com.simibubi.create.content.logistics.box;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PackageRenderer extends EntityRenderer<PackageEntity, PackageRenderer.PackageRenderState> {

	public PackageRenderer(Context pContext) {
		super(pContext);
		shadowRadius = 0.5f;
	}

	public void render(PackageEntity entity, float yaw, float pt, PoseStack ms, MultiBufferSource buffer, int light) {}

	public static void renderBox(Entity entity, float yaw, PoseStack ms, MultiBufferSource buffer, int light,
		PartialModel model) {}

	@Override
	public void extractRenderState(PackageEntity entity, PackageRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		ItemStack box = entity.box;
		state.box = box.isEmpty() || !PackageItem.isPackage(box) ? ItemStack.EMPTY : box.copy();
		state.entity = entity;
		state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
	}

	@Override
	public void submit(PackageRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (state.box.isEmpty() || state.entity == null)
			return;

		ms.pushPose();
		ms.mulPose(Axis.YP.rotationDegrees(-state.yaw - 90));
		ms.translate(0, .5, 0);
		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, state.box, ItemDisplayContext.FIXED, state.entity.level(), state.entity,
				state.entity.getId());
		itemState.submit(ms, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	public Identifier getTextureLocation(PackageEntity pEntity) {
		return null;
	}

	@Override
	public PackageRenderState createRenderState() {
		return new PackageRenderState();
	}

	public static class PackageRenderState extends EntityRenderState {
		private PackageEntity entity;
		private ItemStack box = ItemStack.EMPTY;
		private float yaw;
	}
}
