package net.createmod.catnip.api.platform;

import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

/// Helper class for loading services with [ServiceLoader].
public final class ServiceHelper {
	private ServiceHelper() {}

	/// Load an implementation of the given class, assuming that exactly one should exist.
	public static <T> T load(Class<T> clazz) {
		List<Provider<T>> providers = ServiceLoader.load(clazz).stream().toList();

		if (providers.isEmpty()) {
			throw new IllegalStateException("Failed to find implementation of " + clazz);
		} else if (providers.size() > 1) {
			throw new IllegalStateException("Found more than one implementation of " + clazz);
		}

		// if an error occurs during instantiation the error message is reasonable
		return providers.getFirst().get();
	}
}
