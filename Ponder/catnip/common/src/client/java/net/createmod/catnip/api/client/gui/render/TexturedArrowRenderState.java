package net.createmod.catnip.api.client.gui.render;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;

public record TexturedArrowRenderState(
	Matrix3x2f pose, TextureSetup textureSetup, int size, float alpha, float x, float y, float width, float height
) implements GuiElementRenderState {
	@Override
	public RenderPipeline pipeline() {
		return RenderPipelines.GUI_TEXTURED;
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		consumer.addVertexWith2DPose(pose, -1, -1).setColor(1, 1, 1, alpha).setUv(x, y);
		consumer.addVertexWith2DPose(pose, -1, 1).setColor(1, 1, 1, alpha).setUv(x, y + height);
		consumer.addVertexWith2DPose(pose, 1, 1).setColor(1, 1, 1, alpha).setUv(x + width, y + height);
		consumer.addVertexWith2DPose(pose, 1, -1).setColor(1, 1, 1, alpha).setUv(x + width, y);
	}

	@Override
	public @Nullable ScreenRectangle scissorArea() {
		return null;
	}

	@Override
	public ScreenRectangle bounds() {
		return new ScreenRectangle(0, 0, size, size).transformMaxBounds(pose);
	}
}
