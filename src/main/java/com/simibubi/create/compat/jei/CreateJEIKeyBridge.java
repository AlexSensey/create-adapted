package com.simibubi.create.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class CreateJEIKeyBridge {

	private static final String JEI_RECIPES_GUI = "mezz.jei.gui.recipes.RecipesGui";
	private static final int WINDOWS_SCAN_R = 19;
	private static final int WINDOWS_SCAN_U = 22;

	public static void register(IEventBus eventBus) {
		eventBus.addListener(CreateJEIKeyBridge::onKey);
	}

	@SubscribeEvent
	public static void onKey(ScreenEvent.KeyPressed.Pre event) {
		Screen screen = event.getScreen();
		if (CreateJEI.runtime == null || !isRecipesGui(screen))
			return;

		int keyCode = canonicalKey(event);
		if (keyCode == InputConstants.UNKNOWN.getValue())
			return;

		RecipeIngredientRole role = keyCode == InputConstants.KEY_R ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT;
		KeyEvent jeiKey = new KeyEvent(keyCode, event.getScanCode(), event.getModifiers());

		if (screen.keyPressed(jeiKey) || showFocused(role))
			event.setCanceled(true);
	}

	private static boolean showFocused(RecipeIngredientRole role) {
		var recipesGui = CreateJEI.runtime.getRecipesGui();
		var focusFactory = CreateJEI.runtime.getJeiHelpers()
			.getFocusFactory();

		var item = recipesGui.getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
		if (item.isPresent() && !item.get()
			.isEmpty()) {
			recipesGui.show(focusFactory.createFocus(role, VanillaTypes.ITEM_STACK, item.get()));
			return true;
		}

		var fluid = recipesGui.getIngredientUnderMouse(NeoForgeTypes.FLUID_STACK);
		if (fluid.isPresent() && !fluid.get()
			.isEmpty()) {
			recipesGui.show(focusFactory.createFocus(role, NeoForgeTypes.FLUID_STACK, fluid.get()));
			return true;
		}
		return false;
	}

	private static boolean matchesKey(ScreenEvent.KeyPressed.Pre event, int keyCode, int scanCode) {
		return event.getKeyCode() == keyCode || event.getScanCode() == scanCode;
	}

	private static int canonicalKey(ScreenEvent.KeyPressed.Pre event) {
		if (matchesKey(event, InputConstants.KEY_R, WINDOWS_SCAN_R))
			return InputConstants.KEY_R;
		if (matchesKey(event, InputConstants.KEY_U, WINDOWS_SCAN_U))
			return InputConstants.KEY_U;
		return InputConstants.UNKNOWN.getValue();
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
}
