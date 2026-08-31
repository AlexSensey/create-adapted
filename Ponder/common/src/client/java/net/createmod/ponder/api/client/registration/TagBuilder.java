package net.createmod.ponder.api.client.registration;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;

public interface TagBuilder {

	TagBuilder title(String title);

	TagBuilder description(String description);

	TagBuilder addToIndex();

	TagBuilder icon(Identifier identifier);

	TagBuilder icon(String path);

	TagBuilder idAsIcon();

	TagBuilder item(ItemLike item, boolean useAsIcon, boolean useAsMainItem);

	default TagBuilder item(ItemLike item) {
		return item(item, true, true);
	}

	void register();

}
