package com.simibubi.create.content.decoration.slidingDoor;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public class DoorControlBehaviour extends BlockEntityBehaviour {

	public static final BehaviourType<DoorControlBehaviour> TYPE = new BehaviourType<>();

	public DoorControl mode;

	public DoorControlBehaviour(SmartBlockEntity be) {
		super(be);
		mode = DoorControl.ALL;
	}

	public void set(DoorControl mode) {
		if (this.mode == mode)
			return;
		this.mode = mode;
		blockEntity.notifyUpdate();
	}

	@Override
	public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		nbt.putString("DoorControl", mode.name());
		super.write(nbt, registries, clientPacket);
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		mode = readDoorControl(nbt.getStringOr("DoorControl", DoorControl.ALL.name()));
		super.read(nbt, registries, clientPacket);
	}

	private static DoorControl readDoorControl(String value) {
		try {
			return DoorControl.valueOf(value);
		} catch (IllegalArgumentException e) {
			return DoorControl.ALL;
		}
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

}
