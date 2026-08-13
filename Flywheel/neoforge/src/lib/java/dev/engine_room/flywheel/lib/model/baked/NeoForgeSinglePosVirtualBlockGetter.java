package dev.engine_room.flywheel.lib.model.baked;

import java.util.function.ToIntFunction;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** NeoForge 26.2 no longer exposes per-position ModelData through BlockAndTintGetter. */
public class NeoForgeSinglePosVirtualBlockGetter extends SinglePosVirtualBlockGetter {
	public NeoForgeSinglePosVirtualBlockGetter(ToIntFunction<BlockPos> blockLightFunc, ToIntFunction<BlockPos> skyLightFunc) {
		super(blockLightFunc, skyLightFunc);
	}

	public static NeoForgeSinglePosVirtualBlockGetter createFullDark() {
		return new NeoForgeSinglePosVirtualBlockGetter(p -> 0, p -> 0);
	}

	public static NeoForgeSinglePosVirtualBlockGetter createFullBright() {
		return new NeoForgeSinglePosVirtualBlockGetter(p -> 15, p -> 15);
	}

	@Override
	public NeoForgeSinglePosVirtualBlockGetter pos(BlockPos pos) {
		super.pos(pos);
		return this;
	}

	@Override
	public NeoForgeSinglePosVirtualBlockGetter blockState(BlockState state) {
		super.blockState(state);
		return this;
	}

	@Override
	public NeoForgeSinglePosVirtualBlockGetter blockEntity(@Nullable BlockEntity blockEntity) {
		super.blockEntity(blockEntity);
		return this;
	}
}
