package net.createmod.catnip.api.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public interface BindableTexture {
	default TextureSetup bind() {
		TextureManager manager = Minecraft.getInstance().getTextureManager();
		AbstractTexture texture = manager.getTexture(getId());
		return TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());
	}

	Identifier getId();
}
