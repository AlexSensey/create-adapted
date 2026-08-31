package net.createmod.catnip.impl.fabric.client.service;

import net.createmod.catnip.api.client.gui.HudElements;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public final class FabricHudElements implements HudElements {
	@Override
	public void register(Identifier id, Element element) {
		HudElementRegistry.addLast(id, element::render);
	}
}
