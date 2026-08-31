package net.createmod.ponder.api.client.element;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

public interface AnimatedSceneElement extends PonderSceneElement {
	void forceApplyFade(float fade);

	void setFade(float fade);

	void setFadeVec(@Nullable Vec3 fadeVec);
}
