package com.simibubi.create.content.equipment.sandPaper;

import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import com.simibubi.create.AllDataComponents;

import it.unimi.dsi.fastutil.ints.IntList;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 26.2 item-model replacement for the former BEWLR sandpaper renderer. */
public class SandPaperItemRenderer implements ItemModel {

	private final ItemModel baseModel;

	public SandPaperItemRenderer(ItemModel baseModel) {
		this.baseModel = baseModel;
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
		ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
		boolean leftHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
		boolean firstPerson = leftHand || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
		LivingEntity living = owner == null ? null : owner.asLivingEntity();
		boolean inUse = living != null && living.getUseItem() == stack && living.getUseItemRemainingTicks() > 0;

		Matrix4f baseTransform = new Matrix4f();
		if (firstPerson && inUse) {
			int modifier = leftHand ? -1 : 1;
			baseTransform.translate(modifier * .5f, 0, -.25f)
				.rotateZ((float) Math.toRadians(modifier * 40))
				.rotateX((float) Math.toRadians(modifier * 10))
				.rotateY((float) Math.toRadians(modifier * 90));
		}
		baseModel.update(new TransformedState(state, baseTransform), stack, resolver, displayContext, level, owner, seed);

		SandPaperItemComponent polishing = stack.get(AllDataComponents.SAND_PAPER_POLISHING);
		if (polishing == null || polishing.item().isEmpty())
			return;

		ItemStack polished = polishing.item();
		boolean jeiMode = stack.has(AllDataComponents.SAND_PAPER_JEI);
		float partialTicks = AnimationTickHolder.getPartialTicks();
		int duration = Math.max(1, stack.getUseDuration(living));
		float remaining = jeiMode ? (-AnimationTickHolder.getTicks()) % duration
			: living != null ? living.getUseItemRemainingTicks() : duration;
		float time = remaining - partialTicks + 1;
		float bobbing = time / duration < .8f
			? -Mth.abs(Mth.cos(time / 4f * Mth.PI) * .1f)
			: 0;

		Matrix4f polishedTransform = new Matrix4f();
		if (displayContext == ItemDisplayContext.GUI)
			polishedTransform.translate(bobbing, .2f + bobbing, 1).scale(.75f);
		else {
			int modifier = leftHand ? -1 : 1;
			polishedTransform.rotateY((float) Math.toRadians(modifier * 40)).translate(0, bobbing, 0);
		}

		state.setAnimated();
		state.appendModelIdentityElement((int) (time * 4));
		resolver.appendItemLayers(new TransformedState(state, polishedTransform), polished, ItemDisplayContext.GUI,
			level, owner, seed + 1);
	}

	private static class TransformedState extends ItemStackRenderState {
		private final ItemStackRenderState delegate;
		private final Matrix4fc transform;

		private TransformedState(ItemStackRenderState delegate, Matrix4fc transform) {
			this.delegate = delegate;
			this.transform = transform;
		}

		@Override public LayerRenderState newLayer() {
			return new TransformedLayer(delegate, delegate.newLayer(), transform);
		}
		@Override public void setAnimated() { delegate.setAnimated(); }
		@Override public void appendModelIdentityElement(Object object) { delegate.appendModelIdentityElement(object); }
		@Override public void setOversizedInGui(boolean oversized) { delegate.setOversizedInGui(oversized); }
	}

	private static class TransformedLayer extends LayerRenderState {
		private final LayerRenderState delegate;
		private final Matrix4fc transform;

		private TransformedLayer(ItemStackRenderState owner, LayerRenderState delegate, Matrix4fc transform) {
			owner.super();
			this.delegate = delegate;
			this.transform = transform;
		}

		@Override public List<BakedQuad> prepareQuadList() { return delegate.prepareQuadList(); }
		@Override public void setUsesBlockLight(boolean value) { delegate.setUsesBlockLight(value); }
		@Override public void setExtents(Supplier<Vector3fc[]> extents) { delegate.setExtents(extents); }
		@Override public void setParticleMaterial(Material.Baked material) { delegate.setParticleMaterial(material); }
		@Override public void setItemTransform(ItemTransform itemTransform) { delegate.setItemTransform(itemTransform); }
		@Override public void setLocalTransform(Matrix4fc localTransform) {
			delegate.setLocalTransform(new Matrix4f(transform).mul(localTransform));
		}
		@Override public <T> void setupSpecialModel(SpecialModelRenderer<T> renderer, T value) {
			delegate.setupSpecialModel(renderer, value);
		}
		@Override public void setFoilType(FoilType foilType) { delegate.setFoilType(foilType); }
		@Override public IntList tintLayers() { return delegate.tintLayers(); }
	}
}
