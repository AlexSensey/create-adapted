package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

public record BlazeBurnerGuiRenderState(
	HeatLevel heatLevel,
	float animationTime,
	Matrix3x2f pose,
	int x0, int y0, int x1, int y1,
	float scale,
	@Nullable ScreenRectangle scissorArea,
	@Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
}
