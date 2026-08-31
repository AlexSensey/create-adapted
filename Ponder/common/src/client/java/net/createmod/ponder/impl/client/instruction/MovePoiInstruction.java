package net.createmod.ponder.impl.client.instruction;

import net.createmod.ponder.api.client.instruction.PonderInstruction;
import net.createmod.ponder.api.client.scene.PonderScene;
import net.minecraft.world.phys.Vec3;

public class MovePoiInstruction extends PonderInstruction {
	private final Vec3 poi;

	public MovePoiInstruction(Vec3 poi) {
		this.poi = poi;
	}

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public void tick(PonderScene scene) {
		scene.setPointOfInterest(poi);
	}
}
