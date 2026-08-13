package com.simibubi.create.infrastructure.data;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.compat.curios.CuriosDataGenerator;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.data.CreateDatamapProvider;
import com.simibubi.create.foundation.data.DamageTypeTagGen;
import com.simibubi.create.foundation.data.Registrate26Compat;
import com.simibubi.create.foundation.ponder.CreatePonderPlugin;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;

import net.createmod.ponder.api.client.PonderIndex;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Minecraft 26.2 server-data bridge for Create's non-recipe providers.
 *
 * <p>The generated registry provider must be threaded into every provider that
 * references Create's data-driven registries (notably enchantments and damage
 * types). The old generic gather-data event used to do this in one callback;
 * 26.2 exposes the concrete server event instead.</p>
 */
public final class CreateDataGen26 {
	private CreateDataGen26() {
	}

	public static void prepareRegistrateData(GatherDataEvent.Server event) {
		CreateRegistrateTags.addGenerators();
		addExtraRegistrateLang();
	}

	private static void addExtraRegistrateLang() {
		Create.registrate().addDataGenerator(ProviderType.LANG, provider -> {
			BiConsumer<String, String> langConsumer = provider::add;

			provideDefaultLang("interface", langConsumer);
			provideDefaultLang("tooltips", langConsumer);
			AllAdvancements.provideLang(langConsumer);
			AllSoundEvents.provideLang(langConsumer);
			AllKeys.provideLang(langConsumer);
			providePonderLang(langConsumer);
			new TagLangGenerator(langConsumer).generate();
		});
	}

	private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
		String path = "assets/create/lang/default/" + fileName + ".json";
		JsonElement jsonElement = FilesHelper.loadJsonResource(path);
		if (jsonElement == null)
			throw new IllegalStateException("Could not find default lang file: " + path);

		JsonObject jsonObject = jsonElement.getAsJsonObject();
		for (Entry<String, JsonElement> entry : jsonObject.entrySet())
			consumer.accept(entry.getKey(), entry.getValue().getAsString());
	}

	private static void providePonderLang(BiConsumer<String, String> consumer) {
		// Client setup is not fired in a data run, so register the Ponder plugin
		// before collecting its scene/tag language entries.
		PonderIndex.addPlugin(new CreatePonderPlugin());
		PonderIndex.getLangAccess().provideLang(Create.ID, consumer);
	}

	public static void gatherData(GatherDataEvent.Server event) {
		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();
		ExistingFileHelper existingFileHelper = Registrate26Compat.existingFileHelper(event);

		GeneratedEntriesProvider generatedEntries =
			new GeneratedEntriesProvider(output, event.getLookupProvider());
		CompletableFuture<HolderLookup.Provider> registries = generatedEntries.getRegistryProvider();
		generator.addProvider(true, generatedEntries);

		generator.addProvider(true,
			new CreateRecipeSerializerTagsProvider(output, registries, existingFileHelper));
		generator.addProvider(true,
			new CreateContraptionTypeTagsProvider(output, registries, existingFileHelper));
		generator.addProvider(true,
			new CreateMountedItemStorageTypeTagsProvider(output, registries, existingFileHelper));
		generator.addProvider(true, new DamageTypeTagGen(output, registries, existingFileHelper));
		generator.addProvider(true,
			new CreateEnchantmentTagsProvider(output, registries, existingFileHelper));
		generator.addProvider(true, new CreateDatamapProvider(output, registries));
		generator.addProvider(true, new AllAdvancements(output, registries));
		generator.addProvider(true, new VanillaHatOffsetGenerator(output, registries));
		// Curios is an optional compile-time integration. Loading its provider when
		// Curios is absent resolves the provider's Curios superclass and aborts the
		// entire gather-data event before any of Create's own providers can run.
		if (Mods.CURIOS.isLoaded())
			generator.addProvider(true, new CuriosDataGenerator(output, registries, existingFileHelper));
	}
}
