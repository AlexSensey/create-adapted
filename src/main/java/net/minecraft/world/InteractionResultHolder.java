package net.minecraft.world;

public record InteractionResultHolder<T>(InteractionResult result, T object) {
	public InteractionResult getResult() {
		return result;
	}

	public T getObject() {
		return object;
	}

	public static <T> InteractionResultHolder<T> success(T object) {
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, object);
	}

	public static <T> InteractionResultHolder<T> sidedSuccess(T object, boolean clientSide) {
		return success(object);
	}

	public static <T> InteractionResultHolder<T> consume(T object) {
		return new InteractionResultHolder<>(InteractionResult.CONSUME, object);
	}

	public static <T> InteractionResultHolder<T> pass(T object) {
		return new InteractionResultHolder<>(InteractionResult.PASS, object);
	}

	public static <T> InteractionResultHolder<T> fail(T object) {
		return new InteractionResultHolder<>(InteractionResult.FAIL, object);
	}
}
