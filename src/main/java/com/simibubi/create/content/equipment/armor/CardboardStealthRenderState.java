package com.simibubi.create.content.equipment.armor;

import net.minecraft.client.renderer.item.ItemStackRenderState;

public interface CardboardStealthRenderState {
	boolean create$isCardboardStealth();

	void create$setCardboardStealth(boolean stealth);

	boolean create$isCardboardOnGround();

	void create$setCardboardOnGround(boolean onGround);

	float create$getCardboardMovement();

	void create$setCardboardMovement(float movement);

	ItemStackRenderState create$getCardboardBox();
}
