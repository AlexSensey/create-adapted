package com.simibubi.create.content.equipment.symmetryWand;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

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
import net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SymmetryWandItemRenderer implements ItemModel {
	private final ItemModel base;
	private final ItemModel bits;
	private final ItemModel core;
	private final ItemModel coreGlow;

	public SymmetryWandItemRenderer(ItemModel base, ItemModel bits, ItemModel core, ItemModel coreGlow) {
		this.base = base;
		this.bits = bits;
		this.core = core;
		this.coreGlow = coreGlow;
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
		ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
		base.update(state, stack, resolver, displayContext, level, owner, seed);
		renderLit(core, new Matrix4f(), state, stack, resolver, displayContext, level, owner, seed);
		renderLit(coreGlow, new Matrix4f(), state, stack, resolver, displayContext, level, owner, seed);

		float worldTime = AnimationTickHolder.getRenderTime() / 20;
		float floating = Mth.sin(worldTime) * .05f;
		float angle = worldTime * -10 % 360;
		state.setAnimated();
		state.appendModelIdentityElement((int) (worldTime * 20));
		Matrix4f transform = new Matrix4f()
			.translate(.5f, .5f + floating, .5f)
			.rotateY((float) Math.toRadians(angle))
			.translate(-.5f, -.5f, -.5f);
		renderLit(bits, transform, state, stack, resolver, displayContext, level, owner, seed);
	}

	private static void renderLit(ItemModel model, Matrix4f transform, ItemStackRenderState state, ItemStack stack,
		ItemModelResolver resolver, ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
		if (model != null)
			model.update(new TransformedState(state, transform, true), stack, resolver, displayContext, level, owner, seed);
	}

	private static class TransformedState extends ItemStackRenderState {
		private final ItemStackRenderState delegate;
		private final Matrix4fc transform;
		private final boolean fullBright;

		private TransformedState(ItemStackRenderState delegate, Matrix4fc transform, boolean fullBright) {
			this.delegate = delegate;
			this.transform = transform;
			this.fullBright = fullBright;
		}

		@Override public LayerRenderState newLayer() {
			return new TransformedLayer(delegate, delegate.newLayer(), transform, fullBright);
		}
		@Override public void setAnimated() { delegate.setAnimated(); }
		@Override public void appendModelIdentityElement(Object object) { delegate.appendModelIdentityElement(object); }
		@Override public void setOversizedInGui(boolean oversized) { delegate.setOversizedInGui(oversized); }
	}

	private static class TransformedLayer extends LayerRenderState {
		private final LayerRenderState delegate;
		private final Matrix4fc transform;
		private final boolean fullBright;

		private TransformedLayer(ItemStackRenderState owner, LayerRenderState delegate, Matrix4fc transform,
			boolean fullBright) {
			owner.super();
			this.delegate = delegate;
			this.transform = transform;
			this.fullBright = fullBright;
		}

		@Override public List<BakedQuad> prepareQuadList() {
			List<BakedQuad> quads = delegate.prepareQuadList();
			if (!fullBright)
				return quads;
			return new AbstractList<>() {
				@Override public BakedQuad get(int index) { return quads.get(index); }
				@Override public int size() { return quads.size(); }
				@Override public void add(int index, BakedQuad quad) { quads.add(index, light(quad)); }
				@Override public BakedQuad set(int index, BakedQuad quad) { return quads.set(index, light(quad)); }
				@Override public BakedQuad remove(int index) { return quads.remove(index); }
				@Override public boolean addAll(Collection<? extends BakedQuad> elements) {
					boolean changed = false;
					for (BakedQuad quad : elements)
						changed |= quads.add(light(quad));
					return changed;
				}
				@Override public boolean addAll(int index, Collection<? extends BakedQuad> elements) {
					if (elements.isEmpty())
						return false;
					int insertionIndex = index;
					for (BakedQuad quad : elements)
						quads.add(insertionIndex++, light(quad));
					return true;
				}
			};
		}

		private static BakedQuad light(BakedQuad quad) {
			MaterialInfo material = quad.materialInfo();
			MaterialInfo fullBrightMaterial = new MaterialInfo(material.sprite(), material.layer(),
				material.itemRenderType(), material.tintIndex(), material.shade(), 15);
			return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
				quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
				fullBrightMaterial);
		}
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
