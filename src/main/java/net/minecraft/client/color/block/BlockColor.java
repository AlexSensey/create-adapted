package net.minecraft.client.color.block;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface BlockColor {
	int getColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex);
}
