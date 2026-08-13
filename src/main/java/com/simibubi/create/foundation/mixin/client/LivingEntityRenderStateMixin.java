package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.simibubi.create.content.equipment.hats.CreateHatRenderState;
import com.simibubi.create.content.equipment.armor.CardboardStealthRenderState;
import com.simibubi.create.content.trains.schedule.hat.TrainHatInfo;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements CreateHatRenderState, CardboardStealthRenderState {
	@Unique
	private int create$hatType;
	@Unique
	private TrainHatInfo create$hatInfo;
	@Unique
	private boolean create$cardboardStealth;
	@Unique
	private boolean create$cardboardOnGround;
	@Unique
	private float create$cardboardMovement;
	@Unique
	private final ItemStackRenderState create$cardboardBox = new ItemStackRenderState();

	@Override
	public int create$getHatType() {
		return create$hatType;
	}

	@Override
	public void create$setHatType(int type) {
		create$hatType = type;
	}

	@Override
	public TrainHatInfo create$getHatInfo() {
		return create$hatInfo;
	}

	@Override
	public void create$setHatInfo(TrainHatInfo info) {
		create$hatInfo = info;
	}

	@Override
	public boolean create$isCardboardStealth() {
		return create$cardboardStealth;
	}

	@Override
	public void create$setCardboardStealth(boolean stealth) {
		create$cardboardStealth = stealth;
		if (!stealth)
			create$cardboardBox.clear();
	}

	@Override
	public boolean create$isCardboardOnGround() {
		return create$cardboardOnGround;
	}

	@Override
	public void create$setCardboardOnGround(boolean onGround) {
		create$cardboardOnGround = onGround;
	}

	@Override
	public float create$getCardboardMovement() {
		return create$cardboardMovement;
	}

	@Override
	public void create$setCardboardMovement(float movement) {
		create$cardboardMovement = movement;
	}

	@Override
	public ItemStackRenderState create$getCardboardBox() {
		return create$cardboardBox;
	}
}
