package com.simibubi.create.foundation.item.render;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class CustomRenderedItems {

	private static final Set<Item> ITEMS = new ReferenceOpenHashSet<>();
	private static final Map<Item, CustomRenderedItemModel> MODELS = new java.util.IdentityHashMap<>();
	private static final Map<Identifier, BakedModel> PARTIAL_MODELS = new java.util.HashMap<>();
	private static boolean itemsFiltered = false;

	/**
	 * Track an item that uses a subclass of {@link CustomRenderedItemModelRenderer} as its custom renderer
	 * to automatically wrap its model with {@link CustomRenderedItemModel}.
	 * @param item The item that should have its model swapped.
	 */
	public static void register(Item item) {
		ITEMS.add(item);
	}

	/**
	 * This method must not be called before item registration is finished!
	 */
	public static void forEach(Consumer<Item> consumer) {
		if (!itemsFiltered) {
			Iterator<Item> iterator = ITEMS.iterator();
			while (iterator.hasNext()) {
				Item item = iterator.next();
				if (!BuiltInRegistries.ITEM.containsValue(item)) {
					iterator.remove();
				}
			}
			itemsFiltered = true;
		}
		ITEMS.forEach(consumer);
	}

	public static void registerModel(Item item, CustomRenderedItemModel model) {
		MODELS.put(item, model);
	}

	public static CustomRenderedItemModel getModel(Item item) {
		return MODELS.get(item);
	}

	public static void registerPartialModel(Identifier id, BakedModel model) {
		PARTIAL_MODELS.put(id, model);
	}

	public static BakedModel getPartialModel(Identifier id) {
		return PARTIAL_MODELS.get(id);
	}

}
