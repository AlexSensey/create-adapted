package com.simibubi.create.foundation.data;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateProvider;
import com.tterrag.registrate.providers.RegistrateLookupFillerProvider;
import com.tterrag.registrate.providers.RegistrateTagsProvider;

import net.minecraft.data.CachedOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.fml.LogicalSide;

public class Registrate26Compat {

	public static String makeDescriptionId(String type, Identifier id) {
		return Util.makeDescriptionId(type, id);
	}

	public static String makeDescriptionIdLegacy(String type, Identifier id) {
		return Util.makeDescriptionId(type, id);
	}

	public static ResourceKey<EntityType<?>> entityTypeKey(AbstractBuilder builder, String id) {
		return ResourceKey.create(Registries.ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(builder.getOwner().getModid(), id));
	}

	public static boolean includeServer(GatherDataEvent event) {
		return event instanceof GatherDataEvent.Server;
	}

	public static boolean includeClient(GatherDataEvent event) {
		// The compatibility run requests --all from the concrete server-data
		// entrypoint. PackOutput can emit assets as well as data, so allow the
		// restored client providers to participate in that combined run.
		return event instanceof GatherDataEvent.Server || event instanceof GatherDataEvent.Client;
	}

	public static ExistingFileHelper existingFileHelper(GatherDataEvent event) {
		return new ExistingFileHelper();
	}

	public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BlockEntityType.BlockEntitySupplier<? extends T> supplier, Block[] blocks) {
		return new BlockEntityType<>(supplier, blocks);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ProviderType noopProviderType(String name) {
		ProviderType type = new ProviderType<RegistrateProvider>() {
			@Override
			public RegistrateProvider create(AbstractRegistrate<?> parent, GatherDataEvent event,
				Map<ProviderType<?>, RegistrateProvider> existing) {
				return new NoopRegistrateProvider(name, event.getLookupProvider());
			}
		};
		return ProviderType.register(name, type);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ProviderType blockTagsProviderType() {
		return lazyTagProviderType("tags/block", Registries.BLOCK, RegistrateDataProviders26::blockTags);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ProviderType itemTagsProviderType() {
		return lazyTagProviderType("tags/item", Registries.ITEM, RegistrateDataProviders26::itemTags);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ProviderType fluidTagsProviderType() {
		return lazyTagProviderType("tags/fluid", Registries.FLUID, RegistrateDataProviders26::fluidTags);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ProviderType entityTagsProviderType() {
		return lazyTagProviderType("tags/entity", Registries.ENTITY_TYPE, RegistrateDataProviders26::entityTags);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ProviderType langProviderType() {
		return lazyProviderType("lang", RegistrateDataProviders26::lang);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ProviderType lazyProviderType(String name,
		java.util.function.Function<ProviderType.Context, RegistrateProvider> factory) {
		ProviderType.DependencyAwareProviderType<RegistrateProvider> type = context ->
			factory.apply((ProviderType.Context) context);
		return ProviderType.registerProvider(name, type);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ProviderType lazyTagProviderType(String name, ResourceKey<? extends Registry<?>> registry,
		java.util.function.Function<ProviderType.Context, RegistrateProvider> factory) {
		ProviderType.DependencyAwareProviderType<RegistrateTagsProvider> type = context ->
			(RegistrateTagsProvider) factory.apply((ProviderType.Context) context);
		return ProviderType.registerTag(name, (ResourceKey) registry, type);
	}

	private record NoopRegistrateProvider(String name, CompletableFuture<HolderLookup.Provider> registries)
		implements RegistrateLookupFillerProvider {
		@Override
		public LogicalSide getSide() {
			return LogicalSide.SERVER;
		}

		@Override
		public CompletableFuture<?> run(CachedOutput output) {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<HolderLookup.Provider> getFilledProvider() {
			return registries;
		}

		@Override
		public String getName() {
			return "Create compatibility provider: " + name;
		}
	}
}
