package net.createmod.ponder.api.registration;

public interface PonderPlugin {
	default String getModId() {
		return "";
	}

	default void registerScenes(PonderSceneRegistrationHelper<?> helper) {
	}

	default void registerTags(PonderTagRegistrationHelper<?> helper) {
	}
}
