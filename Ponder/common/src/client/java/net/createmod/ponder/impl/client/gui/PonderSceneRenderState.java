package net.createmod.ponder.impl.client.gui;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.ponder.api.client.scene.PonderScene;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

public record PonderSceneRenderState(
	Matrix3x2f pose, PonderScene scene, int width, int height, double slide, LerpedFloat finishingFlash,
	float partialTicks, WindowRenderState window
) implements PictureInPictureRenderState {
	@Override
	public int x0() {
		return 0;
	}

	@Override
	public int y0() {
		return 0;
	}

	@Override
	public int x1() {
		return window.width / window.guiScale;
	}

	@Override
	public int y1() {
		return window.height / window.guiScale;
	}

	@Override
	public float scale() {
		return 1;
	}

	@Override
	public @Nullable ScreenRectangle scissorArea() {
		return null;
	}

	@Override
	public ScreenRectangle bounds() {
		return new ScreenRectangle(x0(), y0(), x1(), y1()).transformMaxBounds(pose);
	}
}
