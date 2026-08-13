package dev.engine_room.flywheel.lib.model.baked;

import org.jetbrains.annotations.ApiStatus;

import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;

@ApiStatus.Internal
public final class PartialModelEventHandler {
	private PartialModelEventHandler() {
	}

	public static void onRegisterStandalone(ModelEvent.RegisterStandalone event) {
		for (PartialModel partial : PartialModel.ALL.values()) {
			event.register(partial.modelKey,
				SimpleUnbakedStandaloneModel.simpleModelWrapper(partial.modelLocation()));
		}
	}
}
