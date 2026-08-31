package net.createmod.catnip.api.client.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.client.gui.element.FadableScreenElement;
import net.createmod.catnip.api.client.gui.element.TextStencilElement;
import net.createmod.catnip.api.client.gui.texture.CatnipGuiTextures;
import net.createmod.catnip.api.client.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.api.client.gui.widget.BoxWidget;
import net.createmod.catnip.api.client.lang.FontHelper;
import net.createmod.catnip.api.client.lang.FontHelper.Palette;
import net.createmod.catnip.api.config.ConfigHelper;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class BaseConfigScreen extends ConfigScreen {

	public static final Color COLOR_TITLE_A = new Color(0xff_c69fbc).setImmutable();
	public static final Color COLOR_TITLE_B = new Color(0xff_f6b8bb).setImmutable();
	public static final Color COLOR_TITLE_C = new Color(0xff_fbf994).setImmutable();

	public static final FadableScreenElement DISABLED_RENDERER = (ms, width, height, alpha) -> UIRenderHelper.angledGradient(ms, 0, 0, height / 2, height, width, AbstractSimiWidget.COLOR_DISABLED);
	private static final Map<String, UnaryOperator<BaseConfigScreen>> DEFAULTS = new HashMap<>();

	/**
	 * If you want to change the config labels, add a default action here.
	 * Make sure you call either {@link #withSpecs(ModConfigSpec, ModConfigSpec, ModConfigSpec)}
	 * or {@link #searchForConfigSpecs()}
	 *
	 * @param modID the modID of your addon/mod
	 */
	public static void setDefaultActionFor(String modID, UnaryOperator<BaseConfigScreen> transform) {
		DEFAULTS.put(modID, transform);
	}

	@Nullable
	BoxWidget clientConfigWidget;
	@Nullable
	BoxWidget commonConfigWidget;
	@Nullable
	BoxWidget serverConfigWidget;
	@Nullable
	BoxWidget goBack;
	@Nullable
	BoxWidget others;
	@Nullable
	BoxWidget title;

	@Nullable
	ModConfigSpec clientSpec;
	@Nullable
	ModConfigSpec commonSpec;
	@Nullable
	ModConfigSpec serverSpec;
	String clientButtonLabel = "Client Config";
	String commonButtonLabel = "Common Config";
	String serverButtonLabel = "Server Config";
	String modID;
	protected boolean returnOnClose;

	public BaseConfigScreen(@Nullable Screen parent, String modID) {
		super(parent);
		this.modID = modID;

		if (DEFAULTS.containsKey(modID))
			DEFAULTS.get(modID).apply(this);
		else {
			this.searchForConfigSpecs();
		}
	}

	/**
	 * If you have static references to your Configs or ConfigSpecs (like Create does in AllConfigs),
	 * please use {@link #withSpecs(ModConfigSpec, ModConfigSpec, ModConfigSpec)} instead
	 */
	public BaseConfigScreen searchForConfigSpecs() {
		if (!net.createmod.catnip.api.config.ConfigHelper.hasAnyForgeConfig(this.modID)) {
			return this;
		}

		try {
			clientSpec = net.createmod.catnip.api.config.ConfigHelper.findModConfigSpecFor(ModConfig.Type.CLIENT, modID);
		} catch (ClassCastException | NullPointerException e) {
			ConfigHelper.LOGGER.debug("Unable to find ClientConfigSpec for mod: {}", modID);
		}

		try {
			commonSpec = net.createmod.catnip.api.config.ConfigHelper.findModConfigSpecFor(ModConfig.Type.COMMON, modID);
		} catch (ClassCastException | NullPointerException e) {
			ConfigHelper.LOGGER.debug("Unable to find CommonConfigSpec for mod: {}", modID);
		}

		try {
			serverSpec = net.createmod.catnip.api.config.ConfigHelper.findModConfigSpecFor(ModConfig.Type.SERVER, modID);
		} catch (ClassCastException | NullPointerException e) {
			ConfigHelper.LOGGER.debug("Unable to find ServerConfigSpec for mod: {}", modID);
		}

		return this;
	}

	public BaseConfigScreen withSpecs(@Nullable ModConfigSpec client, @Nullable ModConfigSpec common, @Nullable ModConfigSpec server) {
		clientSpec = client;
		commonSpec = common;
		serverSpec = server;
		return this;
	}

	public BaseConfigScreen withButtonLabels(@Nullable String client, @Nullable String common, @Nullable String server) {
		if (client != null)
			clientButtonLabel = client;

		if (common != null)
			commonButtonLabel = common;

		if (server != null)
			serverButtonLabel = server;

		return this;
	}

	@Override
	protected void init() {
		super.init();
		returnOnClose = true;

		TextStencilElement clientText = new TextStencilElement(font, Component.translatable("catnip.ui.client_config_button_label")).centered(true, true);
		addRenderableWidget(clientConfigWidget = new BoxWidget(width / 2 - 100, height / 2 - 15 - 30, 200, 16).showingElement(clientText));

		if (clientSpec != null) {
			clientConfigWidget.withCallback(() -> linkTo(new SubMenuConfigScreen(this, ModConfig.Type.CLIENT, clientSpec)));
			clientText.withElementRenderer(BoxWidget.gradientFactory.apply(clientConfigWidget));
		} else {
			clientConfigWidget.active = false;
			clientConfigWidget.updateGradientFromState();
			clientText.withElementRenderer(DISABLED_RENDERER);
		}

		TextStencilElement commonText = new TextStencilElement(font, Component.translatable("catnip.ui.common_config_button_label")).centered(true, true);
		addRenderableWidget(commonConfigWidget = new BoxWidget(width / 2 - 100, height / 2 - 15, 200, 16).showingElement(commonText));

		if (commonSpec != null) {
			commonConfigWidget.withCallback(() -> linkTo(new SubMenuConfigScreen(this, ModConfig.Type.COMMON, commonSpec)));
			commonText.withElementRenderer(BoxWidget.gradientFactory.apply(commonConfigWidget));
		} else {
			commonConfigWidget.active = false;
			commonConfigWidget.updateGradientFromState();
			commonText.withElementRenderer(DISABLED_RENDERER);
		}

		TextStencilElement serverText = new TextStencilElement(font, Component.translatable("catnip.ui.server_config_button_label")).centered(true, true);
		addRenderableWidget(serverConfigWidget = new BoxWidget(width / 2 - 100, height / 2 - 15 + 30, 200, 16).showingElement(serverText));

		if (serverSpec == null) {
			serverConfigWidget.active = false;
			serverConfigWidget.updateGradientFromState();
			serverText.withElementRenderer(DISABLED_RENDERER);
		} else if (minecraft.level == null) {
			serverText.withElementRenderer(DISABLED_RENDERER);
			serverConfigWidget.getToolTip()
				.add(Component.translatable("catnip.ui.server_config_unavailable"));
			serverConfigWidget.getToolTip()
				.addAll(FontHelper.cutTextComponent(
					Component.translatable("catnip.ui.server_config_unavailable_tooltip"),
					Palette.ALL_GRAY));
		} else {
			serverConfigWidget.withCallback(() -> linkTo(new SubMenuConfigScreen(this, ModConfig.Type.SERVER, serverSpec)));
			serverText.withElementRenderer(BoxWidget.gradientFactory.apply(serverConfigWidget));
		}

		TextStencilElement titleText = new TextStencilElement(font, PlatformHelper.INSTANCE.getModDisplayName(modID))
			.centered(true, true)
			.withElementRenderer((ms, w, h, alpha) -> {
				UIRenderHelper.angledGradient(ms, 0, 0, h / 2, h, w / 2, COLOR_TITLE_A, COLOR_TITLE_B);
				UIRenderHelper.angledGradient(ms, 0, w / 2, h / 2, h, w / 2, COLOR_TITLE_B, COLOR_TITLE_C);
			});
		int boxWidth = width + 10;
		int boxHeight = 39;
		int boxPadding = 4;
		title = new BoxWidget(-5, height / 2 - 110, boxWidth, boxHeight)
			//.withCustomBackground(new Color(0x20_000000, true))
			.<BoxWidget>setActive(false)
			.withBorderColors(AbstractSimiWidget.COLOR_IDLE)
			.withPadding(0, boxPadding)
			.rescaleElement(boxWidth / 2f, (boxHeight - 2 * boxPadding) / 2f)//double the text size by telling it the element is only half as big as the available space
			.showingElement(titleText.at(0, 7));

		addRenderableWidget(title);


		ConfigScreen.modID = this.modID;

		goBack = new BoxWidget(width / 2 - 134, height / 2, 20, 20).withPadding(2, 2)
			.withCallback(() -> linkTo(parent));
		goBack.showingElement(CatnipGuiTextures.ICON_CONFIG_BACK.asStencil()
			.withElementRenderer(BoxWidget.gradientFactory.apply(goBack)));
		goBack.getToolTip()
			.add(Component.translatable("catnip.ui.go_back_button"));
		addRenderableWidget(goBack);

		TextStencilElement othersText = new TextStencilElement(font, Component.translatable("catnip.ui.other_mods_config_button_label")).centered(true, true);
		others = new BoxWidget(width / 2 - 100, height / 2 - 15 + 90, 200, 16).showingElement(othersText);
		othersText.withElementRenderer(BoxWidget.gradientFactory.apply(others));
		others.withCallback(() -> linkTo(new ConfigModListScreen(this)));
		addRenderableWidget(others);
	}

	@Override
	protected void renderWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		graphics.centeredText(font, Component.translatable("catnip.ui.other_mods_config_title"), width / 2, height / 2 - 105, UIRenderHelper.COLOR_TEXT_STRONG_ACCENT.getFirst().getRGB());
	}

	private void linkTo(@Nullable Screen screen) {
		returnOnClose = false;
		ScreenOpener.open(screen);
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (super.keyPressed(keyEvent))
			return true;
		if (keyEvent.key() == InputConstants.KEY_BACKSPACE) {
			linkTo(parent);
		}
		return false;
	}
}
