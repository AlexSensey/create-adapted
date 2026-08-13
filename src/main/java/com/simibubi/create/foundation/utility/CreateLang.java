package com.simibubi.create.foundation.utility;

import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

import com.simibubi.create.Create;

import net.createmod.catnip.api.lang.Lang;
import net.createmod.catnip.api.lang.LangBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class CreateLang extends Lang {
	private static final NumberFormat NUMBER_FORMAT = createNumberFormat();

	private static NumberFormat createNumberFormat() {
		NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(0);
		format.setGroupingUsed(true);
		return format;
	}

	/**
	 * legacy-ish. Use CreateLang.translate and other builder methods where possible
	 *

	 */
	public static MutableComponent translateDirect(String key, Object... args) {
        Object[] args1 = LangBuilder.resolveBuilders(args);
        return Component.translatable(Create.ID + "." + key, args1);
    }

	public static List<Component> translatedOptions(String prefix, String... keys) {
		List<Component> result = new ArrayList<>(keys.length);
		for (String key : keys)
			result.add(translate((prefix != null ? prefix + "." : "") + key).component());
		return result;
	}

	//

	public static LangBuilder builder() {
		return new LangBuilder(Create.ID);
	}

	public static LangBuilder blockName(BlockState state) {
		return builder().add(state.getBlock()
				.getName());
	}

	public static LangBuilder itemName(ItemStack stack) {
		return builder().add(stack.getHoverName()
				.copy());
	}

	public static LangBuilder fluidName(FluidStack stack) {
		return builder().add(stack.getHoverName()
				.copy());
	}

	public static LangBuilder number(double d) {
		if (d == -0d)
			d = 0d;
		String formatted;
		synchronized (NUMBER_FORMAT) {
			formatted = NUMBER_FORMAT.format(d).replace('\u00A0', ' ');
		}
		return builder().text(formatted);
	}

	public static LangBuilder translate(String langKey, Object... args) {
		return builder().translate(langKey, args);
	}

	public static LangBuilder text(String text) {
		return builder().text(text);
	}

	@Deprecated // Use while implementing and replace all references with Lang.translate
	public static LangBuilder temporaryText(String text) {
		return builder().text(text);
	}

}
