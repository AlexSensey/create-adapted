package net.createmod.catnip.impl.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.createmod.catnip.api.event.CatnipEvent;

public final class CatnipEventImpl<Callback> implements CatnipEvent<Callback> {
	private final List<Callback> callbacks;
	private final Callback invoker;

	public CatnipEventImpl(InvokerFactory<Callback> invokerFactory) {
		this.callbacks = new ArrayList<>();
		this.invoker = invokerFactory.create(Collections.unmodifiableList(this.callbacks));
	}

	@Override
	public synchronized void subscribe(Callback callback) {
		this.callbacks.add(callback);
	}

	@Override
	public Callback invoker() {
		return this.invoker;
	}
}
