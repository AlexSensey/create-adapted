package net.createmod.ponder.api.client.element;

import net.createmod.ponder.api.client.scene.PonderScene;
import net.createmod.ponder.impl.client.gui.PonderUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface PonderOverlayElement extends PonderElement {

	void render(PonderScene scene, PonderUI screen, GuiGraphicsExtractor graphics, float partialTicks);

}
