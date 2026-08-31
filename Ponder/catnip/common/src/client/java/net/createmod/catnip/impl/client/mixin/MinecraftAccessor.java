package net.createmod.catnip.impl.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
	@Accessor("gui")
	Gui catnip$getGui();
}
