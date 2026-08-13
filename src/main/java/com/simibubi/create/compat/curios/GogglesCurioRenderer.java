package com.simibubi.create.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;

import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Renders Engineer's Goggles from a Curios slot using the 26.2 submit API. */
public class GogglesCurioRenderer implements ICurioRenderer {

	@Override
	public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(ItemStack stack,
		SlotContext slotContext, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, S renderState,
		RenderLayerParent<S, M> renderLayerParent, EntityRendererProvider.Context context, float yRotation,
		float xRotation) {
		if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel))
			return;
		BlockStateModelPart goggles = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.GOGGLES);
		if (goggles == null)
			return;

		poseStack.pushPose();
		// The parent's model has already been animated from the current render state.
		// Applying its head part keeps the goggles attached while looking, crouching and swimming.
		humanoidModel.head.translateAndRotate(poseStack);
		// Curios' 26.2 humanoid head transform is anchored at the top of the 8px head.
		// Move the block-model attachment down one full head height onto the forehead.
		poseStack.translate(0, .5f, 0);
		// Entity model parts use +Y down from the head pivot while block models use +Y up.
		// Flip that local axis so the 0..8px goggles model occupies the actual 8px head
		// instead of growing upwards from it like a pair of ears.
		poseStack.scale(1, -1, 1);
		poseStack.translate(-.5f, 0, -.5f);
		collector.submitBlockModel(poseStack, RenderTypes.cutoutMovingBlock(), List.of(goggles),
			BlockModelRenderState.EMPTY_TINTS, packedLight, OverlayTexture.NO_OVERLAY,
			renderState.outlineColor);
		poseStack.popPose();
	}
}
