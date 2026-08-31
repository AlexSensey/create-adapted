package net.createmod.catnip.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class CatnipCommands {
	public static LiteralCommandNode<CommandSourceStack> buildRedirect(String name, LiteralCommandNode<CommandSourceStack> target) {
		return Commands.literal(name)
			.redirect(target)
			.build();
	}

	public static void createOrAddToShortcut(CommandDispatcher<CommandSourceStack> dispatcher, String name,
		LiteralCommandNode<CommandSourceStack> target) {
		LiteralArgumentBuilder<CommandSourceStack> shortcut = Commands.literal(name)
			.redirect(target);
		dispatcher.register(shortcut);
	}
}
