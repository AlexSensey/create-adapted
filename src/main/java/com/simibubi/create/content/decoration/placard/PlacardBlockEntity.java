package com.simibubi.create.content.decoration.placard;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PlacardBlockEntity extends SmartBlockEntity {

	ItemStack heldItem;
	int poweredTicks;

	public PlacardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		heldItem = ItemStack.EMPTY;
		poweredTicks = 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (level.isClientSide())
			return;
		if (poweredTicks == 0)
			return;

		poweredTicks--;
		if (poweredTicks > 0)
			return;

		BlockState blockState = getBlockState();
		level.setBlock(worldPosition, blockState.setValue(PlacardBlock.POWERED, false), Block.UPDATE_ALL);
		PlacardBlock.updateNeighbours(blockState, level, worldPosition);
	}

	public ItemStack getHeldItem() {
		return heldItem;
	}

	public void setHeldItem(ItemStack heldItem) {
		this.heldItem = heldItem;
		notifyUpdate();
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		tag.putInt("PoweredTicks", poweredTicks);
		tag.put("Item", writeItemStack(registries, heldItem));
		super.write(tag, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		int prevTicks = poweredTicks;
		poweredTicks = tag.getIntOr("PoweredTicks", 0);
		heldItem = readItemStack(registries, tag.getCompoundOrEmpty("Item"));
		super.read(tag, registries, clientPacket);

		if (clientPacket && prevTicks < poweredTicks)
			spawnParticles();
	}

	private static ItemStack readItemStack(HolderLookup.Provider registries, Tag tag) {
		return ItemStack.OPTIONAL_CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag)
			.map(result -> result.getFirst())
			.result()
			.orElse(ItemStack.EMPTY);
	}

	private static CompoundTag writeItemStack(HolderLookup.Provider registries, ItemStack stack) {
		return ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
			.result()
			.filter(CompoundTag.class::isInstance)
			.map(CompoundTag.class::cast)
			.orElseGet(CompoundTag::new);
	}

	private void spawnParticles() {
		BlockState blockState = getBlockState();
		if (!AllBlocks.PLACARD.has(blockState))
			return;

		DustParticleOptions pParticleData = DustParticleOptions.REDSTONE;
		Vec3 centerOf = VecHelper.getCenterOf(worldPosition);
		Vec3 normal = Vec3.atLowerCornerOf(PlacardBlock.connectedDirection(blockState)
			.getUnitVec3i());
		Vec3 offset = VecHelper.axisAlingedPlaneOf(normal);

		for (int i = 0; i < 10; i++) {
			Vec3 v = VecHelper.offsetRandomly(Vec3.ZERO, level.getRandom(), .5f)
				.multiply(offset)
				.normalize()
				.scale(.45f)
				.add(normal.scale(-.45f))
				.add(centerOf);
			level.addParticle(pParticleData, v.x, v.y, v.z, 0, 0, 0);
		}
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

}
