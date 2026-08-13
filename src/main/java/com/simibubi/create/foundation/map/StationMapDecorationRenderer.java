package com.simibubi.create.foundation.map;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.neoforge.client.gui.map.IMapDecorationRenderer;

public class StationMapDecorationRenderer implements IMapDecorationRenderer {
	@Override
	public boolean render(@NotNull MapRenderState.MapDecorationRenderState decoration, PoseStack poseStack,
		SubmitNodeCollector collector, @NotNull MapRenderState mapState, TextureAtlas textureAtlas,
		boolean inItemFrame, int packedLight, int index) {
		// Minecraft 26.2's standard map renderer already handles the extracted
		// sprite and label. Returning false lets it draw this decoration.
		return false;
	}
}
