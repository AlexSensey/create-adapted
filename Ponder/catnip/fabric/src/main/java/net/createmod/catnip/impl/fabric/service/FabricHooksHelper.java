package net.createmod.catnip.impl.fabric.service;

import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.platform.services.ModHooksHelper;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public class FabricHooksHelper implements ModHooksHelper {
	@Nullable
	private static MinecraftServer currentServer;

	static {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
		ServerLifecycleEvents.SERVER_STOPPED.register(_ -> currentServer = null);
	}

	@Override
	public boolean playerPlaceSingleBlock(Player player, Level level, BlockPos pos, BlockState newState) {
		level.setBlockAndUpdate(pos, newState);
		return false;
	}

	@Override
	public ItemStack getCloneItemFromBlockstate(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
		return state.getCloneItemStack(level, pos, true);
	}

	@Override
	public boolean isPlayerFake(ServerPlayer player) {
		return player instanceof FakePlayer;
	}

	@Override
	public MinecraftServer getServer() {
		return currentServer;
	}
}
