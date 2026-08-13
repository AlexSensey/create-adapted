package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.content.equipment.goggles.GoggleConfigScreen;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class OverlayConfigCommand {
	public static int reset(CommandSourceStack source) {
		AllConfigs.client().overlayOffsetX.set(0);
		AllConfigs.client().overlayOffsetY.set(0);
		source.sendSuccess(() -> Component.literal("Create Goggle Overlay has been reset to default position"), true);
		return Command.SINGLE_SUCCESS;
	}

	public static int open(CommandSourceStack source) {
		ScreenOpener.open(new GoggleConfigScreen());
		return Command.SINGLE_SUCCESS;
	}

	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("overlay")
			.requires(cs -> true)
			.then(Commands.literal("reset")
				.executes(ctx -> {
					return reset(ctx.getSource());
				})
			)
			.executes(ctx -> {
				return open(ctx.getSource());
			});

	}
}
