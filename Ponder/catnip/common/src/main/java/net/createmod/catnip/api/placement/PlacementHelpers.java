package net.createmod.catnip.api.placement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlacementHelpers {
	private static final List<IPlacementHelper> registry = new ArrayList<>();
	private static final List<IPlacementHelper> registryView = Collections.unmodifiableList(registry);

	public static <T extends IPlacementHelper> T register(T helper) {
		registry.add(helper);
		return helper;
	}

	public static List<IPlacementHelper> get() {
		return registryView;
	}
}
