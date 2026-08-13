package com.simibubi.create.foundation.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.SubtitleOverlay;

@Mixin(Hud.class)
public interface HudAccessor {
	@Accessor("subtitleOverlay")
	SubtitleOverlay create$getSubtitleOverlay();
}
