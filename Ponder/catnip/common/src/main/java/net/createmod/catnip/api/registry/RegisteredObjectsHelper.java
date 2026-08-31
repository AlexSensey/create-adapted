package net.createmod.catnip.api.registry;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder.Reference;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

public class RegisteredObjectsHelper {
	public static <V> Identifier getKeyOrThrow(Registry<V> registry, V value) {
		Identifier key = registry.getKey(value);
		if (key == null) {
			throw new IllegalArgumentException("Could not get key for value " + value + "!");
		}
		return key;
	}

	public static Identifier getKeyOrThrow(Block value) {
		return getKeyOrThrow(BuiltInRegistries.BLOCK, value);
	}

	public static Identifier getKeyOrThrow(Item value) {
		return getKeyOrThrow(BuiltInRegistries.ITEM, value);
	}

	public static Identifier getKeyOrThrow(Fluid value) {
		return getKeyOrThrow(BuiltInRegistries.FLUID, value);
	}

	public static Identifier getKeyOrThrow(EntityType<?> value) {
		return getKeyOrThrow(BuiltInRegistries.ENTITY_TYPE, value);
	}

	public static Identifier getKeyOrThrow(BlockEntityType<?> value) {
		return getKeyOrThrow(BuiltInRegistries.BLOCK_ENTITY_TYPE, value);
	}

	public static Identifier getKeyOrThrow(Potion value) {
		return getKeyOrThrow(BuiltInRegistries.POTION, value);
	}

	public static Identifier getKeyOrThrow(ParticleType<?> value) {
		return getKeyOrThrow(BuiltInRegistries.PARTICLE_TYPE, value);
	}

	public static Identifier getKeyOrThrow(RecipeSerializer<?> value) {
		return getKeyOrThrow(BuiltInRegistries.RECIPE_SERIALIZER, value);
	}

	public static Optional<Item> getItem(Identifier id) {
		return BuiltInRegistries.ITEM.get(id).map(Reference::value);
	}

	public static Optional<Block> getBlock(Identifier id) {
		return BuiltInRegistries.BLOCK.get(id).map(Reference::value);
	}

	@Nullable
	public static ItemLike getItemOrBlock(Identifier id) {
		Optional<Item> item = getItem(id);
		if (item.isPresent())
			return item.get();

		Optional<Block> block = getBlock(id);
		return block.orElse(null);
	}

	public static Identifier getKeyOrThrow(ItemLike itemLike) {
		if (itemLike instanceof Item item) {
			return getKeyOrThrow(item);
		} else if (itemLike instanceof Block block) {
			return getKeyOrThrow(block);
		}

		throw new IllegalArgumentException("Could not get key for itemLike " + itemLike + "!");
	}
}
