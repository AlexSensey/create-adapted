package com.simibubi.create.content.schematics.client.tools;

import com.simibubi.create.AllDataComponents;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import com.simibubi.create.content.contraptions.glue.SuperGlueRenderer;

import net.createmod.catnip.api.client.render.SuperRenderTypeBuffer;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.outliner.AABBOutline;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DeployTool extends PlacementToolBase {

	@Override
	public void init() {
		super.init();
		selectionRange = -1;
	}

	@Override
	public void updateSelection() {
		if (schematicHandler.isActive() && selectionRange == -1) {
			selectionRange = (int) (schematicHandler.getBounds()
				.getCenter()
				.length() / 2);
			selectionRange = Mth.clamp(selectionRange, 1, 100);
		}
		selectIgnoreBlocks = AllKeys.ACTIVATE_TOOL.isPressed();
		super.updateSelection();
	}

	@Override
	public void renderTool(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera) {
		super.renderTool(ms, buffer, camera);

		if (selectedPos == null)
			return;

		ms.pushPose();
		float pt = AnimationTickHolder.getPartialTicks();
		double x = Mth.lerp(pt, lastChasingSelectedPos.x, chasingSelectedPos.x);
		double y = Mth.lerp(pt, lastChasingSelectedPos.y, chasingSelectedPos.y);
		double z = Mth.lerp(pt, lastChasingSelectedPos.z, chasingSelectedPos.z);

		SchematicTransformation transformation = schematicHandler.getTransformation();
		AABB bounds = schematicHandler.getBounds();
		Vec3 center = bounds.getCenter();
		Vec3 rotationOffset = transformation.getRotationOffset(true);
		int centerX = (int) center.x;
		int centerZ = (int) center.z;
		double xOrigin = bounds.getXsize() / 2f;
		double zOrigin = bounds.getZsize() / 2f;
		Vec3 origin = new Vec3(xOrigin, 0, zOrigin);

		ms.translate(x - centerX - camera.x, y - camera.y, z - centerZ - camera.z);
		TransformStack.of(ms)
			.translate(origin)
			.translate(rotationOffset)
			.rotateYDegrees(transformation.getCurrentRotation())
			.translateBack(rotationOffset)
			.translateBack(origin);

		AABBOutline outline = schematicHandler.getOutline();
		outline.render(ms, buffer, Vec3.ZERO, pt);
		outline.getParams()
			.clearTextures();
		ms.popPose();
	}

	@Override
	public void submitTool(PoseStack ms, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		if (selectedPos == null || schematicHandler.isDeployed())
			return;

		float pt = AnimationTickHolder.getPartialTicks();
		double x = Mth.lerp(pt, lastChasingSelectedPos.x, chasingSelectedPos.x);
		double y = Mth.lerp(pt, lastChasingSelectedPos.y, chasingSelectedPos.y);
		double z = Mth.lerp(pt, lastChasingSelectedPos.z, chasingSelectedPos.z);
		SchematicTransformation transformation = schematicHandler.getTransformation();
		AABB bounds = schematicHandler.getBounds();
		Vec3 center = bounds.getCenter();
		Vec3 rotationOffset = transformation.getRotationOffset(true);
		Vec3 origin = new Vec3(bounds.getXsize() / 2f, 0, bounds.getZsize() / 2f);
		Vec3 camera = cameraRenderState.pos;

		ms.pushPose();
		ms.translate(x - (int) center.x - camera.x, y - camera.y, z - (int) center.z - camera.z);
		TransformStack.of(ms)
			.translate(origin)
			.translate(rotationOffset)
			.rotateYDegrees(transformation.getCurrentRotation())
			.translateBack(rotationOffset)
			.translateBack(origin);
		schematicHandler.submitSchematic(ms, collector, cameraRenderState);
		collector.submitCustomGeometry(ms, RenderTypes.debugQuads(),
			(pose, consumer) -> SuperGlueRenderer.renderWireframe(pose, consumer, bounds, 0x6886c5, 235, 1 / 16f));
		ms.popPose();
	}

	@Override
	public boolean handleMouseWheel(double delta) {
		if (!selectIgnoreBlocks)
			return super.handleMouseWheel(delta);
		selectionRange += delta;
		selectionRange = Mth.clamp(selectionRange, 1, 100);
		return true;
	}

	@Override
	public boolean handleRightClick() {
		if (selectedPos == null)
			return super.handleRightClick();
		Vec3 center = schematicHandler.getBounds()
			.getCenter();
		BlockPos target = selectedPos.offset(-((int) center.x), 0, -((int) center.z));

		ItemStack item = schematicHandler.getActiveSchematicItem();
		if (item != null) {
			item.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
			item.set(AllDataComponents.SCHEMATIC_ANCHOR, target);
			schematicHandler.getTransformation()
				.startAt(target);
		}

		schematicHandler.getTransformation()
			.moveTo(target);
		schematicHandler.markDirty();
		schematicHandler.deploy();
		return true;
	}

}
