package com.simibubi.create.infrastructure.assets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.CreatePaths;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.FilePackResources.FileResourcesSupplier;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.Pack.Metadata;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExternalCreateAssets {
   public static final String CONFIG_FILE_NAME = "create-adapted-external-assets.toml";
   public static final String CONFIG_PATH_KEY = "official_create_path";
   public static final String DEFAULT_DIRECTORY_NAME = "create-adapted/external-assets";
   /** @deprecated Kept as a lookup fallback for existing installations. */
   @Deprecated
   public static final String DIRECTORY_NAME = "create_original";
   public static final String OFFICIAL_CREATE_VERSION = "6.0.10";
   public static final String OFFICIAL_MINECRAFT_VERSION = "1.21.1";
   public static final String TARGET_MINECRAFT_VERSION = "26.2";
   public static final String TESTED_NEOFORGE_VERSION = "26.2.0.59";
   public static final String DOWNLOAD_URL = "https://www.curseforge.com/minecraft/mc-mods/create/files/all?page=1&pageSize=20&version=1.21.1";
   private static final List<String> REQUIRED_ENTRIES = List.of(
      "assets/create/lang/en_us.json", "assets/create/textures/block/andesite_casing.png", "assets/create/models/block/cogwheel.json"
   );
   private static final Map<String, String> RESOURCE_ALIASES = Map.ofEntries(
      Map.entry("textures/block/goggles_model.png", "textures/item/goggles_model.png"),
      Map.entry("textures/block/package/cardboard.png", "textures/item/package/cardboard.png"),
      Map.entry("textures/block/package/cardboard_particle.png", "textures/item/package/cardboard_particle.png"),
      Map.entry("textures/block/package/rare_creeper.png", "textures/item/package/rare_creeper.png"),
      Map.entry("textures/block/package/rare_darcy.png", "textures/item/package/rare_darcy.png"),
      Map.entry("textures/block/package/rare_evan.png", "textures/item/package/rare_evan.png"),
      Map.entry("textures/block/package/rare_jinx.png", "textures/item/package/rare_jinx.png"),
      Map.entry("textures/block/package/rare_kryppers.png", "textures/item/package/rare_kryppers.png"),
      Map.entry("textures/block/package/rare_simi.png", "textures/item/package/rare_simi.png"),
      Map.entry("textures/block/package/rare_starlotte.png", "textures/item/package/rare_starlotte.png"),
      Map.entry("textures/block/package/rare_thunder.png", "textures/item/package/rare_thunder.png"),
      Map.entry("textures/block/package/rare_up.png", "textures/item/package/rare_up.png"),
      Map.entry("textures/block/package/rare_vector.png", "textures/item/package/rare_vector.png"),
      Map.entry("textures/item/mechanical_press_pole.png", "textures/block/mechanical_press_pole.png"),
      Map.entry("textures/item/redstone_antenna.png", "textures/block/redstone_antenna.png"),
      Map.entry("textures/item/redstone_antenna_powered.png", "textures/block/redstone_antenna_powered.png"),
      Map.entry("textures/entity/equipment/humanoid/cardboard.png", "textures/models/armor/cardboard_layer_1.png"),
      Map.entry("textures/entity/equipment/humanoid/copper_diving.png", "textures/models/armor/copper_diving_layer_1.png"),
      Map.entry("textures/entity/equipment/humanoid/netherite_diving_layer_1.png", "textures/models/armor/netherite_diving_layer_1.png"),
      Map.entry("textures/entity/equipment/humanoid/netherite_diving_layer_2.png", "textures/models/armor/netherite_diving_layer_2.png"),
      Map.entry("textures/entity/equipment/humanoid_baby/cardboard.png", "textures/models/armor/cardboard_layer_1.png"),
      Map.entry("textures/entity/equipment/humanoid_baby/copper_diving.png", "textures/models/armor/copper_diving_layer_1.png"),
      Map.entry("textures/entity/equipment/humanoid_baby/netherite_diving_layer_1.png", "textures/models/armor/netherite_diving_layer_1.png"),
      Map.entry("textures/entity/equipment/humanoid_baby/netherite_diving_layer_2.png", "textures/models/armor/netherite_diving_layer_2.png"),
      Map.entry("textures/entity/equipment/humanoid_leggings/cardboard_leggings.png", "textures/models/armor/cardboard_layer_2.png")
   );
   private static final Map<String, String> ITEM_TEXTURE_ALIASES = Map.of(
      "create:block/mechanical_press_pole",
      "create:item/mechanical_press_pole",
      "create:block/redstone_antenna",
      "create:item/redstone_antenna",
      "create:block/redstone_antenna_powered",
      "create:item/redstone_antenna_powered"
   );
   private static final Map<String, String> MODEL_TEXTURE_ALIASES = Map.of(
      "create:item/goggles_model", "create:block/goggles_model"
   );
   private static final String ITEM_PACKAGE_TEXTURE_PREFIX = "textures/item/package/";
   private static final String BLOCK_PACKAGE_TEXTURE_PREFIX = "textures/block/package/";
   private static final String ITEM_PACKAGE_TEXTURE_ID_PREFIX = "create:item/package/";
   private static final String BLOCK_PACKAGE_TEXTURE_ID_PREFIX = "create:block/package/";
   private static final Map<String, String> GENERATED_JSON = Map.ofEntries(
      Map.entry(
         "models/block/empty.json",
         "{\"parent\":\"minecraft:block/block\",\"textures\":{\"particle\":\"create:block/valve_handle/valve_handle_copper\"},\"elements\":[]}"
      ),
      Map.entry("models/block/turntable/top.json", "{\"parent\":\"create:block/turntable\"}"),
      Map.entry("models/item/extendo_grip/hand.json", "{\"parent\":\"create:block/deployer/hand_punching\"}"),
      Map.entry("models/item/extendo_grip/hand_holding.json", "{\"parent\":\"create:block/deployer/hand_holding\"}"),
      Map.entry(
         "models/item/cardboard_helmet_resin_trim.json",
         "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"create:item/cardboard_helmet\",\"layer1\":\"create:trims/items/card_helmet_trim_resin\"}}"
      ),
      Map.entry(
         "models/item/cardboard_chestplate_resin_trim.json",
         "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"create:item/cardboard_chestplate\",\"layer1\":\"create:trims/items/card_chestplate_trim_resin\"}}"
      ),
      Map.entry(
         "models/item/cardboard_leggings_resin_trim.json",
         "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"create:item/cardboard_leggings\",\"layer1\":\"create:trims/items/card_leggings_trim_resin\"}}"
      ),
      Map.entry(
         "models/item/cardboard_boots_resin_trim.json",
         "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"create:item/cardboard_boots\",\"layer1\":\"create:trims/items/card_boots_trim_resin\"}}"
      ),
      Map.entry(
         "models/item/symmetry/plane_preview_0.json",
         "{\"parent\":\"create:block/symmetry_effect/plane\",\"display\":{\"gui\":{\"rotation\":[30,225,0],\"scale\":[0.8,0.8,0.8]}}}"
      ),
      Map.entry(
         "models/item/symmetry/plane_preview_1.json",
         "{\"parent\":\"create:block/symmetry_effect/plane\",\"display\":{\"gui\":{\"rotation\":[30,135,0],\"scale\":[0.8,0.8,0.8]}}}"
      ),
      Map.entry(
         "models/item/symmetry/crossplane_preview_0.json",
         "{\"parent\":\"create:block/symmetry_effect/crossplane\",\"display\":{\"gui\":{\"rotation\":[30,225,0],\"scale\":[0.8,0.8,0.8]}}}"
      ),
      Map.entry(
         "models/item/symmetry/crossplane_preview_1.json",
         "{\"parent\":\"create:block/symmetry_effect/crossplane\",\"display\":{\"gui\":{\"rotation\":[30,135,0],\"scale\":[0.8,0.8,0.8]}}}"
      ),
      Map.entry(
         "models/item/symmetry/tripleplane_preview_0.json",
         "{\"parent\":\"create:block/symmetry_effect/tripleplane\",\"display\":{\"gui\":{\"rotation\":[30,225,0],\"scale\":[0.8,0.8,0.8]}}}"
      )
   );
   private static boolean checked;
   private static Path configFile;
   private static Path directory;
   private static Path originalJar;
   private static String failure;

   private ExternalCreateAssets() {
   }

   public static boolean isExternalEdition() {
      return ExternalCreateAssets.class.getResource("/EXTERNAL_ASSETS.md") != null;
   }

    /**
     * External assets are installed as a built-in pack before model baking, so
     * both public and full editions can use the native Flywheel visual path.
     */
    public static boolean shouldUseFlywheelVisuals() {
        return true;
    }

   public static void initialize() {
      if (!checked) {
         checked = true;
         Path gameDirectory = CreatePaths.GAME_DIR.toAbsolutePath().normalize();
         Path defaultDirectory = CreatePaths.CONFIG_DIR.resolve(DEFAULT_DIRECTORY_NAME).toAbsolutePath().normalize();
         Path legacyDirectory = gameDirectory.resolve(DIRECTORY_NAME).normalize();
         configFile = CreatePaths.CONFIG_DIR.resolve(CONFIG_FILE_NAME).toAbsolutePath().normalize();
         directory = defaultDirectory;

         try {
            Files.createDirectories(defaultDirectory);
            writeProviderDescriptor();
            Path configuredLocation = readOrCreateConfiguredLocation(gameDirectory);
            if (configuredLocation != null) {
               directory = directoryFor(configuredLocation, defaultDirectory);
            }

            LinkedHashSet<Path> searchLocations = new LinkedHashSet<>();
            if (configuredLocation != null) {
               searchLocations.add(configuredLocation);
            }
            searchLocations.add(defaultDirectory);
            searchLocations.add(legacyDirectory);

            for (Path location : searchLocations) {
               originalJar = findOriginalJar(location);
               if (originalJar != null) {
                  directory = originalJar.getParent();
                  break;
               }
            }

            if (originalJar == null) {
               failure = "Official Create 6.0.10 for Minecraft 1.21.1 was not found.";
               Create.LOGGER.error("{} Checked locations: {}", failure, searchLocations);
            } else {
               Create.LOGGER.info("Using player-supplied official Create assets from {}", originalJar);
            }
         } catch (IOException e) {
            failure = "Could not inspect " + directory + ": " + e.getMessage();
            Create.LOGGER.error(failure, e);
         }
      }
   }

   private static void writeProviderDescriptor() throws IOException {
      Path registryDirectory = CreatePaths.CONFIG_DIR.resolve("adapted-external-assets/providers");
      Files.createDirectories(registryDirectory);
      Properties properties = new Properties();
      properties.setProperty("provider_class", ExternalCreateAssets.class.getName());
      properties.setProperty("display_name", "Create");
      properties.setProperty("required_artifact", "Official Create " + OFFICIAL_CREATE_VERSION
         + " for Minecraft " + OFFICIAL_MINECRAFT_VERSION);
      properties.setProperty("download_url", DOWNLOAD_URL);
      try (var output = Files.newOutputStream(registryDirectory.resolve("create.properties"))) {
         properties.store(output, "Registered external assets provider");
      }
   }

   @Nullable
   private static Path readOrCreateConfiguredLocation(Path gameDirectory) throws IOException {
      if (Files.notExists(configFile)) {
         Files.createDirectories(configFile.getParent());
         Files.writeString(configFile, defaultConfigContents(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
      }

      for (String sourceLine : Files.readAllLines(configFile, StandardCharsets.UTF_8)) {
         String line = sourceLine.strip();
         if (line.isEmpty() || line.startsWith("#")) {
            continue;
         }
         int equals = line.indexOf('=');
         if (equals < 0 || !line.substring(0, equals).strip().equals(CONFIG_PATH_KEY)) {
            continue;
         }

         String configured = readTomlString(line.substring(equals + 1).strip());
         if (configured == null || configured.isBlank()) {
            return null;
         }
         try {
            Path path = Path.of(configured);
            return (path.isAbsolute() ? path : gameDirectory.resolve(path)).toAbsolutePath().normalize();
         } catch (InvalidPathException e) {
            Create.LOGGER.warn("Ignoring invalid {} in {}: {}", CONFIG_PATH_KEY, configFile, configured);
            return null;
         }
      }
      return null;
   }

   private static String defaultConfigContents() {
      return "# Official Create 6.0.10 for Minecraft 1.21.1 used as the external resource source.\n"
         + "# Set this to a JAR file or a directory containing it.\n"
         + "# Relative paths are resolved from the game directory. Use an absolute path to share one JAR between instances.\n"
         + "# Forward slashes are recommended on Windows, for example: C:/Minecraft/shared/create-1.21.1-6.0.10.jar\n"
         + CONFIG_PATH_KEY + " = \"\"\n";
   }

   @Nullable
   private static String readTomlString(String value) {
      int comment = value.indexOf('#');
      if (comment >= 0) {
         value = value.substring(0, comment).stripTrailing();
      }
      if (value.length() < 2) {
         return value;
      }
      char quote = value.charAt(0);
      if ((quote == '\'' || quote == '"') && value.charAt(value.length() - 1) == quote) {
         String unquoted = value.substring(1, value.length() - 1);
         if (quote == '"') {
            unquoted = unquoted.replace("\\\\", "\\").replace("\\\"", "\"");
         }
         return unquoted;
      }
      return value;
   }

   private static Path directoryFor(Path location, Path fallback) {
      if (Files.isDirectory(location)) {
         return location;
      }
      Path parent = location.getParent();
      return parent == null ? fallback : parent;
   }

   @Nullable
   private static Path findOriginalJar(Path location) throws IOException {
      if (Files.isRegularFile(location)) {
         return isOfficialAssetsJar(location) ? location : null;
      }
      if (!Files.isDirectory(location)) {
         return null;
      }
      try (Stream<Path> files = Files.list(location)) {
         for (Path candidate : files.filter(x$0 -> Files.isRegularFile(x$0))
            .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".jar"))
            .filter(path -> path.getFileName().toString().contains("6.0.10"))
            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
            .toList()) {
            if (isOfficialAssetsJar(candidate)) {
               return candidate;
            }
         }
      }

      return null;
   }

   private static boolean isOfficialAssetsJar(Path candidate) {
      if (!candidate.getFileName().toString().toLowerCase().endsWith(".jar")) {
         return false;
      }
      try (ZipFile zip = new ZipFile(candidate.toFile())) {
         return REQUIRED_ENTRIES.stream().allMatch(entry -> zip.getEntry(entry) != null);
      } catch (IOException e) {
         Create.LOGGER.warn("Ignoring unreadable Create asset jar {}", candidate, e);
         return false;
      }
   }

   public static void addPackFinders(AddPackFindersEvent event) {
      initialize();
      if (event.getPackType() == PackType.CLIENT_RESOURCES && originalJar != null) {
         final Path jar = originalJar;
         event.addRepositorySource(
            new RepositorySource() {
               public void loadPacks(@NotNull Consumer<Pack> output) {
                  PackLocationInfo location = new PackLocationInfo(
                     "create_official_external_assets", Component.literal("Official Create 6.0.10 assets"), PackSource.BUILT_IN, Optional.empty()
                  );
                  final FileResourcesSupplier delegate = new FileResourcesSupplier(jar);
                  ResourcesSupplier supplier = new ResourcesSupplier() {
                     public PackResources openPrimary(PackLocationInfo info) {
                        return new ExternalCreateAssets.AdaptedResources(delegate.openPrimary(info));
                     }

                     public PackResources openFull(PackLocationInfo info, Metadata metadata) {
                        return new ExternalCreateAssets.AdaptedResources(delegate.openFull(info, metadata));
                     }
                  };
                  Pack pack = Pack.readMetaAndCreate(location, supplier, PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Position.BOTTOM, false));
                  if (pack != null) {
                     output.accept(pack);
                  }
               }
            }
         );
      }
   }

   public static boolean isMissing() {
      if (!isExternalEdition()) {
         return false;
      }
      initialize();
      return originalJar == null;
   }

   public static Path directory() {
      initialize();
      return directory;
   }

   public static Path configFile() {
      initialize();
      return configFile;
   }

   public static String failure() {
      initialize();
      return failure == null ? "Official Create assets are unavailable." : failure;
   }

   private static final class AdaptedResources implements PackResources {
      private final PackResources delegate;

      private AdaptedResources(PackResources delegate) {
         this.delegate = delegate;
      }

      @Nullable
      public IoSupplier<InputStream> getRootResource(String @NotNull ... elements) {
         return this.delegate.getRootResource(elements);
      }

      @Nullable
      public IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull Identifier location) {
         IoSupplier<InputStream> source = this.delegate.getResource(type, location);
         if (type == PackType.CLIENT_RESOURCES) {
            if (!location.getNamespace().equals("create")) {
               return adaptSupplier(location, source);
            }
            if (source == null) {
               String original = ExternalCreateAssets.RESOURCE_ALIASES.get(location.getPath());
               if (original != null) {
                  source = this.delegate.getResource(type, Identifier.fromNamespaceAndPath("create", original));
               }
            }

            if (source == null && location.getPath().startsWith(BLOCK_PACKAGE_TEXTURE_PREFIX)) {
               String original = ITEM_PACKAGE_TEXTURE_PREFIX + location.getPath().substring(BLOCK_PACKAGE_TEXTURE_PREFIX.length());
               source = this.delegate.getResource(type, Identifier.fromNamespaceAndPath("create", original));
            }

            if (source == null) {
               String generated = ExternalCreateAssets.GENERATED_JSON.get(location.getPath());
               if (generated != null) {
                  source = () -> new ByteArrayInputStream(generated.getBytes(StandardCharsets.UTF_8));
               }
            }

            return adaptSupplier(location, source);
         } else {
            return source;
         }
      }

      public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path, @NotNull ResourceOutput output) {
         this.delegate.listResources(type, namespace, path, (location, source) -> output.accept(location, adaptSupplier(location, source)));
         if (type == PackType.CLIENT_RESOURCES && namespace.equals("create")) {
            ExternalCreateAssets.RESOURCE_ALIASES.forEach((alias, original) -> {
               if (isUnder(alias, path)) {
                  Identifier originalLocation = Identifier.fromNamespaceAndPath("create", original);
                  IoSupplier<InputStream> source = this.delegate.getResource(type, originalLocation);
                  if (source != null) {
                     output.accept(Identifier.fromNamespaceAndPath("create", alias), source);
                  }
               }
            });
            this.delegate.listResources(type, namespace, ITEM_PACKAGE_TEXTURE_PREFIX, (location, source) -> {
               String original = location.getPath();
               if (original.startsWith(ITEM_PACKAGE_TEXTURE_PREFIX)) {
                  String alias = BLOCK_PACKAGE_TEXTURE_PREFIX + original.substring(ITEM_PACKAGE_TEXTURE_PREFIX.length());
                  if (isUnder(alias, path)) {
                     output.accept(Identifier.fromNamespaceAndPath("create", alias), source);
                  }
               }
            });
            ExternalCreateAssets.GENERATED_JSON
               .forEach(
                  (resource, json) -> {
                     if (isUnder(resource, path)) {
                        output.accept(
                           Identifier.fromNamespaceAndPath("create", resource),
                           (IoSupplier)() -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
                        );
                     }
                  }
               );
         }
      }

      private static boolean isUnder(String resource, String path) {
         return path.isEmpty() || resource.equals(path) || resource.startsWith(path.endsWith("/") ? path : path + "/");
      }

      @Nullable
      private static IoSupplier<InputStream> adaptSupplier(Identifier location, @Nullable IoSupplier<InputStream> source) {
         if (source == null) {
            return null;
         } else if (location.getNamespace().equals("minecraft") && location.getPath().equals("atlases/blocks.json")) {
            IoSupplier<InputStream> original = source;
            return () -> {
               try (InputStream stream = original.get()) {
                  JsonObject atlas = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                  JsonArray sources = atlas.getAsJsonArray("sources");
                  if (sources != null) {
                     for (int i = sources.size() - 1; i >= 0; i--) {
                        if (isLegacyCardboardItemTrimSource(sources.get(i))) {
                           sources.remove(i);
                        }
                     }
                  }
                  return new ByteArrayInputStream(Create.GSON.toJson(atlas).getBytes(StandardCharsets.UTF_8));
               }
            };
         } else if (!location.getNamespace().equals("create")) {
            return source;
         } else if (location.getPath().startsWith("lang/") && location.getPath().endsWith(".json")) {
            IoSupplier<InputStream> original = source;
            return () -> {
               try (InputStream stream = (InputStream)original.get()) {
                  JsonObject language = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                  Map<String, JsonElement> additions = new HashMap<>();
                  language.entrySet().forEach(entry -> {
                     if (((String)entry.getKey()).startsWith("block.create.")) {
                        String itemKey = "item.create." + ((String)entry.getKey()).substring("block.create.".length());
                        if (!language.has(itemKey)) {
                           additions.put(itemKey, ((JsonElement)entry.getValue()).deepCopy());
                        }
                     }
                  });
                  additions.forEach(language::add);
                  return new ByteArrayInputStream(Create.GSON.toJson(language).getBytes(StandardCharsets.UTF_8));
               }
            };
         } else if (isValveHandleBlockState(location.getPath())) {
            IoSupplier<InputStream> original = source;
            return () -> {
               try (InputStream stream = (InputStream)original.get()) {
                  JsonElement json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                  replaceModelReferences(json, "create:block/empty");
                  return new ByteArrayInputStream(Create.GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
               }
            };
         } else if (location.getPath().startsWith("models/") && location.getPath().endsWith(".json")) {
            IoSupplier<InputStream> original = source;
            return () -> {
               try (InputStream stream = (InputStream)original.get()) {
                  JsonElement json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                  adaptModel(json, location.getPath(), location.getPath().startsWith("models/item/"));
                  return new ByteArrayInputStream(Create.GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
               }
            };
         } else {
            return source;
         }
      }

      private static boolean isLegacyCardboardItemTrimSource(JsonElement source) {
         if (!source.isJsonObject()) {
            return false;
         }
         JsonElement textures = source.getAsJsonObject().get("textures");
         if (textures == null || !textures.isJsonArray()) {
            return false;
         }
         for (JsonElement texture : textures.getAsJsonArray()) {
            if (texture.isJsonPrimitive() && texture.getAsString().startsWith("create:trims/items/")) {
               return true;
            }
         }
         return false;
      }

      private static boolean isValveHandleBlockState(String path) {
         if (path.startsWith("blockstates/") && path.endsWith("_valve_handle.json")) {
            String name = path.substring("blockstates/".length(), path.length() - ".json".length());
            return name.equals("copper_valve_handle")
               || Arrays.stream(DyeColor.values()).anyMatch(color -> name.equals(color.getSerializedName() + "_valve_handle"));
         } else {
            return false;
         }
      }

      private static void replaceModelReferences(JsonElement element, String replacement) {
         if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
               replaceModelReferences(child, replacement);
            }
         } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();

            for (Entry<String, JsonElement> entry : new HashSet<>(object.entrySet())) {
               if (entry.getKey().equals("model") && entry.getValue().isJsonPrimitive()) {
                  entry.setValue(new JsonPrimitive(replacement));
               } else {
                  replaceModelReferences(entry.getValue(), replacement);
               }
            }
         }
      }

      private static void adaptModel(JsonElement element, String path, boolean itemModel) {
         if (element.isJsonObject()) {
            JsonObject model = element.getAsJsonObject();
            JsonObject textures = model.has("textures") && model.get("textures").isJsonObject() ? model.getAsJsonObject("textures") : null;
            if (textures == null && path.equals("models/item/package_transforms.json")) {
               textures = new JsonObject();
               textures.addProperty("particle", BLOCK_PACKAGE_TEXTURE_ID_PREFIX + "cardboard");
               model.add("textures", textures);
            }
            if (textures != null) {
               for (Entry<String, JsonElement> entry : textures.entrySet()) {
                  if (entry.getValue().isJsonPrimitive()) {
                     String texture = entry.getValue().getAsString();
                     String replacement = ExternalCreateAssets.MODEL_TEXTURE_ALIASES.get(texture);
                     if (replacement == null && texture.startsWith(ITEM_PACKAGE_TEXTURE_ID_PREFIX)) {
                        replacement = BLOCK_PACKAGE_TEXTURE_ID_PREFIX + texture.substring(ITEM_PACKAGE_TEXTURE_ID_PREFIX.length());
                     }
                     if (replacement == null && itemModel) {
                        replacement = ExternalCreateAssets.ITEM_TEXTURE_ALIASES.get(texture);
                     }
                     if (replacement != null) {
                        entry.setValue(new JsonPrimitive(replacement));
                     }
                  }
               }

               if (!textures.has("particle") && !textures.entrySet().isEmpty()) {
                  textures.addProperty("particle", "#" + (String)((Entry)textures.entrySet().iterator().next()).getKey());
               }
            }

            if (path.equals("models/item/cardboard_sword/item_in_hand.json")) {
               model.add(
                  "display",
                  JsonParser.parseString(
                           "{\"thirdperson_righthand\":{\"rotation\":[0,90,-35],\"translation\":[0,1.25,-3.5],\"scale\":[0.85,0.85,0.85]},"
                              + "\"thirdperson_lefthand\":{\"rotation\":[0,-90,35],\"translation\":[0,1.25,-3.5],\"scale\":[0.85,0.85,0.85]},"
                           + "\"firstperson_righthand\":{\"rotation\":[0,-90,25],\"translation\":[1.13,3.2,1.13],\"scale\":[0.68,0.68,0.68]},"
                           + "\"firstperson_lefthand\":{\"rotation\":[0,90,-25],\"translation\":[1.13,3.2,1.13],\"scale\":[0.68,0.68,0.68]},"
                           + "\"ground\":{\"translation\":[0,2,0],\"scale\":[0.5,0.5,0.5]},"
                           + "\"gui\":{\"rotation\":[15,-25,-5],\"translation\":[2,3,0],\"scale\":[0.65,0.65,0.65]},"
                           + "\"fixed\":{\"rotation\":[0,180,0],\"translation\":[0,0,0],\"scale\":[1,1,1]}}"
                     )
                     .getAsJsonObject()
               );
            }

            normalizeUvs(model);
         }
      }

      private static void normalizeUvs(JsonElement element) {
         if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
               normalizeUvs(child);
            }
         } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();

            for (Entry<String, JsonElement> entry : new HashSet<>(object.entrySet())) {
               if (entry.getKey().equals("uv") && entry.getValue().isJsonArray() && entry.getValue().getAsJsonArray().size() == 4) {
                  JsonArray uv = entry.getValue().getAsJsonArray();
                  double x1 = uv.get(0).getAsDouble();
                  double y1 = uv.get(1).getAsDouble();
                  double x2 = uv.get(2).getAsDouble();
                  double y2 = uv.get(3).getAsDouble();
                  double dx = Math.max(0.0, Math.max(x1, x2) - 16.0) + Math.min(0.0, Math.min(x1, x2));
                  double dy = Math.max(0.0, Math.max(y1, y2) - 16.0) + Math.min(0.0, Math.min(y1, y2));
                  JsonArray fixed = new JsonArray();
                  fixed.add(x1 - dx);
                  fixed.add(y1 - dy);
                  fixed.add(x2 - dx);
                  fixed.add(y2 - dy);
                  entry.setValue(fixed);
               } else {
                  normalizeUvs(entry.getValue());
               }
            }
         }
      }

      @NotNull
      public Set<String> getNamespaces(PackType type) {
         return this.delegate.getNamespaces(type);
      }

      @Nullable
      public <T> T getMetadataSection(@NotNull MetadataSectionType<T> type) throws IOException {
         return (T)this.delegate.getMetadataSection(type);
      }

      @NotNull
      public PackLocationInfo location() {
         return this.delegate.location();
      }

      @NotNull
      public String packId() {
         return this.delegate.packId();
      }

      public void close() {
         this.delegate.close();
      }
   }
}
