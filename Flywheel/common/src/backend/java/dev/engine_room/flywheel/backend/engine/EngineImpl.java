package dev.engine_room.flywheel.backend.engine;

import java.util.List;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;

import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.backend.FlwBackend;
import dev.engine_room.flywheel.backend.engine.embed.EmbeddedEnvironment;
import dev.engine_room.flywheel.backend.engine.embed.Environment;
import dev.engine_room.flywheel.backend.engine.embed.EnvironmentStorage;
import dev.engine_room.flywheel.backend.engine.uniform.Uniforms;
import dev.engine_room.flywheel.backend.gl.GlStateTracker;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class EngineImpl implements Engine {
	private final DrawManager<? extends AbstractInstancer<?>> drawManager;
	private final int sqrMaxOriginDistance;
	private final EnvironmentStorage environmentStorage;
	private final LightStorage lightStorage;
	private int worldTargetFbo = -1;

	private BlockPos renderOrigin = BlockPos.ZERO;

	public EngineImpl(LevelAccessor level, DrawManager<? extends AbstractInstancer<?>> drawManager, int maxOriginDistance) {
		this.drawManager = drawManager;
		sqrMaxOriginDistance = maxOriginDistance * maxOriginDistance;
		environmentStorage = new EnvironmentStorage();
		lightStorage = new LightStorage(level);
	}

	@Override
	public VisualizationContext createVisualizationContext() {
		return new VisualizationContextImpl();
	}

	@Override
	public Plan<RenderContext> createFramePlan() {
		return drawManager.createFramePlan()
				.and(lightStorage.createFramePlan());
	}

	@Override
	public Vec3i renderOrigin() {
		return renderOrigin;
	}

	@Override
	public boolean updateRenderOrigin(Vec3 cameraPos) {
		double dx = renderOrigin.getX() - cameraPos.x;
		double dy = renderOrigin.getY() - cameraPos.y;
		double dz = renderOrigin.getZ() - cameraPos.z;
		double distanceSqr = dx * dx + dy * dy + dz * dz;

		if (distanceSqr <= sqrMaxOriginDistance) {
			return false;
		}

		renderOrigin = BlockPos.containing(cameraPos);
		drawManager.onRenderOriginChanged();
		return true;
	}

	@Override
	public void lightSections(LongSet sections) {
		lightStorage.sections(sections);
	}

	@Override
	public void onLightUpdate(SectionPos sectionPos, LightLayer layer) {
		lightStorage.onLightUpdate(sectionPos.asLong());
	}

	@Override
	public void render(RenderContext context) {
		try (var state = GlStateTracker.getRestoreState()) {
			bindWorldTarget();
			Uniforms.update(context);
			environmentStorage.flush();
			drawManager.render(lightStorage, environmentStorage);
		} catch (Exception e) {
			FlwBackend.LOGGER.error("Falling back", e);
			triggerFallback();
		}
	}

	@Override
	public void renderCrumbling(RenderContext context, List<CrumblingBlock> crumblingBlocks) {
		try (var state = GlStateTracker.getRestoreState()) {
			drawManager.renderCrumbling(crumblingBlocks);
		} catch (Exception e) {
			FlwBackend.LOGGER.error("Falling back", e);
			triggerFallback();
		}
	}

	@Override
	public void delete() {
		drawManager.delete();
		lightStorage.delete();
		environmentStorage.delete();
		if (worldTargetFbo != -1) {
			GL32.glDeleteFramebuffers(worldTargetFbo);
			worldTargetFbo = -1;
		}
	}

	/**
	 * NeoForge 26.2 fires level-stage events after the preceding GPU render pass
	 * has been closed.  The OpenGL backend therefore has framebuffer 0 bound at
	 * this point, while the level is rendered into Minecraft's main target.
	 * Attach and bind that target explicitly for Flywheel's legacy raw-GL draws.
	 */
	private void bindWorldTarget() {
		RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
		if (worldTargetFbo == -1) {
			worldTargetFbo = GL32.glGenFramebuffers();
		}

		GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, worldTargetFbo);
		int color = ((GlTexture) target.getColorTexture()).glId();
		GL32.glFramebufferTexture(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, color, 0);
		if (target.getDepthTexture() != null) {
			int depth = ((GlTexture) target.getDepthTexture()).glId();
			GL32.glFramebufferTexture(GL32.GL_FRAMEBUFFER, GL32.GL_DEPTH_ATTACHMENT, depth, 0);
		} else {
			GL32.glFramebufferTexture(GL32.GL_FRAMEBUFFER, GL32.GL_DEPTH_ATTACHMENT, 0, 0);
		}
		GL32.glDrawBuffer(GL32.GL_COLOR_ATTACHMENT0);
		GL32.glViewport(0, 0, target.width, target.height);
	}

	private void triggerFallback() {
		drawManager.triggerFallback();
	}

	public <I extends Instance> Instancer<I> instancer(Environment environment, InstanceType<I> type, Model model, int bias) {
		return drawManager.getInstancer(environment, type, model, bias);
	}

	public EnvironmentStorage environmentStorage() {
		return environmentStorage;
	}

	public LightStorage lightStorage() {
		return lightStorage;
	}

	public DrawManager<? extends AbstractInstancer<?>> drawManager() {
		return drawManager;
	}

	private class VisualizationContextImpl implements VisualizationContext {
		private final InstancerProviderImpl instancerProvider;

		public VisualizationContextImpl() {
			instancerProvider = new InstancerProviderImpl(EngineImpl.this);
		}

		@Override
		public InstancerProvider instancerProvider() {
			return instancerProvider;
		}

		@Override
		public Vec3i renderOrigin() {
			return EngineImpl.this.renderOrigin();
		}

		@Override
		public VisualEmbedding createEmbedding(Vec3i renderOrigin) {
			var out = new EmbeddedEnvironment(EngineImpl.this, renderOrigin);
			environmentStorage.track(out);
			return out;
		}
	}
}
