package com.simibubi.create.foundation;

import java.util.List;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;

import net.createmod.catnip.api.data.codec.CatnipCodecUtils;
import net.createmod.catnip.api.nbt.NBTProcessors;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public class CreateNBTProcessors {
	public static void register() {
		NBTProcessors.addProcessor(BlockEntityTypes.LECTERN, CreateNBTProcessors::lecternProcessor);

		NBTProcessors.addProcessor(AllBlockEntityTypes.CLIPBOARD.get(), CreateNBTProcessors::clipboardProcessor);

		NBTProcessors.addProcessor(AllBlockEntityTypes.CREATIVE_CRATE.get(), NBTProcessors.itemProcessor("Filter"));
	}

	public static CompoundTag clipboardProcessor(CompoundTag data) {
		CompoundTag encodedComponents = data.getCompound("components")
			.orElse(null);
		if (encodedComponents == null)
			return data;

		DataComponentMap components = CatnipCodecUtils.decodeOrNull(DataComponentMap.CODEC, encodedComponents);
		if (components == null)
			return data;

		ClipboardContent content = components.get(AllDataComponents.CLIPBOARD_CONTENT);
		if (content == null)
			return data;

		for (List<ClipboardEntry> entries : content.pages())
			for (ClipboardEntry entry : entries)
				if (NBTProcessors.textComponentHasClickEvent(entry.text))
					return null;

		return data;
	}

	private static CompoundTag lecternProcessor(CompoundTag data) {
		CompoundTag encodedBook = data.getCompound("Book")
			.orElse(null);
		if (encodedBook == null)
			return data;

		ItemStack book = CatnipCodecUtils.decodeOrNull(ItemStack.CODEC, encodedBook);
		if (book == null || book.is(Items.WRITABLE_BOOK))
			return data;

		WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
		if (content == null)
			return data;

		for (Filterable<Component> page : content.pages())
			if (NBTProcessors.textComponentHasClickEvent(page.get(false)))
				return null;

		return data;
	}
}
