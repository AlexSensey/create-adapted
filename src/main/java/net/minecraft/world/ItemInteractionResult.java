package net.minecraft.world;

public enum ItemInteractionResult {
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
