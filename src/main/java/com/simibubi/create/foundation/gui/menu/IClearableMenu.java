package com.simibubi.create.foundation.gui.menu;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;

public interface IClearableMenu {

	default void sendClearPacket() {
		ClientNetworkHelper.INSTANCE.sendToServer(ClearMenuPacket.INSTANCE);
	}

	void clearContents();

}
