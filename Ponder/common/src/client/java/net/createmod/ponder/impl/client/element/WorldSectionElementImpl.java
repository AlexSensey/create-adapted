package net.createmod.ponder.impl.client.element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.outliner.AABBOutline;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.createmod.catnip.api.client.render.RenderHelper;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.client.render.SuperByteBufferBuilder;
import net.createmod.catnip.api.client.render.SuperByteBufferCache;
import net.createmod.catnip.api.client.render.SuperByteBufferCache.Compartment;
import net.createmod.catnip.api.client.render.SuperRenderTypeBuffer;
import net.createmod.catnip.api.client.render.model.BakedModelBufferer;
import net.createmod.catnip.api.client.render.model.ShadeSeparatedResultConsumer;
import net.createmod.catnip.api.data.Pair;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.api.client.element.WorldSectionElement;
import net.createmod.ponder.api.client.level.PonderLevel;
import net.createmod.ponder.api.client.scene.PonderScene;
import net.createmod.ponder.api.client.scene.Selection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WorldSectionElementImpl extends AnimatedSceneElementBase implements WorldSectionElement {

	public static final Compartment<Pair<Integer, Integer>> PONDER_WORLD_SECTION = new Compartment<>();

	private static final CardinalLighting SCENE_LIGHTING = new CardinalLighting(0.4f, 1f, 0.7f, 1f, 0.5f, 0.6f);
	private static final ThreadLocal<ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(ThreadLocalObjects::new);

	@Nullable
	List<BlockEntity> renderedBlockEntities;
	@Nullable
	List<Pair<BlockEntity, Consumer<Level>>> tickableBlockEntities;
	@Nullable
	List<RenderedStaticBlock> renderedStaticBlocks;
	@Nullable
	Selection section;
	boolean redraw;

	Vec3 prevAnimatedOffset = Vec3.ZERO;
	Vec3 animatedOffset = Vec3.ZERO;
	Vec3 prevAnimatedRotation = Vec3.ZERO;
	Vec3 animatedRotation = Vec3.ZERO;
	Vec3 centerOfRotation = Vec3.ZERO;
	@Nullable
	Vec3 stabilizationAnchor = null;

	@Nullable
	BlockPos selectedBlock;

	public WorldSectionElementImpl() {
	}

	public WorldSectionElementImpl(Selection section) {
		this.section = section.copy();
		centerOfRotation = section.getCenter();
	}

	@Override
	public void mergeOnto(WorldSectionElement other) {
		setVisible(false);
		if (other.isEmpty())
			other.set(section);
		else
			other.add(section);
	}

	@Override
	public void set(Selection selection) {
		applyNewSelection(selection.copy());
	}

	@Override
	public void add(Selection toAdd) {
		applyNewSelection(this.section.add(toAdd));
	}

	@Override
	public void erase(Selection toErase) {
		applyNewSelection(this.section.substract(toErase));
	}

	private void applyNewSelection(Selection selection) {
		this.section = selection;
		queueRedraw();
	}

	@Override
	public void setCenterOfRotation(Vec3 center) {
		centerOfRotation = center;
	}

	@Override
	public void stabilizeRotation(Vec3 anchor) {
		stabilizationAnchor = anchor;
	}

	@Override
	public void reset(PonderScene scene) {
		super.reset(scene);
		resetAnimatedTransform();
		resetSelectedBlock();
	}

	@Override
	public void selectBlock(BlockPos pos) {
		selectedBlock = pos;
	}

	@Override
	public void resetSelectedBlock() {
		selectedBlock = null;
	}

	public void resetAnimatedTransform() {
		prevAnimatedOffset = Vec3.ZERO;
		animatedOffset = Vec3.ZERO;
		prevAnimatedRotation = Vec3.ZERO;
		animatedRotation = Vec3.ZERO;
	}

	@Override
	public void queueRedraw() {
		redraw = true;
	}

	@Override
	public boolean isEmpty() {
		return section == null;
	}

	@Override
	public void setEmpty() {
		section = null;
	}

	@Override
	public void setAnimatedRotation(Vec3 eulerAngles, boolean force) {
		this.animatedRotation = eulerAngles;
		if (force)
			prevAnimatedRotation = animatedRotation;
	}

	@Override
	public Vec3 getAnimatedRotation() {
		return animatedRotation;
	}

	@Override
	public void setAnimatedOffset(Vec3 offset, boolean force) {
		this.animatedOffset = offset;
		if (force)
			prevAnimatedOffset = animatedOffset;
	}

	@Override
	public Vec3 getAnimatedOffset() {
		return animatedOffset;
	}

	@Override
	public boolean isVisible() {
		return super.isVisible() && !isEmpty();
	}

	@Override
	public Pair<Vec3, BlockHitResult> rayTrace(PonderLevel world, Vec3 source, Vec3 target) {
		Vec3 transformedSource = reverseTransformVec(source);
		Vec3 transformedTarget = reverseTransformVec(target);
		BlockHitResult rayTraceBlocks = null;
		double nearestDistance = Double.MAX_VALUE;

		// Level.clip() only visits the block cells crossed by the ray. Some Create
		// blocks (most notably large cogwheels) have an outline extending outside
		// their own cell, so their visible teeth could not be selected. Test every
		// block in this world section directly against its complete outline shape.
		for (BlockPos pos : section) {
			BlockState state = world.getBlockState(pos);
			if (state.isAir())
				continue;
			VoxelShape shape = getHoverShape(world, pos, state);
			if (shape.isEmpty())
				continue;
			BlockHitResult hit = shape.clip(transformedSource, transformedTarget, pos);
			if (hit == null)
				continue;
			double distance = hit.getLocation()
				.distanceToSqr(transformedSource);
			if (distance >= nearestDistance)
				continue;
			nearestDistance = distance;
			rayTraceBlocks = hit;
		}

		if (rayTraceBlocks == null)
			return null;

		double t = rayTraceBlocks.getLocation()
			.subtract(transformedTarget)
			.lengthSqr()
			/ source.subtract(target)
			.lengthSqr();
		Vec3 actualHit = VecHelper.lerp((float) t, target, source);
		return Pair.of(actualHit, rayTraceBlocks);
	}

	private VoxelShape getHoverShape(PonderLevel world, BlockPos pos, BlockState state) {
		VoxelShape shape = state.getShape(world, pos, CollisionContext.empty());
		String blockPath = RegisteredObjectsHelper.getKeyOrThrow(state.getBlock())
			.getPath();
		if (!blockPath.equals("large_cogwheel") || !state.hasProperty(BlockStateProperties.AXIS))
			return shape;

		// The large cogwheel model reaches from -7 to 23 model pixels on the
		// two axes perpendicular to its shaft. Its vanilla outline shape stays
		// inside 0..16, which leaves the visible teeth outside the hover target.
		double min = -7 / 16d;
		double max = 23 / 16d;
		double faceMin = 5.75 / 16d;
		double faceMax = 10.25 / 16d;
		AABB gearFace = switch (state.getValue(BlockStateProperties.AXIS)) {
			case X -> new AABB(faceMin, min, min, faceMax, max, max);
			case Y -> new AABB(min, faceMin, min, max, faceMax, max);
			case Z -> new AABB(min, min, faceMin, max, max, faceMax);
		};
		return Shapes.or(shape, Shapes.create(gearFace));
	}

	private Vec3 reverseTransformVec(Vec3 in) {
		float pt = AnimationTickHolder.getPartialTicks();
		in = in.subtract(VecHelper.lerp(pt, prevAnimatedOffset, animatedOffset));
		if (!animatedRotation.equals(Vec3.ZERO) || !prevAnimatedRotation.equals(Vec3.ZERO)) {
			double rotX = Mth.lerp(pt, prevAnimatedRotation.x, animatedRotation.x);
			double rotZ = Mth.lerp(pt, prevAnimatedRotation.z, animatedRotation.z);
			double rotY = Mth.lerp(pt, prevAnimatedRotation.y, animatedRotation.y);
			in = in.subtract(centerOfRotation);
			in = VecHelper.rotate(in, -rotX, Direction.Axis.X);
			in = VecHelper.rotate(in, -rotZ, Direction.Axis.Z);
			in = VecHelper.rotate(in, -rotY, Direction.Axis.Y);
			in = in.add(centerOfRotation);
			if (stabilizationAnchor != null) {
				in = in.subtract(stabilizationAnchor);
				in = VecHelper.rotate(in, rotX, Direction.Axis.X);
				in = VecHelper.rotate(in, rotZ, Direction.Axis.Z);
				in = VecHelper.rotate(in, rotY, Direction.Axis.Y);
				in = in.add(stabilizationAnchor);
			}
		}
		return in;
	}

	public void transformMS(PoseStack ms, float pt) {

		Vec3 vec = VecHelper.lerp(pt, prevAnimatedOffset, animatedOffset);
		ms.translate(vec.x, vec.y, vec.z);
		if (!animatedRotation.equals(Vec3.ZERO) || !prevAnimatedRotation.equals(Vec3.ZERO)) {
			double rotX = Mth.lerp(pt, prevAnimatedRotation.x, animatedRotation.x);
			double rotZ = Mth.lerp(pt, prevAnimatedRotation.z, animatedRotation.z);
			double rotY = Mth.lerp(pt, prevAnimatedRotation.y, animatedRotation.y);

			ms.translate(centerOfRotation);
			ms.mulPose(Axis.XP.rotationDegrees((float) rotX));
			ms.mulPose(Axis.YP.rotationDegrees((float) rotY));
			ms.mulPose(Axis.ZP.rotationDegrees((float) rotZ));
			ms.translate(-centerOfRotation.x, -centerOfRotation.y, -centerOfRotation.z);

			if (stabilizationAnchor != null) {
				ms.translate(stabilizationAnchor);
				ms.mulPose(Axis.XP.rotationDegrees((float) -rotX));
				ms.mulPose(Axis.YP.rotationDegrees((float) -rotY));
				ms.mulPose(Axis.ZP.rotationDegrees((float) -rotZ));
				ms.translate(-stabilizationAnchor.x, -stabilizationAnchor.y, -stabilizationAnchor.z);
			}
		}
	}

	@Override
	public void tick(PonderScene scene) {
		prevAnimatedOffset = animatedOffset;
		prevAnimatedRotation = animatedRotation;
		if (!isVisible())
			return;
		loadBEsIfMissing(scene.getWorld());
		renderedBlockEntities.removeIf(be -> scene.getWorld()
			.getBlockEntity(be.getBlockPos()) != be);
		tickableBlockEntities.removeIf(be -> scene.getWorld()
			.getBlockEntity(be.getFirst()
				.getBlockPos()) != be.getFirst());
		tickableBlockEntities.forEach(be -> be.getSecond()
			.accept(scene.getWorld()));
	}

	@Override
	public void whileSkipping(PonderScene scene) {
		if (redraw) {
			renderedBlockEntities = null;
			tickableBlockEntities = null;
			renderedStaticBlocks = null;
		}
		redraw = false;
	}

	protected void loadBEsIfMissing(PonderLevel world) {
		if (renderedBlockEntities != null)
			return;
		tickableBlockEntities = new ArrayList<>();
		renderedBlockEntities = new ArrayList<>();
		section.forEach(pos -> {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			BlockState blockState = world.getBlockState(pos);
			Block block = blockState.getBlock();
			if (blockEntity == null)
				return;
			if (!(block instanceof EntityBlock))
				return;
			blockEntity.setBlockState(world.getBlockState(pos));
			BlockEntityTicker<?> ticker = ((EntityBlock) block).getTicker(world, blockState, blockEntity.getType());
			if (ticker != null)
				addTicker(blockEntity, ticker);
			renderedBlockEntities.add(blockEntity);
		});
	}

	@SuppressWarnings("unchecked")
	private <T extends BlockEntity> void addTicker(T blockEntity, BlockEntityTicker<?> ticker) {
		tickableBlockEntities.add(Pair.of(blockEntity, w -> ((BlockEntityTicker<T>) ticker).tick(w,
			blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity)));
	}

	@Override
	protected void renderFirst(PonderLevel level, MultiBufferSource buffer, SubmitNodeCollector queue, Camera camera,
	                           CameraRenderState cameraRenderState, PoseStack poseStack, float fade, float pt) {
		int light = -1;
		if (fade != 1)
			light = (int) (Mth.lerp(fade, 5, 15));
		if (redraw) {
			renderedBlockEntities = null;
			tickableBlockEntities = null;
		}

		poseStack.pushPose();
		transformMS(poseStack, pt);
		level.pushFakeLight(light);
		level.pushCardinalLighting(SCENE_LIGHTING);
		renderBlockEntities(level, poseStack, queue, camera, cameraRenderState, poseStack, pt);
		level.popCardinalLighting();
		level.popLight();

		Map<BlockPos, Integer> blockBreakingProgressions = level.getBlockBreakingProgressions();
		PoseStack overlayMS = null;

		for (Entry<BlockPos, Integer> entry : blockBreakingProgressions.entrySet()) {
			BlockPos pos = entry.getKey();
			if (!section.test(pos))
				continue;

			if (overlayMS == null) {
				overlayMS = new PoseStack();
				overlayMS.last().pose().set(poseStack.last().pose());
				overlayMS.last().normal().set(poseStack.last().normal());
			}

			int progress = entry.getValue();

			poseStack.pushPose();
			poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
			BlockState state = level.getBlockState(pos);
			BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
			List<BlockStateModelPart> parts = new ArrayList<>();
			model.collectParts(RandomSource.create(state.getSeed(pos)), parts);
			queue.submitBreakingBlockModel(poseStack, parts, progress);
			poseStack.popPose();
		}

		poseStack.popPose();
	}

	@Override
	protected void renderLayer(PonderLevel world, MultiBufferSource buffer, ChunkSectionLayer layer,
							   SubmitNodeCollector queue, Camera camera, CameraRenderState cameraRenderState,
							   PoseStack poseStack, float fade, float pt) {
		// The legacy immediate SuperByteBuffer path is not captured by 26.2's
		// picture-in-picture render queue. Submit the complete static block models
		// once through the new queue; dynamic block-entity parts are submitted in
		// renderFirst().
		if (layer != ChunkSectionLayer.CUTOUT)
			return;

		if (redraw)
			renderedStaticBlocks = null;
		loadStaticBlocksIfMissing(world);
		int light = lightCoordsFromFade(fade);
		poseStack.pushPose();
		transformMS(poseStack, pt);
		for (RenderedStaticBlock rendered : renderedStaticBlocks) {
			poseStack.pushPose();
			poseStack.translate(rendered.pos().getX(), rendered.pos().getY(), rendered.pos().getZ());
			// GUI/PIP transforms flip one axis. The item sheet keeps the visible
			// faces from being rejected by back-face culling after that flip.
			queue.submitBlockModel(poseStack, Sheets.cutoutBlockItemSheet(), rendered.parts(),
				rendered.tints(), light, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	private void loadStaticBlocksIfMissing(PonderLevel world) {
		if (renderedStaticBlocks != null)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		renderedStaticBlocks = new ArrayList<>();
		section.forEach(pos -> {
			BlockState state = world.getBlockState(pos);
			if (state.getRenderShape() != RenderShape.MODEL)
				return;

			BlockStateModel model = minecraft.getModelManager()
				.getBlockStateModelSet()
				.get(state);
			List<BlockStateModelPart> parts = new ArrayList<>();
			// Use the world-aware path. Create's model wrappers use it for CT,
			// pipe attachments and, importantly, hiding the static copy of moving
			// shafts/cogwheels while leaving their BE-rendered model visible.
			model.collectParts(world, pos, state, RandomSource.create(state.getSeed(pos)), parts);
			if (parts.isEmpty())
				return;

			List<BlockTintSource> tintSources = minecraft.getBlockColors().getTintSources(state);
			int[] tints = tintSources.isEmpty() ? BlockModelRenderState.EMPTY_TINTS : new int[tintSources.size()];
			for (int i = 0; i < tintSources.size(); i++)
				tints[i] = tintSources.get(i).colorInWorld(state, world, pos);

			renderedStaticBlocks.add(new RenderedStaticBlock(pos.immutable(), List.copyOf(parts), tints));
		});
	}

	@Override
	protected void renderLast(PonderLevel world, MultiBufferSource buffer, SubmitNodeCollector queue, Camera camera,
							  CameraRenderState cameraRenderState, PoseStack poseStack, float fade, float pt) {
		redraw = false;
		if (selectedBlock == null)
			return;
		BlockState blockState = world.getBlockState(selectedBlock);
		if (blockState.isAir())
			return;
		VoxelShape shape =
			blockState.getShape(world, selectedBlock, CollisionContext.of(Minecraft.getInstance().player));
		if (shape.isEmpty())
			return;

		poseStack.pushPose();
		transformMS(poseStack, pt);
		poseStack.translate(selectedBlock.getX(), selectedBlock.getY(), selectedBlock.getZ());

		AABBOutline aabbOutline = new AABBOutline(shape.bounds());
		aabbOutline.getParams()
			.lineWidth(1 / 64f)
			.colored(0xefefef)
			.disableLineNormals();
		Outliner.submitOutline(poseStack, queue, aabbOutline, Vec3.ZERO, pt);

		poseStack.popPose();
	}

	private void renderBlockEntities(PonderLevel world, PoseStack ms, SubmitNodeCollector queue, Camera camera,
									 CameraRenderState cameraRenderState, PoseStack poseStack, float pt) {
		loadBEsIfMissing(world);

		Iterator<BlockEntity> iterator = renderedBlockEntities.iterator();
		while (iterator.hasNext()) {
			BlockEntity blockEntity = iterator.next();
			BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = Minecraft.getInstance()
				.getBlockEntityRenderDispatcher()
				.getRenderer(blockEntity);
			if (renderer == null) {
				iterator.remove();
				continue;
			}

			BlockPos pos = blockEntity.getBlockPos();
			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());

			BlockEntityRenderState state = renderer.createRenderState();
			renderer.extractRenderState(blockEntity, state, pt, camera.position(), null);

			try {
				renderer.submit(state, poseStack, queue, cameraRenderState);
			} catch (Exception e) {
				iterator.remove();
				String message = "BlockEntity " + RegisteredObjectsHelper.getKeyOrThrow(blockEntity.getType()) + " could not be rendered virtually.";
				Ponder.LOGGER.error(message, e);
			}

			ms.popPose();
		}
	}

	private SuperByteBuffer buildStructureBuffer(PonderLevel world, ChunkSectionLayer layer) {
		ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();
		SbbBuilder sbbBuilder = objects.sbbBuilder;
		sbbBuilder.prepare(layer.pipeline());

		world.setMask(section);
		world.pushFakeLight(0);
		world.pushCardinalLighting(SCENE_LIGHTING);

		BakedModelBufferer.bufferBlocks(section.iterator(), world, null, true, sbbBuilder);

		world.popCardinalLighting();
		world.popLight();
		world.clearMask();

		return sbbBuilder.build();
	}

	private static class SbbBuilder extends SuperByteBufferBuilder implements ShadeSeparatedResultConsumer {
		private RenderPipeline pipeline;

		public void prepare(RenderPipeline pipeline) {
			prepare();
			this.pipeline = pipeline;
		}

		@Override
		public void accept(RenderPipeline pipeline, boolean shaded, MeshData data) {
			if (pipeline != this.pipeline) {
				return;
			}

			add(data, shaded);
		}
	}

	private static class ThreadLocalObjects {
		public final SbbBuilder sbbBuilder = new SbbBuilder();
	}

	private record RenderedStaticBlock(BlockPos pos, List<BlockStateModelPart> parts, int[] tints) {}
}
