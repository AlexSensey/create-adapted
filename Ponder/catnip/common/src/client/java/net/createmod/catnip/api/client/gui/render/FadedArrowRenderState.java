package net.createmod.catnip.api.client.gui.render;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.client.render.CatnipRenderPipelines;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

public record FadedArrowRenderState(
	Matrix3x2f pose, int size, float length, float r, float g, float b, float a
) implements GuiElementRenderState {
	@Override
	public RenderPipeline pipeline() {
		return CatnipRenderPipelines.TRIANGLE_FAN;
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		consumer.addVertexWith2DPose(pose, 0, -(10 + length)).setColor(r, g, b, a);

		consumer.addVertexWith2DPose(pose, -9, -3).setColor(r, g, b, 0);
		consumer.addVertexWith2DPose(pose, -6, -6).setColor(r, g, b, 0);
		consumer.addVertexWith2DPose(pose, -3, -8).setColor(r, g, b, 0);
		consumer.addVertexWith2DPose(pose, 0, -8.5f).setColor(r, g, b, 0);
		consumer.addVertexWith2DPose(pose, 3, -8).setColor(r, g, b, 0);
		consumer.addVertexWith2DPose(pose, 6, -6).setColor(r, g, b, 0);
		consumer.addVertexWith2DPose(pose, 9, -3).setColor(r, g, b, 0);
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
		return new ScreenRectangle(0, 0, size, size).transformMaxBounds(pose);
	}
}
