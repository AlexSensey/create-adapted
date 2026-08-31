package net.createmod.catnip.api.platform.services;

import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.platform.ServiceHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public interface ModHooksHelper {
	ModHooksHelper INSTANCE = ServiceHelper.load(ModHooksHelper.class);

	/**
	 * Attempts to place a single Block as a Player, and should fire according events
	 *
	 * @return True if the event got canceled or the Block was not be placed, False otherwise
	 */
	boolean playerPlaceSingleBlock(Player player, Level level, BlockPos pos, BlockState newState);

	ItemStack getCloneItemFromBlockstate(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player);

	boolean isPlayerFake(ServerPlayer player);

	/// @return the current [MinecraftServer], if present
	@Nullable
	MinecraftServer getServer();

	default MinecraftServer getServerOrThrow() {
		MinecraftServer server = this.getServer();
		if (server != null)
			return server;

		throw new IllegalStateException("No server is currently available");
	}
}
