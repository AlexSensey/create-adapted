package com.simibubi.create.content.equipment.extendoGrip;

import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPartialModels;

import it.unimi.dsi.fastutil.ints.IntList;
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
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ExtendoGripItemRenderer implements ItemModel {

	private final ItemModel base;
	private final ItemModel cog;
	private final ItemModel thinShort;
	private final ItemModel wideShort;
	private final ItemModel thinLong;
	private final ItemModel wideLong;
	private final ItemModel hand;
	private final ItemModel holdingHand;

	public ExtendoGripItemRenderer(ItemModel base, ItemModel cog, ItemModel thinShort, ItemModel wideShort,
		ItemModel thinLong, ItemModel wideLong, ItemModel hand, ItemModel holdingHand) {
		this.base = base;
		this.cog = cog;
		this.thinShort = thinShort;
		this.wideShort = wideShort;
		this.thinLong = thinLong;
		this.wideLong = wideLong;
		this.hand = hand;
		this.holdingHand = holdingHand;
	}

	@Override
	public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
		ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
		if (base == null)
			return;

		float partialTicks = AnimationTickHolder.getPartialTicks();
		boolean firstPerson = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
			|| displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
		float animation = firstPerson
			? Mth.lerp(partialTicks, ExtendoGripRenderHandler.lastMainHandAnimation,
				ExtendoGripRenderHandler.mainHandAnimation)
			: .25f;
		animation = animation * animation * animation;
		float extensionAngle = Mth.lerp(animation, 24, 156);
		float halfAngle = extensionAngle / 2;
		float oppositeAngle = 180 - extensionAngle;

		ItemTransform[] sharedItemTransform = new ItemTransform[1];
		base.update(new TransformedState(state, new Matrix4f(), sharedItemTransform, false), stack, resolver,
			displayContext, level, owner, seed);
		state.setAnimated();
		state.appendModelIdentityElement((int) (animation * 1000));

		Matrix4f common = new Matrix4f()
			.translate(0, 1 / 16f, -7 / 16f)
			.scale(1, 1, 1 + animation);

		Matrix4f branch = new Matrix4f(common);
		branch.rotateX(radians(-halfAngle))
			.translate(0, .5f, .5f);
		render(thinShort, branch, state, stack, resolver, displayContext, level, owner, seed, sharedItemTransform);
		branch.translate(0, -.5f, -.5f)
			.translate(0, 5.5f / 16f, 0)
			.rotateX(radians(-oppositeAngle))
			.translate(0, .5f, .5f);
		render(wideLong, branch, state, stack, resolver, displayContext, level, owner, seed, sharedItemTransform);
		branch.translate(0, -.5f, -.5f)
			.translate(0, 11 / 16f, 0)
			.rotateX(radians(oppositeAngle))
			.translate(0, .5f, .5f)
			.translate(0, .5f / 16f, 0);
		render(thinShort, branch, state, stack, resolver, displayContext, level, owner, seed, sharedItemTransform);

		branch = new Matrix4f(common);
		branch.rotateX(radians(-180 + halfAngle))
			.translate(0, .5f, .5f);
		render(wideShort, branch, state, stack, resolver, displayContext, level, owner, seed, sharedItemTransform);
		branch.translate(0, -.5f, -.5f)
			.translate(0, 5.5f / 16f, 0)
			.rotateX(radians(oppositeAngle))
			.translate(0, .5f, .5f);
		render(thinLong, branch, state, stack, resolver, displayContext, level, owner, seed, sharedItemTransform);
		branch.translate(0, -.5f, -.5f)
			.translate(0, 11 / 16f, 0)
			.rotateX(radians(-oppositeAngle))
			.translate(0, .5f, .5f)
			.translate(0, .5f / 16f, 0);
		render(wideShort, branch, state, stack, resolver, displayContext, level, owner, seed, sharedItemTransform);

		branch = getHandTransform(animation, false);
		ItemModel renderedHand = firstPerson && ExtendoGripRenderHandler.pose == AllPartialModels.DEPLOYER_HAND_HOLDING
			? holdingHand : hand;
		render(renderedHand, branch, state, stack, resolver, displayContext, level, owner, seed, sharedItemTransform);

		var player = Minecraft.getInstance().player;
		if (firstPerson && renderedHand == holdingHand && player != null
			&& AllItems.EXTENDO_GRIP.isIn(player.getOffhandItem())) {
			ItemStack heldStack = player.getMainHandItem();
			// Swapping hands can leave the interpolated HOLDING pose alive for one
			// frame after the grip moved to the main hand. Never append the grip as
			// its own held-item model; that recursively updates forever and hangs JVM.
			if (heldStack.getItem() instanceof BlockItem && !AllItems.EXTENDO_GRIP.isIn(heldStack)) {
				float flip = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
				// Match the original first-person renderer. The carried item is positioned
				// relative to the grip's item transform, then moved along the extending
				// mechanism. The hand model uses a different local origin and cannot be
				// used as the carried item's attachment transform.
				// ItemTransform.apply() in 26.2 already finishes by centering the
				// Extendo model at (-.5, -.5, -.5). The old ClientHooks call applied
				// the attachment before that centering step, so undo it here first.
				Matrix4f heldTransform = new Matrix4f()
					.translate(.5f, .5f, .5f)
					.translate(flip * -.05f, .15f, -1.2f)
					.translate(0, 0, -animation * 2.25f)
					.rotateY(radians(flip * 45))
					.translate(flip * .15f, -.15f, -.05f)
					.scale(1.25f);
				resolver.appendItemLayers(new AttachedItemState(state, heldTransform, sharedItemTransform,
					displayContext.leftHand()),
					heldStack, displayContext, level, owner, seed + 1);
			}
		}

		float cogAngle = AnimationTickHolder.getRenderTime() * -2;
		if (firstPerson)
			cogAngle += 360 * animation;
		Matrix4f cogTransform = new Matrix4f()
			.translate(0, 1 / 16f, 0)
			.rotateZ((float) Math.toRadians(cogAngle))
			.translate(0, -1 / 16f, 0);
		render(cog, cogTransform, state, stack, resolver, displayContext, level, owner, seed, sharedItemTransform);
	}

	private static float radians(float degrees) {
		return (float) Math.toRadians(degrees);
	}

	public static Matrix4f getHandTransform(float animation, boolean centered) {
		float extensionAngle = Mth.lerp(animation, 24, 156);
		float halfAngle = extensionAngle / 2;
		float oppositeAngle = 180 - extensionAngle;
		Matrix4f transform = new Matrix4f()
			.translate(0, 1 / 16f, -7 / 16f)
			.scale(1, 1, 1 + animation)
			.rotateX(radians(-180 + halfAngle))
			.translate(0, .5f, .5f)
			.translate(0, -.5f, -.5f)
			.translate(0, 5.5f / 16f, 0)
			.rotateX(radians(oppositeAngle))
			.translate(0, .5f, .5f)
			.translate(0, -.5f, -.5f)
			.translate(0, 11 / 16f, 0)
			.rotateX(radians(-oppositeAngle))
			.translate(0, .5f, .5f)
			.translate(0, .5f / 16f, 0)
			.translate(0, -.5f, -.5f)
			.translate(0, 5.5f / 16f, 0)
			.rotateX(radians(180 - halfAngle))
			.rotateY((float) Math.PI)
			.translate(0, 0, -4 / 16f)
			.scale(1, 1, 1 / (1 + animation));
		if (!centered)
			return transform;
		return new Matrix4f().translate(.5f, .5f, .5f)
			.mul(transform)
			.translate(-.5f, -.5f, -.5f);
	}

	private static void render(ItemModel model, Matrix4f centeredTransform, ItemStackRenderState state,
		ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, ClientLevel level,
		ItemOwner owner, int seed, ItemTransform[] sharedItemTransform) {
		if (model == null)
			return;
		Matrix4f itemTransform = new Matrix4f()
			.translate(.5f, .5f, .5f)
			.mul(centeredTransform)
			.translate(-.5f, -.5f, -.5f);
		model.update(new TransformedState(state, itemTransform, sharedItemTransform, true), stack, resolver,
			displayContext, level, owner, seed);
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

		@Override public LayerRenderState newLayer() {
			return new TransformedLayer(delegate, delegate.newLayer(), transform, sharedItemTransform,
				reuseItemTransform);
		}
		@Override public void setAnimated() { delegate.setAnimated(); }
		@Override public void appendModelIdentityElement(Object object) { delegate.appendModelIdentityElement(object); }
		@Override public void setOversizedInGui(boolean oversized) { delegate.setOversizedInGui(oversized); }
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

		@Override public List<BakedQuad> prepareQuadList() { return delegate.prepareQuadList(); }
		@Override public void setUsesBlockLight(boolean value) { delegate.setUsesBlockLight(value); }
		@Override public void setExtents(Supplier<Vector3fc[]> extents) { delegate.setExtents(extents); }
		@Override public void setParticleMaterial(Material.Baked material) { delegate.setParticleMaterial(material); }
		@Override public void setItemTransform(ItemTransform itemTransform) {
			if (reuseItemTransform && sharedItemTransform[0] != null)
				delegate.setItemTransform(sharedItemTransform[0]);
			else {
				sharedItemTransform[0] = itemTransform;
				delegate.setItemTransform(itemTransform);
			}
		}
		@Override public void setLocalTransform(Matrix4fc localTransform) {
			delegate.setLocalTransform(new Matrix4f(transform).mul(localTransform));
		}
		@Override public <T> void setupSpecialModel(SpecialModelRenderer<T> renderer, T value) {
			delegate.setupSpecialModel(renderer, value);
		}
		@Override public void setFoilType(FoilType foilType) { delegate.setFoilType(foilType); }
		@Override public IntList tintLayers() { return delegate.tintLayers(); }
	}

	private static class AttachedItemState extends ItemStackRenderState {
		private final ItemStackRenderState delegate;
		private final Matrix4fc attachmentTransform;
		private final ItemTransform[] sharedItemTransform;
		private final boolean leftHand;

		private AttachedItemState(ItemStackRenderState delegate, Matrix4fc attachmentTransform,
			ItemTransform[] sharedItemTransform, boolean leftHand) {
			this.delegate = delegate;
			this.attachmentTransform = attachmentTransform;
			this.sharedItemTransform = sharedItemTransform;
			this.leftHand = leftHand;
		}

		@Override public LayerRenderState newLayer() {
			return new AttachedItemLayer(delegate, delegate.newLayer(), attachmentTransform, sharedItemTransform,
				leftHand);
		}
		@Override public void setAnimated() { delegate.setAnimated(); }
		@Override public void appendModelIdentityElement(Object object) { delegate.appendModelIdentityElement(object); }
		@Override public void setOversizedInGui(boolean oversized) { delegate.setOversizedInGui(oversized); }
	}

	private static class AttachedItemLayer extends LayerRenderState {
		private final LayerRenderState delegate;
		private final Matrix4fc attachmentTransform;
		private final ItemTransform[] sharedItemTransform;
		private final boolean leftHand;
		private final Matrix4f itemTransform = new Matrix4f();
		private final Matrix4f modelTransform = new Matrix4f();

		private AttachedItemLayer(ItemStackRenderState owner, LayerRenderState delegate,
			Matrix4fc attachmentTransform, ItemTransform[] sharedItemTransform, boolean leftHand) {
			owner.super();
			this.delegate = delegate;
			this.attachmentTransform = attachmentTransform;
			this.sharedItemTransform = sharedItemTransform;
			this.leftHand = leftHand;
			if (sharedItemTransform[0] != null)
				delegate.setItemTransform(sharedItemTransform[0]);
			updateTransform();
		}

		private void updateTransform() {
			delegate.setLocalTransform(new Matrix4f(attachmentTransform).mul(itemTransform).mul(modelTransform));
		}

		@Override public List<BakedQuad> prepareQuadList() { return delegate.prepareQuadList(); }
		@Override public void setUsesBlockLight(boolean value) { delegate.setUsesBlockLight(value); }
		@Override public void setExtents(Supplier<Vector3fc[]> extents) { delegate.setExtents(extents); }
		@Override public void setParticleMaterial(Material.Baked material) { delegate.setParticleMaterial(material); }
		@Override public void setItemTransform(ItemTransform transform) {
			if (sharedItemTransform[0] != null)
				delegate.setItemTransform(sharedItemTransform[0]);
			PoseStack stack = new PoseStack();
			transform.apply(leftHand, stack.last());
			itemTransform.set(stack.last().pose());
			updateTransform();
		}
		@Override public void setLocalTransform(Matrix4fc localTransform) {
			modelTransform.set(localTransform);
			updateTransform();
		}
		@Override public <T> void setupSpecialModel(SpecialModelRenderer<T> renderer, T value) {
			delegate.setupSpecialModel(renderer, value);
		}
		@Override public void setFoilType(FoilType foilType) { delegate.setFoilType(foilType); }
		@Override public IntList tintLayers() { return delegate.tintLayers(); }
	}
}
