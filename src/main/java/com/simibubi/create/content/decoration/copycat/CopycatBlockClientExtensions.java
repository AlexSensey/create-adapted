package com.simibubi.create.content.decoration.copycat;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import org.jspecify.annotations.Nullable;

/** Client-side effects whose appearance has to follow the material stored in the copycat block entity. */
public class CopycatBlockClientExtensions implements IClientBlockExtensions {

	@Override
	public boolean addHitEffects(BlockState state, Level level, @Nullable HitResult target, ParticleEngine manager) {
		if (!(level instanceof ClientLevel clientLevel) || !(target instanceof BlockHitResult blockHit))
			return false;

		BlockPos pos = blockHit.getBlockPos();
		BlockState material = CopycatBlock.getMaterial(level, pos);
		if (material.isAir())
			return false;

		Direction face = blockHit.getDirection();
		Vec3 location = blockHit.getLocation();
		double x = location.x + face.getStepX() * .01;
		double y = location.y + face.getStepY() * .01;
		double z = location.z + face.getStepZ() * .01;
		manager.add(new TerrainParticle(clientLevel, x, y, z, 0, 0, 0, material, pos)
			.updateSprite(material, pos));
		return true;
	}

	@Override
	public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
		if (!(level instanceof ClientLevel clientLevel))
			return false;

		BlockState material = CopycatBlock.getMaterial(level, pos);
		if (material.isAir())
			return false;

		VoxelShape shape = state.getShape(level, pos);
		shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
			double width = Math.min(1, x2 - x1);
			double height = Math.min(1, y2 - y1);
			double depth = Math.min(1, z2 - z1);
			int xParts = Math.max(2, (int) Math.ceil(width * 4));
			int yParts = Math.max(2, (int) Math.ceil(height * 4));
			int zParts = Math.max(2, (int) Math.ceil(depth * 4));

			for (int xi = 0; xi < xParts; xi++)
				for (int yi = 0; yi < yParts; yi++)
					for (int zi = 0; zi < zParts; zi++) {
						double dx = (xi + .5) / xParts;
						double dy = (yi + .5) / yParts;
						double dz = (zi + .5) / zParts;
						double x = pos.getX() + x1 + dx * (x2 - x1);
						double y = pos.getY() + y1 + dy * (y2 - y1);
						double z = pos.getZ() + z1 + dz * (z2 - z1);
						manager.add(new TerrainParticle(clientLevel, x, y, z, dx - .5, dy - .5, dz - .5,
							material, pos).updateSprite(material, pos));
					}
		});
		return true;
	}

	@Override
	public void collectDynamicTintValues(BlockState state, BlockAndTintGetter level, BlockPos pos,
		IntList tintValues) {
		BlockState material = CopycatBlock.getMaterial(level, pos);
		for (BlockTintSource source : Minecraft.getInstance().getBlockColors().getTintSources(material))
			tintValues.add(source.colorInWorld(material, level, pos));
	}
}
