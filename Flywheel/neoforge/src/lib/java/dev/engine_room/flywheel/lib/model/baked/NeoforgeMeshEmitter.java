package dev.engine_room.flywheel.lib.model.baked;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import org.joml.Vector3f;

import dev.engine_room.flywheel.api.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.quad.BakedNormals;

/** Bridges Minecraft 26.2's extracted baked quads into Flywheel block meshes. */
@ApiStatus.Internal
public class NeoforgeMeshEmitter extends MeshEmitter {
	private final RenderType renderType;

	NeoforgeMeshEmitter(ByteBufferBuilderStack byteBufferBuilderStack, RenderType renderType) {
		super(byteBufferBuilderStack, renderType);
		this.renderType = renderType;
	}

	public void put(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance, BlockAndTintGetter level,
		BlockPos pos, BlockState state) {
		var info = quad.materialInfo();
		Material material = blockMaterialFunction.apply(renderType, info.shade(), info.ambientOcclusion());
		if (material == null)
			return;

		// Minecraft 26.2's ModelBlockRenderer bakes ambient-occlusion brightness
		// and cardinal shading into QuadInstance colors before invoking
		// BlockQuadOutput. Flywheel applies both after transforming the mesh, so the
		// baked shading must be removed. Recompute only the actual block tint here;
		// otherwise rotating faces pulse and receive lighting twice.
		int color = 0xFFFFFFFF;
		if (info.isTinted()) {
			var tintSource = Minecraft.getInstance()
				.getBlockColors()
				.getTintSource(state, info.tintIndex());
			if (tintSource != null)
				color = tintSource.colorInWorld(state, level, pos);
		}
		instance.setColor(color);

		BufferBuilder buffer = getBuffer(material);
		buffer.putBakedQuad(pose, quad, instance);
		for (int vertex = 0; vertex < 4; vertex++) {
			int packed = quad.bakedNormals().normal(vertex);
			Vector3f normal = BakedNormals.isUnspecified(packed)
				? new Vector3f(quad.direction().getUnitVec3f())
				: BakedNormals.unpack(packed, new Vector3f());
			recordVertexNormal(buffer, pose.transformNormal(normal, normal));
		}
	}
}
