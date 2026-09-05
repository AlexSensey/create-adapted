package com.simibubi.create.infrastructure.assets;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent.Opening;

@EventBusSubscriber(value = Dist.CLIENT, modid = "create")
public final class ExternalCreateAssetsWarningToast {
    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId(15_000L);
    private static boolean shown;

    private ExternalCreateAssetsWarningToast() {
    }

    @SubscribeEvent
    public static void onScreenOpening(Opening event) {
        if (shown || !(event.getNewScreen() instanceof TitleScreen)) return;
        if (!ExternalCreateAssets.isExternalEdition() || !ExternalCreateAssets.isMissing()) return;

        shown = true;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.toastManager().addToast(new SystemToast(
            TOAST_ID,
            title().withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
            message().withStyle(ChatFormatting.RED)
        )));
    }

    private static boolean isUkrainian() {
        return "uk_ua".equalsIgnoreCase(Minecraft.getInstance().getLanguageManager().getSelected());
    }

    private static MutableComponent title() {
        return Component.literal(isUkrainian()
            ? "Не знайдено ресурси Create"
            : "Create resources not found");
    }

    private static MutableComponent message() {
        return Component.literal(isUkrainian()
            ? "Додайте офіційний JAR 6.0.10 у config/create-adapted/external-assets"
            : "Add the official 6.0.10 JAR to config/create-adapted/external-assets");
    }
}
