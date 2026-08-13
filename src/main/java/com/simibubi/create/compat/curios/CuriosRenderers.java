package com.simibubi.create.compat.curios;

import com.simibubi.create.AllItems;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import top.theillusivec4.curios.api.client.ICurioRenderer;

public class CuriosRenderers {
	public static void register() {
		ICurioRenderer.register(AllItems.GOGGLES.get(), GogglesCurioRenderer::new);
	}

	public static void onLayerRegister(final EntityRenderersEvent.RegisterLayerDefinitions event) {
		// The 26.2 renderer follows the already-baked parent humanoid head directly.
	}
}
