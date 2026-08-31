package net.createmod.catnip.api.client.gui.render;

import org.joml.Matrix3x2f;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

public record TexturedQuadRenderState(
	Matrix3x2f pose, ScreenRectangle scissorArea, TextureSetup textureSetup, Color color,
	int left, int right, int top, int bot, float u1, float u2, float v1, float v2
) implements GuiElementRenderState {
	@Override
	public RenderPipeline pipeline() {
		return RenderPipelines.GUI_TEXTURED;
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		consumer.addVertexWith2DPose(pose, (float) left, (float) top).setUv(u1, v1).setColor(color.getRGB());
		consumer.addVertexWith2DPose(pose, (float) left, (float) bot).setUv(u1, v2).setColor(color.getRGB());
		consumer.addVertexWith2DPose(pose, (float) right, (float) bot).setUv(u2, v2).setColor(color.getRGB());
		consumer.addVertexWith2DPose(pose, (float) right, (float) top).setUv(u2, v1).setColor(color.getRGB());
	}

	@Override
	public ScreenRectangle bounds() {
		ScreenRectangle bounds = new ScreenRectangle(left, top, right - left, bot - top).transformMaxBounds(pose);
		return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
	}
}
