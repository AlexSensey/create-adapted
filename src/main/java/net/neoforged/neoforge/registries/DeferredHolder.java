package net.neoforged.neoforge.registries;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Either;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import net.neoforged.neoforge.registries.datamaps.DataMapType;

public class DeferredHolder<R, T extends R> implements Holder<R>, java.util.function.Supplier<T> {
	protected final ResourceKey<R> key;
	private Holder<R> holder;

	public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<? extends Registry<R>> registryKey,
															   Identifier key) {
		return new DeferredHolder<>(ResourceKey.create(registryKey, key));
	}

	public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<? extends Registry<R>> registryKey,
															   ResourceLocation key) {
		return create(registryKey, key.asIdentifier());
	}

	public static <R, T extends R> DeferredHolder<R, T> create(Identifier registryName, Identifier key) {
		return create(ResourceKey.createRegistryKey(registryName), key);
	}

	public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<R> key) {
		return new DeferredHolder<>(key);
	}

	protected DeferredHolder(ResourceKey<R> key) {
		this.key = key;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T value() {
		if (holder != null)
			return (T) holder.value();
		return (T) getRegistry().getValue(key);
	}

	@Override
	public T get() {
		return value();
	}

	public Optional<T> asOptional() {
		return Optional.ofNullable(value());
	}

	@SuppressWarnings("unchecked")
	protected Registry<R> getRegistry() {
		return (Registry<R>) BuiltInRegistries.REGISTRY.getValue(key.registry());
	}

	protected final void bind(boolean throwOnMissingRegistry) {
		Registry<R> registry = getRegistry();
		holder = registry != null ? registry.get(key).orElse(null) : null;
		if (holder == null && throwOnMissingRegistry)
			throw new IllegalStateException("Unable to bind " + key);
	}

	public Identifier getId() {
		return key.identifier();
	}

	public ResourceKey<R> getKey() {
		return key;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof DeferredHolder<?, ?> holder && holder.key.equals(key);
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public String toString() {
		return "DeferredHolder[" + key + "]";
	}

	@Override
	public boolean isBound() {
		return holder != null || getRegistry().containsKey(key);
	}

	@Override
	public boolean areComponentsBound() {
		return holder != null && holder.areComponentsBound();
	}

	@Override
	public DataComponentMap components() {
		return holder != null ? holder.components() : DataComponentMap.EMPTY;
	}

	@Override
	public boolean is(Identifier id) {
		return key.identifier().equals(id);
	}

	@Override
	public boolean is(ResourceKey<R> key) {
		return this.key.equals(key);
	}

	@Override
	public boolean is(Predicate<ResourceKey<R>> predicate) {
		return predicate.test(key);
	}

	@Override
	public boolean is(TagKey<R> tag) {
		return holder != null && holder.is(tag);
	}

	@Override
	public boolean is(Holder<R> holder) {
		return equals(holder);
	}

	public <Z> Z getData(DataMapType<R, Z> type) {
		return holder != null ? holder.getData(type) : null;
	}

	@Override
	public Stream<TagKey<R>> tags() {
		return holder != null ? holder.tags() : Stream.empty();
	}

	@Override
	public Either<ResourceKey<R>, R> unwrap() {
		return Either.left(key);
	}

	@Override
	public Optional<ResourceKey<R>> unwrapKey() {
		return Optional.of(key);
	}

	@Override
	public Kind kind() {
		return Kind.REFERENCE;
	}

	@Override
	public boolean canSerializeIn(HolderOwner<R> owner) {
		return holder == null || holder.canSerializeIn(owner);
	}

	public Holder<R> getDelegate() {
		return holder;
	}
}
