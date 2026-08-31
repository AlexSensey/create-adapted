package com.simibubi.create.content.processing.burner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.catnip.api.math.AngleHelper;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class BlazeBurnerMovementClient {
	private BlazeBurnerMovementClient() {}

	public static void tick(BlazeBurnerMovementBehaviour behaviour, MovementContext context) {
		if (!behaviour.shouldRender(context))
			return;
		RandomSource random = context.world.getRandom();
		Vec3 center = context.position;
		Vec3 particlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, .125f).multiply(1, 0, 1));
		if (random.nextInt(3) == 0 && context.motion.length() < 1 / 64f)
			context.world.addParticle(ParticleTypes.LARGE_SMOKE, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
		LerpedFloat headAngle = behaviour.getHeadAngle(context);
		boolean quickTurn = behaviour.shouldRenderHat(context) && !Mth.equal(context.relativeMotion.length(), 0);
		headAngle.chase(headAngle.getValue()
			+ AngleHelper.getShortestAngleDiff(headAngle.getValue(), getTargetAngle(behaviour, context)), .5f,
			quickTurn ? Chaser.EXP : Chaser.exp(5));
		headAngle.tickChaser();
	}

	static float getTargetAngle(BlazeBurnerMovementBehaviour behaviour, MovementContext context) {
		if (behaviour.shouldRenderHat(context) && !Mth.equal(context.relativeMotion.length(), 0)
			&& context.contraption.entity instanceof CarriageContraptionEntity carriage) {
			float angle = AngleHelper.deg(-Mth.atan2(context.relativeMotion.x, context.relativeMotion.z));
			return carriage.getInitialOrientation().getAxis() == Axis.X ? angle + 180 : angle;
		}
		Entity camera = Minecraft.getInstance().getCameraEntity();
		if (camera != null && !camera.isInvisible() && context.position != null) {
			Vec3 rotated = context.contraption.entity.reverseRotation(camera.position().subtract(context.position), 1);
			return AngleHelper.deg(-Mth.atan2(rotated.z, rotated.x)) - 90;
		}
		return 0;
	}

	public static void render(BlazeBurnerMovementBehaviour behaviour, MovementContext context,
		VirtualRenderWorld world, ContraptionMatrices matrices, MultiBufferSource buffer) {
		if (behaviour.shouldRender(context))
			BlazeBurnerRenderer.renderInContraption(context, world, matrices, buffer, behaviour.getHeadAngle(context),
				behaviour.shouldRenderHat(context));
	}

	public static void submit(BlazeBurnerMovementBehaviour behaviour, MovementContext context, PoseStack ms,
		SubmitNodeCollector collector, int light) {
		if (behaviour.shouldRender(context))
			BlazeBurnerRenderer.submitInContraption(context, ms, collector, light, behaviour.getHeadAngle(context),
				behaviour.shouldRenderHat(context));
	}
}
