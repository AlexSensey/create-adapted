package net.createmod.catnip.api.client.gui.render;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;

public record BoxElementRenderState(
	Matrix3x2f pose, float x, float y, float width, float height, int f, int c1, int c2, int c3
) implements GuiElementRenderState {
	@Override
	public RenderPipeline pipeline() {
		return RenderPipelines.DEBUG_QUADS; // TODO - Check if this should be GUI or this
	}

	//total box width = 1 * 2 (outer border) + 1 * 2 (inner color border) + 2 * borderOffset + width
	//defaults to 2 + 2 + 4 + 16 = 24px
	@Override
	public void buildVertices(VertexConsumer consumer) {
		/*
		 *          _____________
		 *        _|_____________|_
		 *       | | ___________ | |
		 *       | | |  |      | | |
		 *       | | |  |      | | |
		 *       | | |--*   |  | | |
		 *       | | |      h  | | |
		 *       | | |  --w-+  | | |
		 *       | | |         | | |
		 *       | | |_________| | |
		 *       |_|_____________|_|
		 *         |_____________|
		 *
		 * */

		//outer top
		consumer.addVertexWith2DPose(pose, x - f - 1, y - f - 2).setColor(c1);
		consumer.addVertexWith2DPose(pose, x - f - 1, y - f - 1).setColor(c1);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y - f - 1).setColor(c1);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y - f - 2).setColor(c1);
		//outer left
		consumer.addVertexWith2DPose(pose, x - f - 2, y - f - 1).setColor(c1);
		consumer.addVertexWith2DPose(pose, x - f - 2, y + f + 1 + height).setColor(c1);
		consumer.addVertexWith2DPose(pose, x - f - 1, y + f + 1 + height).setColor(c1);
		consumer.addVertexWith2DPose(pose, x - f - 1, y - f - 1).setColor(c1);
		//outer bottom
		consumer.addVertexWith2DPose(pose, x - f - 1, y + f + 1 + height).setColor(c1);
		consumer.addVertexWith2DPose(pose, x - f - 1, y + f + 2 + height).setColor(c1);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y + f + 2 + height).setColor(c1);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y + f + 1 + height).setColor(c1);
		//outer right
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y - f - 1).setColor(c1);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y + f + 1 + height).setColor(c1);
		consumer.addVertexWith2DPose(pose, x + f + 2 + width, y + f + 1 + height).setColor(c1);
		consumer.addVertexWith2DPose(pose, x + f + 2 + width, y - f - 1).setColor(c1);
		//inner background - also render behind the inner edges
		consumer.addVertexWith2DPose(pose, x - f - 1, y - f - 1).setColor(c1);
		consumer.addVertexWith2DPose(pose, x - f - 1, y + f + 1 + height).setColor(c1);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y + f + 1 + height).setColor(c1);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y - f - 1).setColor(c1);
		//inner top - includes corners
		consumer.addVertexWith2DPose(pose, x - f - 1, y - f - 1).setColor(c2);
		consumer.addVertexWith2DPose(pose, x - f - 1, y - f).setColor(c2);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y - f).setColor(c2);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y - f - 1).setColor(c2);
		//inner left - excludes corners
		consumer.addVertexWith2DPose(pose, x - f - 1, y - f).setColor(c2);
		consumer.addVertexWith2DPose(pose, x - f - 1, y + f + height).setColor(c3);
		consumer.addVertexWith2DPose(pose, x - f, y + f + height).setColor(c3);
		consumer.addVertexWith2DPose(pose, x - f, y - f).setColor(c2);
		//inner bottom - includes corners
		consumer.addVertexWith2DPose(pose, x - f - 1, y + f + height).setColor(c3);
		consumer.addVertexWith2DPose(pose, x - f - 1, y + f + 1 + height).setColor(c3);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y + f + 1 + height).setColor(c3);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y + f + height).setColor(c3);
		//inner right - excludes corners
		consumer.addVertexWith2DPose(pose, x + f + width, y - f).setColor(c2);
		consumer.addVertexWith2DPose(pose, x + f + width, y + f + height).setColor(c3);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y + f + height).setColor(c3);
		consumer.addVertexWith2DPose(pose, x + f + 1 + width, y - f).setColor(c2);
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
		return new ScreenRectangle((int) x, (int) y, (int) width, (int) height).transformMaxBounds(pose);
	}
}
