package net.createmod.catnip.api.client.event;

import net.createmod.catnip.api.event.CatnipEvent;
import net.minecraft.client.renderer.texture.TextureAtlas;

/// Invoked after a [TextureAtlas] has been stitched together.
@FunctionalInterface
public interface AtlasStitchedCallback {
	CatnipEvent<AtlasStitchedCallback> EVENT = CatnipEvent.create(callbacks -> atlas -> {
		for (AtlasStitchedCallback callback : callbacks) {
			callback.afterStitch(atlas);
		}
	});

	void afterStitch(TextureAtlas atlas);
}
