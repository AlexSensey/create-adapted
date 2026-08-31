package net.createmod.catnip.api.lang;

import java.text.NumberFormat;
import java.util.Locale;

public class LangNumberFormat {
	private static final NumberFormat FORMAT = createFormat();

	private static NumberFormat createFormat() {
		NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(0);
		format.setGroupingUsed(true);
		return format;
	}

	public static String format(double value) {
		if (value == -0d)
			value = 0d;
		synchronized (FORMAT) {
			return FORMAT.format(value).replace('\u00A0', ' ');
		}
	}
}
