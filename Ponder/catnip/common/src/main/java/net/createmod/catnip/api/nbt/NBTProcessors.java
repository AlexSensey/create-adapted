package net.createmod.catnip.api.nbt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.data.codec.CatnipCodecUtils;
import net.createmod.catnip.api.data.component.ComponentProcessors;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class NBTProcessors {
	private static final Map<BlockEntityType<?>, UnaryOperator<CompoundTag>> processors = new HashMap<>();
	private static final Map<BlockEntityType<?>, UnaryOperator<CompoundTag>> survivalProcessors = new HashMap<>();

	public static synchronized void addProcessor(BlockEntityType<?> type, UnaryOperator<CompoundTag> processor) {
		processors.put(type, processor);
	}

	public static synchronized void addSurvivalProcessor(BlockEntityType<?> type, UnaryOperator<CompoundTag> processor) {
		survivalProcessors.put(type, processor);
	}

	// Triggered by block tag, not BE type
	private static final UnaryOperator<CompoundTag> signProcessor = data -> {
		for (String key : List.of("front_text", "back_text")) {
			Optional<CompoundTag> tag = data.getCompound(key);
			if (tag.isEmpty())
				return null;

			SignText text = CatnipCodecUtils.decode(SignText.DIRECT_CODEC, tag.get()).orElse(null);

			if (text != null) {
				for (Component component : text.getMessages(false)) {
					if (textComponentHasClickEvent(component))
						return null;
				}
			}
		}
		if (data.contains("front_item") || data.contains("back_item"))
			return null; // "Amendments" compat: sign data contains itemstacks
		return data;
	};

	public static UnaryOperator<CompoundTag> itemProcessor(String tagKey) {
		return data -> {
			CompoundTag compound = data.getCompoundOrEmpty(tagKey);
			Optional<CompoundTag> componentsOptional = compound.getCompound("components");
			if (componentsOptional.isEmpty())
				return data;
			CompoundTag components = componentsOptional.get();
			for (String key : components.keySet()) {
				Optional<DataComponentType<?>> optionalType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(Identifier.parse(key)).map(Reference::value);
				optionalType.ifPresent(type -> {
					if (ComponentProcessors.isUnsafeItemComponent(type))
						components.remove(key);
				});
			}
			if (components.isEmpty())
				compound.remove("components");
			return data;
		};
	}

	public static boolean textComponentHasClickEvent(Component component) {
		for (Component sibling : component.getSiblings()) {
			if (textComponentHasClickEvent(sibling)) {
				return true;
			}
		}
		return component.getStyle().getClickEvent() != null;
	}

	private NBTProcessors() {
	}

	@Nullable
	public static CompoundTag process(BlockState state, BlockEntity blockEntity, @Nullable CompoundTag compound, boolean survival) {
		if (compound == null)
			return null;
		BlockEntityType<?> type = blockEntity.getType();
		if (survival && survivalProcessors.containsKey(type))
			compound = survivalProcessors.get(type).apply(compound);
		if (compound != null && processors.containsKey(type))
			return processors.get(type).apply(compound);
		if (blockEntity instanceof SpawnerBlockEntity)
			return compound;
		if (state.is(BlockTags.ALL_SIGNS))
			return signProcessor.apply(compound);
		if (blockEntity.getType().onlyOpCanSetNbt())
			return null;
		return compound;
	}
}
