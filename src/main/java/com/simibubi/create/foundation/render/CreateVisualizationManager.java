package com.simibubi.create.foundation.render;

import com.simibubi.create.infrastructure.assets.ExternalCreateAssets;

import net.minecraft.world.level.LevelAccessor;

/**
 * Selects Create's rendering path without disabling Flywheel for other mods.
 *
 * <p>The public edition obtains models from a player-supplied built-in resource
 * pack. Those models are available to Minecraft's 26.2 submit renderers, but
 * not early enough for Flywheel's instance model capture. Full editions keep
 * using Flywheel visuals; the external-assets edition keeps its complete
 * submit renderers active.</p>
 */
public final class CreateVisualizationManager {

	private CreateVisualizationManager() {}

	public static boolean supportsVisualization(LevelAccessor level) {
		return ExternalCreateAssets.shouldUseFlywheelVisuals()
			&& dev.engine_room.flywheel.api.visualization.VisualizationManager.supportsVisualization(level);
	}
}
