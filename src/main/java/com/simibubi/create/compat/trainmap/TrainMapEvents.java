package com.simibubi.create.compat.trainmap;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.compat.Mods;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class TrainMapEvents {

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;

		if (Mods.XAEROWORLDMAP.isLoaded())
			XaeroTrainMap.tick();
		if (Mods.JOURNEYMAP.isLoaded())
			JourneyTrainMap.tick();
	}

	@SubscribeEvent
	public static void mouseClick(InputEvent.MouseButton.Pre event) {
		if (event.getAction() != InputConstants.PRESS)
			return;

		if (Mods.XAEROWORLDMAP.isLoaded())
			XaeroTrainMap.mouseClick(event);
		if (Mods.JOURNEYMAP.isLoaded())
			JourneyTrainMap.mouseClick(event);
	}
}
