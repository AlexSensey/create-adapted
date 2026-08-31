package net.createmod.catnip.impl.neoforge.service;

import net.createmod.catnip.api.placement.IPlacementHelper;
import net.createmod.catnip.api.platform.services.ModHooksHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class NeoForgeHooksHelper implements ModHooksHelper {

	@Override
	public boolean playerPlaceSingleBlock(Player player, Level level, BlockPos pos, BlockState newState) {
		BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
		level.setBlockAndUpdate(pos, newState);

		BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(snapshot, IPlacementHelper.ID, player);
		if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
			snapshot.restore(Block.UPDATE_CLIENTS); // TODO - Checkover
			return true;
		}

		return false;
	}

	@Override
	public ItemStack getCloneItemFromBlockstate(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
		return state.getCloneItemStack(pos, level, true, player);
	}

	@Override
	public boolean isPlayerFake(ServerPlayer player) {
		return player instanceof FakePlayer;
	}

	@Override
	public MinecraftServer getServer() {
		return ServerLifecycleHooks.getCurrentServer();
	}
}
