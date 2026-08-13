package com.simibubi.create.content.kinetics.base;

import java.util.concurrent.atomic.AtomicInteger;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.foundation.utility.BlockHelper;

import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class BlockBreakingKineticBlockEntity extends KineticBlockEntity {

	public static final AtomicInteger NEXT_BREAKER_ID = new AtomicInteger();
	protected int ticksUntilNextProgress;
	protected int destroyProgress;
	protected int breakerId = -NEXT_BREAKER_ID.incrementAndGet();
	protected BlockPos breakingPos;

	public BlockBreakingKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		if (destroyProgress == -1)
			destroyNextTick();
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (ticksUntilNextProgress == -1)
			destroyNextTick();
	}

	public void destroyNextTick() {
		ticksUntilNextProgress = 1;
	}

	protected abstract BlockPos getBreakingPos();

	protected boolean shouldRun() {
		return true;
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putInt("Progress", destroyProgress);
		compound.putInt("NextTick", ticksUntilNextProgress);
		if (breakingPos != null)
			compound.put("Breaking", writeBlockPos(breakingPos));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		destroyProgress = compound.getIntOr("Progress", 0);
		ticksUntilNextProgress = compound.getIntOr("NextTick", 0);
		breakingPos = null;
		if (compound.contains("Breaking"))
			breakingPos = readBlockPos(compound, "Breaking");
		super.read(compound, registries, clientPacket);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (!level.isClientSide() && destroyProgress != 0)
			level.destroyBlockProgress(breakerId, breakingPos, -1);
	}

	@Override
	public void tick() {
		super.tick();

		if (level.isClientSide())
			return;
		if (!shouldRun())
			return;
		if (getSpeed() == 0)
			return;

		breakingPos = getBreakingPos();

		if (ticksUntilNextProgress < 0)
			return;
		if (ticksUntilNextProgress-- > 0)
			return;

		BlockState stateToBreak = level.getBlockState(breakingPos);
		float blockHardness = stateToBreak.getDestroySpeed(level, breakingPos);

		if (!canBreak(stateToBreak, blockHardness)) {
			if (destroyProgress != 0) {
				destroyProgress = 0;
				level.destroyBlockProgress(breakerId, breakingPos, -1);
			}
			return;
		}

		float breakSpeed = getBreakSpeed();
		destroyProgress += Mth.clamp((int) (breakSpeed / blockHardness), 1, 10 - destroyProgress);
		level.playSound(null, worldPosition, stateToBreak.getSoundType()
			.getHitSound(), SoundSource.BLOCKS, .25f, 1);

		if (destroyProgress >= 10) {
			onBlockBroken(stateToBreak);
			destroyProgress = 0;
			ticksUntilNextProgress = -1;
			level.destroyBlockProgress(breakerId, breakingPos, -1);
			return;
		}

		ticksUntilNextProgress = (int) (blockHardness / breakSpeed);
		level.destroyBlockProgress(breakerId, breakingPos, (int) destroyProgress);
	}

	public boolean canBreak(BlockState stateToBreak, float blockHardness) {
		return isBreakable(stateToBreak, blockHardness);
	}

	public static boolean isBreakable(BlockState stateToBreak, float blockHardness) {
		return !(stateToBreak.liquid() || stateToBreak.getBlock() instanceof AirBlock || blockHardness == -1 || AllBlockTags.NON_BREAKABLE.matches(stateToBreak));
	}

	public void onBlockBroken(BlockState stateToBreak) {
		Vec3 vec = VecHelper.offsetRandomly(VecHelper.getCenterOf(breakingPos), level.getRandom(), .125f);
		BlockHelper.destroyBlock(level, breakingPos, 1f, (stack) -> {
			if (stack.isEmpty())
				return;
			if (level.restoringBlockSnapshots)
				return;

			ItemEntity itementity = new ItemEntity(level, vec.x, vec.y, vec.z, stack);
			itementity.setDefaultPickUpDelay();
			itementity.setDeltaMovement(Vec3.ZERO);
			level.addFreshEntity(itementity);
		});
	}

	protected float getBreakSpeed() {
		return Math.abs(getSpeed() / 100f);
	}

	private static CompoundTag writeBlockPos(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
		return tag;
	}

	private static BlockPos readBlockPos(CompoundTag tag, String key) {
		Tag value = tag.get(key);
		if (value instanceof CompoundTag compound)
			return new BlockPos(compound.getIntOr("X", 0), compound.getIntOr("Y", 0), compound.getIntOr("Z", 0));
		if (value instanceof ListTag list && list.size() >= 3)
			return new BlockPos(list.getIntOr(0, 0), list.getIntOr(1, 0), list.getIntOr(2, 0));
		return BlockPos.of(tag.getLongOr(key, 0));
	}

}
