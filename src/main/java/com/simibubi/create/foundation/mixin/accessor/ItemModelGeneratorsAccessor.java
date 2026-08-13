package com.simibubi.create.foundation.mixin.accessor;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators.TrimMaterialData;

@Mixin(ItemModelGenerators.class)
public interface ItemModelGeneratorsAccessor {
	@Accessor("GENERATED_TRIM_MODELS")
	static List<TrimMaterialData> create$getGENERATED_TRIM_MODELS() {
		throw new AssertionError();
	}
}
