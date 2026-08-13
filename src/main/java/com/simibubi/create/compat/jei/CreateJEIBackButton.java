package com.simibubi.create.compat.jei;

import java.lang.reflect.Method;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class CreateJEIBackButton {

	private static final boolean ENABLED = false;
	private static final String JEI_RECIPES_GUI = "mezz.jei.gui.recipes.RecipesGui";
	private static final int SIZE = 18;
	private static final int PADDING = 6;
	private static Screen activeRecipesScreen;

	public static void register(IEventBus eventBus) {
		eventBus.addListener(CreateJEIBackButton::render);
		eventBus.addListener(CreateJEIBackButton::mouseClick);
	}

	@SubscribeEvent
	public static void render(ScreenEvent.Render.Post event) {
		if (!ENABLED)
			return;
		Screen screen = event.getScreen();
		if (!isRecipesGui(screen)) {
			activeRecipesScreen = null;
			return;
		}
		activeRecipesScreen = screen;

		int x = getButtonX(screen);
		int y = getButtonY(screen);
		GuiGraphicsExtractor graphics = event.getGuiGraphics();
		boolean hovered = isMouseOver(x, y, event.getMouseX(), event.getMouseY());
		int background = hovered ? 0xff6f6f6f : 0xff4a4a4a;
		int border = 0xff1f1f1f;
		int arrow = 0xffffffff;

		graphics.fill(x, y, x + SIZE, y + SIZE, border);
		graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, background);
		graphics.fill(x + 7, y + 8, x + 14, y + 10, arrow);
		graphics.fill(x + 5, y + 7, x + 8, y + 11, arrow);
		graphics.fill(x + 4, y + 6, x + 6, y + 12, arrow);
	}

	@SubscribeEvent
	public static void mouseClick(InputEvent.MouseButton.Pre event) {
		if (!ENABLED)
			return;
		if (event.getAction() != InputConstants.PRESS)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		Screen screen = activeRecipesScreen;
		if (!isRecipesGui(screen))
			return;

		double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow()
			.getGuiScaledWidth() / minecraft.getWindow()
				.getScreenWidth();
		double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow()
			.getGuiScaledHeight() / minecraft.getWindow()
				.getScreenHeight();

		if (!isMouseOver(getButtonX(screen), getButtonY(screen), mouseX, mouseY))
			return;

		if (call(screen, "back") != null)
			event.setCanceled(true);
	}

	private static boolean isRecipesGui(Screen screen) {
		if (screen == null)
			return false;
		Class<?> type = screen.getClass();
		while (type != null) {
			if (JEI_RECIPES_GUI.equals(type.getName()))
				return true;
			type = type.getSuperclass();
		}
		return screen.getClass()
			.getName()
			.contains("RecipesGui");
	}

	private static int getButtonX(Screen screen) {
		Integer areaX = getAreaCoordinate(screen, "getX");
		return areaX != null ? areaX + PADDING : screen.width / 2 - 96;
	}

	private static int getButtonY(Screen screen) {
		Integer areaY = getAreaCoordinate(screen, "getY");
		return areaY != null ? areaY + PADDING : screen.height / 2 - 86;
	}

	private static Integer getAreaCoordinate(Screen screen, String methodName) {
		Object area = call(screen, "getArea");
		if (area == null)
			return null;
		Object result = call(area, methodName);
		return result instanceof Integer value ? value : null;
	}

	private static Object call(Object target, String methodName) {
		try {
			Method method = target.getClass()
				.getMethod(methodName);
			return method.invoke(target);
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}

	private static boolean isMouseOver(int x, int y, double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
	}
}
