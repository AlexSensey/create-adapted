package com.simibubi.create.content.fluids.pipes;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.DOWN;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.UP;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST;

import java.util.Map;
import java.util.function.Supplier;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.ticks.TickPriority;

public class EncasedPipeBlock extends Block
	implements IWrenchable, SpecialBlockItemRequirement, IBE<FluidPipeBlockEntity>, EncasedBlock, TransformableBlock {
	public static final Map<Direction, BooleanProperty> FACING_TO_PROPERTY_MAP = PipeBlock.PROPERTY_BY_DIRECTION;

	private final Supplier<Block> casing;

	public EncasedPipeBlock(Properties properties, Supplier<Block> casing) {
		super(properties);
		this.casing = casing;
		registerDefaultState(defaultBlockState().setValue(NORTH, false)
			.setValue(SOUTH, false)
			.setValue(DOWN, false)
			.setValue(UP, false)
			.setValue(WEST, false)
			.setValue(EAST, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
		boolean blockTypeChanged = state.getBlock() != newState.getBlock();
		if (blockTypeChanged && !world.isClientSide())
			FluidPropagator.propagateChangedPipe(world, pos, state);
		if (state.hasBlockEntity() && (blockTypeChanged || !newState.hasBlockEntity()))
			world.removeBlockEntity(pos);
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (!world.isClientSide() && state != oldState)
			world.scheduleTick(pos, this, 1, TickPriority.HIGH);
	}

	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
		return AllBlocks.FLUID_PIPE.asStack();
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block otherBlock, Orientation orientation,
		boolean isMoving) {
		if (!world.isClientSide())
			world.scheduleTick(pos, this, 1, TickPriority.HIGH);
	}

	@Override
	protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource r) {
		FluidPropagator.propagateChangedPipe(world, pos, state);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean isMoving) {
		FluidPropagator.propagateChangedPipe(world, pos, state);
		super.affectNeighborsAfterRemoval(state, world, pos, isMoving);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();

		if (world.isClientSide())
			return InteractionResult.SUCCESS;

		context.getLevel()
			.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, context.getClickedPos(), Block.getId(state));
		BlockState equivalentPipe = transferSixWayProperties(state, AllBlocks.FLUID_PIPE.getDefaultState());

		FluidTransportBehaviour.cacheFlows(world, pos);
		// The encased pipe already contains the exact six-way connection state. Recalculating it
		// while the encased block entity is still at this position can discard an isolated
		// vertical axis on newer versions. Preserve the state verbatim when removing the casing;
		// normal neighbour updates will maintain it afterwards.
		world.setBlockAndUpdate(pos, equivalentPipe);
		FluidTransportBehaviour.loadFlows(world, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		// Encased shafts remove only their casing when sneak-wrenched. Pipes should behave
		// consistently instead of dismantling the entire pipe into the player's inventory.
		return onWrenched(state, context);
	}

	public static BlockState transferSixWayProperties(BlockState from, BlockState to) {
		for (Direction d : Iterate.directions) {
			BooleanProperty property = FACING_TO_PROPERTY_MAP.get(d);
			to = to.setValue(property, from.getValue(property));
		}
		return to;
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		return ItemRequirement.of(AllBlocks.FLUID_PIPE.getDefaultState(), be);
	}

	@Override
	public Class<FluidPipeBlockEntity> getBlockEntityClass() {
		return FluidPipeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ENCASED_FLUID_PIPE.get();
	}

	@Override
	public Block getCasing() {
		return casing.get();
	}

	@Override
	public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player, InteractionHand hand,
	    BlockHitResult ray) {
		FluidTransportBehaviour.cacheFlows(level, pos);
		level.setBlockAndUpdate(pos,
				EncasedPipeBlock.transferSixWayProperties(state, defaultBlockState()));
		FluidTransportBehaviour.loadFlows(level, pos);
	}

	@Override
	public BlockState rotate(BlockState pState, Rotation pRotation) {
		return FluidPipeBlockRotation.rotate(pState, pRotation);
	}

	@Override
	public BlockState mirror(BlockState pState, Mirror pMirror) {
		return FluidPipeBlockRotation.mirror(pState, pMirror);
	}

	@Override
	public BlockState transform(BlockState state, StructureTransform transform) {
		return FluidPipeBlockRotation.transform(state, transform);
	}

}
