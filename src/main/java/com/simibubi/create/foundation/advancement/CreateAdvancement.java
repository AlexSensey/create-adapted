package com.simibubi.create.foundation.advancement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.simibubi.create.Create;
import com.tterrag.registrate.util.entry.ItemProviderEntry;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.triggers.ItemUsedOnLocationTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public class CreateAdvancement {

	// Since 26.2 advancement backgrounds use a ClientAsset id; Minecraft adds
	// the textures/ prefix and .png suffix when resolving the resource.
	static final Identifier BACKGROUND = Create.asResource("gui/advancements");
	static final String LANG = "advancement." + Create.ID + ".";
	static final String SECRET_SUFFIX = "\n\u00A77(Hidden Advancement)";

	private final Advancement.Builder mcBuilder = Advancement.Builder.advancement();
	private SimpleCreateTrigger builtinTrigger;
	private CreateAdvancement parent;
	private final Builder createBuilder = new Builder();

	AdvancementHolder datagenResult;

	private String id;
	private String title;
	private String description;

	public CreateAdvancement(String id, UnaryOperator<Builder> b) {
		this.id = id;

		b.apply(createBuilder);

		if (!createBuilder.externalTrigger) {
			builtinTrigger = AllTriggers.addSimple(id + "_builtin");
			mcBuilder.addCriterion("0", builtinTrigger.createCriterion(builtinTrigger.instance()));
		}

		if (createBuilder.type == TaskType.SECRET)
			description += SECRET_SUFFIX;

		AllAdvancements.ENTRIES.add(this);
	}

	private String titleKey() {
		return LANG + id;
	}

	private String descriptionKey() {
		return titleKey() + ".desc";
	}

	public boolean isAlreadyAwardedTo(Player player) {
		if (!(player instanceof ServerPlayer sp))
			return true;
		AdvancementHolder advancement = sp.level()
			.getServer()
			.getAdvancements()
			.get(Create.asResource(id));
		if (advancement == null)
			return true;
		return sp.getAdvancements()
			.getOrStartProgress(advancement)
			.isDone();
	}

	public void awardTo(Player player) {
		if (!(player instanceof ServerPlayer sp))
			return;
		if (builtinTrigger == null)
			throw new UnsupportedOperationException(
				"Advancement " + id + " uses external Triggers, it cannot be awarded directly");
		builtinTrigger.trigger(sp);
	}

	void save(Consumer<AdvancementHolder> t, HolderLookup.Provider registries) {
		if (parent != null)
			mcBuilder.parent(parent.datagenResult);
		for (ItemLike collectedItem : createBuilder.collectedItems)
			mcBuilder.addCriterion(String.valueOf(createBuilder.keyIndex++),
				InventoryChangeTrigger.TriggerInstance.hasItems(collectedItem));
		if (createBuilder.collectedTag != null) {
			ItemPredicate predicate = ItemPredicate.Builder.item()
				.of(registries.lookupOrThrow(Registries.ITEM), createBuilder.collectedTag)
				.build();
			mcBuilder.addCriterion(String.valueOf(createBuilder.keyIndex++),
				InventoryChangeTrigger.TriggerInstance.hasItems(predicate));
		}

		if (createBuilder.func != null)
			createBuilder.icon(createBuilder.func.apply(registries));

		mcBuilder.display(createBuilder.icon.getItem(), Component.translatable(titleKey()),
			Component.translatable(descriptionKey()).withStyle(s -> s.withColor(0xDBA213)),
			id.equals("root") ? BACKGROUND : null, createBuilder.type.advancementType, createBuilder.type.toast,
			createBuilder.type.announce, createBuilder.type.hide);

		datagenResult = mcBuilder.save(t, Create.asResource(id)
			.toString());
	}

	void provideLang(BiConsumer<String, String> consumer) {
		consumer.accept(titleKey(), title);
		consumer.accept(descriptionKey(), description);
	}

	static enum TaskType {

		SILENT(AdvancementType.TASK, false, false, false),
		NORMAL(AdvancementType.TASK, true, false, false),
		NOISY(AdvancementType.TASK, true, true, false),
		EXPERT(AdvancementType.GOAL, true, true, false),
		SECRET(AdvancementType.GOAL, true, true, true),

		;

		private final AdvancementType advancementType;
		private final boolean toast;
		private final boolean announce;
		private final boolean hide;

		TaskType(AdvancementType advancementType, boolean toast, boolean announce, boolean hide) {
			this.advancementType = advancementType;
			this.toast = toast;
			this.announce = announce;
			this.hide = hide;
		}
	}

	class Builder {

		private TaskType type = TaskType.NORMAL;
		private boolean externalTrigger;
		private int keyIndex;
		private ItemStack icon;
		private ItemLike iconItem;
		private Function<Provider, ItemStack> func;
		private final List<ItemLike> collectedItems = new ArrayList<>();
		private TagKey<Item> collectedTag;

		Builder special(TaskType type) {
			this.type = type;
			return this;
		}

		Builder after(CreateAdvancement other) {
			CreateAdvancement.this.parent = other;
			return this;
		}

		Builder icon(ItemProviderEntry<?, ?> item) {
			iconItem = item;
			return icon(provider -> item.asStack());
		}

		Builder icon(ItemLike item) {
			iconItem = item;
			return icon(provider -> new ItemStack(item));
		}

		Builder icon(ItemStack stack) {
			icon = stack;
			iconItem = stack.getItem();
			return this;
		}

		Builder icon(Function<Provider, ItemStack> func) {
			this.func = func;
			return this;
		}

		Builder title(String title) {
			CreateAdvancement.this.title = title;
			return this;
		}

		Builder description(String description) {
			CreateAdvancement.this.description = description;
			return this;
		}

		Builder whenBlockPlaced(Block block) {
			return externalTrigger(ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block));
		}

		Builder whenIconCollected() {
			return whenItemCollected(iconItem);
		}

		Builder whenItemCollected(ItemProviderEntry<?, ?> item) {
			return whenItemCollected((ItemLike) item);
		}

		Builder whenItemCollected(ItemLike itemProvider) {
			collectedItems.add(itemProvider);
			externalTrigger = true;
			return this;
		}

		Builder whenItemCollected(TagKey<Item> tag) {
			collectedTag = tag;
			externalTrigger = true;
			return this;
		}

		Builder awardedForFree() {
			return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[] {}));
		}

		Builder externalTrigger(Criterion<?> trigger) {
			mcBuilder.addCriterion(String.valueOf(keyIndex), trigger);
			externalTrigger = true;
			keyIndex++;
			return this;
		}

	}

}
