package com.simibubi.create.content.equipment.armor;

import java.util.EnumMap;
import java.util.Map;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;

import org.jetbrains.annotations.ApiStatus.Internal;

public class AllArmorMaterials {

	public static final ArmorMaterial COPPER = register("copper", new int[] { 2, 4, 3, 1 }, 7,
		AllSoundEvents.COPPER_ARMOR_EQUIP.getMainEventHolder(), 0.0F, 0.0F, Tags.Items.INGOTS_COPPER);

	public static final ArmorMaterial CARDBOARD = register("cardboard", new int[] { 1, 1, 1, 1 }, 4,
		SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, AllTags.AllItemTags.CARDBOARD_PLATES.tag);

	public static final ArmorMaterial NETHERITE_DIVING = withAsset(ArmorMaterials.NETHERITE, "netherite_diving");

	private static ArmorMaterial withAsset(ArmorMaterial material, String name) {
		return new ArmorMaterial(material.durability(), material.defense(), material.enchantmentValue(),
			material.equipSound(), material.toughness(), material.knockbackResistance(), material.repairIngredient(),
			net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
				Create.asResource(name)));
	}

	private static ArmorMaterial register(String name, int[] defense, int enchantmentValue,
		net.minecraft.core.Holder<SoundEvent> equipSound, float toughness, float knockbackResistance,
		TagKey<Item> repairIngredient) {
		EnumMap<ArmorType, Integer> defenses = new EnumMap<>(ArmorType.class);
		defenses.put(ArmorType.BOOTS, defense[0]);
		defenses.put(ArmorType.LEGGINGS, defense[1]);
		defenses.put(ArmorType.CHESTPLATE, defense[2]);
		defenses.put(ArmorType.HELMET, defense[3]);
		return new ArmorMaterial(15, Map.copyOf(defenses), enchantmentValue, equipSound, toughness,
			knockbackResistance, repairIngredient, net.minecraft.resources.ResourceKey.create(
			net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Create.asResource(name)));
	}

	@Internal
	public static void register(IEventBus eventBus) {
	}
}
