package com.simibubi.create.content.logistics.item.filter.attribute;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.AddedByAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.BookAuthorAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.BookCopyAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.ColorAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.EnchantAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.FluidContentsAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.InItemGroupAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.InTagAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.ItemNameAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.ShulkerFillLevelAttribute;
import com.simibubi.create.foundation.recipe.CreateRecipeClientCache;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;

// TODO - Documentation
public class AllItemAttributeTypes {
	public static final ItemAttributeType
		PLACEABLE = singleton("placeable", s -> s.getItem() instanceof BlockItem),
		CONSUMABLE = singleton("consumable", s -> s.has(DataComponents.FOOD)),
		FLUID_CONTAINER = singleton("fluid_container", s -> ItemAccess.forStack(s.copy())
			.oneByOne()
			.getCapability(Capabilities.Fluid.ITEM) != null),
		ENCHANTED = singleton("enchanted", ItemStack::isEnchanted),
		MAX_ENCHANTED = singleton("max_enchanted", AllItemAttributeTypes::maxEnchanted),
		RENAMED = singleton("renamed", s -> s.has(DataComponents.CUSTOM_NAME)),
		DAMAGED = singleton("damaged", ItemStack::isDamaged),
		BADLY_DAMAGED = singleton("badly_damaged", s -> s.isDamaged() && (float) s.getDamageValue() / s.getMaxDamage() > 3 / 4f),
		NOT_STACKABLE = singleton("not_stackable", ((Predicate<ItemStack>) ItemStack::isStackable).negate()),
		EQUIPABLE = singleton("equipable", s -> {
			Equippable equippable = s.get(DataComponents.EQUIPPABLE);
			EquipmentSlot.Type type = equippable != null ? equippable.slot().getType() : EquipmentSlot.MAINHAND.getType();
			return type != EquipmentSlot.Type.HAND;
		}),
		FURNACE_FUEL = singleton("furnace_fuel", (s, w) -> w.fuelValues().burnDuration(s) > 0),
		WASHABLE = singleton("washable", AllFanProcessingTypes.SPLASHING::canProcess),
		HAUNTABLE = singleton("hauntable", AllFanProcessingTypes.HAUNTING::canProcess),
		CRUSHABLE = singleton("crushable", (s, w) -> testRecipe(s, w, AllRecipeTypes.CRUSHING.getType())
			|| testRecipe(s, w, AllRecipeTypes.MILLING.getType())),
		SMELTABLE = singleton("smeltable", (s, w) -> w.recipeAccess()
			.propertySet(RecipePropertySet.FURNACE_INPUT).test(s)),
		SMOKABLE = singleton("smokable", (s, w) -> w.recipeAccess()
			.propertySet(RecipePropertySet.SMOKER_INPUT).test(s)),
		BLASTABLE = singleton("blastable", (s, w) -> w.recipeAccess()
			.propertySet(RecipePropertySet.BLAST_FURNACE_INPUT).test(s)),
		COMPOSTABLE = singleton("compostable", s -> ComposterBlock.getValue(s) > 0),

	IN_TAG = register("in_tag", new InTagAttribute.Type()),
		IN_ITEM_GROUP = register("in_item_group", new InItemGroupAttribute.Type()),
		ADDED_BY = register("added_by", new AddedByAttribute.Type()),
		HAS_ENCHANT = register("has_enchant", new EnchantAttribute.Type()),
		SHULKER_FILL_LEVEL = register("shulker_fill_level", new ShulkerFillLevelAttribute.Type()),
		HAS_COLOR = register("has_color", new ColorAttribute.Type()),
		HAS_FLUID = register("has_fluid", new FluidContentsAttribute.Type()),
		HAS_NAME = register("has_name", new ItemNameAttribute.Type()),
		BOOK_AUTHOR = register("book_author", new BookAuthorAttribute.Type()),
		BOOK_COPY = register("book_copy", new BookCopyAttribute.Type());

	private static <T extends Recipe<SingleRecipeInput>> boolean testRecipe(ItemStack s, Level w, RecipeType<T> type) {
		SingleRecipeInput input = new SingleRecipeInput(s.copy());
		if (w instanceof ServerLevel serverLevel)
			return serverLevel.recipeAccess().getRecipeFor(type, input, w).isPresent();
		return CreateRecipeClientCache.find(type, input, w).isPresent();
	}

	private static boolean maxEnchanted(ItemStack s) {
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : s.getTagEnchantments().entrySet()) {
			if (entry.getKey().value().getMaxLevel() <= entry.getIntValue())
				return true;
		}

		return false;
	}

	private static ItemAttributeType singleton(String id, Predicate<ItemStack> predicate) {
		return register(id, new SingletonItemAttribute.Type(type -> new SingletonItemAttribute(type, (stack, level) -> predicate.test(stack), id)));
	}

	private static ItemAttributeType singleton(String id, BiPredicate<ItemStack, Level> predicate) {
		return register(id, new SingletonItemAttribute.Type(type -> new SingletonItemAttribute(type, predicate, id)));
	}

	private static ItemAttributeType register(String id, ItemAttributeType type) {
		return Registry.register(CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE, Create.asResource(id), type);
	}

	public static void init() {
	}

}
