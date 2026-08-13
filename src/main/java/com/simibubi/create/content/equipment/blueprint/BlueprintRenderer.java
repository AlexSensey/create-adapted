package com.simibubi.create.content.equipment.blueprint;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.equipment.blueprint.BlueprintEntity.BlueprintSection;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class BlueprintRenderer
	extends EntityRenderer<BlueprintEntity, BlueprintRenderer.BlueprintRenderState> {

	public BlueprintRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void extractRenderState(BlueprintEntity entity, BlueprintRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.size = entity.size;
		state.yRot = entity.getYRot();
		state.xRot = entity.getXRot();
		state.level = entity.level();
		state.entityId = entity.getId();
		state.displayItems.clear();
		for (int sectionIndex = 0; sectionIndex < entity.size * entity.size; sectionIndex++) {
			BlueprintSection section = entity.getSection(sectionIndex);
			state.displayItems.add(section.getDisplayItems()
				.getFirst()
				.copy());
			state.displayItems.add(section.getDisplayItems()
				.getSecond()
				.copy());
		}
	}

	@Override
	public void submit(BlueprintRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		StandaloneModelKey<BlockStateModelPart> modelKey = state.size == 3
			? CreateStandaloneModels.CRAFTING_BLUEPRINT_LARGE
			: state.size == 2 ? CreateStandaloneModels.CRAFTING_BLUEPRINT_MEDIUM
			: CreateStandaloneModels.CRAFTING_BLUEPRINT_SMALL;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(modelKey);

		ms.pushPose();
		ms.mulPose(Axis.YP.rotationDegrees(-state.yRot));
		ms.mulPose(Axis.XP.rotationDegrees(90 + state.xRot));
		ms.translate(-.5, -1 / 32f, -.5);
		if (state.size == 2)
			ms.translate(.5, 0, -.5);
		if (model != null)
			collector.submitBlockModel(ms, RenderTypes.solidMovingBlock(), List.of(model),
				BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();

		int blockLight = state.lightCoords >> 4 & 0xf;
		int skyLight = state.lightCoords >> 20 & 0xf;
		if (state.xRot == 90 || state.yRot % 180 != 0) {
			blockLight /= 1.35;
			skyLight /= 1.35;
		}
		int itemLight = Mth.floor(skyLight + .5) << 20 | (Mth.floor(blockLight + .5) & 0xf) << 4;

		ms.pushPose();
		ms.mulPose(Axis.YP.rotationDegrees(-state.yRot));
		ms.mulPose(Axis.XP.rotationDegrees(state.xRot));
		ms.translate(0, 0, 1 / 32f + .001);
		if (state.size == 3)
			ms.translate(-1, -1, 0);

		for (int x = 0; x < state.size; x++) {
			ms.pushPose();
			for (int y = 0; y < state.size; y++) {
				int sectionIndex = x * state.size + y;
				ms.pushPose();
				ms.scale(.5f, .5f, 1 / 1024f);
				submitItem(state, sectionIndex * 2, ms, collector, itemLight, false);
				submitItem(state, sectionIndex * 2 + 1, ms, collector, itemLight, true);
				ms.popPose();
				ms.translate(1, 0, 0);
			}
			ms.popPose();
			ms.translate(0, 1, 0);
		}
		ms.popPose();
	}

	private static void submitItem(BlueprintRenderState state, int index, PoseStack ms,
		SubmitNodeCollector collector, int light, boolean secondary) {
		if (index >= state.displayItems.size())
			return;
		ItemStack stack = state.displayItems.get(index);
		if (stack.isEmpty() || state.level == null)
			return;

		ms.pushPose();
		if (secondary) {
			ms.translate(.325f, -.325f, 1);
			ms.scale(.625f, .625f, 1);
		}

		ItemStackRenderState itemState = new ItemStackRenderState();
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(itemState, stack, ItemDisplayContext.GUI, state.level, null, state.entityId);
		itemState.submit(ms, collector, light, OverlayTexture.NO_OVERLAY, 0);
		ms.popPose();
	}

	public Identifier getTextureLocation(BlueprintEntity entity) {
		return null;
	}

	@Override
	public BlueprintRenderState createRenderState() {
		return new BlueprintRenderState();
	}

	public static class BlueprintRenderState extends EntityRenderState {
		private final List<ItemStack> displayItems = new ArrayList<>();
		private Level level;
		private int entityId;
		private int size = 1;
		private float yRot;
		private float xRot;
	}
}
