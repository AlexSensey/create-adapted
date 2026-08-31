package net.createmod.catnip.client;

import net.minecraft.client.KeyMapping;

public class ConflictSafeKeyMapping extends KeyMapping {
	public ConflictSafeKeyMapping(String name, int keyCode, String category) {
		super(name, keyCode, KeyMapping.Category.GAMEPLAY);
	}
}
