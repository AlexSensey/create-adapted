package com.simibubi.create.content.equipment.potatoCannon;

import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import com.simibubi.create.CreateClient;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem.Ammo;

import it.unimi.dsi.fastutil.ints.IntList;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
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
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

public class PotatoCannonItemRenderer implements ItemModel {
	public static final IItemDecorator DECORATOR = PotatoCannonItemRenderer::renderAmmo;

	private final ItemModel baseModel;
	private final ItemModel cogModel;

	public PotatoCannonItemRenderer(ItemModel baseModel, ItemModel cogModel) {
		this.baseModel = baseModel;
		this.cogModel = cogModel;
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
		ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
		float partialTicks = AnimationTickHolder.getPartialTicks();
		float angle = AnimationTickHolder.getRenderTime() * -2.5f;
		float recoil = 0;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			boolean inMainHand = player.getMainHandItem() == stack;
			boolean inOffHand = player.getOffhandItem() == stack;
			if (inMainHand || inOffHand) {
				boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
				float speed = CreateClient.POTATO_CANNON_RENDER_HANDLER
					.getAnimation(inMainHand ^ leftHanded, partialTicks);
				recoil = speed;
				angle += 360 * Mth.clamp(speed * 5, 0, 1);
			}
		}
		angle %= 360;
		Matrix4f rootTransform = new Matrix4f();
		if (displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
			|| displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
			// 26.2 applies this transform in model space, unlike the old outer PoseStack.
			// Move the intact model up and away instead of scaling it around the lower corner.
			rootTransform.translate(0, 0, recoil * .07f)
				.rotateX((float) Math.toRadians(recoil * 80));
		}

		ItemTransform[] sharedItemTransform = new ItemTransform[1];
		baseModel.update(new TransformedState(state, rootTransform, sharedItemTransform, false), stack, resolver,
			displayContext, level, owner, seed);
		if (cogModel == null)
			return;

		state.setAnimated();
		state.appendModelIdentityElement((int) angle);
		cogModel.update(new TransformedState(state, new Matrix4f(rootTransform).mul(cogTransform(angle)),
			sharedItemTransform, true), stack, resolver, displayContext, level, owner, seed);
	}

	private static boolean renderAmmo(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
		ItemStack stack, int xOffset, int yOffset) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return false;
		Ammo ammo = PotatoCannonItem.getAmmo(player, stack);
		if (ammo == null || AllItems.POTATO_CANNON.is(ammo.stack()))
			return false;

		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(xOffset, yOffset + 8);
		pose.scale(.5f, .5f);
		graphics.nextStratum();
		graphics.item(ammo.stack(), 0, 0);
		pose.popMatrix();
		return false;
	}

	private static Matrix4f cogTransform(float angle) {
		return new Matrix4f()
			.translate(.5f, .5f + .5f / 16f, .5f)
			.rotateZ((float) Math.toRadians(angle))
			.translate(-.5f, -.5f - .5f / 16f, -.5f);
	}

	private static class TransformedState extends ItemStackRenderState {
		private final ItemStackRenderState delegate;
		private final Matrix4fc transform;
		private final ItemTransform[] sharedItemTransform;
		private final boolean reuseItemTransform;

		private TransformedState(ItemStackRenderState delegate, Matrix4fc transform,
			ItemTransform[] sharedItemTransform, boolean reuseItemTransform) {
			this.delegate = delegate;
			this.transform = transform;
			this.sharedItemTransform = sharedItemTransform;
			this.reuseItemTransform = reuseItemTransform;
		}

		@Override
		public LayerRenderState newLayer() {
			return new TransformedLayer(delegate, delegate.newLayer(), transform, sharedItemTransform,
				reuseItemTransform);
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

	private static class TransformedLayer extends LayerRenderState {
		private final LayerRenderState delegate;
		private final Matrix4fc transform;
		private final ItemTransform[] sharedItemTransform;
		private final boolean reuseItemTransform;

		private TransformedLayer(ItemStackRenderState owner, LayerRenderState delegate, Matrix4fc transform,
			ItemTransform[] sharedItemTransform, boolean reuseItemTransform) {
			owner.super();
			this.delegate = delegate;
			this.transform = transform;
			this.sharedItemTransform = sharedItemTransform;
			this.reuseItemTransform = reuseItemTransform;
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
			if (reuseItemTransform && sharedItemTransform[0] != null)
				delegate.setItemTransform(sharedItemTransform[0]);
			else {
				sharedItemTransform[0] = itemTransform;
				delegate.setItemTransform(itemTransform);
			}
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
