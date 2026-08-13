package com.simibubi.create.content.contraptions.minecart;

import static net.minecraft.util.Mth.lerp;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.phys.Vec3;

public class CouplingRenderer {

	public static void renderAll(PoseStack ms, MultiBufferSource buffer, Vec3 camera) {}

	public static void tickDebugModeRenders() {}

	public static void submit(PoseStack ms, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		if (Minecraft.getInstance().level == null)
			return;
		CouplingHandler.forEachLoadedCoupling(Minecraft.getInstance().level, coupling -> {
			if (coupling.getFirst().hasContraptionCoupling(true))
				return;
			submitCoupling(ms, collector, cameraRenderState.pos, coupling.map(MinecartController::cart));
		});
	}

	private static void submitCoupling(PoseStack ms, SubmitNodeCollector collector, Vec3 camera,
		Couple<AbstractMinecart> carts) {
		AbstractMinecart firstCart = carts.getFirst();
		AbstractMinecart secondCart = carts.getSecond();
		if (firstCart == null || secondCart == null || Minecraft.getInstance().level == null)
			return;

		float partialTicks = AnimationTickHolder.getPartialTicks();
		Vec3 firstPosition = interpolatedPosition(firstCart, partialTicks);
		Vec3 secondPosition = interpolatedPosition(secondCart, partialTicks);
		Vec3 center = firstPosition.add(secondPosition).scale(.5);
		CartEndpoint first = endpoint(firstCart, firstPosition, center, partialTicks);
		CartEndpoint second = endpoint(secondCart, secondPosition, center, partialTicks);
		Vec3 firstEndpoint = first.position();
		Vec3 secondEndpoint = second.position();
		Vec3 difference = secondEndpoint.subtract(firstEndpoint);
		if (difference.lengthSqr() < 1e-6)
			return;

		float connectorYaw = (float) (-Math.atan2(difference.z, difference.x) * Mth.RAD_TO_DEG);
		float connectorPitch = (float) (Math.atan2(difference.y, difference.multiply(1, 0, 1).length())
			* Mth.RAD_TO_DEG);
		int firstLight = LightCoordsUtil.getLightCoords(Minecraft.getInstance().level,
			BlockPos.containing(firstCart.getBoundingBox().getCenter()));
		int secondLight = LightCoordsUtil.getLightCoords(Minecraft.getInstance().level,
			BlockPos.containing(secondCart.getBoundingBox().getCenter()));
		int connectorLight = meanLight(firstLight, secondLight);

		submitEndpoint(ms, collector, camera, first, connectorYaw, firstLight);
		submitEndpoint(ms, collector, camera, second, connectorYaw, secondLight);

		BlockStateModelPart connector = model(CreateStandaloneModels.MINECART_COUPLING_CONNECTOR);
		if (connector == null)
			return;
		ms.pushPose();
		ms.translate(firstEndpoint.x - camera.x, firstEndpoint.y - camera.y, firstEndpoint.z - camera.z);
		ms.mulPose(Axis.YP.rotationDegrees(connectorYaw));
		ms.mulPose(Axis.ZP.rotationDegrees(connectorPitch));
		ms.scale((float) difference.length(), 1, 1);
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(connector),
			BlockModelRenderState.EMPTY_TINTS, connectorLight, 0, 0);
		ms.popPose();
	}

	private static void submitEndpoint(PoseStack ms, SubmitNodeCollector collector, Vec3 camera,
		CartEndpoint endpoint, float connectorYaw, int light) {
		BlockStateModelPart attachment = model(CreateStandaloneModels.MINECART_COUPLING_ATTACHMENT);
		BlockStateModelPart ring = model(CreateStandaloneModels.MINECART_COUPLING_RING);
		if (attachment == null || ring == null)
			return;

		ms.pushPose();
		ms.translate(endpoint.origin.x - camera.x, endpoint.origin.y - camera.y, endpoint.origin.z - camera.z);
		ms.mulPose(Axis.YP.rotationDegrees(endpoint.yaw));
		ms.mulPose(Axis.ZP.rotationDegrees(endpoint.pitch));
		ms.mulPose(Axis.XP.rotationDegrees(endpoint.roll));
		ms.translate(endpoint.offset, 0, 0);
		ms.mulPose(Axis.YP.rotationDegrees(endpoint.flip ? 180 : 0));
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(attachment),
			BlockModelRenderState.EMPTY_TINTS, light, 0, 0);
		ms.mulPose(Axis.YP.rotationDegrees(connectorYaw - endpoint.yaw));
		collector.submitBlockModel(ms, RenderTypes.cutoutMovingBlock(), List.of(ring),
			BlockModelRenderState.EMPTY_TINTS, light, 0, 0);
		ms.popPose();
	}

	private static CartEndpoint endpoint(AbstractMinecart cart, Vec3 position, Vec3 center, float partialTicks) {
		long seed = cart.getId() * 493286711L;
		seed = seed * seed * 4392167121L + seed * 98761L;
		double x = (((float) (seed >> 16 & 7L) + .5f) / 8f - .5f) * .004f;
		double y = (((float) (seed >> 20 & 7L) + .5f) / 8f - .5f) * .004f + .375f;
		double z = (((float) (seed >> 24 & 7L) + .5f) / 8f - .5f) * .004f;

		float yaw = lerp(partialTicks, cart.yRotO, cart.getYRot());
		float pitch = lerp(partialTicks, cart.xRotO, cart.getXRot());
		float roll = cart.getHurtTime() - partialTicks;
		float rollAmplifier = Math.max(0, cart.getDamage() - partialTicks);
		roll = roll > 0 ? Mth.sin(roll) * roll * rollAmplifier / 10f * cart.getHurtDir() : 0;

		Vec3 front = position.add(VecHelper.rotate(new Vec3(.5, 0, 0), 180 - yaw, Direction.Axis.Y));
		Vec3 back = position.add(VecHelper.rotate(new Vec3(-.5, 0, 0), 180 - yaw, Direction.Axis.Y));
		Vec3 railPosition = null;
		if (cart.getBehavior() instanceof OldMinecartBehavior behavior)
			railPosition = behavior.getPos(position.x, position.y, position.z);

		if (railPosition != null && cart.getBehavior() instanceof OldMinecartBehavior behavior) {
			Vec3 railFront = behavior.getPosOffs(position.x, position.y, position.z, .3);
			Vec3 railBack = behavior.getPosOffs(position.x, position.y, position.z, -.3);
			front = railFront == null ? railPosition : railFront;
			back = railBack == null ? railPosition : railBack;
			x += railPosition.x;
			y += (front.y + back.y) / 2;
			z += railPosition.z;
			Vec3 railDirection = back.subtract(front);
			if (railDirection.lengthSqr() > 1e-6) {
				railDirection = railDirection.normalize();
				yaw = (float) (Math.atan2(railDirection.z, railDirection.x) * Mth.RAD_TO_DEG);
				pitch = (float) (Math.atan(railDirection.y) * 73);
			}
		} else {
			x += position.x;
			y += position.y;
			z += position.z;
		}

		boolean flip = front.distanceToSqr(center) > back.distanceToSqr(center);
		float offset = flip ? -13 / 16f : 13 / 16f;
		return new CartEndpoint(new Vec3(x, y + 2 / 16f, z), 180 - yaw, -pitch, roll, offset, flip);
	}

	private static Vec3 interpolatedPosition(AbstractMinecart cart, float partialTicks) {
		return new Vec3(lerp(partialTicks, cart.xOld, cart.getX()), lerp(partialTicks, cart.yOld, cart.getY()),
			lerp(partialTicks, cart.zOld, cart.getZ()));
	}

	private static BlockStateModelPart model(
		net.neoforged.neoforge.client.model.standalone.StandaloneModelKey<BlockStateModelPart> key) {
		return Minecraft.getInstance().getModelManager().getStandaloneModel(key);
	}

	private static int meanLight(int first, int second) {
		int block = (((first >> 4) & 0xf) + ((second >> 4) & 0xf)) / 2;
		int sky = (((first >> 20) & 0xf) + ((second >> 20) & 0xf)) / 2;
		return sky << 20 | block << 4;
	}

	private record CartEndpoint(Vec3 origin, float yaw, float pitch, float roll, float offset, boolean flip) {
		private Vec3 position() {
			Vec3 position = new Vec3(offset, 0, 0);
			position = VecHelper.rotate(position, roll, Direction.Axis.X);
			position = VecHelper.rotate(position, pitch, Direction.Axis.Z);
			position = VecHelper.rotate(position, yaw, Direction.Axis.Y);
			return position.add(origin);
		}
	}
}
