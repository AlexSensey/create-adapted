package com.simibubi.create.foundation.data;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.simibubi.create.foundation.data.recipe.CommonMetal;
import com.simibubi.create.foundation.data.recipe.Mods;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TagGen {
	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> axeOrPickaxe() {
		return b -> b.tag(BlockTags.MINEABLE_WITH_AXE)
			.tag(BlockTags.MINEABLE_WITH_PICKAXE);
	}

	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> axeOnly() {
		return b -> b.tag(BlockTags.MINEABLE_WITH_AXE);
	}

	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> pickaxeOnly() {
		return b -> b.tag(BlockTags.MINEABLE_WITH_PICKAXE);
	}

	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, ItemBuilder<BlockItem, BlockBuilder<T, P>>> tagBlockAndItem(
		CommonMetal.ItemLikeTag tag) {
		return tagBlockAndItem(Map.of(tag.blocks(), tag.items()));
	}

	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, ItemBuilder<BlockItem, BlockBuilder<T, P>>> tagBlockAndItem(
		TagKey<Block> blockTag, TagKey<Item> itemTag) {
		return tagBlockAndItem(Map.of(blockTag, itemTag));
	}

	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, ItemBuilder<BlockItem, BlockBuilder<T, P>>> tagBlockAndItem(
		Map<TagKey<Block>, TagKey<Item>> tags) {
		return b -> {
			for (TagKey<Block> blockTag : tags.keySet()) {
				b.tag(blockTag);
			}
			ItemBuilder<BlockItem, BlockBuilder<T, P>> item = b.item();
			for (TagKey<Item> itemTag : tags.values()) {
				item.tag(itemTag);
			}
			return item;
		};
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <T extends TagAppender<?>> T addOptional(T appender, Mods mod, String id) {
		if (appender instanceof CreateTagAppender<?> createAppender)
			createAppender.addOptionalElement(mod.asResource(id));
		else
			((TagAppender) appender).addOptional(ResourceKey.create(null, mod.asResource(id)));
		return appender;
	}

	public static <T extends TagAppender<?>> T addOptional(T appender, Mods mod, List<String> ids) {
		for (String id : ids) {
			addOptional(appender, mod, id);
		}
		return appender;
	}

	public static class CreateTagsProvider<T> {
		private final RegistrateTagsProvider<T> provider;
		private final Function<T, ResourceKey<T>> keyExtractor;

		public CreateTagsProvider(RegistrateTagsProvider<T> provider, Function<T, Holder.Reference<T>> refExtractor) {
			this.provider = provider;
			this.keyExtractor = refExtractor.andThen(Holder.Reference::key);
		}

		public CreateTagAppender<T> tag(TagKey<T> tag) {
			return new CreateTagAppender<>(provider.addTag(tag), keyExtractor, provider.registry());
		}
	}

	public static class CreateTagAppender<T> implements TagAppender<T> {

		private final TagAppender<T> delegate;
		private final Function<T, ResourceKey<T>> keyExtractor;
		private final ResourceKey<? extends Registry<T>> registry;

		public CreateTagAppender(TagAppender<T> delegate, Function<T, ResourceKey<T>> pKeyExtractor,
			ResourceKey<? extends Registry<T>> registry) {
			this.delegate = delegate;
			this.keyExtractor = pKeyExtractor;
			this.registry = registry;
		}

		@Override
		public CreateTagAppender<T> add(ResourceKey<T> key) {
			delegate.add(key);
			return this;
		}

		public CreateTagAppender<T> add(T entry) {
			this.add(this.keyExtractor.apply(entry));
			return this;
		}

		@SafeVarargs
		public final CreateTagAppender<T> add(T... entries) {
			Stream.<T>of(entries)
				.map(this.keyExtractor)
				.forEach(this::add);
			return this;
		}

		@SafeVarargs
		public final CreateTagAppender<T> remove(T... entries) {
			Stream.<T>of(entries)
				.map(this.keyExtractor)
				.forEach(this::remove);
			return this;
		}

		@Override
		public CreateTagAppender<T> addOptional(ResourceKey<T> key) {
			delegate.addOptional(key);
			return this;
		}

		public CreateTagAppender<T> addOptionalElement(Identifier id) {
			delegate.addOptional(ResourceKey.create(registry, id));
			return this;
		}

		@Override
		public CreateTagAppender<T> addTag(TagKey<T> tag) {
			delegate.addTag(tag);
			return this;
		}

		@Override
		public CreateTagAppender<T> addOptionalTag(TagKey<T> tag) {
			delegate.addOptionalTag(tag);
			return this;
		}

		@Override
		public CreateTagAppender<T> add(TagEntry entry) {
			delegate.add(entry);
			return this;
		}

		@Override
		public CreateTagAppender<T> remove(ResourceKey<T> key) {
			delegate.remove(key);
			return this;
		}

		@Override
		public CreateTagAppender<T> replace(boolean value) {
			delegate.replace(value);
			return this;
		}

		public CreateTagAppender<T> remove(TagKey<T> tag) {
			delegate.remove(tag);
			return this;
		}

	}
}
