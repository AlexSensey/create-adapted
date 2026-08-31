package com.simibubi.create.content.logistics.factoryBoard;

import net.createmod.catnip.api.client.gui.ScreenOpener;

public final class FactoryPanelClient {

	private FactoryPanelClient() {}

	public static void openScreen(FactoryPanelBehaviour behaviour) {
		ScreenOpener.open(new FactoryPanelScreen(behaviour));
	}
}
