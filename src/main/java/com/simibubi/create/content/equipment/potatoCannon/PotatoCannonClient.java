package com.simibubi.create.content.equipment.potatoCannon;

import java.util.function.Consumer;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem.Ammo;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class PotatoCannonClient {
	private PotatoCannonClient() {
	}

	public static void dontAnimateItem(InteractionHand hand) {
		CreateClient.POTATO_CANNON_RENDER_HANDLER.dontAnimateItem(hand);
	}

	public static void appendHoverText(ItemStack stack, TooltipContext context, Consumer<Component> tooltip) {
		if (!(Minecraft.getInstance().player instanceof Player player))
			return;

		Ammo ammo = PotatoCannonItem.getAmmo(player, stack);
		if (ammo == null)
			return;

		HolderLookup.Provider registries = context.registries();
		if (registries == null)
			return;

		HolderLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
		int power = stack.getEnchantmentLevel(lookup.getOrThrow(Enchantments.POWER));
		int punch = stack.getEnchantmentLevel(lookup.getOrThrow(Enchantments.PUNCH));
		float damageMultiplier = 1 + power * .2f;
		float additionalKnockback = punch * .5f;

		tooltip.accept(CommonComponents.EMPTY);
		tooltip.accept(ammo.stack().getHoverName().copy()
			.append(Component.literal(":"))
			.withStyle(ChatFormatting.GRAY));

		ChatFormatting green = ChatFormatting.GREEN;
		ChatFormatting darkGreen = ChatFormatting.DARK_GREEN;
		float damageValue = ammo.type().damage() * damageMultiplier;
		MutableComponent damage = Component.literal(damageValue == Mth.floor(damageValue)
			? Integer.toString(Mth.floor(damageValue)) : Float.toString(damageValue))
			.withStyle(damageMultiplier > 1 ? green : darkGreen);
		MutableComponent reloadTicks = Component.literal(Integer.toString(ammo.type().reloadTicks()))
			.withStyle(darkGreen);
		MutableComponent knockback = Component.literal(Float.toString(ammo.type().knockback() + additionalKnockback))
			.withStyle(additionalKnockback > 0 ? green : darkGreen);

		tooltip.accept(CommonComponents.space().copy()
			.append(CreateLang.translateDirect("potato_cannon.ammo.attack_damage", damage).withStyle(darkGreen)));
		tooltip.accept(CommonComponents.space().copy()
			.append(CreateLang.translateDirect("potato_cannon.ammo.reload_ticks", reloadTicks).withStyle(darkGreen)));
		tooltip.accept(CommonComponents.space().copy()
			.append(CreateLang.translateDirect("potato_cannon.ammo.knockback", knockback).withStyle(darkGreen)));
	}
}
