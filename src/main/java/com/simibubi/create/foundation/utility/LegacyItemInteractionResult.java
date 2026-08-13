package com.simibubi.create.foundation.utility;

import net.minecraft.world.InteractionResult;

/** Internal replacement for the item-specific interaction result removed in 26.2. */
public enum LegacyItemInteractionResult {
	SUCCESS,
	FAIL,
	PASS_TO_DEFAULT_BLOCK_INTERACTION;

	public InteractionResult asInteractionResult() {
		return switch (this) {
			case SUCCESS -> InteractionResult.SUCCESS;
			case FAIL -> InteractionResult.FAIL;
			case PASS_TO_DEFAULT_BLOCK_INTERACTION -> InteractionResult.PASS;
		};
	}
}
