package net.minecraft.world.item;

import net.minecraft.world.item.component.ItemAttributeModifiers;

public class SwordItem extends Item {
	public SwordItem(Tier material, Properties properties) {
		this(ToolMaterial.WOOD, properties);
	}

	public SwordItem(ToolMaterial material, Properties properties) {
		super(properties.sword(material, 3, -2.4F));
	}

	public static ItemAttributeModifiers createAttributes(Tier material, int attackDamage, float attackSpeed) {
		return createAttributes(ToolMaterial.WOOD, attackDamage, attackSpeed);
	}

	public static ItemAttributeModifiers createAttributes(ToolMaterial material, int attackDamage, float attackSpeed) {
		return ItemAttributeModifiers.EMPTY;
	}
}
