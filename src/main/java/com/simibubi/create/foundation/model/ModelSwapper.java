package com.simibubi.create.foundation.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.Create;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.WrenchItemRenderer;
import com.simibubi.create.content.equipment.goggles.GogglesItemRenderer;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItemRenderer;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItemRenderer;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItemRenderer;
import com.simibubi.create.content.equipment.tool.CardboardSwordItemRenderer;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItemRenderer;
import com.simibubi.create.content.equipment.zapper.terrainzapper.WorldshaperItemRenderer;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import com.simibubi.create.content.decoration.copycat.CopycatStepBlock;
import com.simibubi.create.content.decoration.girder.GirderBlock;
import com.simibubi.create.content.decoration.girder.GirderEncasedShaftBlock;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlock;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes.ComponentPartials;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerItemModel;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackShape;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.CTContext;
import com.simibubi.create.foundation.block.render.CustomBlockModels;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.render.CustomItemModels;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.resources.model.sprite.Material;

import org.joml.Vector3f;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class ModelSwapper {

	protected CustomBlockModels customBlockModels = new CustomBlockModels();
	protected CustomItemModels customItemModels = new CustomItemModels();
	protected CustomConnectedTextures customConnectedTextures = new CustomConnectedTextures();

	public CustomBlockModels getCustomBlockModels() {
		return customBlockModels;
	}

	public CustomItemModels getCustomItemModels() {
		return customItemModels;
	}

	public CustomConnectedTextures getCustomConnectedTextures() {
		return customConnectedTextures;
	}

	public void onModelBake(ModelEvent.ModifyBakingResult event) {
		event.getBakingResult()
			.blockStateModels()
			.replaceAll((state, model) -> {
				if (state.getBlock() instanceof CopycatPanelBlock)
					model = new CopycatPanelBlockStateModel(model);
				if (state.getBlock() instanceof CopycatStepBlock)
					model = new CopycatStepBlockStateModel(model);
				if (state.getBlock() instanceof GirderBlock || state.getBlock() instanceof GirderEncasedShaftBlock)
					model = new ConnectedGirderBlockStateModel(model);
				if (state.getBlock() instanceof FluidTankBlock) {
					boolean creative = AllBlocks.CREATIVE_FLUID_TANK.has(state);
					FluidTankCTBehaviour tankCT = creative
						? new FluidTankCTBehaviour(AllSpriteShifts.CREATIVE_FLUID_TANK,
							AllSpriteShifts.CREATIVE_CASING, AllSpriteShifts.CREATIVE_CASING)
						: new FluidTankCTBehaviour(AllSpriteShifts.FLUID_TANK, AllSpriteShifts.FLUID_TANK_TOP,
							AllSpriteShifts.FLUID_TANK_INNER);
					model = new FluidTankBlockStateModel(new ConnectedTextureBlockStateModel(model, tankCT));
				}
				if (state.getBlock() instanceof BeltBlock)
					model = new BeltBlockStateModel(model);
				if (state.getBlock() instanceof TrackBlock)
					model = new TiltedTrackBlockStateModel(model);
				if (state.getBlock() instanceof TableClothBlock)
					model = new TableClothBlockStateModel(model);
				if (isPipeAttachmentBlock(state.getBlock()))
					model = new PipeAttachmentBlockStateModel(model);
				ConnectedTextureBehaviour ct = customConnectedTextures.get(state.getBlock());
				if (ct != null)
					model = new ConnectedTextureBlockStateModel(model, ct);
				if (shouldHideStaticKineticModel(state))
					model = new StaticHiddenBlockStateModel(model);
				return model;
			});
		var itemModels = event.getBakingResult()
			.itemStackModels();
		var gearModel = itemModels.get(Create.asResource("wrench_gear"));
		var potatoCannonCogModel = itemModels.get(Create.asResource("potato_cannon_cog"));
		var extendoGripCogModel = itemModels.get(Create.asResource("extendo_grip_cog"));
		var extendoThinShortModel = itemModels.get(Create.asResource("extendo_grip_thin_short"));
		var extendoWideShortModel = itemModels.get(Create.asResource("extendo_grip_wide_short"));
		var extendoThinLongModel = itemModels.get(Create.asResource("extendo_grip_thin_long"));
		var extendoWideLongModel = itemModels.get(Create.asResource("extendo_grip_wide_long"));
		var extendoHandModel = itemModels.get(Create.asResource("extendo_grip_hand"));
		var extendoHoldingHandModel = itemModels.get(Create.asResource("extendo_grip_hand_holding"));
		var symmetryWandBitsModel = itemModels.get(Create.asResource("wand_of_symmetry_bits"));
		var symmetryWandCoreModel = itemModels.get(Create.asResource("wand_of_symmetry_core"));
		var symmetryWandCoreGlowModel = itemModels.get(Create.asResource("wand_of_symmetry_core_glow"));
		var linkedControllerPoweredModel = itemModels.get(Create.asResource("linked_controller_powered"));
		var linkedControllerButtonModel = itemModels.get(Create.asResource("linked_controller_button"));
		var worldshaperCoreModel = itemModels.get(Create.asResource("handheld_worldshaper_core"));
		var worldshaperCoreGlowModel = itemModels.get(Create.asResource("handheld_worldshaper_core_glow"));
		var worldshaperAcceleratorModel = itemModels.get(Create.asResource("handheld_worldshaper_accelerator"));
		var cardboardSwordHeldModel = itemModels.get(Create.asResource("cardboard_sword_in_hand"));
		var gogglesWornModel = itemModels.get(Create.asResource("goggles_worn"));
		itemModels.replaceAll((id, model) -> id.equals(Create.asResource("wrench"))
			? new WrenchItemRenderer(model, gearModel)
			: id.equals(Create.asResource("sand_paper")) || id.equals(Create.asResource("red_sand_paper"))
				? new SandPaperItemRenderer(model)
			: id.equals(Create.asResource("potato_cannon"))
				? new PotatoCannonItemRenderer(model, potatoCannonCogModel)
			: id.equals(Create.asResource("extendo_grip"))
				? new ExtendoGripItemRenderer(model, extendoGripCogModel, extendoThinShortModel,
					extendoWideShortModel, extendoThinLongModel, extendoWideLongModel, extendoHandModel,
					extendoHoldingHandModel)
			: id.equals(Create.asResource("linked_controller"))
				? new LinkedControllerItemModel(model, linkedControllerPoweredModel, linkedControllerButtonModel)
			: id.equals(Create.asResource("wand_of_symmetry"))
				? new SymmetryWandItemRenderer(model, symmetryWandBitsModel, symmetryWandCoreModel,
					symmetryWandCoreGlowModel)
			: id.equals(Create.asResource("handheld_worldshaper"))
				? new WorldshaperItemRenderer(model, worldshaperCoreModel, worldshaperCoreGlowModel,
					worldshaperAcceleratorModel)
			: id.equals(Create.asResource("cardboard_sword"))
				? new CardboardSwordItemRenderer(model, cardboardSwordHeldModel)
			: id.equals(Create.asResource("goggles"))
				? new GogglesItemRenderer(model, gogglesWornModel)
			: model);
	}

	private static class CopycatPanelBlockStateModel extends DelegateBlockStateModel {
		private static final float THICKNESS = 3 / 16f;

		private CopycatPanelBlockStateModel(BlockStateModel delegate) {
			super(delegate);
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
			if (!(level.getBlockEntity(pos) instanceof CopycatBlockEntity copycat) || !copycat.hasCustomMaterial()) {
				super.collectParts(level, pos, state, random, parts);
				return;
			}

			BlockState material = copycat.getMaterial();
			BlockStateModel materialModel = Minecraft.getInstance()
				.getModelManager()
				.getBlockStateModelSet()
				.get(material);
			if (materialModel == null) {
				super.collectParts(level, pos, state, random, parts);
				return;
			}

			List<BlockStateModelPart> materialParts = new ArrayList<>();
			materialModel.collectParts(level, pos, material, random, materialParts);
			Direction facing = state.getValue(CopycatPanelBlock.FACING);
			for (BlockStateModelPart part : materialParts)
				parts.add(new CopycatPanelPart(part, facing));
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
			if (level.getBlockEntity(pos) instanceof CopycatBlockEntity copycat && copycat.hasCustomMaterial())
				return Arrays.asList(copycat.getMaterial(), state.getValue(CopycatPanelBlock.FACING));
			return super.createGeometryKey(level, pos, state, random);
		}
	}

	private static class CopycatPanelPart implements BlockStateModelPart {
		private final BlockStateModelPart delegate;
		private final List<BakedQuad> unculled = new ArrayList<>();

		private CopycatPanelPart(BlockStateModelPart delegate, Direction facing) {
			this.delegate = delegate;
			for (Direction sourceSide : Direction.values())
				transform(delegate.getQuads(sourceSide), facing);
			transform(delegate.getQuads(null), facing);
		}

		private void transform(List<BakedQuad> source, Direction facing) {
			for (BakedQuad quad : source) {
				if (quad.direction() != facing)
					unculled.add(crop(quad, facing, true));
				if (quad.direction() != facing.getOpposite())
					unculled.add(crop(quad, facing, false));
			}
		}

		private static BakedQuad crop(BakedQuad quad, Direction facing, boolean front) {
			float nx = facing.getStepX();
			float ny = facing.getStepY();
			float nz = facing.getStepZ();
			float depth = (front ? 1 : 2) / 16f;
			float sampleOffset = front ? 0 : 14 / 16f;
			float outputOffset = front ? 0 : -13 / 16f;

			Vector3f min = new Vector3f(0);
			Vector3f max = new Vector3f(1);
			float start = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE
				? sampleOffset
				: 1 - sampleOffset - depth;
			switch (facing.getAxis()) {
				case X -> {
					min.x = start;
					max.x = start + depth;
				}
				case Y -> {
					min.y = start;
					max.y = start + depth;
				}
				case Z -> {
					min.z = start;
					max.z = start + depth;
				}
			}

			return cropAndMove(quad, min, max,
				new Vector3f(nx * outputOffset, ny * outputOffset, nz * outputOffset));
		}

		private static BakedQuad cropAndMove(BakedQuad quad, Vector3f min, Vector3f max, Vector3f move) {
			org.joml.Vector3fc[] positions = { quad.position0(), quad.position1(), quad.position2(), quad.position3() };
			long[] packedUvs = { quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3() };
			Vector3f[] cropped = new Vector3f[4];
			long[] croppedUvs = new long[4];

			Vector3f edge1 = new Vector3f(positions[1]).sub(positions[0]);
			Vector3f edge3 = new Vector3f(positions[3]).sub(positions[0]);
			float edge1LengthSquared = edge1.lengthSquared();
			float edge3LengthSquared = edge3.lengthSquared();
			float u0 = unpackU(packedUvs[0]);
			float v0 = unpackV(packedUvs[0]);
			float u1Delta = unpackU(packedUvs[1]) - u0;
			float v1Delta = unpackV(packedUvs[1]) - v0;
			float u3Delta = unpackU(packedUvs[3]) - u0;
			float v3Delta = unpackV(packedUvs[3]) - v0;

			for (int i = 0; i < 4; i++) {
				org.joml.Vector3fc original = positions[i];
				Vector3f clamped = new Vector3f(
					Math.max(min.x, Math.min(max.x, original.x())),
					Math.max(min.y, Math.min(max.y, original.y())),
					Math.max(min.z, Math.min(max.z, original.z())));
				Vector3f fromOrigin = new Vector3f(clamped).sub(positions[0]);
				float edge1Factor = edge1LengthSquared == 0 ? 0 : edge1.dot(fromOrigin) / edge1LengthSquared;
				float edge3Factor = edge3LengthSquared == 0 ? 0 : edge3.dot(fromOrigin) / edge3LengthSquared;
				float u = u0 + edge1Factor * u1Delta + edge3Factor * u3Delta;
				float v = v0 + edge1Factor * v1Delta + edge3Factor * v3Delta;
				cropped[i] = clamped.add(move);
				croppedUvs[i] = packUv(u, v);
			}

			return new BakedQuad(cropped[0], cropped[1], cropped[2], cropped[3], croppedUvs[0], croppedUvs[1],
				croppedUvs[2], croppedUvs[3], quad.direction(), quad.materialInfo());
		}

		private static float unpackU(long packedUv) {
			return Float.intBitsToFloat((int) (packedUv >>> 32));
		}

		private static float unpackV(long packedUv) {
			return Float.intBitsToFloat((int) packedUv);
		}

		private static long packUv(float u, float v) {
			return Integer.toUnsignedLong(Float.floatToIntBits(u)) << 32
				| Integer.toUnsignedLong(Float.floatToIntBits(v));
		}

		@Override
		public List<BakedQuad> getQuads(Direction side) {
			return side == null ? unculled : Collections.emptyList();
		}

		@Override
		public boolean useAmbientOcclusion() {
			return delegate.useAmbientOcclusion();
		}

		@Override
		public Material.Baked particleMaterial() {
			return delegate.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return delegate.materialFlags();
		}
	}

	private static class CopycatStepBlockStateModel extends DelegateBlockStateModel {
		private CopycatStepBlockStateModel(BlockStateModel delegate) {
			super(delegate);
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
			if (!(level.getBlockEntity(pos) instanceof CopycatBlockEntity copycat) || !copycat.hasCustomMaterial()) {
				super.collectParts(level, pos, state, random, parts);
				return;
			}

			BlockState material = copycat.getMaterial();
			BlockStateModel materialModel = Minecraft.getInstance()
				.getModelManager()
				.getBlockStateModelSet()
				.get(material);
			if (materialModel == null) {
				super.collectParts(level, pos, state, random, parts);
				return;
			}

			List<BlockStateModelPart> materialParts = new ArrayList<>();
			materialModel.collectParts(level, pos, material, random, materialParts);
			Direction facing = state.getValue(CopycatStepBlock.FACING);
			boolean upperHalf = state.getValue(CopycatStepBlock.HALF) == Half.TOP;
			for (BlockStateModelPart part : materialParts)
				parts.add(new CopycatStepPart(part, facing, upperHalf));
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
			if (level.getBlockEntity(pos) instanceof CopycatBlockEntity copycat && copycat.hasCustomMaterial())
				return Arrays.asList(copycat.getMaterial(), state.getValue(CopycatStepBlock.FACING),
					state.getValue(CopycatStepBlock.HALF));
			return super.createGeometryKey(level, pos, state, random);
		}
	}

	private static class CopycatStepPart implements BlockStateModelPart {
		private final BlockStateModelPart delegate;
		private final List<BakedQuad> unculled = new ArrayList<>();

		private CopycatStepPart(BlockStateModelPart delegate, Direction facing, boolean upperHalf) {
			this.delegate = delegate;
			List<BakedQuad> source = new ArrayList<>();
			for (Direction sourceSide : Direction.values())
				source.addAll(delegate.getQuads(sourceSide));
			source.addAll(delegate.getQuads(null));
			buildPieces(source, facing, upperHalf);
		}

		private void buildPieces(List<BakedQuad> source, Direction facing, boolean upperHalf) {
			float nx = facing.getStepX();
			float nz = facing.getStepZ();
			Vector3f baseMin = new Vector3f(0);
			Vector3f baseMax = new Vector3f(1, .25f, 1);
			if (nx > 0)
				baseMin.x = .75f;
			else if (nx < 0)
				baseMax.x = .25f;
			if (nz > 0)
				baseMin.z = .75f;
			else if (nz < 0)
				baseMax.z = .25f;

			for (boolean top : List.of(false, true)) {
				for (boolean front : List.of(false, true)) {
					Vector3f sampleMove = new Vector3f();
					if (front)
						sampleMove.add(nx * -.75f, 0, nz * -.75f);
					if (top)
						sampleMove.add(0, .75f, 0);
					Vector3f min = new Vector3f(baseMin).add(sampleMove);
					Vector3f max = new Vector3f(baseMax).add(sampleMove);

					Vector3f outputMove = new Vector3f();
					if (front)
						outputMove.add(nx * .5f, 0, nz * .5f);
					if (top != upperHalf)
						outputMove.add(0, upperHalf ? .5f : -.5f, 0);

					for (BakedQuad quad : source) {
						Direction direction = quad.direction();
						if (front && direction == facing)
							continue;
						if (!front && direction == facing.getOpposite())
							continue;
						if (!top && direction == Direction.UP)
							continue;
						if (top && direction == Direction.DOWN)
							continue;
						unculled.add(CopycatPanelPart.cropAndMove(quad, min, max, outputMove));
					}
				}
			}
		}

		@Override
		public List<BakedQuad> getQuads(Direction side) {
			return side == null ? unculled : Collections.emptyList();
		}

		@Override
		public boolean useAmbientOcclusion() {
			return delegate.useAmbientOcclusion();
		}

		@Override
		public Material.Baked particleMaterial() {
			return delegate.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return delegate.materialFlags();
		}
	}

	public void registerListeners(IEventBus modEventBus) {
		modEventBus.addListener(this::onModelBake);
	}

	public static <T extends BakedModel> void swapModels(Map<ModelResourceLocation, BakedModel> modelRegistry,
		List<ModelResourceLocation> locations, Function<BakedModel, T> factory) {
		locations.forEach(location -> {
			swapModels(modelRegistry, location, factory);
		});
	}

	public static <T extends BakedModel> void swapModels(Map<ModelResourceLocation, BakedModel> modelRegistry,
		ModelResourceLocation location, Function<BakedModel, T> factory) {
		modelRegistry.put(location, factory.apply(modelRegistry.get(location)));
	}

	public static List<ModelResourceLocation> getAllBlockStateModelLocations(Block block) {
		List<ModelResourceLocation> models = new ArrayList<>();
		Identifier blockRl = RegisteredObjectsHelper.getKeyOrThrow(block);
		block.getStateDefinition()
			.getPossibleStates()
			.forEach(state -> {
				models.add(BlockModelShaper.stateToModelLocation(blockRl, state));
			});
		return models;
	}

	public static ModelResourceLocation getItemModelLocation(Item item) {
		return new ModelResourceLocation(RegisteredObjectsHelper.getKeyOrThrow(item), "inventory");
	}

	private static boolean shouldHideStaticKineticModel(BlockState state) {
		return AllBlocks.SHAFT.has(state)
			|| AllBlocks.COGWHEEL.has(state)
			|| AllBlocks.LARGE_COGWHEEL.has(state)
			|| AllBlocks.CRUSHING_WHEEL.has(state);
	}

	private static boolean isPipeAttachmentBlock(Block block) {
		return AllBlocks.FLUID_PIPE.get() == block
			|| AllBlocks.ENCASED_FLUID_PIPE.get() == block
			|| AllBlocks.GLASS_FLUID_PIPE.get() == block
			|| AllBlocks.MECHANICAL_PUMP.get() == block
			|| AllBlocks.SMART_FLUID_PIPE.get() == block
			|| AllBlocks.FLUID_VALVE.get() == block;
	}

	private static class TableClothBlockStateModel extends DelegateBlockStateModel {
		private BlockStateModelPart[] corners;

		private TableClothBlockStateModel(BlockStateModel delegate) {
			super(delegate);
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
			super.collectParts(level, pos, state, random, parts);
			int mask = cornerMask(level, pos, state);
			if (mask == 0)
				return;

			ensureCornersLoaded();
			for (int i = 0; i < corners.length; i++)
				if ((mask & 1 << i) != 0 && corners[i] != null)
					parts.add(corners[i]);
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random) {
			return Arrays.asList(super.createGeometryKey(level, pos, state, random), cornerMask(level, pos, state));
		}

		private int cornerMask(BlockAndTintGetter level, BlockPos pos, BlockState state) {
			int mask = 0;
			for (Direction side : Direction.Plane.HORIZONTAL) {
				Direction clockwise = side.getClockWise();
				if (isFaceVisible(level, pos, state, side) && isFaceVisible(level, pos, state, clockwise))
					mask |= 1 << side.get2DDataValue();
			}
			return mask;
		}

		private static boolean isFaceVisible(BlockAndTintGetter level, BlockPos pos, BlockState state,
			Direction side) {
			return Block.shouldRenderFace(state, level.getBlockState(pos.relative(side)), side);
		}

		private void ensureCornersLoaded() {
			if (corners != null)
				return;
			TextureAtlasSprite target = particleMaterial().sprite();
			corners = new BlockStateModelPart[] {
				retexture(CreateStandaloneModels.TABLE_CLOTH_CORNER_SW, target),
				retexture(CreateStandaloneModels.TABLE_CLOTH_CORNER_NW, target),
				retexture(CreateStandaloneModels.TABLE_CLOTH_CORNER_NE, target),
				retexture(CreateStandaloneModels.TABLE_CLOTH_CORNER_SE, target)
			};
		}

		private static BlockStateModelPart retexture(StandaloneModelKey<BlockStateModelPart> key,
			TextureAtlasSprite target) {
			BlockStateModelPart part = Minecraft.getInstance().getModelManager().getStandaloneModel(key);
			return part == null ? null : new RetexturedBlockStateModelPart(part, target);
		}
	}

	private static class RetexturedBlockStateModelPart implements BlockStateModelPart {
		private final BlockStateModelPart delegate;
		private final TextureAtlasSprite target;
		private final Map<Direction, List<BakedQuad>> cache = new HashMap<>();
		private final List<BakedQuad> nullQuads;

		private RetexturedBlockStateModelPart(BlockStateModelPart delegate, TextureAtlasSprite target) {
			this.delegate = delegate;
			this.target = target;
			this.nullQuads = transform(delegate.getQuads(null));
			for (Direction direction : Direction.values())
				cache.put(direction, transform(delegate.getQuads(direction)));
		}

		@Override
		public List<BakedQuad> getQuads(Direction side) {
			return side == null ? nullQuads : cache.getOrDefault(side, List.of());
		}

		private List<BakedQuad> transform(List<BakedQuad> quads) {
			return quads.stream().map(this::transform).toList();
		}

		private BakedQuad transform(BakedQuad quad) {
			TextureAtlasSprite source = quad.materialInfo().sprite();
			BakedQuad.MaterialInfo material = quad.materialInfo();
			BakedQuad.MaterialInfo replaced = new BakedQuad.MaterialInfo(target, material.layer(),
				material.itemRenderType(), material.tintIndex(), material.shade(), material.lightEmission());
			return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
				remapUv(quad.packedUV0(), source), remapUv(quad.packedUV1(), source),
				remapUv(quad.packedUV2(), source), remapUv(quad.packedUV3(), source), quad.direction(), replaced);
		}

		private long remapUv(long packedUv, TextureAtlasSprite source) {
			float u = Float.intBitsToFloat((int) (packedUv >>> 32));
			float v = Float.intBitsToFloat((int) packedUv);
			// TextureAtlasSprite#getU/getV take normalized 0..1 coordinates in 26.2
			// (older versions accepted the traditional 0..16 model-space range).
			float localU = (u - source.getU0()) / (source.getU1() - source.getU0());
			float localV = (v - source.getV0()) / (source.getV1() - source.getV0());
			long remappedU = Integer.toUnsignedLong(Float.floatToIntBits(target.getU(localU)));
			long remappedV = Integer.toUnsignedLong(Float.floatToIntBits(target.getV(localV)));
			return remappedU << 32 | remappedV;
		}

		@Override
		public boolean useAmbientOcclusion() {
			return false;
		}

		@Override
		public Material.Baked particleMaterial() {
			return new Material.Baked(target, delegate.particleMaterial().forceTranslucent());
		}

		@Override
		public int materialFlags() {
			return delegate.materialFlags();
		}
	}

	private static class BeltBlockStateModel extends DelegateBlockStateModel {
		private BeltBlockStateModel(BlockStateModel delegate) {
			super(delegate);
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
			List<BlockStateModelPart> originalParts = new ArrayList<>();
			super.collectParts(level, pos, state, random, originalParts);
			BeltVisualData data = data(level, pos, state);
			if (data.casing == CasingType.ANDESITE) {
				TextureAtlasSprite target = AllSpriteShifts.ANDESIDE_BELT_CASING.getTarget();
				for (BlockStateModelPart part : originalParts)
					parts.add(new RetexturedBlockStateModelPart(part, target));
			} else {
				parts.addAll(originalParts);
			}

			if (!data.covered || data.casing == CasingType.NONE)
				return;
			boolean alongX = data.axis == Direction.Axis.X;
			StandaloneModelKey<BlockStateModelPart> key = data.casing == CasingType.BRASS
				? alongX ? CreateStandaloneModels.BRASS_BELT_COVER_X : CreateStandaloneModels.BRASS_BELT_COVER_Z
				: alongX ? CreateStandaloneModels.ANDESITE_BELT_COVER_X : CreateStandaloneModels.ANDESITE_BELT_COVER_Z;
			BlockStateModelPart cover = Minecraft.getInstance().getModelManager().getStandaloneModel(key);
			if (cover != null)
				parts.add(cover);
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random) {
			return Arrays.asList(super.createGeometryKey(level, pos, state, random), data(level, pos, state));
		}

		private static BeltVisualData data(BlockAndTintGetter level, BlockPos pos, BlockState state) {
			CasingType casing = state.getValue(BeltBlock.CASING) ? CasingType.BRASS : CasingType.NONE;
			boolean covered = false;
			if (level.getBlockEntity(pos) instanceof BeltBlockEntity belt) {
				casing = belt.casing;
				covered = belt.covered;
			}
			return new BeltVisualData(casing, covered, state.getValue(BeltBlock.HORIZONTAL_FACING).getAxis());
		}
	}

	private record BeltVisualData(CasingType casing, boolean covered, Direction.Axis axis) {}

	private static class TiltedTrackBlockStateModel extends DelegateBlockStateModel {
		private TiltedTrackBlockStateModel(BlockStateModel delegate) {
			super(delegate);
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
			List<BlockStateModelPart> originalParts = new ArrayList<>();
			super.collectParts(level, pos, state, random, originalParts);
			Double angle = tiltAngle(level, pos);
			if (angle == null) {
				parts.addAll(originalParts);
				return;
			}
			for (BlockStateModelPart part : originalParts)
				parts.add(new TiltedTrackPart(part, state.getValue(TrackBlock.SHAPE), angle));
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random) {
			return Arrays.asList(super.createGeometryKey(level, pos, state, random), tiltAngle(level, pos));
		}

		private static Double tiltAngle(BlockAndTintGetter level, BlockPos pos) {
			if (level.getBlockEntity(pos) instanceof TrackBlockEntity track && track.isTilted())
				return track.tilt.smoothingAngle.orElse(null);
			return null;
		}
	}

	private static class TiltedTrackPart implements BlockStateModelPart {
		private final BlockStateModelPart delegate;
		private final Map<Direction, List<BakedQuad>> cache = new HashMap<>();
		private final List<BakedQuad> nullQuads;

		private TiltedTrackPart(BlockStateModelPart delegate, TrackShape shape, double angleIn) {
			this.delegate = delegate;
			this.nullQuads = transform(delegate.getQuads(null), shape, angleIn);
			for (Direction direction : Direction.values())
				cache.put(direction, transform(delegate.getQuads(direction), shape, angleIn));
		}

		private static List<BakedQuad> transform(List<BakedQuad> quads, TrackShape shape, double angleIn) {
			return quads.stream()
				.map(quad -> transform(quad, shape, angleIn))
				.toList();
		}

		private static BakedQuad transform(BakedQuad quad, TrackShape shape, double angleIn) {
			double angle = Math.abs(angleIn);
			boolean flip = angleIn < 0;
			double horizontalAngle = switch (shape) {
				case XO -> 0;
				case PD -> 45;
				case ZO -> 90;
				case ND -> 135;
				default -> 0;
			};
			Vec3 rotationPoint = shape == TrackShape.ND || shape == TrackShape.PD
				? new Vec3((Mth.SQRT_OF_TWO - 1) / 2, 0, 0)
				: Vec3.ZERO;

			Vector3f[] transformed = new Vector3f[4];
			org.joml.Vector3fc[] positions = { quad.position0(), quad.position1(), quad.position2(), quad.position3() };
			for (int i = 0; i < positions.length; i++) {
				org.joml.Vector3fc position = positions[i];
				Vec3 vertex = new Vec3(position.x(), position.y(), position.z()).add(0, -.25, 0);
				vertex = net.createmod.catnip.api.math.VecHelper.rotateCentered(vertex, horizontalAngle, Axis.Y);
				vertex = vertex.add(rotationPoint);
				vertex = net.createmod.catnip.api.math.VecHelper.rotate(vertex, angle, Axis.Z);
				vertex = vertex.subtract(rotationPoint);
				vertex = net.createmod.catnip.api.math.VecHelper.rotateCentered(vertex,
					-horizontalAngle + (flip ? 180 : 0), Axis.Y);
				vertex = vertex.add(0, .25, 0);
				transformed[i] = new Vector3f((float) vertex.x, (float) vertex.y, (float) vertex.z);
			}
			return new BakedQuad(transformed[0], transformed[1], transformed[2], transformed[3], quad.packedUV0(),
				quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), quad.materialInfo());
		}

		@Override
		public List<BakedQuad> getQuads(Direction side) {
			return side == null ? nullQuads : cache.getOrDefault(side, List.of());
		}

		@Override
		public boolean useAmbientOcclusion() {
			return delegate.useAmbientOcclusion();
		}

		@Override
		public Material.Baked particleMaterial() {
			return delegate.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return delegate.materialFlags();
		}
	}

	private static class FluidTankBlockStateModel extends DelegateBlockStateModel {
		private FluidTankBlockStateModel(BlockStateModel delegate) {
			super(delegate);
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
			List<BlockStateModelPart> originalParts = new ArrayList<>();
			super.collectParts(level, pos, state, random, originalParts);
			int culledFaces = connectedFaces(level, pos);
			for (BlockStateModelPart part : originalParts)
				parts.add(new FluidTankPart(part, culledFaces));
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random) {
			return Arrays.asList(super.createGeometryKey(level, pos, state, random), connectedFaces(level, pos));
		}

		private static int connectedFaces(BlockAndTintGetter level, BlockPos pos) {
			int mask = 0;
			for (Direction direction : net.createmod.catnip.api.data.Iterate.horizontalDirections)
				if (ConnectivityHandler.isConnected(level, pos, pos.relative(direction)))
					mask |= 1 << direction.get3DDataValue();
			return mask;
		}
	}

	private static class FluidTankPart implements BlockStateModelPart {
		private final BlockStateModelPart delegate;
		private final int culledFaces;
		private final List<BakedQuad> quads;

		private FluidTankPart(BlockStateModelPart delegate, int culledFaces) {
			this.delegate = delegate;
			this.culledFaces = culledFaces;
			List<BakedQuad> combined = new ArrayList<>(delegate.getQuads(null));
			for (Direction direction : Direction.values())
				if ((culledFaces & 1 << direction.get3DDataValue()) == 0)
					combined.addAll(delegate.getQuads(direction));
			quads = List.copyOf(combined);
		}

		@Override
		public List<BakedQuad> getQuads(Direction direction) {
			return direction == null ? quads : List.of();
		}

		@Override public boolean useAmbientOcclusion() { return delegate.useAmbientOcclusion(); }
		@Override public Material.Baked particleMaterial() { return delegate.particleMaterial(); }
		@Override public int materialFlags() { return delegate.materialFlags(); }
	}

	private static class ConnectedGirderBlockStateModel extends DelegateBlockStateModel {
		private ConnectedGirderBlockStateModel(BlockStateModel delegate) {
			super(delegate);
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
			super.collectParts(level, pos, state, random, parts);
			for (Direction direction : net.createmod.catnip.api.data.Iterate.horizontalDirections) {
				if (!GirderBlock.isConnected(level, pos, state, direction))
					continue;
				BlockStateModelPart bracket = Minecraft.getInstance()
					.getModelManager()
					.getStandaloneModel(CreateStandaloneModels.METAL_GIRDER_BRACKETS.get(direction));
				if (bracket != null)
					parts.add(bracket);
			}
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state,
			RandomSource random) {
			int connections = 0;
			for (Direction direction : net.createmod.catnip.api.data.Iterate.horizontalDirections)
				if (GirderBlock.isConnected(level, pos, state, direction))
					connections |= 1 << direction.get2DDataValue();
			return Arrays.asList(super.createGeometryKey(level, pos, state, random), connections);
		}
	}

	private static class PipeAttachmentBlockStateModel extends DelegateBlockStateModel {
		protected PipeAttachmentBlockStateModel(BlockStateModel delegate) {
			super(delegate);
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
			super.collectParts(level, pos, state, random, parts);

			PipeModelData data = createPipeData(level, pos, state);
			for (Direction direction : Direction.values()) {
				AttachmentTypes type = data.getAttachment(direction);
				for (ComponentPartials partial : type.partials)
					addPart(parts, CreateStandaloneModels.PIPE_ATTACHMENTS.get(partial)
						.get(direction));
			}

			if (data.encased)
				addPart(parts, CreateStandaloneModels.FLUID_PIPE_CASING);

			if (data.bracket != null) {
				BlockStateModel bracketModel = Minecraft.getInstance()
					.getModelManager()
					.getBlockStateModelSet()
					.get(data.bracket);
				bracketModel.collectParts(RandomSource.create(data.bracket.getSeed(pos)), parts);
			}
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
			return Arrays.asList(super.createGeometryKey(level, pos, state, random), createPipeData(level, pos, state).asKey());
		}

		private static void addPart(List<BlockStateModelPart> parts, StandaloneModelKey<BlockStateModelPart> key) {
			BlockStateModelPart part = Minecraft.getInstance()
				.getModelManager()
				.getStandaloneModel(key);
			if (part != null)
				parts.add(part);
		}

		private static PipeModelData createPipeData(BlockAndTintGetter level, BlockPos pos, BlockState state) {
			PipeModelData data = new PipeModelData();
			FluidTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, FluidTransportBehaviour.TYPE);
			BracketedBlockEntityBehaviour bracket =
				BlockEntityBehaviour.get(level, pos, BracketedBlockEntityBehaviour.TYPE);
			if (transport != null)
				for (Direction direction : Direction.values())
					data.putAttachment(direction, transport.getRenderedRimAttachment(level, pos, state, direction));
			if (bracket != null)
				data.bracket = bracket.getBracket();
			data.encased = FluidPipeBlock.shouldDrawCasing(level, pos, state);
			return data;
		}
	}

	private static class PipeModelData {
		private final AttachmentTypes[] attachments = new AttachmentTypes[6];
		private boolean encased;
		private BlockState bracket;

		private PipeModelData() {
			Arrays.fill(attachments, AttachmentTypes.NONE);
		}

		private void putAttachment(Direction direction, AttachmentTypes type) {
			attachments[direction.get3DDataValue()] = type;
		}

		private AttachmentTypes getAttachment(Direction direction) {
			return attachments[direction.get3DDataValue()];
		}

		private Object asKey() {
			return Arrays.asList(Arrays.asList(attachments), encased, bracket);
		}
	}

	private static class StaticHiddenBlockStateModel extends DelegateBlockStateModel {

		protected StaticHiddenBlockStateModel(BlockStateModel delegate) {
			super(delegate);
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
			// The block entity renderer still calls the legacy collectParts(random, parts)
			// path to fetch the moving shaft/cog geometry. Hide only the static chunk mesh.
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
			return Collections.emptyList();
		}
	}

	public static class CustomConnectedTextures {
		private final Map<Block, ConnectedTextureBehaviour> behaviours = new HashMap<>();

		public void register(Block block, ConnectedTextureBehaviour behaviour) {
			behaviours.put(block, behaviour);
		}

		public ConnectedTextureBehaviour get(Block block) {
			return behaviours.get(block);
		}
	}

	private static class ConnectedTextureBlockStateModel extends DelegateBlockStateModel {
		private final ConnectedTextureBehaviour behaviour;

		protected ConnectedTextureBlockStateModel(BlockStateModel delegate, ConnectedTextureBehaviour behaviour) {
			super(delegate);
			this.behaviour = behaviour;
		}

		@Override
		public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			List<BlockStateModelPart> parts) {
			List<BlockStateModelPart> originalParts = new ArrayList<>();
			super.collectParts(level, pos, state, random, originalParts);

			CTData data = createCTData(level, pos, state);
			for (BlockStateModelPart part : originalParts)
				parts.add(new ConnectedTexturePart(part, behaviour, data, state, random));
		}

		private CTData createCTData(BlockAndTintGetter level, BlockPos pos, BlockState state) {
			CTData data = new CTData();
			for (Direction face : Direction.values()) {
				if (!behaviour.buildContextForOccludedDirections()) {
					BlockState adjacentState = level.getBlockState(pos.relative(face));
					if (!Block.shouldRenderFace(state, adjacentState, face))
						continue;
				}

				CTType type = behaviour.getDataType(level, pos, state, face);
				if (type == null)
					continue;

				CTContext context = behaviour.buildContext(level, pos, state, face, type.getContextRequirement());
				data.put(face, type.getTextureIndex(context));
			}
			return data;
		}

		@Override
		public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
			Object delegateKey = super.createGeometryKey(level, pos, state, random);
			return Arrays.asList(delegateKey, createCTData(level, pos, state).asKey());
		}
	}

	private static class ConnectedTexturePart implements BlockStateModelPart {
		private final BlockStateModelPart delegate;
		private final ConnectedTextureBehaviour behaviour;
		private final CTData data;
		private final BlockState state;
		private final RandomSource random;

		private ConnectedTexturePart(BlockStateModelPart delegate, ConnectedTextureBehaviour behaviour, CTData data,
			BlockState state, RandomSource random) {
			this.delegate = delegate;
			this.behaviour = behaviour;
			this.data = data;
			this.state = state;
			this.random = random;
		}

		@Override
		public List<BakedQuad> getQuads(Direction side) {
			List<BakedQuad> original = delegate.getQuads(side);
			if (original.isEmpty())
				return original;

			List<BakedQuad> transformed = new ArrayList<>(original.size());
			for (BakedQuad quad : original)
				transformed.add(transform(quad));
			return transformed;
		}

		private BakedQuad transform(BakedQuad quad) {
			int index = data.get(quad.direction());
			if (index == -1)
				return quad;

			CTSpriteShiftEntry shift = behaviour.getShift(state, random, quad.direction(), quad.materialInfo().sprite());
			if (shift == null || shift.getOriginal() != quad.materialInfo().sprite())
				return quad;

			return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
				shiftUv(quad.packedUV0(), shift, index), shiftUv(quad.packedUV1(), shift, index),
				shiftUv(quad.packedUV2(), shift, index), shiftUv(quad.packedUV3(), shift, index), quad.direction(),
				quad.materialInfo());
		}

		private static long shiftUv(long packedUv, CTSpriteShiftEntry shift, int index) {
			float u = Float.intBitsToFloat((int) (packedUv >>> 32));
			float v = Float.intBitsToFloat((int) packedUv);
			long shiftedU = Integer.toUnsignedLong(Float.floatToIntBits(shift.getTargetU(u, index)));
			long shiftedV = Integer.toUnsignedLong(Float.floatToIntBits(shift.getTargetV(v, index)));
			return (shiftedU << 32) | shiftedV;
		}

		@Override
		public boolean useAmbientOcclusion() {
			return delegate.useAmbientOcclusion();
		}

		@Override
		public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
			return delegate.particleMaterial();
		}

		@Override
		public int materialFlags() {
			return delegate.materialFlags();
		}
	}

	private static class CTData {
		private final int[] indices = new int[6];

		private CTData() {
			Arrays.fill(indices, -1);
		}

		private void put(Direction face, int texture) {
			indices[face.get3DDataValue()] = texture;
		}

		private int get(Direction face) {
			return indices[face.get3DDataValue()];
		}

		private String asKey() {
			return Arrays.toString(indices);
		}
	}

}
