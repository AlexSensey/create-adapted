package net.createmod.catnip.api.client.gui.render;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;

public record GradientRectRenderState(
	Matrix3x2f pose, float left, float top, float right, float bottom, Color startColor, Color endColor
) implements GuiElementRenderState {
	@Override
	public RenderPipeline pipeline() {
		return RenderPipelines.DEBUG_QUADS; // TODO - Check if this should be GUI or this
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		consumer.addVertexWith2DPose(pose, right, top).setColor(startColor.getRGB());
		consumer.addVertexWith2DPose(pose, left, top).setColor(startColor.getRGB());
		consumer.addVertexWith2DPose(pose, left, bottom).setColor(endColor.getRGB());
		consumer.addVertexWith2DPose(pose, right, bottom).setColor(endColor.getRGB());
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.noTexture();
	}

	@Override
	public @Nullable ScreenRectangle scissorArea() {
		return null;
	}

	@Override
	public ScreenRectangle bounds() {
		return new ScreenRectangle((int) left, (int) top, (int) (right - left), (int) (bottom - top)).transformMaxBounds(pose);
	}
}
