package net.createmod.catnip.api.client.event;

import net.createmod.catnip.api.event.CatnipEvent;
import net.minecraft.client.renderer.LevelRenderer;

/// Invoked when the [LevelRenderer] reloads. Some situations where this happens:
/// - Manual reload with F3 + A
/// - Changes in resource packs
/// - Changes in some video settings
@FunctionalInterface
public interface LevelRendererReloadCallback {
	CatnipEvent<LevelRendererReloadCallback> EVENT = CatnipEvent.create(callbacks -> () -> callbacks.forEach(LevelRendererReloadCallback::onReload));

	void onReload();
}
