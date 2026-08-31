package net.createmod.catnip.impl.neoforge.render;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.client.render.ShadedBlockSbbBuilder;

import net.neoforged.neoforge.client.model.quad.MutableQuad;

@Deprecated(forRemoval = true)
public class NeoForgeShadedBlockSbbBuilder extends ShadedBlockSbbBuilder implements VertexConsumer {
	@Override
	public void putMutableQuad(Pose pose, MutableQuad quad, QuadInstance instance) {
		this.prepareForGeometry(quad.shade());
		this.bufferBuilder.putMutableQuad(pose, quad, instance);
	}
}
