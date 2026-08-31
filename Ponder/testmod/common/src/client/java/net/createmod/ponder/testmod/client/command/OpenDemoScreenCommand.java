package net.createmod.ponder.testmod.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.ponder.testmod.client.DemoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import static net.createmod.catnip.api.client.command.ClientCommands.literal;

public final class OpenDemoScreenCommand {
	public static LiteralArgumentBuilder<SharedSuggestionProvider> build() {
		return literal("demoscreen").executes(context -> {
			Minecraft mc = Minecraft.getInstance();
			mc.schedule(() -> ScreenOpener.transitionTo(new DemoScreen()));
			mc.player.sendSystemMessage(Component.literal("h"));
			return 1;
		});
	}
}
