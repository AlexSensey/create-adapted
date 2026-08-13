package com.simibubi.create.content.equipment.armor;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class BacktankArmorLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
	extends RenderLayer<S, M> {

	public BacktankArmorLayer(RenderLayerParent<S, M> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack ms, SubmitNodeCollector collector, int light, S renderState,
		float yRot, float xRot) {
		if (!(renderState instanceof HumanoidRenderState humanoid) || humanoid.hasPose(Pose.SLEEPING))
			return;
		ItemStack stack = humanoid.chestEquipment;
		if (!(stack.getItem() instanceof BacktankItem item))
			return;
		if (!(getParentModel() instanceof HumanoidModel<?> model))
			return;

		BlockState blockState = item.getBlock().defaultBlockState();
		boolean netherite = AllBlocks.NETHERITE_BACKTANK.has(blockState);
		BlockStateModelPart body = model(netherite ? CreateStandaloneModels.NETHERITE_BACKTANK_BODY
			: CreateStandaloneModels.COPPER_BACKTANK_BODY);
		BlockStateModelPart shaft = model(netherite ? CreateStandaloneModels.NETHERITE_BACKTANK_SHAFT
			: CreateStandaloneModels.COPPER_BACKTANK_SHAFT);
		BlockStateModelPart cogs = model(netherite ? CreateStandaloneModels.NETHERITE_BACKTANK_COGS
			: CreateStandaloneModels.COPPER_BACKTANK_COGS);

		ms.pushPose();
		model.body.translateAndRotate(ms);
		ms.translate(-.5f, 10 / 16f, 1);
		ms.scale(1, -1, -1);
		// The old CachedBuffers call applied the SOUTH blockstate rotation before
		// the individual partial transforms. Standalone models are unrotated.
		rotateCentered(ms, Axis.YP, 180);

		submitPart(ms, collector, body, light);
		if (shaft != null) {
			ms.pushPose();
			ms.translate(0, -3f / 16, 0);
			submitPart(ms, collector, shaft, light);
			ms.popPose();
		}
		if (cogs != null) {
			ms.pushPose();
			ms.translate(0, 6.5f / 16, 11f / 16);
			ms.mulPose(Axis.XP.rotationDegrees(2 * AnimationTickHolder.getRenderTime() % 360));
			ms.translate(0, -6.5f / 16, -11f / 16);
			submitPart(ms, collector, cogs, light);
			ms.popPose();
		}
		ms.popPose();
	}

	private static BlockStateModelPart model(StandaloneModelKey<BlockStateModelPart> key) {
		return Minecraft.getInstance().getModelManager().getStandaloneModel(key);
	}

	private static void submitPart(PoseStack ms, SubmitNodeCollector collector, BlockStateModelPart part, int light) {
		if (part != null)
			collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(part),
				BlockModelRenderState.EMPTY_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
	}

	private static void rotateCentered(PoseStack ms, Axis axis, float degrees) {
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(axis.rotationDegrees(degrees));
		ms.translate(-.5f, -.5f, -.5f);
	}

	public static void register(EntityRenderersEvent.AddLayers event) {
		for (var skin : event.getSkins())
			registerOn(event.getPlayerRenderer(skin));
		for (var type : event.getEntityTypes())
			registerOn(event.getRenderer(type));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void registerOn(EntityRenderer<?, ?> renderer) {
		if (!(renderer instanceof LivingEntityRenderer livingRenderer))
			return;
		if (!(livingRenderer.getModel() instanceof HumanoidModel))
			return;
		livingRenderer.addLayer(new BacktankArmorLayer(livingRenderer));
	}
}
