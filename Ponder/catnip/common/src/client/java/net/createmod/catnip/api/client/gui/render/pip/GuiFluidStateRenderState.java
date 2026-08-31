package net.createmod.catnip.api.client.gui.render.pip;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.level.material.FluidState;

public record GuiFluidStateRenderState(
	FluidState fluidState,
	Matrix3x2f pose, // FIXME: actually do something with this information
	int x0, int y0,
	int x1, int y1,
	float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
}
