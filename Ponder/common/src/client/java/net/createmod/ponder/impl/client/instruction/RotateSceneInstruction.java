package net.createmod.ponder.impl.client.instruction;

import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.ponder.api.client.instruction.PonderInstruction;
import net.createmod.ponder.api.client.scene.PonderScene;
import net.createmod.ponder.api.client.scene.PonderScene.SceneTransform;

public class RotateSceneInstruction extends PonderInstruction {
	private final float xRot;
	private final float yRot;
	private final boolean relative;

	public RotateSceneInstruction(float xRot, float yRot, boolean relative) {
		this.xRot = xRot;
		this.yRot = yRot;
		this.relative = relative;
	}

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public void tick(PonderScene scene) {
		SceneTransform transform = scene.getTransform();
		float targetX = relative ? transform.xRotation.getChaseTarget() + xRot : xRot;
		float targetY = relative ? transform.yRotation.getChaseTarget() + yRot : yRot;
		transform.xRotation.chase(targetX, .1f, Chaser.EXP);
		transform.yRotation.chase(targetY, .1f, Chaser.EXP);
	}
}
