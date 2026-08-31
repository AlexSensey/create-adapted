package net.createmod.ponder.api.client;

/**
 * Used for simulating BE's in a client-only setting (like Ponder)
 */
public interface VirtualBlockEntity {

	void markVirtual();

	boolean isVirtual();

}
