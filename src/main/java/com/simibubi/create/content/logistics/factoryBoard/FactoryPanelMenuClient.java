package com.simibubi.create.content.logistics.factoryBoard;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FactoryPanelMenuClient {

	public static FactoryPanelBehaviour getBehaviour(FactoryPanelPosition pos) {
		if (Minecraft.getInstance().level == null)
			throw new IllegalStateException("Cannot open a Factory Panel menu without a client level");
		FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at((Level) Minecraft.getInstance().level, pos);
		if (behaviour == null)
			throw new IllegalStateException("Factory Panel is missing at " + pos);
		return behaviour;
	}
}
