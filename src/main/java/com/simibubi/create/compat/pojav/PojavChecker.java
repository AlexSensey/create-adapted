package com.simibubi.create.compat.pojav;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Mobile devices have low quality graphics drivers that cause visual issues.
 * This class checks if Pojav is present and shows a warning screen if so.
 */
public class PojavChecker {
	private static final Logger LOGGER = LoggerFactory.getLogger(PojavChecker.class);
	private static final Pattern KNOWN_ANDROID_PATH = Pattern.compile("/data/user/[0-9]+/net\\.kdt\\.pojavlaunch(?:/.*)?");

	public static final boolean IS_PRESENT = Util.make(() -> {
		if (System.getenv("POJAV_RENDERER") != null) {
			LOGGER.warn("[Create]: Detected POJAV_RENDERER, which indicates that Create is running on Android");
			return true;
		}

		String librarySearchPaths = System.getProperty("java.library.path");
		if (librarySearchPaths != null) {
			for (String path : librarySearchPaths.split(Pattern.quote(System.getProperty("path.separator")))) {
				if (isKnownAndroidPath(path)) {
					LOGGER.warn("[Create]: Found an Android library search path: {}", path);
					return true;
				}
			}
		}

		String workingDirectory = System.getProperty("user.home");
		if (workingDirectory != null && isKnownAndroidPath(workingDirectory)) {
			LOGGER.warn("[Create]: The working directory is hosted in an Android filesystem: {}", workingDirectory);
			return true;
		}

		return false;
	});

	private static boolean screenShown;

	private PojavChecker() {}

	public static void init() {
		if (IS_PRESENT)
			NeoForge.EVENT_BUS.addListener(PojavChecker::onScreenInit);
	}

	private static void onScreenInit(ScreenEvent.Init.Post event) {
		if (screenShown || !(event.getScreen() instanceof TitleScreen titleScreen))
			return;

		screenShown = true;
		Minecraft.getInstance()
			.setScreenAndShow(new PojavWarningScreen(titleScreen));
	}

	private static boolean isKnownAndroidPath(String path) {
		return KNOWN_ANDROID_PATH.matcher(path.replace('\\', '/'))
			.matches();
	}
}
