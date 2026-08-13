package com.simibubi.create.content.equipment.goggles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.model.CreateBakedModelWrapper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;

public class GogglesModel extends CreateBakedModelWrapper<BakedModel> {

	public GogglesModel(BakedModel template) {
		super(template);
	}

	public BakedModel applyTransform(ItemDisplayContext cameraItemDisplayContext, PoseStack mat, boolean leftHanded) {
		return super.applyTransform(cameraItemDisplayContext, mat, leftHanded);
	}

}
