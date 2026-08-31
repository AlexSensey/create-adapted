package net.createmod.catnip.api.client.gui.render;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.client.render.CatnipRenderPipelines;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

public record RadialSectorRenderState(
	Matrix3x2f pose, List<Vec2> innerPoints, List<Vec2> outerPoints, Color innerColor, Color outerColor
) implements GuiElementRenderState {
	public RadialSectorRenderState(Matrix3x2f pose, float innerRadius, float outerRadius, float startAngle, float arcAngle, Color innerColor, Color outerColor) {
		// if arcAngle > 0, start with inner. otherwise start with outer
		List<Vec2> innerPoints = getPointsForCircleArc(innerRadius, startAngle, arcAngle);
		List<Vec2> outerPoints = getPointsForCircleArc(outerRadius, startAngle, arcAngle);

		this(
			pose,
			innerPoints,
			outerPoints,
			innerColor,
			outerColor
		);
	}

	@Override
	public RenderPipeline pipeline() {
		return CatnipRenderPipelines.POSITION_COLOR_STRIP;
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		for (int i = 0; i < innerPoints.size(); i++) {
			Vec2 point = outerPoints.get(i);
			consumer.addVertexWith2DPose(pose, point.x, point.y).setColor(outerColor.getRGB());

			point = innerPoints.get(i);
			consumer.addVertexWith2DPose(pose, point.x, point.y).setColor(innerColor.getRGB());
		}
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
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (Vec2 point : innerPoints) {
			minX = Math.min(minX, point.x);
			maxX = Math.max(maxX, point.x);
			minY = Math.min(minY, point.y);
			maxY = Math.max(maxY, point.y);
		}
		for (Vec2 point : outerPoints) {
			minX = Math.min(minX, point.x);
			maxX = Math.max(maxX, point.x);
			minY = Math.min(minY, point.y);
			maxY = Math.max(maxY, point.y);
		}

		return new ScreenRectangle((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY)).transformMaxBounds(pose);
	}

	private static List<Vec2> getPointsForCircleArc(float radius, float startAngle, float arcAngle) {
		int segmentCount = Math.abs(arcAngle) <= 90 ? 16 : 32;
		List<Vec2> points = new ArrayList<>(segmentCount);

		float theta = (Mth.DEG_TO_RAD * arcAngle) / (float) (segmentCount - 1);
		float t = Mth.DEG_TO_RAD * startAngle;

		for (int i = 0; i < segmentCount; i++) {
			points.add(new Vec2(
				(float) (radius * Math.cos(t)),
				(float) (radius * Math.sin(t))
			));

			t += theta;
		}

		return points;
	}
}
