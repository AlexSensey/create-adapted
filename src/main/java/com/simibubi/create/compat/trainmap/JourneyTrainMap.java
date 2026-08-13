package com.simibubi.create.compat.trainmap;

import java.util.List;

import org.joml.Matrix3x2fStack;

import com.mojang.blaze3d.platform.Window;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.event.FullscreenRenderEvent;
import journeymap.api.v2.client.fullscreen.IFullscreen;
import journeymap.api.v2.client.util.UIState;
import journeymap.api.v2.common.Context.UI;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.FullscreenEventRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre;

@JourneyMapPlugin(apiVersion = "2.0.0")
public class JourneyTrainMap implements IClientPlugin {
	private static boolean requesting;
	private static int framesSinceRender = 100;
	private static ResourceKey<Level> renderedDimension;

	@Override
	public void initialize(IClientAPI clientApi) {
		FullscreenEventRegistry.FULLSCREEN_RENDER_EVENT.subscribe(Create.ID, JourneyTrainMap::onRender);
	}

	@Override
	public String getModId() {
		return Create.ID;
	}

	public static void tick() {
		framesSinceRender++;
		if (!AllConfigs.client().showTrainMapOverlay.get() || framesSinceRender > 2 || renderedDimension == null) {
			if (requesting)
				TrainMapSyncClient.stopRequesting();
			requesting = false;
			return;
		}
		TrainMapManager.tick(renderedDimension);
		requesting = true;
		TrainMapSyncClient.requestData();
	}

	public static void mouseClick(Pre event) {
		if (framesSinceRender > 2)
			return;
		Minecraft mc = Minecraft.getInstance();
		Window window = mc.getWindow();
		double mouseX = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
		double mouseY = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
		if (TrainMapManager.handleToggleWidgetClick(Mth.floor(mouseX), Mth.floor(mouseY), 3, 30))
			event.setCanceled(true);
	}

	public static void onRender(FullscreenRenderEvent event) {
		GuiGraphicsExtractor graphics = event.getGraphics();
		IFullscreen fullscreen = event.getFullscreen();
		Screen screen = fullscreen.getScreen();
		UIState state = fullscreen.getUiState();
		if (state == null || state.ui != UI.Fullscreen || !state.active)
			return;

		framesSinceRender = 0;
		renderedDimension = state.dimension;
		int mouseX = event.getMouseX();
		int mouseY = event.getMouseY();
		if (!AllConfigs.client().showTrainMapOverlay.get()) {
			renderToggleWidgetAndTooltip(graphics, mouseX, mouseY);
			return;
		}

		double centerX = fullscreen.getCenterBlockX(true);
		double centerZ = fullscreen.getCenterBlockZ(true);
		Window window = Minecraft.getInstance().getWindow();
		double guiScale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
		double scale = state.blockSize / guiScale;

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(screen.width / 2.0f, screen.height / 2.0f);
		pose.scale((float) scale, (float) scale);
		pose.translate((float) -centerX, (float) -centerZ);

		float mapMouseX = (float) ((mouseX - screen.width / 2.0f) / scale + centerX);
		float mapMouseY = (float) ((mouseY - screen.height / 2.0f) / scale + centerZ);
		Rect2i bounds = new Rect2i(Mth.floor(-screen.width / 2.0f / scale + centerX),
			Mth.floor(-screen.height / 2.0f / scale + centerZ), Mth.floor(screen.width / scale),
			Mth.floor(screen.height / scale));
		List<FormattedText> tooltip = TrainMapManager.renderAndPick(graphics, Mth.floor(mapMouseX),
			Mth.floor(mapMouseY), false, bounds);
		pose.popMatrix();

		if (!renderToggleWidgetAndTooltip(graphics, mouseX, mouseY) && tooltip != null) {
			Minecraft mc = Minecraft.getInstance();
			graphics.setComponentTooltipForNextFrame(mc.font, tooltip.stream()
				.map(line -> line instanceof Component component ? component : Component.literal(line.getString()))
				.toList(), mouseX, mouseY);
		}
	}

	private static boolean renderToggleWidgetAndTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		TrainMapManager.renderToggleWidget(graphics, 3, 30);
		if (!TrainMapManager.isToggleWidgetHovered(mouseX, mouseY, 3, 30))
			return false;
		graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font,
			List.of(CreateLang.translate("train_map.toggle").component()), mouseX, mouseY + 20);
		return true;
	}
}
