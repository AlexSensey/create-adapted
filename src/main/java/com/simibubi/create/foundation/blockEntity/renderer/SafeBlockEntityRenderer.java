package com.simibubi.create.foundation.blockEntity.renderer;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;

import net.createmod.ponder.api.client.level.PonderLevel;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class SafeBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, BlockEntityRenderState> {
	public final void render(T be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light,
		int overlay) {
		if (isInvalid(be))
			return;
		renderSafe(be, partialTicks, ms, bufferSource, light, overlay);
	}

	@Override
	public BlockEntityRenderState createRenderState() {
		return new BlockEntityRenderState();
	}

	@Override
	public void submit(BlockEntityRenderState state, PoseStack ms, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
	}

	protected abstract void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light,
		int overlay);

	public boolean isInvalid(T be) {
		return !be.hasLevel() || be.getBlockState()
			.getBlock() == Blocks.AIR;
	}

	public boolean shouldCullItem(Vec3 itemPos, Level level) {
		if (level instanceof PonderLevel)
			return false;

		return false;
	}

	public @NotNull AABB getRenderBoundingBox(@NotNull T blockEntity) {
		if (blockEntity instanceof CachedRenderBBBlockEntity cbe)
			return cbe.getRenderBoundingBox();

		return AABB.INFINITE;
	}
}
