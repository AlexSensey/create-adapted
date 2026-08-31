package net.createmod.catnip.api.client.render;

import java.util.HashMap;
import java.util.Map;

import net.createmod.catnip.api.platform.services.PlatformHelper;
import net.minecraft.resources.Identifier;

public class SpriteShifter {
	private static final Map<String, SpriteShiftEntry> ENTRY_CACHE = new HashMap<>();

	public static SpriteShiftEntry get(Identifier originalIdentifier, Identifier targetIdentifier) {
		String key = originalIdentifier + "->" + targetIdentifier;
		if (ENTRY_CACHE.containsKey(key))
			return ENTRY_CACHE.get(key);

		SpriteShiftEntry entry = new SpriteShiftEntry();
		PlatformHelper.INSTANCE.executeOnClientOnly(() -> () -> entry.set(originalIdentifier, targetIdentifier));
		ENTRY_CACHE.put(key, entry);
		return entry;
	}
}
