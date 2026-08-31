package net.createmod.ponder.api.registration;

import java.util.function.Function;

public interface PonderTagRegistrationHelper<T> {
	default <R> PonderTagRegistrationHelper<R> withKeyFunction(Function<R, ?> keyFunction) {
		return new PonderTagRegistrationHelper<>() {
		};
	}

	default TagBuilder<T> registerTag(Object tag) {
		return new TagBuilder<>();
	}

	default TagBuilder<T> addToTag(Object tag) {
		return new TagBuilder<>();
	}

	class TagBuilder<T> {
		public TagBuilder<T> addToIndex() {
			return this;
		}

		public TagBuilder<T> item(Object item) {
			return this;
		}

		public TagBuilder<T> item(Object item, boolean large, boolean icon) {
			return this;
		}

		public TagBuilder<T> title(String title) {
			return this;
		}

		public TagBuilder<T> description(String description) {
			return this;
		}

		public TagBuilder<T> register() {
			return this;
		}

		public TagBuilder<T> add(Object entry) {
			return this;
		}
	}
}
