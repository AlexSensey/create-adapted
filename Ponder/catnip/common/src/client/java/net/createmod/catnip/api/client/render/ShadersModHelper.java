package net.createmod.catnip.api.client.render;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.createmod.catnip.api.platform.services.PlatformHelper;
import net.minecraft.util.Util;

public final class ShadersModHelper {
	private static final Logger logger = LogUtils.getLogger();

	private static final DirectMethodHandleDesc handleDesc = MethodHandleDesc.ofMethod(
		DirectMethodHandleDesc.Kind.STATIC,
		ClassDesc.of("net.irisshaders.iris.api.v0.IrisApi"),
		"isShaderPackInUse",
		MethodTypeDesc.of(ConstantDescs.CD_boolean)
	);

	private static final BooleanSupplier isShaderPackInUse = Util.make(() -> {
		if (!PlatformHelper.INSTANCE.isModLoaded("iris"))
			return () -> false;

		try {
			MethodHandle handle = handleDesc.resolveConstantDesc(MethodHandles.lookup());
			return () -> {
				try {
					return (boolean) handle.invokeExact();
				} catch (Throwable t) {
					throw new RuntimeException("Exception checking for shaders", t);
				}
			};
		} catch (ReflectiveOperationException e) {
			logger.error("Failed to load Iris compat", e);
			return () -> false;
		}
	});

	public static boolean isShaderPackInUse() {
		return isShaderPackInUse.getAsBoolean();
	}
}
