package net.minecraft.world.item;

import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Direct;
import net.minecraft.core.component.DataComponentMap;

public interface ArmorMaterials {
	Holder<ArmorMaterial> NETHERITE = new Direct<>(wrap(net.minecraft.world.item.equipment.ArmorMaterials.NETHERITE),
		DataComponentMap.EMPTY);

	private static ArmorMaterial wrap(net.minecraft.world.item.equipment.ArmorMaterial material) {
		return new ArmorMaterial(
			java.util.Map.of(
				ArmorItem.Type.BOOTS, material.defense().getOrDefault(net.minecraft.world.item.equipment.ArmorType.BOOTS, 0),
				ArmorItem.Type.LEGGINGS, material.defense().getOrDefault(net.minecraft.world.item.equipment.ArmorType.LEGGINGS, 0),
				ArmorItem.Type.CHESTPLATE, material.defense().getOrDefault(net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, 0),
				ArmorItem.Type.HELMET, material.defense().getOrDefault(net.minecraft.world.item.equipment.ArmorType.HELMET, 0)
			),
			material.enchantmentValue(),
			material.equipSound(),
			() -> net.minecraft.world.item.crafting.Ingredient.of(java.util.stream.Stream.empty()),
			java.util.List.of(),
			material.toughness(),
			material.knockbackResistance()
		);
	}
}
