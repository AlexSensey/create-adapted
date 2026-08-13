package com.simibubi.create.content.redstone.link.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler.Mode;

import it.unimi.dsi.fastutil.ints.IntList;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LinkedControllerItemModel implements ItemModel {

	private static final LerpedFloat EQUIP_PROGRESS = LerpedFloat.linear()
		.startWithValue(0);
	private static final List<LerpedFloat> BUTTONS = new ArrayList<>(6);
	private static boolean renderingInLectern;
	private static boolean lecternActive;

	static {
		for (int i = 0; i < 6; i++)
			BUTTONS.add(LerpedFloat.linear()
				.startWithValue(0));
	}

	private final ItemModel baseModel;
	private final ItemModel poweredModel;
	private final ItemModel buttonModel;

	public LinkedControllerItemModel(ItemModel baseModel, ItemModel poweredModel, ItemModel buttonModel) {
		this.baseModel = baseModel;
		this.poweredModel = poweredModel;
		this.buttonModel = buttonModel;
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
		ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
		float partialTicks = AnimationTickHolder.getPartialTicks();
		RenderState renderState = getRenderState(stack, displayContext, partialTicks);
		Matrix4f rootTransform = rootTransform(displayContext, renderState.equip, renderState.active);

		ItemModel body = renderState.active && poweredModel != null ? poweredModel : baseModel;
		if (body != null)
			body.update(new TransformedState(state, rootTransform), stack, resolver, displayContext, level, owner, seed);

		if (!renderState.active || buttonModel == null)
			return;

		int lightIdentity = renderState.bindLightIdentity;
		if (lightIdentity != 0) {
			state.setAnimated();
			state.appendModelIdentityElement(lightIdentity);
		}

		float s = 1 / 16f;
		float b = s * -.75f;
		float[][] positions = {
			{2 * s, 0, 8 * s},
			{6 * s, 0, 8 * s},
			{4 * s, 0, 10 * s},
			{4 * s, 0, 6 * s},
			{3 * s, 0, 3 * s},
			{5 * s, 0, 3 * s}
		};

		for (int i = 0; i < positions.length; i++) {
			float depression = b * BUTTONS.get(i)
				.getValue(partialTicks);
			Matrix4f transform = new Matrix4f(rootTransform)
				.translate(positions[i][0], positions[i][1] + depression, positions[i][2]);
			buttonModel.update(new TransformedState(state, transform), stack, resolver, displayContext, level, owner, seed);
		}
	}

	private static RenderState getRenderState(ItemStack stack, ItemDisplayContext displayContext, float partialTicks) {
		if (renderingInLectern)
			return new RenderState(lecternActive, 0, 0);

		Minecraft mc = Minecraft.getInstance();
		boolean active = false;
		float equip = 0;

		if (mc.player != null) {
			boolean rightHanded = mc.options.mainHand()
				.get() == HumanoidArm.RIGHT;
			ItemDisplayContext mainHand =
				rightHanded ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
			ItemDisplayContext offHand =
				rightHanded ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
			boolean noControllerInMain = !AllItems.LINKED_CONTROLLER.isIn(mc.player.getMainHandItem());

			if (displayContext == mainHand || (displayContext == offHand && noControllerInMain)) {
				equip = EQUIP_PROGRESS.getValue(partialTicks);
				active = true;
			}

			if (displayContext == ItemDisplayContext.GUI) {
				if (stack == mc.player.getMainHandItem())
					active = true;
				if (stack == mc.player.getOffhandItem() && noControllerInMain)
					active = true;
			}
		}

		active &= LinkedControllerClientHandler.MODE != Mode.IDLE;

		int bindLightIdentity = 0;
		if (active && LinkedControllerClientHandler.MODE == Mode.BIND)
			bindLightIdentity = (int) Mth.lerp((Mth.sin(AnimationTickHolder.getRenderTime() / 4f) + 1) / 2, 5, 15);

		return new RenderState(active, equip, bindLightIdentity);
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.isPaused())
			return;

		boolean active = LinkedControllerClientHandler.MODE != Mode.IDLE;
		EQUIP_PROGRESS.chase(active ? 1 : 0, .2f, Chaser.EXP);
		EQUIP_PROGRESS.tickChaser();

		for (int i = 0; i < BUTTONS.size(); i++) {
			LerpedFloat button = BUTTONS.get(i);
			button.chase(active && LinkedControllerClientHandler.currentlyPressed.contains(i) ? 1 : 0, .4f,
				Chaser.EXP);
			button.tickChaser();
		}
	}

	public static void resetButtons() {
		for (LerpedFloat button : BUTTONS)
			button.startWithValue(0);
	}

	public static void beginLecternRender(boolean active) {
		renderingInLectern = true;
		lecternActive = active;
	}

	public static void endLecternRender() {
		renderingInLectern = false;
		lecternActive = false;
	}

	private static Matrix4f rootTransform(ItemDisplayContext displayContext, float equip, boolean active) {
		Matrix4f transform = new Matrix4f();
		if (!active || equip == 0)
			return transform;

		int handModifier = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
		return transform.translate(0, equip * .45f, equip * .32f * handModifier)
			.rotateY((float) Math.toRadians(equip * -24 * handModifier))
			.rotateZ((float) Math.toRadians(equip * -24));
	}

	private record RenderState(boolean active, float equip, int bindLightIdentity) {
	}

	private static class TransformedState extends ItemStackRenderState {
		private final ItemStackRenderState delegate;
		private final Matrix4fc transform;

		private TransformedState(ItemStackRenderState delegate, Matrix4fc transform) {
			this.delegate = delegate;
			this.transform = transform;
		}

		@Override
		public LayerRenderState newLayer() {
			return new TransformedLayer(delegate, delegate.newLayer(), transform);
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

		private TransformedLayer(ItemStackRenderState owner, LayerRenderState delegate, Matrix4fc transform) {
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
