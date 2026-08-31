package net.minecraft.util;

public class FastColor {
	public static class ARGB32 {
		public static int color(int alpha, int red, int green, int blue) {
			return alpha << 24 | red << 16 | green << 8 | blue;
		}

		public static int color(int alpha, int rgb) {
			return alpha << 24 | rgb & 0xFFFFFF;
		}
	}

	public static class ABGR32 {
		public static int color(int alpha, int blue, int green, int red) {
			return alpha << 24 | blue << 16 | green << 8 | red;
		}

		public static int color(int alpha, int bgr) {
			return alpha << 24 | bgr & 0xFFFFFF;
		}
	}
}
