package com.simibubi.create.content.kinetics.speedController;

import java.util.List;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class SpeedControllerBlockEntity extends KineticBlockEntity {

	public static final int DEFAULT_SPEED = 16;
	public ScrollValueBehaviour targetSpeed;
	public AbstractComputerBehaviour computerBehaviour;

	boolean hasBracket;

	public SpeedControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		hasBracket = false;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		if (Mods.COMPUTERCRAFT.isLoaded()) {
			event.registerBlockEntity(
					PeripheralCapability.get(),
					AllBlockEntityTypes.ROTATION_SPEED_CONTROLLER.get(),
					(be, context) -> be.computerBehaviour.getPeripheralCapability()
			);
		}
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		updateBracket();
	}

	@Override
	public void initialize() {
		super.initialize();
		if (level == null)
			return;
		if (level.isClientSide())
			requestModelDataUpdate();
		else
			sendData();
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (tag.contains("TargetSpeed")) {
			int previousValue = targetSpeed.value;
			targetSpeed.value = tag.getIntOr("TargetSpeed", targetSpeed.value);
			if (!clientPacket && level != null && !level.isClientSide() && previousValue != targetSpeed.value)
				updateTargetRotation();
		}
		if (level != null && level.isClientSide())
			requestModelDataUpdate();
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putInt("TargetSpeed", targetSpeed.getValue());
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		Integer max = AllConfigs.server().kinetics.maxRotationSpeed.get();

		targetSpeed = new KineticScrollValueBehaviour(CreateLang.translateDirect("kinetics.speed_controller.rotation_speed"),
			this, new ControllerValueBoxTransform());
		targetSpeed.between(-max, max);
		targetSpeed.value = DEFAULT_SPEED;
		targetSpeed.withCallback(i -> this.updateTargetRotation());
		behaviours.add(targetSpeed);
		behaviours.add(computerBehaviour = ComputerCraftProxy.behaviour(this));

		registerAwardables(behaviours, AllAdvancements.SPEED_CONTROLLER);
	}

	private void updateTargetRotation() {
		if (level == null)
			return;
		if (hasNetwork())
			getOrCreateNetwork().remove(this);
		RotationPropagator.handleRemoved(level, worldPosition, this);
		removeSource();
		attachKinetics();

		if (isCogwheelPresent() && getSpeed() != 0)
			award(AllAdvancements.SPEED_CONTROLLER);
	}

	public static float getConveyedSpeed(KineticBlockEntity cogWheel, KineticBlockEntity speedControllerIn,
		boolean targetingController) {
		if (!(speedControllerIn instanceof SpeedControllerBlockEntity))
			return 0;

		float speed = speedControllerIn.getTheoreticalSpeed();
		float wheelSpeed = cogWheel.getTheoreticalSpeed();
		float desiredOutputSpeed = getDesiredOutputSpeed(cogWheel, speedControllerIn, targetingController);

		float compareSpeed = targetingController ? speed : wheelSpeed;
		if (desiredOutputSpeed >= 0 && compareSpeed >= 0)
			return Math.max(desiredOutputSpeed, compareSpeed);
		if (desiredOutputSpeed < 0 && compareSpeed < 0)
			return Math.min(desiredOutputSpeed, compareSpeed);

		return desiredOutputSpeed;
	}

	public static float getDesiredOutputSpeed(KineticBlockEntity cogWheel, KineticBlockEntity speedControllerIn,
		boolean targetingController) {
		SpeedControllerBlockEntity speedController = (SpeedControllerBlockEntity) speedControllerIn;
		float targetSpeed = speedController.targetSpeed.getValue();
		float speed = speedControllerIn.getTheoreticalSpeed();
		float wheelSpeed = cogWheel.getTheoreticalSpeed();

		if (targetSpeed == 0)
			return 0;
		if (targetingController && wheelSpeed == 0)
			return 0;
		if (!speedController.hasSource()) {
			if (targetingController)
				return targetSpeed;
			return 0;
		}

		boolean wheelPowersController = speedController.source.equals(cogWheel.getBlockPos());

		if (wheelPowersController) {
			if (targetingController)
				return targetSpeed;
			return wheelSpeed;
		}

		if (targetingController)
			return speed;
		return targetSpeed;
	}

	public void updateBracket() {
		if (level != null && level.isClientSide())
			hasBracket = isCogwheelPresent();
	}

	private boolean isCogwheelPresent() {
		BlockState stateAbove = level.getBlockState(worldPosition.above());
		return ICogWheel.isDedicatedCogWheel(stateAbove.getBlock()) && ICogWheel.isLargeCog(stateAbove)
			&& stateAbove.getValue(CogWheelBlock.AXIS)
				.isHorizontal();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		computerBehaviour.removePeripheral();
	}

	private class ControllerValueBoxTransform extends ValueBoxTransform.Sided {

		@Override
		protected Vec3 getSouthLocation() {
			return VecHelper.voxelSpace(8, 11f, 15.5f);
		}

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			if (direction.getAxis()
				.isVertical())
				return false;
			return state.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) != direction.getAxis();
		}

		@Override
		public float getScale() {
			return 0.5f;
		}

	}

}
