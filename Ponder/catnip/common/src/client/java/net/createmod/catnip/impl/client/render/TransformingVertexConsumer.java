package net.createmod.catnip.impl.client.render;

import static org.joml.Math.fma;

import org.jetbrains.annotations.UnknownNullability;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

// https://github.com/Engine-Room/Flywheel/blob/2f67f54c8898d91a48126c3c753eefa6cd224f84/common/src/lib/java/dev/engine_room/flywheel/lib/model/baked/TransformingVertexConsumer.java
public class TransformingVertexConsumer implements VertexConsumer {
	@UnknownNullability
	private VertexConsumer delegate;
	@UnknownNullability
	private PoseStack poseStack;

	public void prepare(VertexConsumer delegate, PoseStack poseStack) {
		this.delegate = delegate;
		this.poseStack = poseStack;
	}

	public void clear() {
		delegate = null;
		poseStack = null;
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		Matrix4f matrix = poseStack.last().pose();

		delegate.addVertex(
			transformPositionX(matrix, x, y, z),
			transformPositionY(matrix, x, y, z),
			transformPositionZ(matrix, x, y, z));
		return this;
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int alpha) {
		delegate.setColor(red, green, blue, alpha);
		return this;
	}

	@Override
	public VertexConsumer setColor(int c) {
		delegate.setColor(c);
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		delegate.setUv(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		delegate.setUv1(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		delegate.setUv2(u, v);
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		Matrix3f matrix = poseStack.last().normal();
		delegate.setNormal(
			transformNormalX(matrix, x, y, z),
			transformNormalY(matrix, x, y, z),
			transformNormalZ(matrix, x, y, z));
		return this;
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		delegate.setLineWidth(width);
		return this;
	}

	// https://github.com/Engine-Room/Flywheel/blob/1.20.1/dev/common/src/lib/java/dev/engine_room/flywheel/lib/math/MatrixMath.java

	public static float transformPositionX(Matrix4f matrix, float x, float y, float z) {
		return fma(matrix.m00(), x, fma(matrix.m10(), y, fma(matrix.m20(), z, matrix.m30())));
	}

	public static float transformPositionY(Matrix4f matrix, float x, float y, float z) {
		return fma(matrix.m01(), x, fma(matrix.m11(), y, fma(matrix.m21(), z, matrix.m31())));
	}

	public static float transformPositionZ(Matrix4f matrix, float x, float y, float z) {
		return fma(matrix.m02(), x, fma(matrix.m12(), y, fma(matrix.m22(), z, matrix.m32())));
	}

	private static float transformNormalX(Matrix3f matrix, float x, float y, float z) {
		return fma(matrix.m00(), x, fma(matrix.m10(), y, matrix.m20() * z));
	}

	private static float transformNormalY(Matrix3f matrix, float x, float y, float z) {
		return fma(matrix.m01(), x, fma(matrix.m11(), y, matrix.m21() * z));
	}

	private static float transformNormalZ(Matrix3f matrix, float x, float y, float z) {
		return fma(matrix.m02(), x, fma(matrix.m12(), y, matrix.m22() * z));
	}
}
