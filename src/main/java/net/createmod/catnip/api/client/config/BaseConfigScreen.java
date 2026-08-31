package net.createmod.catnip.api.client.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import net.minecraft.client.gui.screens.Screen;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BaseConfigScreen extends ConfigScreen {
	private static final Map<String, UnaryOperator<BaseConfigScreen>> DEFAULTS = new HashMap<>();

	public BaseConfigScreen(Screen parent, String modID) {
		super(parent);
		ConfigScreen.modID = modID;
		UnaryOperator<BaseConfigScreen> defaultAction = DEFAULTS.get(modID);
		if (defaultAction != null)
			defaultAction.apply(this);
	}

	public static void setDefaultActionFor(String modID, UnaryOperator<BaseConfigScreen> transform) {
		DEFAULTS.put(modID, transform);
	}

	public BaseConfigScreen withButtonLabels(String client, String common, String server) {
		return this;
	}

	public BaseConfigScreen withSpecs(ModConfigSpec client, ModConfigSpec common, ModConfigSpec server) {
		return this;
	}
}
