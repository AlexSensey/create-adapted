package net.createmod.ponder.api.client.registration;

import net.minecraft.resources.Identifier;

public interface MultiTagBuilder {
	interface Tag<T> {
		Tag<T> add(T component);
	}

	interface Component {
		Component add(Identifier tag);
	}
}
