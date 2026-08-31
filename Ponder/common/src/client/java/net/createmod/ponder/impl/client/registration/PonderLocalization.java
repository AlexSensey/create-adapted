package net.createmod.ponder.impl.client.registration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import net.createmod.catnip.api.data.Couple;
import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.api.client.PonderIndex;
import net.createmod.ponder.api.client.registration.LangRegistryAccess;
import net.createmod.ponder.impl.client.gui.AbstractPonderScreen;
import net.createmod.ponder.impl.client.tooltip.PonderTooltipHandler;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

public class PonderLocalization implements LangRegistryAccess {
	public static final String LANG_PREFIX = "ponder.";
	public static final String UI_PREFIX = "ui.";

	public final Map<Identifier, String> shared = new HashMap<>();
	public final Map<Identifier, Couple<String>> tag = new HashMap<>();
	public final Map<Identifier, Map<String, String>> specific = new HashMap<>();

	//

	public void clearAll() {
		shared.clear();
		tag.clear();
		specific.clear();
	}

	public void clearShared() {
		shared.clear();
	}

	public void registerShared(Identifier key, String enUS) {
		shared.put(key, enUS);
	}

	public void registerTag(Identifier key, String title, String description) {
		tag.put(key, Couple.create(title, description));
	}

	public void registerSpecific(Identifier sceneId, String key, String enUS) {
		specific.computeIfAbsent(sceneId, $ -> new HashMap<>())
			.put(key, enUS);
	}

	//

	protected static String langKeyForShared(Identifier k) {
		return k.getNamespace() + "." + LANG_PREFIX + "shared." + k.getPath();
	}

	protected static String langKeyForTag(Identifier k) {
		return k.getNamespace() + "." + LANG_PREFIX + "tag." + k.getPath();
	}

	protected static String langKeyForTagDescription(Identifier k) {
		return k.getNamespace() + "." + LANG_PREFIX + "tag." + k.getPath() + ".description";
	}

	protected static String langKeyForSpecific(Identifier sceneId, String k) {
		return sceneId.getNamespace() + "." + LANG_PREFIX + sceneId.getPath() + "." + k;
	}

	//

	@Override
	public String getShared(Identifier key) {
		if (PonderIndex.editingModeActive())
			return shared.containsKey(key) ? shared.get(key) : ("unregistered shared entry: " + key);
		return I18n.get(langKeyForShared(key));
	}

	@Override
	public String getShared(Identifier key, Object... params) {
		if (PonderIndex.editingModeActive())
			return shared.containsKey(key) ? String.format(shared.get(key), params) : ("unregistered shared entry: " + key);
		return I18n.get(langKeyForShared(key), params);
	}

	@Override
	public String getTagName(Identifier key) {
		if (PonderIndex.editingModeActive())
			return tag.containsKey(key) ? tag.get(key)
				.getFirst() : ("unregistered tag entry: " + key);
		return I18n.get(langKeyForTag(key));
	}

	@Override
	public String getTagDescription(Identifier key) {
		if (PonderIndex.editingModeActive())
			return tag.containsKey(key) ? tag.get(key)
				.getSecond() : ("unregistered tag entry: " + key);
		return I18n.get(langKeyForTagDescription(key));
	}

	@Override
	public String getSpecific(Identifier sceneId, String k) {
		if (PonderIndex.editingModeActive())
			try {
				return specific.get(sceneId).get(k);
			} catch (Exception e) {
				return "MISSING_SPECIFIC";
			}
		return I18n.get(langKeyForSpecific(sceneId, k));
	}

	@Override
	public String getSpecific(Identifier sceneId, String k, Object... params) {
		if (PonderIndex.editingModeActive())
			try {
				return String.format(specific.get(sceneId).get(k), params);
			} catch (Exception e) {
				return "MISSING_SPECIFIC";
			}
		return I18n.get(langKeyForSpecific(sceneId, k), params);
	}

	//

	private void recordGeneral(BiConsumer<String, String> consumer) {
		addGeneral(consumer, PonderTooltipHandler.HOLD_TO_PONDER, "Hold [%1$s] to Ponder");
		addGeneral(consumer, AbstractPonderScreen.PONDERING, "Pondering about...");
		addGeneral(consumer, AbstractPonderScreen.PONDERING_TAG, "Pondering about...");
		addGeneral(consumer, AbstractPonderScreen.IDENTIFY_MODE, "Identify mode active.\nUnpause with [%1$s]");
		addGeneral(consumer, AbstractPonderScreen.ASSOCIATED, "Associated Entries");

		addGeneral(consumer, AbstractPonderScreen.CLOSE, "Close");
		addGeneral(consumer, AbstractPonderScreen.IDENTIFY, "Identify");
		addGeneral(consumer, AbstractPonderScreen.NEXT, "Next Scene");
		addGeneral(consumer, AbstractPonderScreen.NEXT_UP, "Up Next:");
		addGeneral(consumer, AbstractPonderScreen.PREVIOUS, "Previous Scene");
		addGeneral(consumer, AbstractPonderScreen.REPLAY, "Replay");
		addGeneral(consumer, AbstractPonderScreen.THINK_BACK, "Think Back");
		addGeneral(consumer, AbstractPonderScreen.SLOW_TEXT, "Comfy Reading");

		addGeneral(consumer, AbstractPonderScreen.EXIT, "Exit");
		addGeneral(consumer, AbstractPonderScreen.WELCOME, "Welcome to Ponder");
		addGeneral(consumer, AbstractPonderScreen.CATEGORIES, "Available Categories for %1$s");
		addGeneral(consumer, AbstractPonderScreen.DESCRIPTION, "Click one of the icons below to learn about its associated Items and Blocks");
		addGeneral(consumer, AbstractPonderScreen.INDEX_TITLE, "Ponder Index");
	}

	private void addGeneral(BiConsumer<String, String> consumer, String key, String enUS) {
		consumer.accept(Ponder.MOD_ID + "." + key, enUS);
	}

	public void generateSceneLang() {
		PonderIndex.getSceneAccess()
			.getRegisteredEntries()
			.forEach(entry -> PonderSceneRegistry.compileScene(this, entry.getValue(), null));
	}

	@Override
	public void provideLang(String modId, BiConsumer<String, String> consumer) {
		PonderIndex.registerAll();
		PonderIndex.gatherSharedText();

		generateSceneLang();

		if (modId.equals(Ponder.MOD_ID))
			recordGeneral(consumer);

		shared.forEach((k, v) -> {
			if (k.getNamespace().equals(modId)) {
				consumer.accept(langKeyForShared(k), v);
			}
		});

		tag.forEach((k, v) -> {
			if (k.getNamespace().equals(modId)) {
				consumer.accept(langKeyForTag(k), v.getFirst());
				consumer.accept(langKeyForTagDescription(k), v.getSecond());
			}
		});

		specific.entrySet()
			.stream()
			.filter(entry -> entry.getKey().getNamespace().equals(modId))
			.sorted(Map.Entry.comparingByKey())
			.forEach(entry -> {
				entry.getValue()
					.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByKey())
					.forEach(subEntry -> consumer.accept(
						langKeyForSpecific(entry.getKey(), subEntry.getKey()), subEntry.getValue()));
			});
	}
}
