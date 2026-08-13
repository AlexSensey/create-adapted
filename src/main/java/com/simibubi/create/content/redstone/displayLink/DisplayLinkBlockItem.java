package com.simibubi.create.content.redstone.displayLink;

import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.api.behaviour.display.DisplayTarget;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;


public class DisplayLinkBlockItem extends ClickToLinkBlockItem {

	public DisplayLinkBlockItem(Block pBlock, Properties pProperties) {
		super(pBlock, pProperties);
	}

	public AABB getSelectionBounds(BlockPos pos) {
		if (Minecraft.getInstance().level == null)
			return super.getSelectionBounds(pos);
		DisplayTarget target = DisplayTarget.get(Minecraft.getInstance().level, pos);
		if (target != null)
			return target.getMultiblockBounds(Minecraft.getInstance().level, pos);
		return super.getSelectionBounds(pos);
	}

	@Override
	public int getMaxDistanceFromSelection() {
		return AllConfigs.server().logistics.displayLinkRange.get();
	}

	@Override
	public String getMessageTranslationKey() {
		return "display_link";
	}

}
