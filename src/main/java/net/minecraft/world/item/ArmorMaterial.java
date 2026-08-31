package net.minecraft.world.item;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.equipment.ArmorType;

public record ArmorMaterial(
	Map<ArmorItem.Type, Integer> defense,
	int enchantmentValue,
	Holder<SoundEvent> equipSound,
	Supplier<Ingredient> repairIngredient,
	List<Layer> layers,
	float toughness,
	float knockbackResistance
) {
	public net.minecraft.world.item.equipment.ArmorMaterial unwrap() {
		return new net.minecraft.world.item.equipment.ArmorMaterial(
			15,
			Map.of(
				ArmorType.BOOTS, defense.getOrDefault(ArmorItem.Type.BOOTS, 0),
				ArmorType.LEGGINGS, defense.getOrDefault(ArmorItem.Type.LEGGINGS, 0),
				ArmorType.CHESTPLATE, defense.getOrDefault(ArmorItem.Type.CHESTPLATE, 0),
				ArmorType.HELMET, defense.getOrDefault(ArmorItem.Type.HELMET, 0)
			),
			enchantmentValue,
			equipSound,
			toughness,
			knockbackResistance,
			net.minecraft.tags.ItemTags.REPAIRS_LEATHER_ARMOR,
			net.minecraft.world.item.equipment.EquipmentAssets.LEATHER
		);
	}

	public record Layer(Identifier id) {
	}
}
