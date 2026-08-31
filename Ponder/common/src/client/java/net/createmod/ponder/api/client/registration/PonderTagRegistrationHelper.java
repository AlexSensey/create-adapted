package net.createmod.ponder.api.client.registration;

import java.util.function.Function;

import net.minecraft.resources.Identifier;

public interface PonderTagRegistrationHelper<T> {

	<S> PonderTagRegistrationHelper<S> withKeyFunction(Function<S, T> keyGen);

	TagBuilder registerTag(Identifier identifier);

	TagBuilder registerTag(String id);

	void addTagToComponent(T component, Identifier tag);

	MultiTagBuilder.Tag<T> addToTag(Identifier tag);

	MultiTagBuilder.Tag<T> addToTag(Identifier... tags);

	MultiTagBuilder.Component addToComponent(T component);

	MultiTagBuilder.Component addToComponent(T... component);

}
