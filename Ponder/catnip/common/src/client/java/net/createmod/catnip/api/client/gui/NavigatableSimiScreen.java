package net.createmod.catnip.api.client.gui;

import java.util.List;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;

import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.client.gui.element.BoxElement;
import net.createmod.catnip.api.client.gui.texture.CatnipGuiTextures;
import net.createmod.catnip.api.client.gui.widget.BoxWidget;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.lang.Lang;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class NavigatableSimiScreen extends AbstractSimiScreen {
	public static final Couple<Color> COLOR_NAV_ARROW = Couple.create(
		new Color(0x80_aa9999, true),
		new Color(0x30_aa9999)
	).map(Color::setImmutable);

	protected int depthPointX, depthPointY;
	public final LerpedFloat transition = LerpedFloat.linear()
		.startWithValue(0)
		.chase(0, .1f, LerpedFloat.Chaser.LINEAR);
	protected final LerpedFloat arrowAnimation = LerpedFloat.linear()
		.startWithValue(0)
		.chase(0, 0.075f, LerpedFloat.Chaser.LINEAR);
	@Nullable
	protected BoxWidget backTrack;

	protected NavigatableSimiScreen(Component title) {
		super(title);
		Window window = Minecraft.getInstance().getWindow();
		depthPointX = window.getGuiScaledWidth() / 2;
		depthPointY = window.getGuiScaledHeight() / 2;
	}

	@Override
	public void onClose() {
		ScreenOpener.clearStack();
		super.onClose();
	}

	@Override
	public void tick() {
		super.tick();
		transition.tickChaser();
		arrowAnimation.tickChaser();
	}

	@Override
	protected void init() {
		backTrack = null;
		List<Screen> screenHistory = ScreenOpener.getScreenHistory();
		if (screenHistory.isEmpty())
			return;

		backTrack = addRenderableWidget(new BoxWidget(31, height - 31 - 20)
			.withBounds(20, 20)
			.withCustomBackground(BoxElement.COLOR_BACKGROUND_FLAT)
			.enableFade(0, 5)
			.withPadding(2, 2)
			.fade(1)
			.withCallback(() -> ScreenOpener.openPreviousScreen(this, null)));

		Screen previousScreen = screenHistory.getFirst();
		if (previousScreen instanceof NavigatableSimiScreen screen) {
			screen.initBackTrackIcon(backTrack);
		} else {
			backTrack.showing(CatnipGuiTextures.ICON_DISABLE);
		}

	}

	/**
	 * Called when {@code this} represents the previous screen to
	 * initialize the {@code backTrack} icon of the current screen.
	 *
	 * @param backTrack The backTrack button of the current screen.
	 */
	protected abstract void initBackTrackIcon(BoxWidget backTrack);

	protected @Nullable Component backTrackingComponent() {
		if (ScreenOpener.getBackStepScreen() instanceof NavigatableSimiScreen) {
			return Lang.builder("catnip")
				.translate("gui.step_back")
				.component();
		}

		return Lang.builder("catnip")
			.translate("gui.exit")
			.component();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		// apply transition scaling

		// see the docs on getGuiPartialTicks for why this is used
		float progress = transition.getValue(AnimationTickHolder.getGuiPartialTicks());
		float scale = progress > 0 ? 1 - 0.5f * (1 - progress) : 1 + .5f * (1 + progress);

		Matrix3x2fStack transforms = graphics.pose();
		transforms.pushMatrix();
		transforms.translate(this.depthPointX, this.depthPointY);
		transforms.scale(scale, scale);
		transforms.translate(-this.depthPointX, -this.depthPointY);

		this.extractScaledRenderState(graphics, mouseX, mouseY, partialTicks);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

		transforms.popMatrix();
	}

	public void extractScaledRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		if (this.backTrack != null) {
			int x = (int) Mth.lerp(arrowAnimation.getValue(partialTicks), -9, 21);
			int maxX = backTrack.getX() + backTrack.getWidth();
			Couple<Color> colors = COLOR_NAV_ARROW;

			if (x + 30 < backTrack.getX())
				UIRenderHelper.breadcrumbArrow(graphics, x + 30, height - 51, maxX - (x + 30), 20, 5, colors);

			UIRenderHelper.breadcrumbArrow(graphics, x, height - 51, 30, 20, 5, colors);
			UIRenderHelper.breadcrumbArrow(graphics, x - 30, height - 51, 30, 20, 5, colors);

			if (backTrack.isHoveredOrFocused()) {
				Component component = backTrackingComponent();
				graphics.text(font, component, 41 - font.width(component) / 2, height - 16, UIRenderHelper.COLOR_TEXT_DARKER.getFirst().getRGB(), false);
				if (Mth.equal(arrowAnimation.getValue(), arrowAnimation.getChaseTarget())) {
					arrowAnimation.setValue(1);
					arrowAnimation.setValue(1);// called twice to also set the previous value to 1
				}
			}
		}
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (keyEvent.key() == InputConstants.KEY_BACKSPACE) {
			ScreenOpener.openPreviousScreen(this, null);
			return true;
		}
		return super.keyPressed(keyEvent);
	}

	public void centerScalingOn(int x, int y) {
		depthPointX = x;
		depthPointY = y;
	}

	public void centerScalingOnMouse() {
		Window w = minecraft.getWindow();
		double mouseX = minecraft.mouseHandler.xpos() * w.getGuiScaledWidth() / w.getScreenWidth();
		double mouseY = minecraft.mouseHandler.ypos() * w.getGuiScaledHeight() / w.getScreenHeight();
		centerScalingOn((int) mouseX, (int) mouseY);
	}

	public boolean isEquivalentTo(NavigatableSimiScreen other) {
		return false;
	}

	public void shareContextWith(NavigatableSimiScreen other) {
	}
}
