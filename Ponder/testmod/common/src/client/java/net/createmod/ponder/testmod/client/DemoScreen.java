package net.createmod.ponder.testmod.client;

import net.createmod.catnip.api.client.gui.NavigatableSimiScreen;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.gui.widget.BoxWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

public final class DemoScreen extends NavigatableSimiScreen {
	private final int depth;

	public DemoScreen() {
		this(1);
	}

	private DemoScreen(int depth) {
		super(Component.literal("Demo"));
		this.depth = depth;
	}

	@Override
	protected void init() {
		super.init();

		LinearLayout layout = LinearLayout.vertical();
		layout.defaultCellSetting().alignHorizontallyCenter();

		layout.addChild(new StringWidget(this.title, this.font));
		layout.addChild(new StringWidget(Component.literal("Depth: " + this.depth), this.font));
		layout.addChild(Button.builder(Component.literal("Recurse"), this::recurse).width(80).build());

		layout.arrangeElements();
		layout.visitWidgets(this::addRenderableWidget);
	}

	@Override
	protected void initBackTrackIcon(BoxWidget backTrack) {
		backTrack.showing(GuiGameElement.of(Items.DIRT));
	}

	private void recurse(Button pressed) {
		int centerX = pressed.getX() + (pressed.getWidth() / 2);
		int centerY = pressed.getY() + (pressed.getHeight() / 2);
		DemoScreen screen = new DemoScreen(this.depth + 1);
		screen.centerScalingOn(centerX, centerY);
		ScreenOpener.transitionTo(screen);
	}
}
