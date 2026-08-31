package net.createmod.ponder.impl.client.plugin;

import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.api.client.registration.PonderPlugin;
import net.createmod.ponder.api.client.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.Identifier;

public class DebugPonderPlugin implements PonderPlugin {
	@Override
	public String getModId() {
		return Ponder.MOD_ID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<Identifier> helper) {
		DebugScenes.registerAll(helper);
	}
}
