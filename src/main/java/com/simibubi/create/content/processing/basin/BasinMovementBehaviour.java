package com.simibubi.create.content.processing.basin;

import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.neoforged.neoforge.items.ItemStackHandler;

public class BasinMovementBehaviour implements MovementBehaviour {

	public Map<String, ItemStackHandler> getOrReadInventory(MovementContext context) {
		Map<String, ItemStackHandler> map = new HashMap<>();
		map.put("InputItems", new ItemStackHandler(9));
		map.put("OutputItems", new ItemStackHandler(8));
		return map;
	}

	@Override
	public void tick(MovementContext context) {
		MovementBehaviour.super.tick(context);
	}

}
