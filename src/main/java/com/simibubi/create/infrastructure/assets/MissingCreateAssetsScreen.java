package com.simibubi.create.infrastructure.assets;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ScreenEvent.Opening;

@EventBusSubscriber(value = Dist.CLIENT, modid = "create")
public final class MissingCreateAssetsScreen extends Screen {
   private static final Path PROVIDERS_DIRECTORY = FMLPaths.CONFIGDIR.get()
      .resolve("adapted-external-assets/providers").toAbsolutePath().normalize();
   private static final int ENTRY_HEIGHT = 54;

   private final List<Requirement> missing;
   private final boolean ukrainian;

   private MissingCreateAssetsScreen(Screen parent, List<Requirement> missing) {
      super(Component.literal(isUkrainian() ? "Потрібні оригінальні ресурси" : "Official resources required"));
      this.missing = missing;
      this.ukrainian = isUkrainian();
   }

   @SubscribeEvent
   public static void onScreenOpening(Opening event) {
      if (!(event.getNewScreen() instanceof TitleScreen titleScreen)) return;
      List<Requirement> missing = findMissingRequirements();
      if (!missing.isEmpty()) event.setNewScreen(new MissingCreateAssetsScreen(titleScreen, missing));
   }

   private static boolean isUkrainian() {
      return "uk_ua".equalsIgnoreCase(Minecraft.getInstance().getLanguageManager().getSelected());
   }

   @Override
   protected void init() {
      addRenderableWidget(Button.builder(Component.literal(ukrainian ? "Вийти з гри" : "Quit game"), button -> minecraft.stop())
         .bounds(width / 2 - 75, Math.min(height - 28, contentTop() + missing.size() * ENTRY_HEIGHT + 20), 150, 20)
         .build());
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fillGradient(0, 0, width, height, 0xE0101010, 0xF0202020);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      graphics.centeredText(font, title, width / 2, 22, 0xFFFFAA00);
      graphics.centeredText(font,
         ukrainian ? "Не знайдено оригінальні JAR з ресурсами:" : "The following original resource JARs were not found:",
         width / 2, 42, 0xFFFFFFFF);

      int left = Math.max(18, width / 2 - 260);
      for (int i = 0; i < missing.size(); i++) {
         Requirement requirement = missing.get(i);
         int y = contentTop() + i * ENTRY_HEIGHT;
         graphics.text(font, Component.literal("• " + requirement.requiredArtifact()).withStyle(ChatFormatting.BOLD),
            left, y, 0xFFFFFFFF, false);
         drawLink(graphics, downloadText(), left + 12, y + 15, mouseX, mouseY);
         drawLink(graphics, folderText(), left + 12, y + 29, mouseX, mouseY);
      }

      graphics.centeredText(font,
         ukrainian ? "Оригінальні JAR не потрібно класти у mods. Після додавання перезапусти гру."
            : "Do not put original JARs in mods. Restart the game after adding them.",
         width / 2, contentTop() + missing.size() * ENTRY_HEIGHT, 0xFFAAAAAA);
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
   }

   private void drawLink(GuiGraphicsExtractor graphics, Component text, int x, int y, int mouseX, int mouseY) {
      graphics.text(font, text, x, y, isOver(text, x, y, mouseX, mouseY) ? 0xFFFFFF55 : 0xFF55AAFF, false);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() == 0) {
         int left = Math.max(18, width / 2 - 260) + 12;
         for (int i = 0; i < missing.size(); i++) {
            Requirement requirement = missing.get(i);
            int y = contentTop() + i * ENTRY_HEIGHT;
            if (isOver(downloadText(), left, y + 15, event.x(), event.y())) {
               Util.getPlatform().openUri(requirement.downloadUrl());
               return true;
            }
            if (isOver(folderText(), left, y + 29, event.x(), event.y())) {
               openDirectory(requirement.directory());
               return true;
            }
         }
      }
      return super.mouseClicked(event, doubleClick);
   }

   private int contentTop() {
      return 62;
   }

   private Component downloadText() {
      return Component.literal(ukrainian ? "Завантажити офіційний файл" : "Download the official file")
         .withStyle(ChatFormatting.UNDERLINE);
   }

   private Component folderText() {
      return Component.literal(ukrainian ? "Відкрити потрібну папку config" : "Open the required config folder")
         .withStyle(ChatFormatting.UNDERLINE);
   }

   private boolean isOver(Component text, int x, int y, double mouseX, double mouseY) {
      return mouseX >= x && mouseX < x + font.width(text) && mouseY >= y - 1 && mouseY < y + 10;
   }

   @Override
   public void onClose() {
      minecraft.stop();
   }

   private static List<Requirement> findMissingRequirements() {
      if (!Files.isDirectory(PROVIDERS_DIRECTORY)) return List.of();
      List<Requirement> requirements = new ArrayList<>();
      try (Stream<Path> files = Files.list(PROVIDERS_DIRECTORY)) {
         for (Path file : files.filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".properties"))
            .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList()) {
            Requirement requirement = readRequirement(file);
            if (requirement != null) requirements.add(requirement);
         }
      } catch (IOException ignored) {
      }
      return requirements;
   }

   private static Requirement readRequirement(Path file) {
      Properties properties = new Properties();
      try (InputStream input = Files.newInputStream(file)) {
         properties.load(input);
         Class<?> provider = Class.forName(properties.getProperty("provider_class"));
         Method isExternalEdition = provider.getMethod("isExternalEdition");
         Method isMissing = provider.getMethod("isMissing");
         if (!Boolean.TRUE.equals(isExternalEdition.invoke(null)) || !Boolean.TRUE.equals(isMissing.invoke(null))) return null;
         Path directory = (Path) provider.getMethod("directory").invoke(null);
         return new Requirement(
            properties.getProperty("required_artifact", properties.getProperty("display_name", provider.getSimpleName())),
            properties.getProperty("download_url"), directory.toAbsolutePath().normalize());
      } catch (ReflectiveOperationException | IOException | LinkageError ignored) {
         return null;
      }
   }

   private static void openDirectory(Path directory) {
      try {
         Files.createDirectories(directory);
         if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            new ProcessBuilder("explorer.exe", directory.toString()).start();
            return;
         }
      } catch (IOException ignored) {
      }
      Util.getPlatform().openPath(directory);
   }

   private record Requirement(String requiredArtifact, String downloadUrl, Path directory) {
   }
}
