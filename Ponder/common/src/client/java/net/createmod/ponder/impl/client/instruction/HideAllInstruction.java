package net.createmod.ponder.impl.client.instruction;

import net.createmod.ponder.api.client.scene.PonderScene;
import net.createmod.ponder.impl.client.element.AnimatedOverlayElementBase;
import net.createmod.ponder.impl.client.element.AnimatedSceneElementBase;
import net.minecraft.core.Direction;

public class HideAllInstruction extends TickingInstruction {

	private final Direction fadeOutTo;

	public HideAllInstruction(int fadeOutTicks, Direction fadeOutTo) {
		super(false, fadeOutTicks);
		this.fadeOutTo = fadeOutTo;
	}

	@Override
	protected void firstTick(PonderScene scene) {
		super.firstTick(scene);
		scene.getElements()
			.forEach(element -> {
				if (element instanceof AnimatedSceneElementBase animatedSceneElement) {
					animatedSceneElement.setFade(1);
					animatedSceneElement
						.setFadeVec(fadeOutTo == null ? null : fadeOutTo.getUnitVec3().scale(.5f));
				} else if (element instanceof AnimatedOverlayElementBase animatedSceneElement) {
					animatedSceneElement.setFade(1);
				} else
					element.setVisible(false);
			});
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		float fade = (remainingTicks / (float) totalTicks);

		scene.forEach(AnimatedSceneElementBase.class, ase -> {
			ase.setFade(fade * fade);
			if (remainingTicks == 0)
				ase.setFade(0);
		});

		scene.forEach(AnimatedOverlayElementBase.class, aoe -> {
			aoe.setFade(fade * fade);
			if (remainingTicks == 0)
				aoe.setFade(0);
		});
	}

}
