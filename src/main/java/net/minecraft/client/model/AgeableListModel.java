package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public abstract class AgeableListModel<T extends EntityRenderState> extends EntityModel<T> {
	public boolean young;
	public boolean scaleHead;
	public float babyHeadScale = 1;
	public float babyYHeadOffset;
	public float babyZHeadOffset;

	protected AgeableListModel(ModelPart root) {
		super(root);
	}

	protected Iterable<ModelPart> headParts() {
		return java.util.List.of();
	}

	protected Iterable<ModelPart> bodyParts() {
		return java.util.List.of();
	}
}
