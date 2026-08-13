package com.simibubi.create.content.equipment.wrench;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueHandler;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;
import java.util.function.Supplier;

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
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class WrenchItemRenderer implements ItemModel {

	private final ItemModel baseModel;
	private final ItemModel gearModel;

	public WrenchItemRenderer(ItemModel baseModel, ItemModel gearModel) {
		this.baseModel = baseModel;
		this.gearModel = gearModel;
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
		ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
		baseModel.update(state, stack, resolver, displayContext, level, owner, seed);
		if (gearModel == null)
			return;

		float angle = ScrollValueHandler.getScroll(AnimationTickHolder.getPartialTicks());
		state.setAnimated();
		state.appendModelIdentityElement((int) angle);
		gearModel.update(new RotatingGearState(state, gearTransform(angle)), stack, resolver, displayContext, level, owner, seed);
	}

	private static Matrix4f gearTransform(float angle) {
		return new Matrix4f()
			.translate(9 / 16f, 7.5f / 16f, 8 / 16f)
			.rotateY((float) Math.toRadians(angle))
			.translate(-9 / 16f, -7.5f / 16f, -8 / 16f);
	}

	private static class RotatingGearState extends ItemStackRenderState {
		private final ItemStackRenderState delegate;
		private final Matrix4fc transform;

		private RotatingGearState(ItemStackRenderState delegate, Matrix4fc transform) {
			this.delegate = delegate;
			this.transform = transform;
		}

		@Override
		public LayerRenderState newLayer() {
			return new RotatingGearLayer(delegate, delegate.newLayer(), transform);
		}

		@Override
		public void setAnimated() {
			delegate.setAnimated();
		}

		@Override
		public void appendModelIdentityElement(Object object) {
			delegate.appendModelIdentityElement(object);
		}

		@Override
		public void setOversizedInGui(boolean oversized) {
			delegate.setOversizedInGui(oversized);
		}
	}

	private static class RotatingGearLayer extends LayerRenderState {
		private final LayerRenderState delegate;
		private final Matrix4fc transform;

		private RotatingGearLayer(ItemStackRenderState owner, LayerRenderState delegate, Matrix4fc transform) {
			owner.super();
			this.delegate = delegate;
			this.transform = transform;
		}

		@Override
		public List<BakedQuad> prepareQuadList() {
			return delegate.prepareQuadList();
		}

		@Override
		public void setUsesBlockLight(boolean usesBlockLight) {
			delegate.setUsesBlockLight(usesBlockLight);
		}

		@Override
		public void setExtents(Supplier<Vector3fc[]> extents) {
			delegate.setExtents(extents);
		}

		@Override
		public void setParticleMaterial(Material.Baked particleMaterial) {
			delegate.setParticleMaterial(particleMaterial);
		}

		@Override
		public void setItemTransform(ItemTransform itemTransform) {
			delegate.setItemTransform(itemTransform);
		}

		@Override
		public void setLocalTransform(Matrix4fc localTransform) {
			delegate.setLocalTransform(new Matrix4f(transform).mul(localTransform));
		}

		@Override
		public <T> void setupSpecialModel(SpecialModelRenderer<T> renderer, T value) {
			delegate.setupSpecialModel(renderer, value);
		}

		@Override
		public void setFoilType(FoilType foilType) {
			delegate.setFoilType(foilType);
		}

		@Override
		public IntList tintLayers() {
			return delegate.tintLayers();
		}
	}

}
