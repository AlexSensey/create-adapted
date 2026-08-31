package com.simibubi.create.content.logistics.packager;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import com.simibubi.create.foundation.render.CreateVisualizationManager;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class PackagerRenderer extends SmartBlockEntityRenderer<PackagerBlockEntity> {

	public PackagerRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(PackagerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new PackagerRenderState();
	}

	@Override
	public void extractRenderState(PackagerBlockEntity be, BlockEntityRenderState state, float partialTicks,
		Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
		if (!(state instanceof PackagerRenderState packagerState))
			return;

		packagerState.blockEntity = be;
		packagerState.partialTicks = partialTicks;
		packagerState.facing = be.getBlockState()
			.getOptionalValue(PackagerBlock.FACING)
			.orElse(Direction.NORTH)
			.getOpposite();
		packagerState.trayOffset = be.getTrayOffset(partialTicks);
		packagerState.regularTray = AllBlocks.PACKAGER.has(be.getBlockState());
		packagerState.hatchOpen = isHatchOpen(be);
		packagerState.renderedBox = be.getRenderedBox()
			.copy();
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		if (!(state instanceof PackagerRenderState packagerState) || packagerState.blockEntity == null)
			return;
		submitBehaviours(packagerState.blockEntity, packagerState.partialTicks, ms, collector, state.lightCoords);

		Direction facing = packagerState.facing;

		if (!CreateVisualizationManager.supportsVisualization(packagerState.blockEntity.getLevel())) {
			ms.pushPose();
			ms.translate(facing.getStepX() * .49999f, facing.getStepY() * .49999f, facing.getStepZ() * .49999f);
			rotateCentered(ms, Axis.YP, AngleHelper.horizontalAngle(facing));
			rotateCentered(ms, Axis.XP, AngleHelper.verticalAngle(facing));
			submitPart(packagerState.hatchOpen ? CreateStandaloneModels.PACKAGER_HATCH_OPEN
				: CreateStandaloneModels.PACKAGER_HATCH_CLOSED, RenderTypes.solidMovingBlock(), ms, collector,
				state.lightCoords);
			ms.popPose();

			ms.pushPose();
			ms.translate(facing.getStepX() * packagerState.trayOffset, facing.getStepY() * packagerState.trayOffset,
				facing.getStepZ() * packagerState.trayOffset);
			rotateCentered(ms, Axis.YP, facing.toYRot());
			submitPart(packagerState.regularTray ? CreateStandaloneModels.PACKAGER_TRAY
				: CreateStandaloneModels.REPACKAGER_TRAY, RenderTypes.cutoutMovingBlock(), ms, collector,
				state.lightCoords);
			ms.popPose();
		}

		if (!packagerState.renderedBox.isEmpty()) {
			ms.pushPose();
			ms.translate(facing.getStepX() * packagerState.trayOffset, facing.getStepY() * packagerState.trayOffset,
				facing.getStepZ() * packagerState.trayOffset);
			ms.translate(.5f, .5f, .5f);
			ms.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
			ms.translate(0, 2 / 16f, 0);
			ms.scale(1.49f, 1.49f, 1.49f);

			ItemStackRenderState itemState = new ItemStackRenderState();
			Minecraft.getInstance()
				.getItemModelResolver()
				.updateForTopItem(itemState, packagerState.renderedBox, ItemDisplayContext.FIXED,
					packagerState.blockEntity.getLevel(), null, 0);
			itemState.submit(ms, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			ms.popPose();
		}
	}

	private static void rotateCentered(PoseStack ms, Axis axis, float degrees) {
		ms.translate(.5f, .5f, .5f);
		ms.mulPose(axis.rotationDegrees(degrees));
		ms.translate(-.5f, -.5f, -.5f);
	}

	private static void submitPart(StandaloneModelKey<BlockStateModelPart> key, RenderType renderType, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		BlockStateModelPart part = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(key);
		if (part != null)
			collector.submitBlockModel(ms, renderType, List.of(part), BlockModelRenderState.EMPTY_TINTS, light,
				OverlayTexture.NO_OVERLAY, 0);
	}

	public static PartialModel getTrayModel(BlockState blockState) {
		return AllBlocks.PACKAGER.has(blockState) ? AllPartialModels.PACKAGER_TRAY_REGULAR
			: AllPartialModels.PACKAGER_TRAY_DEFRAG;
	}

	public static PartialModel getHatchModel(PackagerBlockEntity be) {
		return isHatchOpen(be) ? AllPartialModels.PACKAGER_HATCH_OPEN : AllPartialModels.PACKAGER_HATCH_CLOSED;
	}

	public static boolean isHatchOpen(PackagerBlockEntity be) {
		return be.animationTicks > (be.animationInward ? 1 : 5)
			&& be.animationTicks < PackagerBlockEntity.CYCLE - (be.animationInward ? 5 : 1);
	}

	private static class PackagerRenderState extends BlockEntityRenderState {
		private PackagerBlockEntity blockEntity;
		private float partialTicks;
		private Direction facing = Direction.NORTH;
		private float trayOffset;
		private boolean regularTray;
		private boolean hatchOpen;
		private ItemStack renderedBox = ItemStack.EMPTY;
	}
}
