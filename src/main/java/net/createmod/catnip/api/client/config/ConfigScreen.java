package net.createmod.catnip.api.client.config;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public class ConfigScreen extends Screen {
	@FunctionalInterface
	public interface BackgroundRenderer {
		void render(Screen screen, GuiGraphics graphics, float partialTicks);
	}

	public static final Map<String, BackgroundRenderer> backgrounds = new HashMap<>();
	public static BlockState shadowState;
	public static String modID;

	public ConfigScreen(Screen parent) {
		super(Component.empty());
	}

	public static String toHumanReadable(String key) {
		if (key == null || key.isEmpty())
			return "";
		String[] words = key.replace('_', ' ').split(" ");
		StringBuilder builder = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty())
				continue;
			if (!builder.isEmpty())
				builder.append(' ');
			builder.append(Character.toUpperCase(word.charAt(0)));
			if (word.length() > 1)
				builder.append(word.substring(1));
		}
		return builder.toString();
	}
}
