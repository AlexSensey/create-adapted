package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class AllCommands {
	// Client Commands

	public static void registerClient(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("createoverlay")
			.requires(cs -> true)
			.then(Commands.literal("reset")
				.executes(ctx -> OverlayConfigCommand.reset(ctx.getSource())))
			.executes(ctx -> OverlayConfigCommand.open(ctx.getSource()));
		dispatcher.register(root);
	}

	// Server Commands

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("create")
			.requires(cs -> true)
			.then(DumpRailwaysCommand.register())
			.then(PassengerCommand.register())
			.then(CouplingCommand.register())
			.then(TrainCommand.register())
			.then(GlueCommand.register());
		dispatcher.register(root);
	}
}
