package net.createmod.ponder.impl.client;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.api.client.PonderIndex;
import net.createmod.ponder.impl.client.gui.PonderIndexScreen;
import net.createmod.ponder.impl.client.gui.PonderTagIndexScreen;
import net.createmod.ponder.impl.client.gui.PonderUI;
import net.minecraft.resources.Identifier;

public class SimplePonderActions {

	public static void openPonder(String value) {
		if (value.equals("index") || value.equals("ponder:index")) {
			ScreenOpener.transitionTo(new PonderIndexScreen());
			return;
		}

		if (value.equals("ponder:tags")) {
			ScreenOpener.transitionTo(new PonderTagIndexScreen());
			return;
		}

		Identifier id = Identifier.parse(value);
		if (!PonderIndex.getSceneAccess().doScenesExistForId(id)) {
			Ponder.LOGGER.error("Could not find ponder scenes for item: " + id);
			return;
		}

		ScreenOpener.transitionTo(PonderUI.of(id));

	}

	public static void reloadPonder(String value) {
		PonderIndex.reload();
	}

}
