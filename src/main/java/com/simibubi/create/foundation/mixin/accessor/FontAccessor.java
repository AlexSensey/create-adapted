package com.simibubi.create.foundation.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.Font;

@Mixin(Font.class)
public interface FontAccessor {
	@Accessor("provider")
	Font.Provider create$getProvider();
}
