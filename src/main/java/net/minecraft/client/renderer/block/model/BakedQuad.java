package net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public class BakedQuad {
	private final int[] vertices;
	private final int tintIndex;
	private final Direction direction;
	private final TextureAtlasSprite sprite;
	private final boolean shade;

	public BakedQuad(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite sprite, boolean shade) {
		this.vertices = vertices;
		this.tintIndex = tintIndex;
		this.direction = direction;
		this.sprite = sprite;
		this.shade = shade;
	}

	public int[] getVertices() {
		return vertices;
	}

	public int getTintIndex() {
		return tintIndex;
	}

	public Direction getDirection() {
		return direction;
	}

	public TextureAtlasSprite getSprite() {
		return sprite;
	}

	public boolean isShade() {
		return shade;
	}
}
