package com.simibubi.create.content.contraptions.glue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;

public class SuperGlueRenderer extends EntityRenderer<SuperGlueEntity, SuperGlueRenderer.GlueRenderState> {

	private static final int PASSIVE = 0x4D9162;
	private static final int HIGHLIGHT = 0x68c586;
	private static final float SELECTED_EDGE_THICKNESS = 1 / 16f;
	private static final float PASSIVE_EDGE_THICKNESS = 1 / 48f;
	private static final float SURFACE_OFFSET = 1 / 128f;

	public SuperGlueRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public Identifier getTextureLocation(SuperGlueEntity entity) {
		return null;
	}

	@Override
	public GlueRenderState createRenderState() {
		return new GlueRenderState();
	}

	@Override
	public void extractRenderState(SuperGlueEntity entity, GlueRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.localBox = entity.getBoundingBox()
			.move(-entity.getX(), -entity.getY(), -entity.getZ());
		int armPreviewColor = ArmInteractionPointHandler.getPreviewColor(entity);
		state.armPreview = armPreviewColor != -1;
		state.selected = state.armPreview || CreateClient.GLUE_HANDLER.isSelected(entity);
		state.color = state.armPreview ? armPreviewColor : CreateClient.GLUE_HANDLER.getRenderColor(entity, HIGHLIGHT);
	}

	@Override
	public void submit(GlueRenderState state, PoseStack ms, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return;
		if (!state.armPreview && !AllItems.SUPER_GLUE.isIn(player.getMainHandItem()))
			return;

		AABB box = state.localBox;
		if (box == null || box.getSize() <= 0)
			return;

		boolean fresh = state.ageInTicks < 80;
		float thickness = state.selected ? SELECTED_EDGE_THICKNESS : PASSIVE_EDGE_THICKNESS;
		int edgeColor = fresh || state.selected ? state.color : PASSIVE;
		int edgeAlpha = state.selected ? 235 : 135;
		if (!state.armPreview && (fresh || state.selected)) {
			int textureAlpha = fresh ? (int) (150 - state.ageInTicks / 80f * 105) : 55;
			collector.submitCustomGeometry(ms,
				com.simibubi.create.foundation.render.RenderTypes.glueOverlay(AllSpecialTextures.GLUE.getId()),
				(pose, consumer) -> renderGlueTexture(pose, consumer, box, state.color, textureAlpha));
		}
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(), (pose, consumer) -> {
			renderWireframe(pose, consumer, box, edgeColor, edgeAlpha, thickness);
		});
	}

	@Override
	public boolean shouldRender(SuperGlueEntity entity, Frustum frustum, double x, double y, double z) {
		return super.shouldRender(entity, frustum, x, y, z);
	}

	public static void renderWireframe(PoseStack.Pose pose, VertexConsumer consumer, AABB box, int color, int alpha,
		float thickness) {
		float x1 = (float) box.minX - SURFACE_OFFSET;
		float y1 = (float) box.minY - SURFACE_OFFSET;
		float z1 = (float) box.minZ - SURFACE_OFFSET;
		float x2 = (float) box.maxX + SURFACE_OFFSET;
		float y2 = (float) box.maxY + SURFACE_OFFSET;
		float z2 = (float) box.maxZ + SURFACE_OFFSET;
		float t = thickness;

		renderEdgeBox(pose, consumer, x1, y1, z1, x2, y1 + t, z1 + t, color, alpha);
		renderEdgeBox(pose, consumer, x1, y1, z2 - t, x2, y1 + t, z2, color, alpha);
		renderEdgeBox(pose, consumer, x1, y2 - t, z1, x2, y2, z1 + t, color, alpha);
		renderEdgeBox(pose, consumer, x1, y2 - t, z2 - t, x2, y2, z2, color, alpha);

		renderEdgeBox(pose, consumer, x1, y1, z1, x1 + t, y1 + t, z2, color, alpha);
		renderEdgeBox(pose, consumer, x2 - t, y1, z1, x2, y1 + t, z2, color, alpha);
		renderEdgeBox(pose, consumer, x1, y2 - t, z1, x1 + t, y2, z2, color, alpha);
		renderEdgeBox(pose, consumer, x2 - t, y2 - t, z1, x2, y2, z2, color, alpha);

		renderEdgeBox(pose, consumer, x1, y1, z1, x1 + t, y2, z1 + t, color, alpha);
		renderEdgeBox(pose, consumer, x2 - t, y1, z1, x2, y2, z1 + t, color, alpha);
		renderEdgeBox(pose, consumer, x1, y1, z2 - t, x1 + t, y2, z2, color, alpha);
		renderEdgeBox(pose, consumer, x2 - t, y1, z2 - t, x2, y2, z2, color, alpha);
	}

	public static void renderSideEdges(PoseStack.Pose pose, VertexConsumer consumer, AABB box,
		net.minecraft.core.Direction.Axis depthAxis, int color, int alpha, float thickness) {
		float x1 = (float) box.minX - SURFACE_OFFSET;
		float y1 = (float) box.minY - SURFACE_OFFSET;
		float z1 = (float) box.minZ - SURFACE_OFFSET;
		float x2 = (float) box.maxX + SURFACE_OFFSET;
		float y2 = (float) box.maxY + SURFACE_OFFSET;
		float z2 = (float) box.maxZ + SURFACE_OFFSET;
		float t = thickness;

		switch (depthAxis) {
			case X -> {
				renderEdgeBox(pose, consumer, x1, y1, z1, x2, y1 + t, z1 + t, color, alpha);
				renderEdgeBox(pose, consumer, x1, y1, z2 - t, x2, y1 + t, z2, color, alpha);
				renderEdgeBox(pose, consumer, x1, y2 - t, z1, x2, y2, z1 + t, color, alpha);
				renderEdgeBox(pose, consumer, x1, y2 - t, z2 - t, x2, y2, z2, color, alpha);
			}
			case Y -> {
				renderEdgeBox(pose, consumer, x1, y1, z1, x1 + t, y2, z1 + t, color, alpha);
				renderEdgeBox(pose, consumer, x2 - t, y1, z1, x2, y2, z1 + t, color, alpha);
				renderEdgeBox(pose, consumer, x1, y1, z2 - t, x1 + t, y2, z2, color, alpha);
				renderEdgeBox(pose, consumer, x2 - t, y1, z2 - t, x2, y2, z2, color, alpha);
			}
			case Z -> {
				renderEdgeBox(pose, consumer, x1, y1, z1, x1 + t, y1 + t, z2, color, alpha);
				renderEdgeBox(pose, consumer, x2 - t, y1, z1, x2, y1 + t, z2, color, alpha);
				renderEdgeBox(pose, consumer, x1, y2 - t, z1, x1 + t, y2, z2, color, alpha);
				renderEdgeBox(pose, consumer, x2 - t, y2 - t, z1, x2, y2, z2, color, alpha);
			}
		}
	}

	static void renderGlueTexture(PoseStack.Pose pose, VertexConsumer consumer, AABB box, int color, int alpha) {
		float x1 = (float) box.minX - SURFACE_OFFSET * 1.5f;
		float y1 = (float) box.minY - SURFACE_OFFSET * 1.5f;
		float z1 = (float) box.minZ - SURFACE_OFFSET * 1.5f;
		float x2 = (float) box.maxX + SURFACE_OFFSET * 1.5f;
		float y2 = (float) box.maxY + SURFACE_OFFSET * 1.5f;
		float z2 = (float) box.maxZ + SURFACE_OFFSET * 1.5f;
		float xs = Math.max(1, x2 - x1);
		float ys = Math.max(1, y2 - y1);
		float zs = Math.max(1, z2 - z1);

		renderTexturedQuad(pose, consumer, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, 0, xs, 0, zs, color, alpha, 0, 1, 0);
		renderTexturedQuad(pose, consumer, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, 0, xs, 0, zs, color, alpha, 0, -1, 0);
		renderTexturedQuad(pose, consumer, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, 0, xs, 0, ys, color, alpha, 0, 0, -1);
		renderTexturedQuad(pose, consumer, x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2, 0, xs, 0, ys, color, alpha, 0, 0, 1);
		renderTexturedQuad(pose, consumer, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2, 0, zs, 0, ys, color, alpha, -1, 0, 0);
		renderTexturedQuad(pose, consumer, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, 0, zs, 0, ys, color, alpha, 1, 0, 0);
	}

	private static void renderEdgeBox(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1,
		float x2, float y2, float z2, int color, int alpha) {
		if (x2 <= x1 || y2 <= y1 || z2 <= z1)
			return;
		renderQuad(pose, consumer, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, color, alpha);
		renderQuad(pose, consumer, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color, alpha);
		renderQuad(pose, consumer, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, color, alpha);
		renderQuad(pose, consumer, x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2, color, alpha);
		renderQuad(pose, consumer, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2, color, alpha);
		renderQuad(pose, consumer, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, color, alpha);
	}

	private static void renderQuad(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1, float x2,
		float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int color, int alpha) {
		addVertex(pose, consumer, x1, y1, z1, color, alpha);
		addVertex(pose, consumer, x2, y2, z2, color, alpha);
		addVertex(pose, consumer, x3, y3, z3, color, alpha);
		addVertex(pose, consumer, x4, y4, z4, color, alpha);
		addVertex(pose, consumer, x4, y4, z4, color, alpha);
		addVertex(pose, consumer, x3, y3, z3, color, alpha);
		addVertex(pose, consumer, x2, y2, z2, color, alpha);
		addVertex(pose, consumer, x1, y1, z1, color, alpha);
	}

	private static void addVertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, int color,
		int alpha) {
		consumer.addVertex(pose, x, y, z)
			.setColor((alpha & 0xFF) << 24 | color);
	}

	private static void renderTexturedQuad(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1,
		float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
		float minU, float maxU, float minV, float maxV, int color, int alpha, float normalX, float normalY, float normalZ) {
		addTexturedVertex(pose, consumer, x1, y1, z1, maxU, minV, color, alpha, normalX, normalY, normalZ);
		addTexturedVertex(pose, consumer, x2, y2, z2, minU, minV, color, alpha, normalX, normalY, normalZ);
		addTexturedVertex(pose, consumer, x3, y3, z3, minU, maxV, color, alpha, normalX, normalY, normalZ);
		addTexturedVertex(pose, consumer, x4, y4, z4, maxU, maxV, color, alpha, normalX, normalY, normalZ);
	}

	private static void addTexturedVertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z,
		float u, float v, int color, int alpha, float normalX, float normalY, float normalZ) {
		consumer.addVertex(pose, x, y, z)
			.setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(LightCoordsUtil.pack(15, 15))
			.setNormal(pose, normalX, normalY, normalZ);
	}

	public static class GlueRenderState extends EntityRenderState {
		AABB localBox;
		boolean selected;
		boolean armPreview;
		int color = HIGHLIGHT;
	}

}
