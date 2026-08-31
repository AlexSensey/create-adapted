package net.minecraft.client.renderer.entity.player;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public class PlayerRenderer extends EntityRenderer<AbstractClientPlayer, EntityRenderState> {
	public PlayerRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public PlayerModel getModel() {
		return null;
	}

	public Identifier getTextureLocation(EntityRenderState renderState) {
		return null;
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}
}
