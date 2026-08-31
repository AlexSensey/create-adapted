package net.minecraft.world.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public interface Tier {
	int getUses();

	float getSpeed();

	float getAttackDamageBonus();

	TagKey<Block> getIncorrectBlocksForDrops();

	int getEnchantmentValue();

	Ingredient getRepairIngredient();
}
