package net.minecraft.world.item;

import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.DispenserBlock;
import org.jetbrains.annotations.Nullable;

public class ArmorItem extends Item {
	public static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = DispenseItemBehavior.NOOP;

	private final Holder<ArmorMaterial> material;
	private final Type type;

	public ArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
		super(properties.stacksTo(1).humanoidArmor(material.value().unwrap(), type.armorType));
		this.material = material;
		this.type = type;
		DispenserBlock.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
	}

	public Holder<ArmorMaterial> getMaterial() {
		return material;
	}

	public Type getType() {
		return type;
	}

	public boolean canEquip(ItemStack stack, EquipmentSlot slot, Entity entity) {
		return slot == type.slot;
	}

	public @Nullable Identifier getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		return null;
	}

	public enum Type {
		BOOTS(EquipmentSlot.FEET, net.minecraft.world.item.equipment.ArmorType.BOOTS),
		LEGGINGS(EquipmentSlot.LEGS, net.minecraft.world.item.equipment.ArmorType.LEGGINGS),
		CHESTPLATE(EquipmentSlot.CHEST, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE),
		HELMET(EquipmentSlot.HEAD, net.minecraft.world.item.equipment.ArmorType.HELMET);

		private final EquipmentSlot slot;
		private final net.minecraft.world.item.equipment.ArmorType armorType;

		Type(EquipmentSlot slot, net.minecraft.world.item.equipment.ArmorType armorType) {
			this.slot = slot;
			this.armorType = armorType;
		}

		public EquipmentSlot getSlot() {
			return slot;
		}
	}
}
