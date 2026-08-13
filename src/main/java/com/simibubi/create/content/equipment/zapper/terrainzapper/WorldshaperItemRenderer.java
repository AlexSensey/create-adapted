package com.simibubi.create.content.equipment.zapper.terrainzapper;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.CreateClient;

import it.unimi.dsi.fastutil.ints.IntList;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/** 26.2 item-model replacement for the former BEWLR worldshaper renderer. */
public class WorldshaperItemRenderer implements ItemModel {
	public static final IItemDecorator DECORATOR = WorldshaperItemRenderer::renderSelectedBlock;

	private final ItemModel base;
	private final ItemModel core;
	private final ItemModel coreGlow;
	private final ItemModel accelerator;

	public WorldshaperItemRenderer(ItemModel base, ItemModel core, ItemModel coreGlow, ItemModel accelerator) {
		this.base = base;
		this.core = core;
		this.coreGlow = coreGlow;
		this.accelerator = accelerator;
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
		ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
		float partialTicks = AnimationTickHolder.getPartialTicks();
		float renderTime = AnimationTickHolder.getRenderTime();
		float animation = getAnimation(stack, displayContext, partialTicks);

		Matrix4f root = new Matrix4f();
		if (displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
			|| displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
			float flip = displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 1 : -1;
			root.translate(flip * -.1f, .1f, -.4f + animation * .07f)
				.rotateY((float) Math.toRadians(flip * 5))
				.rotateX((float) Math.toRadians(animation * 16));
		}

		render(base, root, state, stack, resolver, displayContext, level, owner, seed);
		boolean held = isHeld(stack, displayContext);
		boolean animateInventoryLamp = displayContext == ItemDisplayContext.GUI && !held;
		// Smooth dark -> white -> dark cycle in inventory only. The old clamped sine
		// spent half of every cycle completely dark, which looked like a stutter.
		float lampCycle = animateInventoryLamp
			? .5f - .5f * Mth.cos(renderTime * Mth.PI / 20f)
			: 0;
		int emission = 4 + Mth.floor(lampCycle * 11);
		int lampShade = 68 + Mth.floor(lampCycle * 187);
		int lampTint = 0xff000000 | lampShade << 16 | lampShade << 8 | lampShade;
		renderLit(core, root, emission, lampTint, state, stack, resolver, displayContext, level, owner, seed + 1);
		renderLit(coreGlow, root, emission, lampTint, state, stack, resolver, displayContext, level, owner, seed + 2);

		float angle = renderTime * -1.25f + animation * 360f;
		Matrix4f acceleratorTransform = new Matrix4f(root)
			.translate(.5f, .5f - .155f, .5f)
			.rotateZ((float) Math.toRadians(angle))
			.translate(-.5f, -.5f + .155f, -.5f);
		render(accelerator, acceleratorTransform, state, stack, resolver, displayContext, level, owner, seed + 3);

		state.setAnimated();
		state.appendModelIdentityElement((int) (renderTime * 4));
	}

	private static boolean renderSelectedBlock(GuiGraphicsExtractor graphics, Font font, ItemStack stack,
		int xOffset, int yOffset) {
		if (!stack.has(AllDataComponents.SHAPER_BLOCK_USED))
			return false;
		ItemStack selected = new ItemStack(stack.get(AllDataComponents.SHAPER_BLOCK_USED).getBlock());
		if (selected.isEmpty())
			return false;
		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(xOffset, yOffset + 8);
		pose.scale(.5f, .5f);
		graphics.nextStratum();
		graphics.item(selected, 0, 0);
		pose.popMatrix();
		return false;
	}

	private static boolean isHeld(ItemStack stack, ItemDisplayContext displayContext) {
		if (displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
			|| displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
			|| displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
			|| displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
			return true;
		LocalPlayer player = Minecraft.getInstance().player;
		// Match the old renderer: only the actual stack instance in a hand is held.
		// Component equality incorrectly disabled animation for matching inventory copies.
		return player != null && (stack == player.getMainHandItem() || stack == player.getOffhandItem());
	}

	private static float getAnimation(ItemStack stack, ItemDisplayContext displayContext, float partialTicks) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return 0;
		if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
			return Mth.clamp(CreateClient.ZAPPER_RENDER_HANDLER.getAnimation(true, partialTicks) * 5, 0, 1);
		if (displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
			return Mth.clamp(CreateClient.ZAPPER_RENDER_HANDLER.getAnimation(false, partialTicks) * 5, 0, 1);
		boolean mainHand = player.getMainHandItem() == stack;
		boolean offHand = player.getOffhandItem() == stack;
		if (!mainHand && !offHand)
			return 0;
		boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
		float value = CreateClient.ZAPPER_RENDER_HANDLER.getAnimation(mainHand ^ leftHanded, partialTicks);
		return Mth.clamp(value * 5, 0, 1);
	}

	private static void render(ItemModel model, Matrix4f transform, ItemStackRenderState state, ItemStack stack,
		ItemModelResolver resolver, ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
		if (model != null)
			model.update(new TransformedState(state, transform), stack, resolver, displayContext, level, owner, seed);
	}

	private static void renderLit(ItemModel model, Matrix4f transform, int emission, int tint, ItemStackRenderState state,
		ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, ClientLevel level,
		ItemOwner owner, int seed) {
		if (model != null)
			model.update(new TransformedState(state, transform, emission, tint), stack, resolver, displayContext, level, owner, seed);
	}

	private static class TransformedState extends ItemStackRenderState {
		private final ItemStackRenderState delegate;
		private final Matrix4fc transform;
		private final int emission;
		private final int tint;

		private TransformedState(ItemStackRenderState delegate, Matrix4fc transform) {
			this(delegate, transform, -1, 0);
		}

		private TransformedState(ItemStackRenderState delegate, Matrix4fc transform, int emission, int tint) {
			this.delegate = delegate;
			this.transform = transform;
			this.emission = emission;
			this.tint = tint;
		}

		@Override public LayerRenderState newLayer() {
			return new TransformedLayer(delegate, delegate.newLayer(), transform, emission, tint);
		}
		@Override public void setAnimated() { delegate.setAnimated(); }
		@Override public void appendModelIdentityElement(Object object) { delegate.appendModelIdentityElement(object); }
		@Override public void setOversizedInGui(boolean oversized) { delegate.setOversizedInGui(oversized); }
	}

	private static class TransformedLayer extends LayerRenderState {
		private final LayerRenderState delegate;
		private final Matrix4fc transform;
		private final int emission;
		private final int tint;
		private int tintIndex = -1;

		private TransformedLayer(ItemStackRenderState owner, LayerRenderState delegate, Matrix4fc transform, int emission,
			int tint) {
			owner.super();
			this.delegate = delegate;
			this.transform = transform;
			this.emission = emission;
			this.tint = tint;
		}

		@Override public List<BakedQuad> prepareQuadList() {
			List<BakedQuad> quads = delegate.prepareQuadList();
			if (emission < 0)
				return quads;
			// CuboidItemModelWrapper fills this list after prepareQuadList() returns.
			// Keep forwarding to its mutable accumulator and light every quad as it is added.
			return new AbstractList<>() {
				@Override public BakedQuad get(int index) { return quads.get(index); }
				@Override public int size() { return quads.size(); }
				@Override public void add(int index, BakedQuad element) { quads.add(index, withEmission(element)); }
				@Override public BakedQuad set(int index, BakedQuad element) {
					return quads.set(index, withEmission(element));
				}
				@Override public BakedQuad remove(int index) { return quads.remove(index); }
				@Override public boolean addAll(Collection<? extends BakedQuad> elements) {
					boolean changed = false;
					for (BakedQuad element : elements)
						changed |= quads.add(withEmission(element));
					return changed;
				}
				@Override public boolean addAll(int index, Collection<? extends BakedQuad> elements) {
					if (elements.isEmpty())
						return false;
					int insertionIndex = index;
					for (BakedQuad element : elements)
						quads.add(insertionIndex++, withEmission(element));
					return true;
				}
			};
		}

		private BakedQuad withEmission(BakedQuad quad) {
				MaterialInfo material = quad.materialInfo();
				if (tintIndex < 0) {
					IntList tints = delegate.tintLayers();
					tintIndex = tints.size();
					tints.add(tint);
				}
				MaterialInfo lit = new MaterialInfo(material.sprite(), material.layer(), material.itemRenderType(),
					tintIndex, material.shade(), emission);
				return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
					quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), lit);
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
