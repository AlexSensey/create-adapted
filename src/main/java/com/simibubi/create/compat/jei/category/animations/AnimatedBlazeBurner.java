package com.simibubi.create.compat.jei.category.animations;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;

public class AnimatedBlazeBurner extends AnimatedKinetics {

	private HeatLevel heatLevel;

	public AnimatedBlazeBurner withHeat(HeatLevel heatLevel) {
		this.heatLevel = heatLevel;
		return this;
	}

	@Override
	protected void drawAnimation(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		Matrix3x2f pose = new Matrix3x2f(graphics.pose()).translate(xOffset, yOffset);
		ScreenRectangle scissor = graphics.peekScissorStack();
		int x0 = -64, y0 = -64, x1 = 64, y1 = 80;
		ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
		if (scissor != null)
			bounds = scissor.intersection(bounds);
		graphics.guiRenderState.addPicturesInPictureState(new BlazeBurnerGuiRenderState(heatLevel,
			getAnimationTime(), pose, x0, y0, x1, y1, 23, scissor, bounds));
	}

	@Override
	protected float getGlobalXRotation() {
		return -15.5f;
	}

	@Override
	protected float getGlobalYRotation() {
		return 22.5f;
	}

}
