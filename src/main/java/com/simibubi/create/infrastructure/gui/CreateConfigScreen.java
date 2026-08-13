package com.simibubi.create.infrastructure.gui;

import com.simibubi.create.Create;

import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen.Context;
import net.neoforged.neoforge.client.gui.ModListScreen;

public class CreateConfigScreen extends Screen {
	private final Screen parent;

	public CreateConfigScreen(Screen parent) {
		super(Component.literal("Create Configuration"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int center = width / 2;
		int top = height / 2 - 70;

		addRenderableWidget(Button.builder(Component.literal("Client Settings"),
			$ -> open(ModConfig.Type.CLIENT, "Client Settings"))
			.bounds(center - 100, top, 200, 20)
			.build());

		addRenderableWidget(Button.builder(Component.literal("World Generation Settings"),
			$ -> open(ModConfig.Type.COMMON, "World Generation Settings"))
			.bounds(center - 100, top + 30, 200, 20)
			.build());

		Button gameplay = Button.builder(Component.literal("Gameplay Settings"),
			$ -> open(ModConfig.Type.SERVER, "Gameplay Settings"))
			.bounds(center - 100, top + 60, 200, 20)
			.tooltip(Tooltip.create(Component.literal("Available after joining a world")))
			.build();
		gameplay.active = minecraft.level != null;
		addRenderableWidget(gameplay);

		addRenderableWidget(Button.builder(Component.literal("Other Mods' Configurations"),
			$ -> ScreenOpener.open(new ModListScreen(this)))
			.bounds(center - 100, top + 100, 200, 20)
			.build());

		addRenderableWidget(Button.builder(Component.literal("Back"), $ -> onClose())
			.bounds(center - 100, top + 130, 200, 20)
			.build());
	}

	private void open(ModConfig.Type type, String label) {
		ModConfigs.getModConfigs(Create.ID).stream()
			.filter(config -> config.getType() == type)
			.findFirst()
			.ifPresent(config -> openRootContents(config, type, label));
	}

	private void openRootContents(ModConfig config, ModConfig.Type type, String label) {
		ModConfigSpec spec = (ModConfigSpec) config.getSpec();
		String rootKey = type.name().toLowerCase(java.util.Locale.ROOT);
		Object valueSpecsRoot = spec.getSpec().get(rootKey);
		Object entriesRoot = spec.getValues().get(rootKey);

		if (valueSpecsRoot instanceof UnmodifiableConfig valueSpecs
			&& entriesRoot instanceof UnmodifiableConfig entries) {
			Context context = Context.top(Create.ID, this, config, ($, $$, element) -> element);
			ScreenOpener.open(new ConfigurationSectionScreen(context, this, valueSpecs.valueMap(), rootKey,
				entries.entrySet(), Component.literal(label)));
			return;
		}

		ScreenOpener.open(new ConfigurationSectionScreen(this, type, config, Component.literal(label)));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		CreateMainMenuScreen.extractCreateBackground(graphics, width, height);
		graphics.fillGradient(0, 0, width, height, 0x90000000, 0xb0000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.centeredText(font, title, width / 2, height / 2 - 105, 0xFFE4BB67);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}
}
