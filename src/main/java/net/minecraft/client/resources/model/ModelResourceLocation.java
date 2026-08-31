package net.minecraft.client.resources.model;

import net.minecraft.resources.Identifier;

public record ModelResourceLocation(Identifier id, String variant) {
	public ModelResourceLocation {
		if (variant == null)
			variant = "";
	}

	@Override
	public String toString() {
		return id + "#" + variant;
	}
}
