package com.simibubi.create.infrastructure.assets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.Create;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Mounts client assets from an official Create jar supplied by the player.
 * The original jar is never placed on the mod classpath and none of its code is executed.
 */
public final class ExternalCreateAssets {
	public static final String DIRECTORY_NAME = "create_original";
	public static final String OFFICIAL_CREATE_VERSION = "6.0.10";
	public static final String OFFICIAL_MINECRAFT_VERSION = "1.21.1";
	public static final String TARGET_MINECRAFT_VERSION = "26.2";
	public static final String TESTED_NEOFORGE_VERSION = "26.2.0.18-beta";
	public static final String DOWNLOAD_URL =
		"https://www.curseforge.com/minecraft/mc-mods/create/files/all?page=1&pageSize=20&version=1.21.1";

	private static final List<String> REQUIRED_ENTRIES = List.of(
		"assets/create/lang/en_us.json",
		"assets/create/textures/block/andesite_casing.png",
		"assets/create/models/block/cogwheel.json"
	);
	private static final Map<String, String> RESOURCE_ALIASES = Map.ofEntries(
		Map.entry("textures/block/goggles_model.png", "textures/item/goggles_model.png"),
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
		"create:block/mechanical_press_pole", "create:item/mechanical_press_pole",
		"create:block/redstone_antenna", "create:item/redstone_antenna",
		"create:block/redstone_antenna_powered", "create:item/redstone_antenna_powered"
	);
	private static final Map<String, String> GENERATED_JSON = Map.of(
		"models/block/empty.json",
		"{\"elements\":[]}",
		"models/item/extendo_grip/hand.json",
		"{\"parent\":\"create:block/deployer/hand_punching\"}",
		"models/item/extendo_grip/hand_holding.json",
		"{\"parent\":\"create:block/deployer/hand_holding\"}"
	);

	private static boolean checked;
	private static Path directory;
	private static Path originalJar;
	private static String failure;

	private ExternalCreateAssets() {
	}

	public static void initialize() {
		if (checked)
			return;
		checked = true;
		directory = Path.of(System.getProperty("user.dir", "."), DIRECTORY_NAME)
			.toAbsolutePath()
			.normalize();

		try {
			Files.createDirectories(directory);
			originalJar = findOriginalJar();
			if (originalJar == null) {
				failure = "Official Create " + OFFICIAL_CREATE_VERSION + " for Minecraft "
					+ OFFICIAL_MINECRAFT_VERSION + " was not found.";
				Create.LOGGER.error("{} Expected it in {}", failure, directory);
			} else {
				Create.LOGGER.info("Using player-supplied official Create assets from {}", originalJar);
			}
		} catch (IOException e) {
			failure = "Could not inspect " + directory + ": " + e.getMessage();
			Create.LOGGER.error(failure, e);
		}
	}

	private static @Nullable Path findOriginalJar() throws IOException {
		try (var files = Files.list(directory)) {
			for (Path candidate : files.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".jar"))
				.filter(path -> path.getFileName().toString().contains(OFFICIAL_CREATE_VERSION))
				.sorted(Comparator.comparing(path -> path.getFileName().toString()))
				.toList()) {
				try (ZipFile zip = new ZipFile(candidate.toFile())) {
					if (REQUIRED_ENTRIES.stream().allMatch(entry -> zip.getEntry(entry) != null))
						return candidate;
				} catch (IOException e) {
					Create.LOGGER.warn("Ignoring unreadable Create asset jar {}", candidate, e);
				}
			}
		}
		return null;
	}

	public static void addPackFinders(AddPackFindersEvent event) {
		initialize();
		if (event.getPackType() != PackType.CLIENT_RESOURCES || originalJar == null)
			return;

		Path jar = originalJar;
		event.addRepositorySource(new RepositorySource() {
			@Override
			public void loadPacks(@NotNull Consumer<Pack> output) {
				PackLocationInfo location = new PackLocationInfo("create_official_external_assets",
					Component.literal("Official Create " + OFFICIAL_CREATE_VERSION + " assets"),
					PackSource.BUILT_IN, Optional.empty());
				FilePackResources.FileResourcesSupplier delegate = new FilePackResources.FileResourcesSupplier(jar);
				Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
					@Override
					public PackResources openPrimary(PackLocationInfo info) {
						return new AdaptedResources(delegate.openPrimary(info));
					}

					@Override
					public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
						return new AdaptedResources(delegate.openFull(info, metadata));
					}
				};
				Pack pack = Pack.readMetaAndCreate(location, supplier, PackType.CLIENT_RESOURCES,
					// The official 1.21.1 jar supplies the base artwork. Keep it below
					// this port's small 26.2 JSON compatibility layer (item definitions,
					// atlas routing, and equipment declarations).
					new PackSelectionConfig(true, Pack.Position.BOTTOM, false));
				if (pack != null)
					output.accept(pack);
			}
		});
	}

	/**
	 * Exposes compatibility aliases and adjusts old 1.21.1 model JSON in memory.
	 * All bytes still come from the player-supplied official jar; no upstream
	 * artwork is copied into the adaptation jar or written to disk.
	 */
	private static final class AdaptedResources implements PackResources {
		private final PackResources delegate;

		private AdaptedResources(PackResources delegate) {
			this.delegate = delegate;
		}

		@Override
		public @Nullable IoSupplier<java.io.InputStream> getRootResource(String @NotNull ... elements) {
			return delegate.getRootResource(elements);
		}

		@Override
		public @Nullable IoSupplier<java.io.InputStream> getResource(@NotNull PackType type,
			@NotNull Identifier location) {
			IoSupplier<java.io.InputStream> source = delegate.getResource(type, location);
			if (type != PackType.CLIENT_RESOURCES || !location.getNamespace().equals(Create.ID))
				return source;

			if (source == null) {
				String original = RESOURCE_ALIASES.get(location.getPath());
				if (original != null)
					source = delegate.getResource(type, Identifier.fromNamespaceAndPath(Create.ID, original));
			}
			if (source == null) {
				String generated = GENERATED_JSON.get(location.getPath());
				if (generated != null)
					source = () -> new ByteArrayInputStream(generated.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}
			return adaptSupplier(location, source);
		}

		@Override
		public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path,
			@NotNull ResourceOutput output) {
			delegate.listResources(type, namespace, path, (location, source) ->
				output.accept(location, adaptSupplier(location, source)));

			if (type != PackType.CLIENT_RESOURCES || !namespace.equals(Create.ID))
				return;
			RESOURCE_ALIASES.forEach((alias, original) -> {
				if (!isUnder(alias, path))
					return;
				Identifier originalLocation = Identifier.fromNamespaceAndPath(Create.ID, original);
				IoSupplier<java.io.InputStream> source = delegate.getResource(type, originalLocation);
				if (source != null)
					output.accept(Identifier.fromNamespaceAndPath(Create.ID, alias), source);
			});
			GENERATED_JSON.forEach((resource, json) -> {
				if (isUnder(resource, path))
					output.accept(Identifier.fromNamespaceAndPath(Create.ID, resource),
						() -> new ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
			});
		}

		private static boolean isUnder(String resource, String path) {
			return path.isEmpty() || resource.equals(path) || resource.startsWith(path.endsWith("/") ? path : path + "/");
		}

		private static @Nullable IoSupplier<java.io.InputStream> adaptSupplier(Identifier location,
			@Nullable IoSupplier<java.io.InputStream> source) {
			if (source == null || !location.getNamespace().equals(Create.ID))
				return source;
			if (location.getPath().startsWith("lang/") && location.getPath().endsWith(".json")) {
				IoSupplier<java.io.InputStream> original = source;
				return () -> {
					try (var stream = original.get()) {
						JsonObject language = JsonParser.parseReader(new java.io.InputStreamReader(stream,
							java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
						Map<String, JsonElement> additions = new HashMap<>();
						language.entrySet().forEach(entry -> {
							if (entry.getKey().startsWith("block.create.")) {
								String itemKey = "item.create." + entry.getKey().substring("block.create.".length());
								if (!language.has(itemKey))
									additions.put(itemKey, entry.getValue().deepCopy());
							}
						});
						additions.forEach(language::add);
						return new ByteArrayInputStream(Create.GSON.toJson(language)
							.getBytes(java.nio.charset.StandardCharsets.UTF_8));
					}
				};
			}
			if (isValveHandleBlockState(location.getPath())) {
				IoSupplier<java.io.InputStream> original = source;
				return () -> {
					try (var stream = original.get()) {
						JsonElement json = JsonParser.parseReader(new java.io.InputStreamReader(stream,
							java.nio.charset.StandardCharsets.UTF_8));
						replaceModelReferences(json, "create:block/empty");
						return new ByteArrayInputStream(Create.GSON.toJson(json)
							.getBytes(java.nio.charset.StandardCharsets.UTF_8));
					}
				};
			}
			if (!location.getPath().startsWith("models/") || !location.getPath().endsWith(".json"))
				return source;
			IoSupplier<java.io.InputStream> original = source;
			return () -> {
				try (var stream = original.get()) {
					JsonElement json = JsonParser.parseReader(new java.io.InputStreamReader(stream,
						java.nio.charset.StandardCharsets.UTF_8));
					adaptModel(json, location.getPath().startsWith("models/item/"));
					return new ByteArrayInputStream(Create.GSON.toJson(json)
						.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				}
			};
		}

		private static boolean isValveHandleBlockState(String path) {
			if (!path.startsWith("blockstates/") || !path.endsWith("_valve_handle.json"))
				return false;
			String name = path.substring("blockstates/".length(), path.length() - ".json".length());
			return name.equals("copper_valve_handle") || java.util.Arrays.stream(net.minecraft.world.item.DyeColor.values())
				.anyMatch(color -> name.equals(color.getSerializedName() + "_valve_handle"));
		}

		private static void replaceModelReferences(JsonElement element, String replacement) {
			if (element.isJsonArray()) {
				for (JsonElement child : element.getAsJsonArray())
					replaceModelReferences(child, replacement);
				return;
			}
			if (!element.isJsonObject())
				return;
			JsonObject object = element.getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : new HashSet<>(object.entrySet())) {
				if (entry.getKey().equals("model") && entry.getValue().isJsonPrimitive())
					entry.setValue(new com.google.gson.JsonPrimitive(replacement));
				else
					replaceModelReferences(entry.getValue(), replacement);
			}
		}

		private static void adaptModel(JsonElement element, boolean itemModel) {
			if (!element.isJsonObject())
				return;
			JsonObject model = element.getAsJsonObject();
			JsonObject textures = model.has("textures") && model.get("textures").isJsonObject()
				? model.getAsJsonObject("textures") : null;
			if (textures != null) {
				for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
					if (itemModel && entry.getValue().isJsonPrimitive()) {
						String replacement = ITEM_TEXTURE_ALIASES.get(entry.getValue().getAsString());
						if (replacement != null)
							entry.setValue(new com.google.gson.JsonPrimitive(replacement));
					}
				}
				if (!textures.has("particle") && !textures.entrySet().isEmpty())
					textures.addProperty("particle", "#" + textures.entrySet().iterator().next().getKey());
			}
			normalizeUvs(model);
		}

		private static void normalizeUvs(JsonElement element) {
			if (element.isJsonArray()) {
				for (JsonElement child : element.getAsJsonArray())
					normalizeUvs(child);
				return;
			}
			if (!element.isJsonObject())
				return;
			JsonObject object = element.getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : new HashSet<>(object.entrySet())) {
				if (entry.getKey().equals("uv") && entry.getValue().isJsonArray()
					&& entry.getValue().getAsJsonArray().size() == 4) {
					JsonArray uv = entry.getValue().getAsJsonArray();
					double x1 = uv.get(0).getAsDouble(), y1 = uv.get(1).getAsDouble();
					double x2 = uv.get(2).getAsDouble(), y2 = uv.get(3).getAsDouble();
					double dx = Math.max(0, Math.max(x1, x2) - 16) + Math.min(0, Math.min(x1, x2));
					double dy = Math.max(0, Math.max(y1, y2) - 16) + Math.min(0, Math.min(y1, y2));
					JsonArray fixed = new JsonArray();
					fixed.add(x1 - dx); fixed.add(y1 - dy); fixed.add(x2 - dx); fixed.add(y2 - dy);
					entry.setValue(fixed);
				} else {
					normalizeUvs(entry.getValue());
				}
			}
		}

		@Override
		public @NotNull Set<String> getNamespaces(PackType type) {
			return delegate.getNamespaces(type);
		}

		@Override
		public @Nullable <T> T getMetadataSection(@NotNull MetadataSectionType<T> type) throws IOException {
			return delegate.getMetadataSection(type);
		}

		@Override
		public @NotNull PackLocationInfo location() {
			return delegate.location();
		}

		@Override
		public @NotNull String packId() {
			return delegate.packId();
		}

		@Override
		public void close() {
			delegate.close();
		}
	}

	public static boolean isMissing() {
		initialize();
		return originalJar == null;
	}

	public static Path directory() {
		initialize();
		return directory;
	}

	public static String failure() {
		initialize();
		return failure == null ? "Official Create assets are unavailable." : failure;
	}
}
