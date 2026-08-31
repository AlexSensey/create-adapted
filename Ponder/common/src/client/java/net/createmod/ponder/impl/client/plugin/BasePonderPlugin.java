package net.createmod.ponder.impl.client.plugin;

import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.api.client.registration.PonderPlugin;
import net.createmod.ponder.api.client.registration.SharedTextRegistrationHelper;

public class BasePonderPlugin implements PonderPlugin {

	@Override
	public String getModId() {
		return Ponder.MOD_ID;
	}

	@Override
	public void registerSharedText(SharedTextRegistrationHelper helper) {
		helper.registerSharedText("sneak_and", "Sneak +");
		helper.registerSharedText("ctrl_and", "Ctrl +");
	}
}
