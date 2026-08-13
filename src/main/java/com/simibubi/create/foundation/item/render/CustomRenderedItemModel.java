package com.simibubi.create.foundation.item.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.model.CreateBakedModelWrapper;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;

public class CustomRenderedItemModel extends CreateBakedModelWrapper<BakedModel> {

	public CustomRenderedItemModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	public boolean isCustomRenderer() {
		return true;
	}

	@Override
	public BakedModel applyTransform(ItemDisplayContext cameraItemDisplayContext, PoseStack mat,
		boolean leftHand) {
		// Super call returns originalModel, but we want to return this, else BEWLR
		// won't be used.
		super.applyTransform(cameraItemDisplayContext, mat, leftHand);
		return this;
	}

	public BakedModel getOriginalModel() {
		return originalModel;
	}

}
