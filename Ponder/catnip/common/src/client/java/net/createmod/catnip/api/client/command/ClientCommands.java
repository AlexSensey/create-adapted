package net.createmod.catnip.api.client.command;

import com.mojang.brigadier.CommandDispatcher;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.createmod.catnip.api.event.CatnipEvent;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;

import org.jetbrains.annotations.ApiStatus.Internal;

/// Infrastructure for client-side commands.
public final class ClientCommands {
	/// Event invoked when client-side commands can be registered.
	public static final CatnipEvent<RegistrationCallback> REGISTER = CatnipEvent.create(callbacks -> (dispatcher, context) -> {
		for (RegistrationCallback callback : callbacks) {
			callback.register(dispatcher, context);
		}
	});

	private ClientCommands() {}

	/// Helper method for creating a literal argument with the right generic type.
	public static LiteralArgumentBuilder<SharedSuggestionProvider> literal(String s) {
		return LiteralArgumentBuilder.literal(s);
	}

	/// Helper method for creating an argument with the right generic type.
	public static <T> RequiredArgumentBuilder<SharedSuggestionProvider, T> argument(String name, ArgumentType<T> type) {
		return RequiredArgumentBuilder.argument(name, type);
	}

	@Internal
	@SuppressWarnings("unchecked") // should be safe? the generics around commands are a mess
	public static void registerCommands(CommandDispatcher<? extends SharedSuggestionProvider> dispatcher, CommandBuildContext context) {
		REGISTER.invoker().register((CommandDispatcher<SharedSuggestionProvider>) dispatcher, context);
	}

	/// A callback that will be invoked to register client-side commands.
	/// @see #REGISTER
	@FunctionalInterface
	public interface RegistrationCallback {
		void register(CommandDispatcher<SharedSuggestionProvider> dispatcher, CommandBuildContext context);
	}
}
