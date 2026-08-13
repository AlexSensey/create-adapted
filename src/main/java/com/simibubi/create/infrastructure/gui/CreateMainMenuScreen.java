package com.simibubi.create.infrastructure.gui;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.CreateBuildInfo;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.data.Iterate;
import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.NavigatableSimiScreen;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.catnip.api.client.gui.element.BoxElement;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.client.lang.FontHelper;
import net.createmod.catnip.api.client.lang.FontHelper.Palette;
import net.createmod.catnip.api.theme.Color;
import net.createmod.ponder.impl.client.gui.PonderTagIndexScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

public class CreateMainMenuScreen extends AbstractSimiScreen {
	private static final Identifier CREATE_BACKGROUND =
		Create.asResource("textures/gui/title/background/panorama_0.png");

	private static final Component CURSEFORGE_TOOLTIP;

	static {
		CURSEFORGE_TOOLTIP = Component.literal("CurseForge").withStyle(s -> s.withColor(0xFC785C).withBold(true));
	}

	private static final Component MODRINTH_TOOLTIP;

	static {
		MODRINTH_TOOLTIP = Component.literal("Modrinth").withStyle(s -> s.withColor(0x3FD32B).withBold(true));
	}

	public static final String CURSEFORGE_LINK = "https://www.curseforge.com/minecraft/mc-mods/create";
	public static final String MODRINTH_LINK = "https://modrinth.com/mod/create";
	public static final String ISSUE_TRACKER_LINK = "https://github.com/Creators-of-Create/Create/issues";
	public static final String SUPPORT_LINK = "https://github.com/Creators-of-Create/Create/wiki/Supporting-the-Project";

	protected final Screen parent;
	protected boolean returnOnClose;

	private Button gettingStarted;

	public CreateMainMenuScreen(Screen parent) {
		super(Component.literal(Create.NAME));
		this.parent = parent;
		returnOnClose = true;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		if (parent instanceof TitleScreen)
			extractCreateBackground(graphics, width, height);
		else {
			Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			parent.extractBackground(graphics, 0, 0, partialTicks);
			pose.popMatrix();
		}
		graphics.fillGradient(0, 0, width, height, 0x90000000, 0xb0000000);
	}

	public static void extractCreateBackground(GuiGraphicsExtractor graphics, int width, int height) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.scale(width / 512f, height / 288f);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CREATE_BACKGROUND, 0, 0, 0, 112,
			512, 288, 512, 512);
		pose.popMatrix();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		for (int side : Iterate.positiveAndNegative) {
			GuiGameElement.of(AllBlocks.LARGE_COGWHEEL.getDefaultState())
				.at(width / 2f + side * 62, 60)
				.scale(24)
				.rotate(45, Util.getMillis() / 32f * side, 0)
				.submit(graphics);
			GuiGameElement.of(AllBlocks.COGWHEEL.getDefaultState())
				.at(width / 2f + side * 38, 72)
				.scale(20)
				.rotate(45, Util.getMillis() / -16f * side + 22.5f, 0)
				.submit(graphics);
		}

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(width / 2f - 32, 28);
		pose.scale(0.25f, 0.25f);
		AllGuiTextures.LOGO.render(graphics, 0, 0);
		pose.popMatrix();

		new BoxElement().withBackground(0x88_000000)
			.flatBorder(new Color(0x01_000000))
			.at(width / 2 - 64, 84, 100)
			.withBounds(128, 11)
			.submit(graphics);

		graphics.centeredText(font, Component.literal(Create.NAME).withStyle(ChatFormatting.BOLD)
				.append(
					Component.literal(" v" + CreateBuildInfo.VERSION).withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE)),
			width / 2, 89, 0xFF_E4BB67);

		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

		if (parent instanceof TitleScreen) {
			if (mouseX < gettingStarted.getX() || mouseX > gettingStarted.getX() + 98)
				return;
			if (mouseY < gettingStarted.getY() || mouseY > gettingStarted.getY() + 20)
				return;
			graphics.setComponentTooltipForNextFrame(font,
				FontHelper.cutTextComponent(CreateLang.translateDirect("menu.only_ingame"), Palette.ALL_GRAY), mouseX, mouseY);
		}
	}

	protected void init() {
		super.init();
		returnOnClose = true;
		this.addButtons();
	}

	private void addButtons() {
		int yStart = height / 4 + 40;
		int center = width / 2;
		int bHeight = 20;
		int bShortWidth = 98;
		int bLongWidth = 200;

		addRenderableWidget(Button.builder(CreateLang.translateDirect("menu.return"), $ -> linkTo(parent))
			.bounds(center - 100, yStart + 92, bLongWidth, bHeight)
			.build());
		Button configure = Button.builder(CreateLang.translateDirect("menu.configure"), $ -> linkTo(new CreateConfigScreen(this)))
			.bounds(center - 100, yStart + 24 + -16, bLongWidth, bHeight)
			.build();
		addRenderableWidget(configure);

		gettingStarted = Button.builder(CreateLang.translateDirect("menu.ponder_index"), $ -> linkTo(new PonderTagIndexScreen()))
			.bounds(center + 2, yStart + 48 + -16, bShortWidth, bHeight)
			.build();
		gettingStarted.active = !(parent instanceof TitleScreen);
		addRenderableWidget(gettingStarted);

		addRenderableWidget(new PlatformIconButton(center - 100, yStart + 48 + -16, bShortWidth / 2, bHeight,
			AllGuiTextures.CURSEFORGE_LOGO, 0.085f,
			b -> linkTo(CURSEFORGE_LINK),
			Tooltip.create(CURSEFORGE_TOOLTIP)));
		addRenderableWidget(new PlatformIconButton(center - 50, yStart + 48 + -16, bShortWidth / 2, bHeight,
			AllGuiTextures.MODRINTH_LOGO, 0.0575f,
			b -> linkTo(MODRINTH_LINK),
			Tooltip.create(MODRINTH_TOOLTIP)));

		addRenderableWidget(Button.builder(CreateLang.translateDirect("menu.report_bugs"), $ -> linkTo(ISSUE_TRACKER_LINK))
			.bounds(center + 2, yStart + 68, bShortWidth, bHeight)
			.build());
		addRenderableWidget(Button.builder(CreateLang.translateDirect("menu.support"), $ -> linkTo(SUPPORT_LINK))
			.bounds(center - 100, yStart + 68, bShortWidth, bHeight)
			.build());
	}

	private void linkTo(Screen screen) {
		returnOnClose = false;
		if (screen instanceof NavigatableSimiScreen navigatableScreen) {
			navigatableScreen.transition.startWithValue(0.001)
				.chase(1, .3f, LerpedFloat.Chaser.EXP);
			ScreenOpener.open(this, navigatableScreen);
		} else
			ScreenOpener.open(screen);
	}

	private void linkTo(String url) {
		returnOnClose = false;
		ScreenOpener.open(new ConfirmLinkScreen((p_213069_2_) -> {
			if (p_213069_2_)
				Util.getPlatform()
					.openUri(url);
			this.minecraft.setScreenAndShow(this);
		}, url, true));
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	protected static class PlatformIconButton extends Button {
		protected final AllGuiTextures icon;
		protected final float scale;

		public PlatformIconButton(int pX, int pY, int pWidth, int pHeight, AllGuiTextures icon, float scale, OnPress pOnPress, Tooltip tooltip) {
			super(pX, pY, pWidth, pHeight, CommonComponents.EMPTY, pOnPress, DEFAULT_NARRATION);
			this.icon = icon;
			this.scale = scale;
			setTooltip(tooltip);
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			pose.translate(getX() + width / 2f - (icon.getWidth() * scale) / 2,
				getY() + height / 2f - (icon.getHeight() * scale) / 2);
			pose.scale(scale, scale);
			icon.render(graphics, 0, 0);
			pose.popMatrix();
		}
	}

}
