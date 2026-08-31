package net.createmod.catnip.api.client.platform;

import net.minecraft.client.renderer.block.BlockAndTintGetter;

import org.jspecify.annotations.Nullable;

import net.createmod.catnip.api.platform.ServiceHelper;
import net.createmod.catnip.api.platform.services.ModFluidHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.level.material.Fluid;

/// Client-side extensions for [ModFluidHelper].
///
/// See that class for information about valid types.
public interface ClientFluidHelper {
	ClientFluidHelper INSTANCE = ServiceHelper.load(ClientFluidHelper.class);

	int getColor(TypedInstance<Fluid> fluid, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos);

	@Nullable
	TextureAtlasSprite getStillTexture(TypedInstance<Fluid> fluid);

	default TextureAtlasSprite getStillTextureOrMissing(TypedInstance<Fluid> fluid) {
		TextureAtlasSprite texture = this.getStillTexture(fluid);
		if (texture != null)
			return texture;

		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS).missingSprite();
	}
}
