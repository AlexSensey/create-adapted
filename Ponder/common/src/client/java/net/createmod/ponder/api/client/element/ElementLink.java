package net.createmod.ponder.api.client.element;

import java.util.UUID;

public interface ElementLink<T extends PonderElement> {
	UUID getId();

	T cast(PonderElement e);
}
