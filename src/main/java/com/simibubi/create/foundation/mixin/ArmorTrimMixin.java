package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.armor.AllArmorMaterials;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

@Mixin(ArmorTrim.class)
public abstract class ArmorTrimMixin {
	@Shadow
	@Final
	private Holder<TrimMaterial> material;

	@Shadow
	@Final
	private Holder<TrimPattern> pattern;

	@Inject(method = "layerAssetId", at = @At("HEAD"), cancellable = true)
	private void create$swapTexturesForCardboardTrims(String layerPrefix,
		ResourceKey<EquipmentAsset> equipmentAsset, CallbackInfoReturnable<Identifier> cir) {
		if (!equipmentAsset.equals(AllArmorMaterials.CARDBOARD.assetId()))
			return;

		String patternPath = pattern.value().assetId().getPath();
		String colorSuffix = material.value().assets().assetId(equipmentAsset).suffix();
		String leggings = layerPrefix.contains("leggings") ? "_leggings" : "";
		cir.setReturnValue(Create.asResource(
			"trims/models/armor/card_" + patternPath + leggings + "_" + colorSuffix));
	}
}
