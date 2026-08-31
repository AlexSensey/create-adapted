package net.createmod.ponder.testmod.client;

import net.createmod.catnip.api.client.command.ClientCommands;
import net.createmod.ponder.testmod.client.command.OpenDemoScreenCommand;

public final class TestmodClient {
	public static void init() {
		ClientCommands.REGISTER.subscribe((dispatcher, _) -> dispatcher.register(
			ClientCommands.literal("testmod")
				.then(OpenDemoScreenCommand.build())
		));
	}
}
