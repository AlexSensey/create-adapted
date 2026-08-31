package net.createmod.catnip.api.client.gui.render.pip;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public record GuiBlockModelRenderState(
	BlockState state,
	@Nullable BlockEntity blockEntity,
	Matrix3x2f pose,
	float xLocal, float yLocal, float zLocal,
	float xRot, float yRot, float zRot,
	int color,
	int x0, int y0,
	int x1, int y1,
	float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
}
