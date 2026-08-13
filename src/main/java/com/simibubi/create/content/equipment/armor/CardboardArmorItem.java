package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.Create;

public class CardboardArmorItem extends BaseArmorItem {
	public CardboardArmorItem(net.minecraft.world.item.equipment.ArmorType type, Properties properties) {
		super(AllArmorMaterials.CARDBOARD, type, properties, Create.asResource("cardboard"));
	}
}
