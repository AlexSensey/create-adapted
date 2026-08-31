package net.createmod.catnip.api.client.event;

import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.api.event.CatnipEvent;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/// Invoked at several points during level rendering.
@FunctionalInterface
public interface LevelRenderCallback {
	/// Invoked right after translucent features are rendered.
	CatnipEvent<LevelRenderCallback> AFTER_TRANSLUCENT_FEATURES = CatnipEvent.create(callbacks -> (renderer, state, transforms) -> {
		for (LevelRenderCallback callback : callbacks) {
			callback.onRender(renderer, state, transforms);
		}
	});

	void onRender(LevelRenderer renderer, LevelRenderState state, PoseStack transforms);
}
