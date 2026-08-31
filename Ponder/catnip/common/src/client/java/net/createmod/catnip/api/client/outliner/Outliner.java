package net.createmod.catnip.api.client.outliner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.client.outliner.LineOutline.EndChasingLineOutline;
import net.createmod.catnip.api.client.outliner.Outline.OutlineParams;
import net.createmod.catnip.api.client.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Outliner {
	// Singleton
	private static final Outliner instance = new Outliner();

	public static Outliner getInstance() {
		return instance;
	}

	//

	public Outliner() {
	}

	private final Map<Object, OutlineEntry> outlines = Collections.synchronizedMap(new HashMap<>());
	private final Map<Object, OutlineEntry> outlinesView = Collections.unmodifiableMap(outlines);

	// Facade

	public OutlineParams showOutline(Object slot, Outline outline) {
		outlines.put(slot, new OutlineEntry(outline));
		return outline.getParams();
	}

	public OutlineParams showLine(Object slot, Vec3 start, Vec3 end) {
		if (!outlines.containsKey(slot)) {
			LineOutline outline = new LineOutline();
			addOutline(slot, outline);
		}
		OutlineEntry entry = outlines.get(slot);
		entry.ticksTillRemoval = 1;
		((LineOutline) entry.outline).set(start, end);
		return entry.outline.getParams();
	}

	public OutlineParams endChasingLine(Object slot, Vec3 start, Vec3 end, float chasingProgress, boolean lockStart) {
		if (!outlines.containsKey(slot)) {
			EndChasingLineOutline outline = new EndChasingLineOutline(lockStart);
			addOutline(slot, outline);
		}
		OutlineEntry entry = outlines.get(slot);
		entry.ticksTillRemoval = 1;
		((EndChasingLineOutline) entry.outline).setProgress(chasingProgress)
			.set(start, end);
		return entry.outline.getParams();
	}

	public OutlineParams showAABB(Object slot, AABB bb, int ttl) {
		createAABBOutlineIfMissing(slot, bb);
		ChasingAABBOutline outline = getAndRefreshAABB(slot, ttl);
		outline.prevBB = outline.targetBB = outline.bb = bb;
		return outline.getParams();
	}

	public OutlineParams showAABB(Object slot, AABB bb) {
		createAABBOutlineIfMissing(slot, bb);
		ChasingAABBOutline outline = getAndRefreshAABB(slot);
		outline.prevBB = outline.targetBB = outline.bb = bb;
		return outline.getParams();
	}

	public OutlineParams chaseAABB(Object slot, AABB bb) {
		createAABBOutlineIfMissing(slot, bb);
		ChasingAABBOutline outline = getAndRefreshAABB(slot);
		outline.targetBB = bb;
		return outline.getParams();
	}

	public OutlineParams showCluster(Object slot, Iterable<BlockPos> selection) {
		BlockClusterOutline outline = new BlockClusterOutline(selection);
		addOutline(slot, outline);
		return outline.getParams();
	}

	//

	public OutlineParams showItem(Object slot, Vec3 pos, ItemStack stack) {
		ItemOutline outline = new ItemOutline(pos, stack);
		OutlineEntry entry = new OutlineEntry(outline);
		outlines.put(slot, entry);
		return entry.getOutline().getParams();
	}

	public void keep(Object slot) {
		if (outlines.containsKey(slot))
			outlines.get(slot).ticksTillRemoval = 1;
	}

	public void remove(Object slot) {
		outlines.remove(slot);
	}

	public Optional<OutlineParams> edit(Object slot) {
		keep(slot);
		if (outlines.containsKey(slot))
			return Optional.of(outlines.get(slot)
				.getOutline()
				.getParams());
		return Optional.empty();
	}

	public Map<Object, OutlineEntry> getOutlines() {
		return outlinesView;
	}

	// Utility

	private void addOutline(Object slot, Outline outline) {
		outlines.put(slot, new OutlineEntry(outline));
	}

	private void createAABBOutlineIfMissing(Object slot, AABB bb) {
		if (!outlines.containsKey(slot) || !(outlines.get(slot).outline instanceof AABBOutline)) {
			ChasingAABBOutline outline = new ChasingAABBOutline(bb);
			addOutline(slot, outline);
		}
	}

	private ChasingAABBOutline getAndRefreshAABB(Object slot) {
		return getAndRefreshAABB(slot, 1);
	}

	private ChasingAABBOutline getAndRefreshAABB(Object slot, int ttl) {
		OutlineEntry entry = outlines.get(slot);
		entry.ticksTillRemoval = ttl;
		return (ChasingAABBOutline) entry.getOutline();
	}

	// Maintenance

	public void tickOutlines() {
		Iterator<OutlineEntry> iterator = outlines.values()
			.iterator();
		while (iterator.hasNext()) {
			OutlineEntry entry = iterator.next();
			entry.tick();
			if (!entry.isAlive())
				iterator.remove();
		}
	}

	public void renderOutlines(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
		outlines.forEach((key, entry) -> {
			Outline outline = entry.getOutline();
			OutlineParams params = outline.getParams();
			params.alpha = 1;
			if (entry.isFading()) {
				int prevTicks = entry.ticksTillRemoval + 1;
				float fadeticks = OutlineEntry.FADE_TICKS;
				float lastAlpha = prevTicks >= 0 ? 1 : 1 + (prevTicks / fadeticks);
				float currentAlpha = 1 + (entry.ticksTillRemoval / fadeticks);
				float alpha = Mth.lerp(pt, lastAlpha, currentAlpha);

				params.alpha = alpha * alpha * alpha;
				if (params.alpha < 1 / 8f)
					return;
			}
			outline.render(ms, buffer, camera, pt);
		});
	}

	/** Submit outlines through the 26.2 render-state queue (including PIP renderers). */
	public void submitOutlines(PoseStack ms, SubmitNodeCollector queue, Vec3 camera, float pt) {
		outlines.forEach((key, entry) -> {
			Outline outline = entry.getOutline();
			if (!prepareOutline(entry, outline, pt))
				return;
			submitOutline(ms, queue, outline, camera, pt);
		});
	}

	/** Submit one standalone outline through the render-state queue. */
	public static void submitOutline(PoseStack ms, SubmitNodeCollector queue, Outline outline, Vec3 camera, float pt) {
		CollectingBuffer collecting = new CollectingBuffer();
		outline.render(ms, collecting, camera, pt);
		for (RenderType type : collecting.types) {
			queue.submitCustomGeometry(ms, type, (pose, consumer) -> {
				PoseStack deferred = new PoseStack();
				deferred.last().pose().set(pose.pose());
				deferred.last().normal().set(pose.normal());
				outline.render(deferred, new SingleTypeBuffer(type, consumer), camera, pt);
			});
		}
	}

	private static boolean prepareOutline(OutlineEntry entry, Outline outline, float pt) {
		OutlineParams params = outline.getParams();
		params.alpha = 1;
		if (!entry.isFading())
			return true;

		int prevTicks = entry.ticksTillRemoval + 1;
		float fadeTicks = OutlineEntry.FADE_TICKS;
		float lastAlpha = prevTicks >= 0 ? 1 : 1 + prevTicks / fadeTicks;
		float currentAlpha = 1 + entry.ticksTillRemoval / fadeTicks;
		float alpha = Mth.lerp(pt, lastAlpha, currentAlpha);
		params.alpha = alpha * alpha * alpha;
		return params.alpha >= 1 / 8f;
	}

	private static final VertexConsumer NOOP_CONSUMER = new VertexConsumer() {
		@Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
		@Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
		@Override public VertexConsumer setColor(int color) { return this; }
		@Override public VertexConsumer setUv(float u, float v) { return this; }
		@Override public VertexConsumer setUv1(int u, int v) { return this; }
		@Override public VertexConsumer setUv2(int u, int v) { return this; }
		@Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
		@Override public VertexConsumer setLineWidth(float width) { return this; }
	};

	private static class CollectingBuffer implements SuperRenderTypeBuffer {
		private final Set<RenderType> types = new LinkedHashSet<>();
		private VertexConsumer collect(RenderType type) { types.add(type); return NOOP_CONSUMER; }
		@Override public VertexConsumer getEarlyBuffer(RenderType type) { return collect(type); }
		@Override public VertexConsumer getBuffer(RenderType type) { return collect(type); }
		@Override public VertexConsumer getLateBuffer(RenderType type) { return collect(type); }
		@Override public void draw() {}
		@Override public void draw(RenderType type) {}
	}

	private static class SingleTypeBuffer implements SuperRenderTypeBuffer {
		private final RenderType target;
		private final VertexConsumer consumer;
		private SingleTypeBuffer(RenderType target, VertexConsumer consumer) {
			this.target = target;
			this.consumer = consumer;
		}
		private VertexConsumer select(RenderType type) { return target.equals(type) ? consumer : NOOP_CONSUMER; }
		@Override public VertexConsumer getEarlyBuffer(RenderType type) { return select(type); }
		@Override public VertexConsumer getBuffer(RenderType type) { return select(type); }
		@Override public VertexConsumer getLateBuffer(RenderType type) { return select(type); }
		@Override public void draw() {}
		@Override public void draw(RenderType type) {}
	}

	public static class OutlineEntry {
		public static final int FADE_TICKS = 8;

		private final Outline outline;
		private int ticksTillRemoval = 1;

		public OutlineEntry(Outline outline) {
			this.outline = outline;
		}

		public Outline getOutline() {
			return outline;
		}

		public int getTicksTillRemoval() {
			return ticksTillRemoval;
		}

		public boolean isAlive() {
			return ticksTillRemoval >= -FADE_TICKS;
		}

		public boolean isFading() {
			return ticksTillRemoval < 0;
		}

		public void tick() {
			ticksTillRemoval--;
			outline.tick();
		}
	}
}
