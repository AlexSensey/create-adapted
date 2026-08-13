package com.simibubi.create.content.equipment.zapper;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllSoundEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ZapperRenderHandler extends ShootableGadgetRenderHandler {

	public List<LaserBeam> cachedBeams;

	@Override
	protected boolean appliesTo(ItemStack stack) {
		return stack.getItem() instanceof ZapperItem;
	}

	@Override
	public void tick() {
		super.tick();
		if (cachedBeams == null)
			cachedBeams = new LinkedList<>();
		cachedBeams.removeIf(b -> b.itensity < .1f);
		cachedBeams.forEach(b -> {
			if (b.age++ > 0)
				b.itensity *= .6f;
		});
	}

	public void submit(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
		if (cachedBeams == null || cachedBeams.isEmpty())
			return;
		List<BeamRenderData> beams = cachedBeams.stream()
			.map(b -> new BeamRenderData(b.start, b.end, b.itensity))
			.toList();
		collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, consumer) -> {
			for (BeamRenderData beam : beams)
				renderBeam(pose, consumer, beam, camera);
		});
	}

	private static void renderBeam(PoseStack.Pose pose, VertexConsumer consumer, BeamRenderData beam, Vec3 camera) {
		// Match the legacy end-chasing beam: it starts at full length, then its tail
		// rapidly catches the fixed impact point. This reads as a short fast shot,
		// rather than a slowly growing ray across the whole range.
		Vec3 start = beam.end.lerp(beam.start, beam.intensity).subtract(camera);
		Vec3 end = beam.end.subtract(camera);
		Vec3 direction = end.subtract(start);
		if (direction.lengthSqr() < 1e-6)
			return;
		direction = direction.normalize();
		Vec3 reference = Math.abs(direction.y) < .9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
		double half = .025 + beam.intensity * .055;
		Vec3 side = direction.cross(reference).normalize().scale(half);
		Vec3 up = direction.cross(side).normalize().scale(half);
		Vec3[] a = {start.add(side).add(up), start.add(side).subtract(up), start.subtract(side).subtract(up), start.subtract(side).add(up)};
		Vec3[] b = {end.add(side).add(up), end.add(side).subtract(up), end.subtract(side).subtract(up), end.subtract(side).add(up)};
		for (int i = 0; i < 4; i++)
			quad(pose, consumer, a[i], a[(i + 1) % 4], b[(i + 1) % 4], b[i]);
		quad(pose, consumer, a[3], a[2], a[1], a[0]);
		quad(pose, consumer, b[0], b[1], b[2], b[3]);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
		for (Vec3 point : new Vec3[] {a, b, c, d, d, c, b, a})
			consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
				.setColor(0xE8FFFFFF);
	}

	@Override
	protected void transformTool(PoseStack ms, float flip, float equipProgress, float recoil, float pt) {
		ms.translate(flip * -0.1f, 0.1f, -0.4f);
		ms.mulPose(Axis.YP.rotationDegrees(flip * 5.0F));
	}

	@Override
	protected void transformHand(PoseStack ms, float flip, float equipProgress, float recoil, float pt) {}

	@Override
	protected void playSound(InteractionHand hand, Vec3 position) {
		float pitch = hand == InteractionHand.MAIN_HAND ? 0.1f : 0.9f;
		Minecraft mc = Minecraft.getInstance();
		AllSoundEvents.WORLDSHAPER_PLACE.play(mc.level, mc.player, position, 0.1f, pitch);
	}

	public void addBeam(LaserBeam beam) {
		if (cachedBeams == null)
			cachedBeams = new LinkedList<>();
		ClientLevel world = Minecraft.getInstance().level;
		if (world == null)
			return;
		RandomSource random = world.getRandom();
		double x = beam.end.x;
		double y = beam.end.y;
		double z = beam.end.z;
		Supplier<Double> randomSpeed = () -> (random.nextDouble() - .5d) * .2f;
		Supplier<Double> randomOffset = () -> (random.nextDouble() - .5d) * .2f;
		for (int i = 0; i < 10; i++) {
			world.addParticle(ParticleTypes.END_ROD, x, y, z, randomSpeed.get(), randomSpeed.get(), randomSpeed.get());
			world.addParticle(ParticleTypes.FIREWORK, x + randomOffset.get(), y + randomOffset.get(),
				z + randomOffset.get(), 0, 0, 0);
		}
		cachedBeams.add(beam);
	}

	public static class LaserBeam {
		float itensity;
		int age;
		Vec3 start;
		Vec3 end;

		public LaserBeam(Vec3 start, Vec3 end) {
			this.start = start;
			this.end = end;
			itensity = 1;
			age = 0;
		}
	}

	private record BeamRenderData(Vec3 start, Vec3 end, float intensity) {}
}
