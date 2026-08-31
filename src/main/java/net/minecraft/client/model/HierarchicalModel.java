package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public abstract class HierarchicalModel<T extends EntityRenderState> extends EntityModel<T> {
	protected HierarchicalModel(ModelPart root) {
		super(root);
	}
}
