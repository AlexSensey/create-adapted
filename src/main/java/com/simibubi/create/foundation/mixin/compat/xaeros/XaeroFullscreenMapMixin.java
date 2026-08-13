package com.simibubi.create.foundation.mixin.compat.xaeros;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.Create;
import com.simibubi.create.compat.trainmap.XaeroTrainMap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xaero.map.gui.GuiMap;

@Mixin(GuiMap.class)
public abstract class XaeroFullscreenMapMixin {
	@Unique
	private boolean create$failedToRenderTrainMap = false;

	@Inject(method = "renderPreDropdown(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"), require = 0)
	private void create$xaeroMapFullscreenRender(GuiGraphicsExtractor guiGraphics, int scaledMouseX, int scaledMouseY, float partialTicks, CallbackInfo ci) {
		try {
			if (!create$failedToRenderTrainMap)
				XaeroTrainMap.onRender(guiGraphics, (GuiMap) (Object) this, scaledMouseX, scaledMouseY, partialTicks);
		} catch (Throwable e) {
			Create.LOGGER.error("Failed to render Xaero's World Map train map integration:", e);
			create$failedToRenderTrainMap = true;
		}
	}
}
