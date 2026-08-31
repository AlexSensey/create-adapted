package net.createmod.catnip.api.event;

import java.util.List;

import net.createmod.catnip.impl.event.CatnipEventImpl;

/// A simple event interface, allowing for callback registration.
///
/// Invocation is wrapped in a single callback instance known as an invoker.
public interface CatnipEvent<Callback> {
	/// Register a new callback to this event. Threadsafe.
	void subscribe(Callback callback);

	/// Get this event's invoker. Will always return the same instance.
	Callback invoker();

	/// Create a new simple event.
	static <Callback> CatnipEvent<Callback> create(InvokerFactory<Callback> invokerFactory) {
		return new CatnipEventImpl<>(invokerFactory);
	}

	/// Create a new [biphasic][Biphasic] event.
	static <Callback> Biphasic<Callback> biphasic(InvokerFactory<Callback> invokerFactory) {
		return new Biphasic<>(create(invokerFactory), create(invokerFactory));
	}

	/// Factory for invokers, which wraps an array of callbacks into a single one.
	@FunctionalInterface
	interface InvokerFactory<Callback> {
		/// Create an invoker wrapping the given list of callbacks.
		///
		/// The given list is an immutable view, but the backing list is live. It will be updated as callbacks are registered.
		Callback create(List<Callback> callbacks);
	}

	/// A biphasic event is a pair of identical events that are invoked before and after some operation occurs.
	record Biphasic<Callback>(CatnipEvent<Callback> pre, CatnipEvent<Callback> post) {
	}
}
