package net.createmod.ponder.impl.client.instruction;

import net.createmod.ponder.api.client.element.ParrotElement;
import net.minecraft.core.Direction;

public class CreateParrotInstruction extends FadeIntoSceneInstruction<ParrotElement> {
	public CreateParrotInstruction(int fadeInTicks, Direction fadeInFrom, ParrotElement element) {
		super(fadeInTicks, fadeInFrom, element);
	}

	@Override
	protected Class<ParrotElement> getElementClass() {
		return ParrotElement.class;
	}
}
