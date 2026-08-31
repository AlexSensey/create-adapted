package net.createmod.ponder.impl.client.scene;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import org.joml.Matrix4f;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.createmod.catnip.impl.client.mixin.ParticleEngineAccessor;
import net.createmod.catnip.api.client.render.PonderRenderTypes;
import net.createmod.ponder.api.client.level.PonderLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.particles.ParticleLimit;

public class PonderWorldParticles {
	private final ParticlesRenderState particleState = new ParticlesRenderState();
	private final Map<ParticleRenderType, ParticleGroup<?>> particles = Maps.newIdentityHashMap();
	private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
	private final Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts = new Object2IntOpenHashMap<>();
	private final ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;

	PonderLevel world;

	public PonderWorldParticles(PonderLevel world) {
		this.world = world;
	}

	public void addParticle(Particle effect) {
		Optional<ParticleLimit> optional = effect.getParticleLimit();
		if (optional.isPresent()) {
			if (this.hasSpaceInParticleLimit(optional.get())) {
				this.particlesToAdd.add(effect);
				this.updateCount(optional.get(), 1);
			}
		} else {
			this.particlesToAdd.add(effect);
		}
	}

	public void tick() {
		this.particles.forEach((renderType, particleGroup) -> particleGroup.tickParticles());

		Particle particle;
		if (!this.particlesToAdd.isEmpty()) {
			while ((particle = this.particlesToAdd.poll()) != null) {
				ParticleEngineAccessor accessor = (ParticleEngineAccessor) this.particleEngine;
				this.particles.computeIfAbsent(particle.getGroup(), accessor::callCreateParticleGroup).add(particle);
			}
		}
	}

	public void renderParticles(PoseStack poseStack, SubmitNodeCollector queue, Camera camera, CameraRenderState cameraRenderState, float partialTick) {
		// Custom geometry is consumed after this method returns. Clear the state
		// from the previous frame before extracting the new one; clearing it after
		// submitting would leave the deferred callbacks with no vertices to draw.
		particleState.reset();
		for (ParticleRenderType renderType : ParticleEngineAccessor.getRENDER_ORDER()) {
			ParticleGroup<?> group = particles.get(renderType);
			if (group != null && !group.isEmpty()) {
				particleState.add(group.extractRenderState(ParticlesFrustum.INSTANCE, camera, partialTick));
			}
		}

		// QuadParticleRenderState stores camera-relative vertices and its normal
		// submit path has no PoseStack. In a PIP scene that would place particles
		// outside the virtual world. Re-submit each layer as custom geometry and
		// apply the Ponder scene transform to every generated vertex.
		poseStack.pushPose();
		poseStack.translate(camera.position().x, camera.position().y, camera.position().z);
		for (ParticleGroupRenderState groupState : particleState.particles) {
			if (groupState instanceof QuadParticleRenderState quads) {
				for (SingleQuadParticle.Layer layer : quads.layers()) {
					RenderType type = PonderRenderTypes.particle(layer);
					queue.submitCustomGeometry(poseStack, type, (pose, consumer) ->
						quads.buildLayer(layer, new PoseVertexConsumer(pose, consumer)));
				}
			} else {
				groupState.submit(queue, cameraRenderState);
			}
		}
		poseStack.popPose();
	}

	private static class PoseVertexConsumer implements VertexConsumer {
		private final PoseStack.Pose pose;
		private final VertexConsumer delegate;

		private PoseVertexConsumer(PoseStack.Pose pose, VertexConsumer delegate) {
			this.pose = pose;
			this.delegate = delegate;
		}

		@Override public VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(pose, x, y, z);
			return this;
		}
		@Override public VertexConsumer setColor(int r, int g, int b, int a) { delegate.setColor(r, g, b, a); return this; }
		@Override public VertexConsumer setColor(int color) { delegate.setColor(color); return this; }
		@Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
		@Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
		@Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
		@Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(pose, x, y, z); return this; }
		@Override public VertexConsumer setLineWidth(float width) { delegate.setLineWidth(width); return this; }
	}

	protected void updateCount(ParticleLimit limit, int count) {
		this.trackedParticleCounts.addTo(limit, count);
	}

	private boolean hasSpaceInParticleLimit(ParticleLimit limit) {
		return this.trackedParticleCounts.getInt(limit) < limit.limit();
	}

	public void clearEffects() {
		this.particles.clear();
		this.particlesToAdd.clear();
		this.trackedParticleCounts.clear();
	}

	public static class ParticlesFrustum extends Frustum {
		public static final Frustum INSTANCE = new ParticlesFrustum();

		private ParticlesFrustum() {
			super(new Matrix4f(), new Matrix4f());
		}

		@Override
		public boolean pointInFrustum(double x, double y, double z) {
			return true;
		}
	}
}
