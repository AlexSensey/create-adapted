package com.simibubi.create.content.equipment.armor;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public class BaseArmorItem extends Item {
	protected final Identifier textureLoc;
	protected final ArmorMaterial armorMaterial;
	protected final ArmorType armorType;

	public BaseArmorItem(ArmorMaterial armorMaterial, ArmorType armorType, Properties properties, Identifier textureLoc) {
		super(properties.stacksTo(1).humanoidArmor(armorMaterial, armorType));
		this.armorMaterial = armorMaterial;
		this.armorType = armorType;
		this.textureLoc = textureLoc;
	}
}
