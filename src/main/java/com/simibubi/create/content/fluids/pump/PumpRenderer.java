package com.simibubi.create.content.fluids.pump;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

public class PumpRenderer extends KineticBlockEntityRenderer<PumpBlockEntity> {
	private List<BlockStateModelPart> cogModel;

	public PumpRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected List<BlockStateModelPart> getRotatingModelParts(PumpBlockEntity be, BlockState renderedState) {
		return getCogModel();
	}

	private List<BlockStateModelPart> getCogModel() {
		if (cogModel != null)
			return cogModel;
		BlockStateModelPart model = Minecraft.getInstance()
			.getModelManager()
			.getStandaloneModel(CreateStandaloneModels.MECHANICAL_PUMP_COG);
		return cogModel = model == null ? List.of() : List.of(model);
	}

	@Override
	protected void transformRotatingModel(PumpBlockEntity be, PoseStack ms, float partialTicks) {
		Axis axis = getRotationAxisOf(be);
		ms.translate(.5, .5, .5);
		ms.mulPose(rotation(axis, getAngleForBe(be, be.getBlockPos(), axis, partialTicks)));
		orientToAxis(axis, ms);
		ms.translate(-.5, -.5, -.5);
	}

	private static void orientToAxis(Axis axis, PoseStack ms) {
		switch (axis) {
			case X -> ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
			case Y -> ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
			case Z -> {
			}
		}
	}

}
