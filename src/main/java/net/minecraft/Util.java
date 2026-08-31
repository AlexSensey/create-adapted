package net.minecraft;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceLocation;

public class Util {
	public static <T> T make(T object, Consumer<T> consumer) {
		consumer.accept(object);
		return object;
	}

	public static <T> T make(Supplier<T> supplier) {
		return supplier.get();
	}

	public static long getMillis() {
		return System.currentTimeMillis();
	}

	public static OS getPlatform() {
		return OS.UNKNOWN;
	}

	public static String makeDescriptionId(String type, Identifier id) {
		return type + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
	}

	public static String makeDescriptionId(String type, ResourceLocation id) {
		return type + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
	}

	public enum OS {
		LINUX,
		SOLARIS,
		WINDOWS,
		OSX,
		UNKNOWN;

		public void openFile(File file) {
		}
	}
}
