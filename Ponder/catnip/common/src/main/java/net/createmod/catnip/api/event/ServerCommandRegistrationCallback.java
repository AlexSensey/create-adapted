package net.createmod.catnip.api.event;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands.CommandSelection;

/// Invoked when server-side commands can be registered.
@FunctionalInterface
public interface ServerCommandRegistrationCallback {
	CatnipEvent<ServerCommandRegistrationCallback> EVENT = CatnipEvent.create(callbacks -> (dispatcher, context, selection) -> {
		for (ServerCommandRegistrationCallback callback : callbacks) {
			callback.register(dispatcher, context, selection);
		}
	});

	void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, CommandSelection selection);
}
