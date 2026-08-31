package dev.engine_room.flywheel.lib.model.baked;

import java.nio.ByteBuffer;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.MeshData;

import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.model.SimpleQuadMesh;
import dev.engine_room.flywheel.lib.vertex.NoOverlayVertexView;
import dev.engine_room.flywheel.lib.vertex.VertexView;

final class MeshHelper {
	private MeshHelper() {
	}

	public static SimpleQuadMesh blockVerticesToMesh(MeshData data, @Nullable String meshDescriptor,
		@Nullable List<Vector3f> vertexNormals) {
		MeshData.DrawState drawState = data.drawState();
		int vertexCount = drawState.vertexCount();
		long srcStride = drawState.format().getVertexSize();

		VertexView vertexView = new NoOverlayVertexView();
		long dstStride = vertexView.stride();

		ByteBuffer src = data.vertexBuffer();
		MemoryBlock dst = MemoryBlock.mallocTracked((long) vertexCount * dstStride);
		long srcPtr = MemoryUtil.memAddress(src);
		long dstPtr = dst.ptr();
		// Minecraft 26.2's block format is 28 bytes and no longer contains a packed normal. Older Flywheel versions
		// assumed a 31-byte prefix and consequently read three bytes past every vanilla vertex. Besides producing
		// random lighting normals, the final read could leave the source buffer entirely and corrupt rendered meshes.
		// Copy only bytes that actually exist. Extended formats that still contain the legacy normal keep working.
		long bytesToCopy = Math.min(srcStride, dstStride);

		for (int i = 0; i < vertexCount; i++) {
			// It is safe to copy bytes directly since the NoOverlayVertexView uses the same memory layout as the first
			// 31 bytes of the block vertex format, vanilla or otherwise.
			MemoryUtil.memCopy(srcPtr + srcStride * i, dstPtr + dstStride * i, bytesToCopy);
		}

		vertexView.ptr(dstPtr);
		vertexView.vertexCount(vertexCount);
		vertexView.nativeMemoryOwner(dst);

		if (srcStride < NoOverlayVertexView.STRIDE) {
			if (vertexNormals != null && vertexNormals.size() == vertexCount)
				applyBakedVertexNormals(vertexView, vertexNormals);
			else
				reconstructQuadNormals(vertexView, vertexCount);
		}

		return new SimpleQuadMesh(vertexView, meshDescriptor);
	}

	private static void applyBakedVertexNormals(VertexView vertices, List<Vector3f> vertexNormals) {
		for (int vertex = 0; vertex < vertexNormals.size(); vertex++) {
			Vector3f normal = vertexNormals.get(vertex);
			vertices.normalX(vertex, normal.x());
			vertices.normalY(vertex, normal.y());
			vertices.normalZ(vertex, normal.z());
		}
	}

	private static void reconstructQuadNormals(VertexView vertices, int vertexCount) {
		int completeVertexCount = vertexCount - vertexCount % 4;

		for (int first = 0; first < completeVertexCount; first += 4) {
			float edge1X = vertices.x(first + 1) - vertices.x(first);
			float edge1Y = vertices.y(first + 1) - vertices.y(first);
			float edge1Z = vertices.z(first + 1) - vertices.z(first);
			float edge2X = vertices.x(first + 2) - vertices.x(first);
			float edge2Y = vertices.y(first + 2) - vertices.y(first);
			float edge2Z = vertices.z(first + 2) - vertices.z(first);

			float normalX = edge1Y * edge2Z - edge1Z * edge2Y;
			float normalY = edge1Z * edge2X - edge1X * edge2Z;
			float normalZ = edge1X * edge2Y - edge1Y * edge2X;
			float lengthSquared = normalX * normalX + normalY * normalY + normalZ * normalZ;

			if (lengthSquared > 1.0e-12f) {
				float inverseLength = (float) (1.0 / Math.sqrt(lengthSquared));
				normalX *= inverseLength;
				normalY *= inverseLength;
				normalZ *= inverseLength;
			} else {
				normalX = 0;
				normalY = 1;
				normalZ = 0;
			}

			for (int vertex = first; vertex < first + 4; vertex++) {
				vertices.normalX(vertex, normalX);
				vertices.normalY(vertex, normalY);
				vertices.normalZ(vertex, normalZ);
			}
		}

		// SimpleQuadMesh normally receives groups of four vertices. Initialize any unexpected remainder as well so
		// malformed input can never expose uninitialized native memory to the GPU.
		for (int vertex = completeVertexCount; vertex < vertexCount; vertex++) {
			vertices.normalX(vertex, 0);
			vertices.normalY(vertex, 1);
			vertices.normalZ(vertex, 0);
		}
	}
}
