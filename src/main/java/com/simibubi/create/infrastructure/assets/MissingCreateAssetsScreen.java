package com.simibubi.create.infrastructure.assets;

import com.simibubi.create.Create;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = Create.ID)
public final class MissingCreateAssetsScreen {
	private MissingCreateAssetsScreen() {
	}

	@SubscribeEvent
	public static void onScreenOpening(ScreenEvent.Opening event) {
		if (!(event.getNewScreen() instanceof TitleScreen) || !ExternalCreateAssets.isMissing())
			return;

		boolean ukrainian = "uk_ua".equalsIgnoreCase(Minecraft.getInstance()
			.getLanguageManager()
			.getSelected());
		Component title = Component.literal(ukrainian
			? "Потрібні оригінальні ресурси Create"
			: "Official Create resources required");
		Component message = Component.literal(ukrainian
			? ukrainianMessage()
			: englishMessage());
		Component downloadButton = Component.literal(ukrainian ? "Відкрити CurseForge" : "Open CurseForge");
		Component folderButton = Component.literal(ukrainian ? "Відкрити папку" : "Open folder");

		event.setNewScreen(new ConfirmScreen(openDownload -> {
			if (openDownload)
				Util.getPlatform().openUri(ExternalCreateAssets.DOWNLOAD_URL);
			else
				Util.getPlatform().openPath(ExternalCreateAssets.directory());
		}, title, message, downloadButton, folderButton));
	}

	private static String ukrainianMessage() {
		return "Офіційний Create " + ExternalCreateAssets.OFFICIAL_CREATE_VERSION + " для Minecraft "
			+ ExternalCreateAssets.OFFICIAL_MINECRAFT_VERSION + " не знайдено.\n\n"
			+ "Завантажте його з офіційної сторінки CurseForge і покладіть JAR сюди:\n"
			+ ExternalCreateAssets.directory() + "\n\n"
			+ "Папка створюється автоматично. Не кладіть оригінальний JAR у mods. "
			+ "Після копіювання перезапустіть гру.\n\n"
			+ "Ця адаптація не поширює оригінальні текстури, моделі, звуки та інші ресурси Create, "
			+ "оскільки Create Team ліцензує їх як All Rights Reserved.\n\n"
			+ "Minecraft: " + ExternalCreateAssets.TARGET_MINECRAFT_VERSION
			+ " | Перевірений NeoForge: " + ExternalCreateAssets.TESTED_NEOFORGE_VERSION;
	}

	private static String englishMessage() {
		return "Official Create " + ExternalCreateAssets.OFFICIAL_CREATE_VERSION + " for Minecraft "
			+ ExternalCreateAssets.OFFICIAL_MINECRAFT_VERSION + " was not found.\n\n"
			+ "Download it from the official CurseForge page and place the JAR here:\n"
			+ ExternalCreateAssets.directory() + "\n\n"
			+ "The folder is created automatically. Do not put the original JAR in mods. "
			+ "Restart the game after copying it.\n\n"
			+ "This adaptation does not redistribute Create's original textures, models, sounds, or other assets "
			+ "because the Create Team licenses them as All Rights Reserved.\n\n"
			+ "Minecraft: " + ExternalCreateAssets.TARGET_MINECRAFT_VERSION
			+ " | Tested NeoForge: " + ExternalCreateAssets.TESTED_NEOFORGE_VERSION;
	}
}
