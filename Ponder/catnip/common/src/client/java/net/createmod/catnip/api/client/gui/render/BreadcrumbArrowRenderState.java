package net.createmod.catnip.api.client.gui.render;

import net.createmod.catnip.api.client.gui.UIRenderHelper;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.client.render.CatnipRenderPipelines;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

public record BreadcrumbArrowRenderState(
	Matrix3x2f pose,
	int width, int height, int indent,
	Color startColor, Color endColor,
	@Nullable ScreenRectangle scissorArea
) implements GuiElementRenderState {

	@Override
	public RenderPipeline pipeline() {
		return CatnipRenderPipelines.GUI_TRIANGLES;
	}

	@Override
	public void buildVertices(VertexConsumer buffer) {
		/*
		 * 0,0       x1,y0 ********************* x2,y0 ***** x3,y0
		 *       ****  |                       /   |  t5  ****
		 *   ****  t1  |       t3         /        |  ****
		 * x0,y1 --- x1,y1           /           x2,y1
		 *   ****  t2  |        /         t4       |  ****
		 *       ****  |   /                       |  t6  ****
		 *           x1,y2 ********************* x2,y2 ***** x3,y2
		 */

		float x0 = 0;
		float x1 = indent;
		float x2 = width;
		float x3 = indent + width;

		float y0 = 0;
		float y1 = height / 2f;
		float y2 = height;

		int indentAbs = Math.abs(indent);
		int widthAbs = Math.abs(width);
		int c1 = Color.mixColors(startColor, endColor, 0).getRGB();
		int c2 = Color.mixColors(startColor, endColor, (indentAbs) / (widthAbs + 2f * indentAbs)).getRGB();
		int c3 = Color.mixColors(startColor, endColor, (indentAbs + widthAbs) / (widthAbs + 2f * indentAbs)).getRGB();
		int c4 = Color.mixColors(startColor, endColor, 1).getRGB();

		// t1
		this.vertex(buffer, x0, y1, c1);
		this.vertex(buffer, x1, y0, c2);
		this.vertex(buffer, x1, y1, c2);

		// t2
		this.vertex(buffer, x0, y1, c1);
		this.vertex(buffer, x1, y1, c2);
		this.vertex(buffer, x1, y2, c2);

		// t3
		this.vertex(buffer, x1, y2, c2);
		this.vertex(buffer, x1, y0, c2);
		this.vertex(buffer, x2, y0, c3);

		// t4
		this.vertex(buffer, x1, y2, c2);
		this.vertex(buffer, x2, y0, c3);
		this.vertex(buffer, x2, y2, c3);

		// t5
		this.vertex(buffer, x2, y1, c3);
		this.vertex(buffer, x2, y0, c3);
		this.vertex(buffer, x3, y0, c4);

		// t6
		this.vertex(buffer, x2, y2, c3);
		this.vertex(buffer, x2, y1, c3);
		this.vertex(buffer, x3, y2, c4);
	}

	private void vertex(VertexConsumer buffer, float x, float y, int color) {
		buffer.addVertexWith2DPose(this.pose, x, y).setColor(color);
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.noTexture();
	}

	@Override
	public ScreenRectangle bounds() {
		return UIRenderHelper.getBounds(new ScreenRectangle(0, 0, width, height), this.pose, this.scissorArea);
	}
}
