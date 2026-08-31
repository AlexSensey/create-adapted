package net.createmod.catnip.api.client.level;

import java.util.function.ToIntFunction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

// https://github.com/Engine-Room/Flywheel/blob/bb43937ca17e5e8ca0b08fe1fa896799e526ec11/common/src/lib/java/dev/engine_room/flywheel/lib/model/baked/VirtualBlockGetter.java
public abstract class VirtualBlockGetter implements BlockAndTintGetter {
	protected final VirtualLightEngine lightEngine;

	public VirtualBlockGetter(ToIntFunction<BlockPos> blockLightFunc, ToIntFunction<BlockPos> skyLightFunc) {
		lightEngine = new VirtualLightEngine(blockLightFunc, skyLightFunc, this);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return lightEngine;
	}

	@Override
	public int getBlockTint(BlockPos pos, ColorResolver resolver) {
		Biome plainsBiome = Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(Registries.BIOME).getValueOrThrow(Biomes.PLAINS);
		return resolver.getColor(plainsBiome, pos.getX(), pos.getZ());
	}

	@Override
	public CardinalLighting cardinalLighting() {
		return CardinalLighting.DEFAULT;
	}
}
