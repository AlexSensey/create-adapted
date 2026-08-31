package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;

import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Compatibility shim for libraries compiled against pre-26 datagen APIs.
 */
public abstract class IntrinsicHolderTagsProvider<T> extends TagsProvider<T> {
	private final Function<T, ResourceKey<T>> keyExtractor;

	protected IntrinsicHolderTagsProvider(PackOutput output, ResourceKey<? extends Registry<T>> registryKey,
										  CompletableFuture<HolderLookup.Provider> lookupProvider,
										  Function<T, ResourceKey<T>> keyExtractor, String modId,
										  ExistingFileHelper existingFileHelper) {
		super(output, registryKey, lookupProvider, modId);
		this.keyExtractor = keyExtractor;
	}

	@Override
	protected IntrinsicTagAppender<T> tag(TagKey<T> tag) {
		return new IntrinsicTagAppender<>(super.tag(tag), keyExtractor);
	}

	public static class IntrinsicTagAppender<T> implements TagAppender<T> {
		private final TagAppender<T> delegate;
		private final Function<T, ResourceKey<T>> keyExtractor;

		public IntrinsicTagAppender(TagAppender<T> delegate, Function<T, ResourceKey<T>> keyExtractor) {
			this.delegate = delegate;
			this.keyExtractor = keyExtractor;
		}

		public IntrinsicTagAppender<T> add(T entry) {
			return add(keyExtractor.apply(entry));
		}

		@Override
		public IntrinsicTagAppender<T> add(ResourceKey<T> key) {
			delegate.add(key);
			return this;
		}

		@Override
		public IntrinsicTagAppender<T> addOptional(ResourceKey<T> key) {
			delegate.addOptional(key);
			return this;
		}

		@Override
		public IntrinsicTagAppender<T> addTag(TagKey<T> tag) {
			delegate.addTag(tag);
			return this;
		}

		@Override
		public IntrinsicTagAppender<T> addOptionalTag(TagKey<T> tag) {
			delegate.addOptionalTag(tag);
			return this;
		}

		@Override
		public IntrinsicTagAppender<T> add(TagEntry entry) {
			delegate.add(entry);
			return this;
		}

		@Override
		public IntrinsicTagAppender<T> replace(boolean value) {
			delegate.replace(value);
			return this;
		}

		@Override
		public IntrinsicTagAppender<T> remove(ResourceKey<T> key) {
			delegate.remove(key);
			return this;
		}

		@Override
		public IntrinsicTagAppender<T> remove(TagKey<T> tag) {
			delegate.remove(tag);
			return this;
		}
	}
}
