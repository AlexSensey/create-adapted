package com.simibubi.create.foundation.data;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateItemTagsProvider;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.providers.RegistrateProvider;
import com.tterrag.registrate.providers.RegistrateTagsProvider;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * Datagen-only Registrate provider construction.
 *
 * Kept outside {@link Registrate26Compat} so a normal client never has to load
 * Minecraft's datagen-only tag provider classes. The methods are reached only
 * when NeoForge actually creates providers for a GatherDataEvent.
 */
final class RegistrateDataProviders26 {

	private RegistrateDataProviders26() {}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static RegistrateProvider blockTags(ProviderType.Context context) {
		return new RegistrateTagsProvider.IntrinsicImpl(context.parent(), context.type(), "Block Tags",
			context.output(), Registries.BLOCK, context.provider(),
			block -> ((Block) block).builtInRegistryHolder().key(), context.fileHelper());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static RegistrateProvider itemTags(ProviderType.Context context) {
		RegistrateTagsProvider<Block> blockTags = (RegistrateTagsProvider<Block>) (Object)
			context.get((ProviderType) ProviderType.BLOCK_TAGS);
		return new RegistrateItemTagsProvider(context.parent(), context.type(), "Item Tags",
			context.output(), context.provider(), blockTags.contentsGetter(), context.fileHelper());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static RegistrateProvider fluidTags(ProviderType.Context context) {
		return new RegistrateTagsProvider.IntrinsicImpl(context.parent(), context.type(), "Fluid Tags",
			context.output(), Registries.FLUID, context.provider(),
			fluid -> ((Fluid) fluid).builtInRegistryHolder().key(), context.fileHelper());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static RegistrateProvider entityTags(ProviderType.Context context) {
		return new RegistrateTagsProvider.IntrinsicImpl(context.parent(), context.type(), "Entity Type Tags",
			context.output(), Registries.ENTITY_TYPE, context.provider(),
			entityType -> ((EntityType<?>) entityType).builtInRegistryHolder().key(), context.fileHelper());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static RegistrateProvider lang(ProviderType.Context context) {
		return new RegistrateLangProvider(context.parent(), context.output());
	}

}
