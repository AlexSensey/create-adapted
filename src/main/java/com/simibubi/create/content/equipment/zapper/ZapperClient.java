package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.zapper.ZapperRenderHandler.LaserBeam;
import com.simibubi.create.content.equipment.zapper.terrainzapper.WorldshaperScreen;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ZapperClient {
	private ZapperClient() {
	}

	public static void openHandgunGUI(ItemStack item, InteractionHand hand) {
		ScreenOpener.open(new WorldshaperScreen(item, hand));
	}

	public static void onUse(InteractionHand hand, Vec3 location, Vec3 target) {
		CreateClient.ZAPPER_RENDER_HANDLER.dontAnimateItem(hand);
		CreateClient.ZAPPER_RENDER_HANDLER.addBeam(new LaserBeam(location, target));
	}
}
