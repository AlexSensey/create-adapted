package net.createmod.catnip.api.client.gui;

import net.createmod.catnip.api.client.render.BindableTexture;

public interface TextureSheetSegment extends BindableTexture {
	int getStartX();

	int getStartY();

	int getWidth();

	int getHeight();
}
