package net.minecraft.client.renderer.item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemProperties {
	@FunctionalInterface
	public interface ItemPropertyFunction {
		float call(ItemStack stack, Level level, LivingEntity entity, int seed);
	}

	public static void register(Item item, Identifier id, ItemPropertyFunction function) {
	}
}
