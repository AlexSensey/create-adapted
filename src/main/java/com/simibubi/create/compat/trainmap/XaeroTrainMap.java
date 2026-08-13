package com.simibubi.create.compat.trainmap;

import java.util.List;

import com.mojang.blaze3d.platform.Window;
import org.joml.Matrix3x2fStack;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.mixin.compat.xaeros.XaeroFullscreenMapAccessor;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.InputEvent;
import xaero.lib.client.gui.ScreenBase;
import xaero.map.gui.GuiMap;

public class XaeroTrainMap {
	private static boolean requesting;
	private static ResourceKey<Level> renderedDimension;
	private static boolean encounteredException;
	private static int framesSinceRender = 100;

	public static void tick() {
		framesSinceRender++;
		if (!AllConfigs.client().showTrainMapOverlay.get() || framesSinceRender > 2) {
			if (requesting)
				TrainMapSyncClient.stopRequesting();
			requesting = false;
			return;
		}
		TrainMapManager.tick();
		requesting = true;
		TrainMapSyncClient.requestData();
	}

	public static void mouseClick(InputEvent.MouseButton.Pre event) {
		if (encounteredException)
			return;

		Minecraft mc = Minecraft.getInstance();
		try {
			if (framesSinceRender > 2)
				return;
		} catch (Throwable e) {
			Create.LOGGER.error("Failed to handle mouse click for Xaero's World Map train map integration", e);
			encounteredException = true;
			return;
		}

		Window window = mc.getWindow();
		double mouseX = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
		double mouseY = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
		if (TrainMapManager.handleToggleWidgetClick(Mth.floor(mouseX), Mth.floor(mouseY), 3, 30))
			event.setCanceled(true);
	}

	public static void onRender(GuiGraphicsExtractor graphics, GuiMap screen, int mouseX, int mouseY,
		float partialTicks) {
		XaeroFullscreenMapAccessor accessor = (XaeroFullscreenMapAccessor) screen;
		framesSinceRender = 0;
		double cameraX = accessor.create$getCameraX();
		double cameraZ = accessor.create$getCameraZ();
		double mapScale = accessor.create$getScale();
		renderedDimension = accessor.create$getMapProcessor()
			.getMapWorld()
			.getCurrentDimension()
			.getDimId();

		if (!AllConfigs.client().showTrainMapOverlay.get()) {
			renderToggleWidgetAndTooltip(graphics, mouseX, mouseY);
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		Window window = mc.getWindow();
		double guiScale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
		double interfaceScale = (double) window.getWidth() / window.getScreenWidth();
		double scale = mapScale / guiScale / interfaceScale;

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(screen.width / 2.0f, screen.height / 2.0f);
		pose.scale((float) scale, (float) scale);
		pose.translate((float) -cameraX, (float) -cameraZ);

		float mapMouseX = (float) ((mouseX - screen.width / 2.0f) / scale + cameraX);
		float mapMouseY = (float) ((mouseY - screen.height / 2.0f) / scale + cameraZ);
		Rect2i bounds = new Rect2i(Mth.floor(-screen.width / 2.0f / scale + cameraX),
			Mth.floor(-screen.height / 2.0f / scale + cameraZ), Mth.floor(screen.width / scale),
			Mth.floor(screen.height / scale));

		List<FormattedText> tooltip = TrainMapManager.renderAndPick(graphics, Mth.floor(mapMouseX),
			Mth.floor(mapMouseY), false, bounds);
		pose.popMatrix();

		if (!renderToggleWidgetAndTooltip(graphics, mouseX, mouseY) && tooltip != null)
			graphics.setComponentTooltipForNextFrame(mc.font, tooltip.stream()
				.map(line -> line instanceof Component component ? component : Component.literal(line.getString()))
				.toList(), mouseX, mouseY);
	}

	private static boolean renderToggleWidgetAndTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		TrainMapManager.renderToggleWidget(graphics, 3, 30);
		if (!TrainMapManager.isToggleWidgetHovered(mouseX, mouseY, 3, 30))
			return false;
		graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font,
			List.of(CreateLang.translate("train_map.toggle").component()), mouseX, mouseY + 20);
		return true;
	}

	public static ResourceKey<Level> getRenderedDimension() {
		return renderedDimension;
	}

	public static boolean isMapOpen(Screen screen) {
		if (encounteredException)
			return false;
		try {
			return screen instanceof ScreenBase screenBase
				&& (screenBase instanceof GuiMap || screenBase.parent instanceof GuiMap);
		} catch (Throwable e) {
			Create.LOGGER.error("Failed to check whether Xaero's World Map is open", e);
			encounteredException = true;
			return false;
		}
	}
}
