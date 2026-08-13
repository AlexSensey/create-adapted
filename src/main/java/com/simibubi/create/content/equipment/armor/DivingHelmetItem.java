package com.simibubi.create.content.equipment.armor;

import java.util.List;

import com.simibubi.create.foundation.advancement.AllAdvancements;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber
public class DivingHelmetItem extends BaseArmorItem {
	public static final EquipmentSlot SLOT = EquipmentSlot.HEAD;
	public static final ArmorType TYPE = ArmorType.HELMET;

	public DivingHelmetItem(ArmorMaterial material, Properties properties, Identifier textureLoc) {
		super(material, TYPE, properties, textureLoc);
	}

	@Override
	public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
		if (enchantment.is(Enchantments.AQUA_AFFINITY))
			return false;
		return super.supportsEnchantment(stack, enchantment);
	}

	public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
		if (enchantment.is(Enchantments.AQUA_AFFINITY))
			return 1;
		return super.getEnchantmentLevel(stack, enchantment);
	}

	@Override
	public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
		ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(super.getAllEnchantments(stack, lookup));
		enchants.set(lookup.getOrThrow(Enchantments.AQUA_AFFINITY), 1);
		return enchants.toImmutable();
	}

	public static boolean isWornBy(Entity entity) {
		return !getWornItem(entity).isEmpty();
	}

	public static ItemStack getWornItem(Entity entity) {
		if (!(entity instanceof LivingEntity livingEntity)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = livingEntity.getItemBySlot(SLOT);
		if (!(stack.getItem() instanceof DivingHelmetItem)) {
			return ItemStack.EMPTY;
		}
		return stack;
	}

	@SubscribeEvent
	public static void breatheUnderwater(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof LivingEntity entity))
			return;

		Level level = entity.level();

		ItemStack helmet = getWornItem(entity);
		if (helmet.isEmpty())
			return;

		boolean lavaDiving = entity.isInLava() || entity.isEyeInFluid(FluidTags.LAVA);
		boolean waterDiving = entity.isEyeInFluid(FluidTags.WATER);
		boolean inBubbleColumn = level.getBlockState(entity.blockPosition())
			.is(Blocks.BUBBLE_COLUMN);
		if ((!waterDiving && !lavaDiving) || inBubbleColumn)
			return;
		if (!lavaDiving && (MobEffectUtil.hasWaterBreathing(entity)
			|| entity instanceof Player player && player.getAbilities().invulnerable))
			return;

		List<ItemStack> backtanks = BacktankUtil.getAllWithAir(entity);
		if (backtanks.isEmpty())
			return;

		if (lavaDiving) {
			if (entity instanceof ServerPlayer sp)
				AllAdvancements.DIVING_SUIT_LAVA.awardTo(sp);
		}

		if (!level.isClientSide() && level.getGameTime() % 20 == 0)
			BacktankUtil.consumeAir(entity, backtanks.get(0), 1);

		if (lavaDiving)
			return;

		if (entity instanceof ServerPlayer sp)
			AllAdvancements.DIVING_SUIT.awardTo(sp);

		entity.setAirSupply(entity.getMaxAirSupply());
	}
}
