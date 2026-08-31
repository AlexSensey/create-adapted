package net.createmod.catnip.api.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public class StitchedSprite {
	private static final Map<Identifier, List<StitchedSprite>> ALL = new HashMap<>();

	protected final Identifier atlasIdentifier;
	protected final Identifier id;
	protected TextureAtlasSprite sprite;

	public StitchedSprite(Identifier atlas, Identifier id) {
		atlasIdentifier = atlas;
		this.id = id;
		ALL.computeIfAbsent(atlasIdentifier, $ -> new ArrayList<>()).add(this);
	}

	public StitchedSprite(Identifier id) {
		this(TextureAtlas.LOCATION_BLOCKS, id);
	}

	public static void afterAtlasStitch(TextureAtlas atlas) {
		Identifier atlasIdentifier = atlas.location();
		List<StitchedSprite> sprites = ALL.get(atlasIdentifier);
		if (sprites != null) {
			for (StitchedSprite sprite : sprites) {
				sprite.loadSprite(atlas);
			}
		}
	}

	protected void loadSprite(TextureAtlas atlas) {
		sprite = atlas.getSprite(id);
	}

	public Identifier getAtlasIdentifier() {
		return atlasIdentifier;
	}

	public Identifier getId() {
		return id;
	}

	public TextureAtlasSprite get() {
		return sprite;
	}
}
