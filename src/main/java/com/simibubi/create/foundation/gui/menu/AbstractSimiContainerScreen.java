package com.simibubi.create.foundation.gui.menu;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.lwjgl.glfw.GLFW;

import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.TickableGuiEventListener;
import net.createmod.catnip.api.client.gui.widget.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;


@ParametersAreNonnullByDefault
public abstract class AbstractSimiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

	protected int windowXOffset, windowYOffset;
	protected int windowWidth = -1;
	protected int windowHeight = -1;

	public AbstractSimiContainerScreen(T container, Inventory inv, Component title) {
		super(container, inv, title);
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowSize(int width, int height) {
		windowWidth = width;
		windowHeight = height;
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowOffset(int xOffset, int yOffset) {
		windowXOffset = xOffset;
		windowYOffset = yOffset;
	}

	@Override
	protected void init() {
		super.init();
		int effectiveWidth = getImageWidth();
		int effectiveHeight = getImageHeight();
		leftPos = (width - effectiveWidth) / 2 + windowXOffset;
		topPos = (height - effectiveHeight) / 2 + windowYOffset;
	}

	@Override
	protected void containerTick() {
		for (GuiEventListener listener : children()) {
			if (listener instanceof TickableGuiEventListener tickable) {
				tickable.tick();
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(W... widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(Collection<W> widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected void removeWidgets(GuiEventListener... widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	protected void removeWidgets(Collection<? extends GuiEventListener> widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	/*@Override
	public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Background(this, pGuiGraphics, pMouseX, pMouseY));
		renderBg(pGuiGraphics, pPartialTick, pMouseX, pMouseY);
	}*/

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		renderBg(graphics, partialTicks, mouseX, mouseY);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		renderForeground(graphics, mouseX, mouseY, partialTicks);
	}

	protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// no-op to prevent screen- and inventory-title from being rendered at incorrect
		// location
	}

	protected void renderForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		for (Renderable widget : renderables) {
			if (!(widget instanceof AbstractSimiWidget simiWidget) || !simiWidget.isMouseOver(mouseX, mouseY))
				continue;
			List<Component> tooltip = simiWidget.getToolTip();
			if (tooltip.isEmpty())
				continue;
			int tooltipX = simiWidget.lockedTooltipX == -1 ? mouseX
				: simiWidget.lockedTooltipX + simiWidget.getX();
			int tooltipY = simiWidget.lockedTooltipY == -1 ? mouseY
				: simiWidget.lockedTooltipY + simiWidget.getY();
			graphics.setComponentTooltipForNextFrame(font, tooltip, tooltipX, tooltipY);
		}
	}

	public int getLeftOfCentered(int textureWidth) {
		return leftPos - windowXOffset + (getImageWidth() - textureWidth) / 2;
	}

	@Override
	public int getImageWidth() {
		return windowWidth == -1 ? super.getImageWidth() : windowWidth;
	}

	@Override
	public int getImageHeight() {
		return windowHeight == -1 ? super.getImageHeight() : windowHeight;
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop) {
		return mouseX < guiLeft || mouseY < guiTop
			|| mouseX >= guiLeft + getImageWidth() || mouseY >= guiTop + getImageHeight();
	}

	public void renderPlayerInventory(GuiGraphicsExtractor graphics, int x, int y) {
		AllGuiTextures.PLAYER_INVENTORY.render(graphics, x, y);
		graphics.text(font, playerInventoryTitle, x + 8, y + 6, 0x404040, false);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (getFocused() instanceof EditBox && event.key() != GLFW.GLFW_KEY_ESCAPE)
			return getFocused().keyPressed(event);
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (getFocused() != null && !getFocused().isMouseOver(event.x(), event.y()))
			setFocused(null);
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public GuiEventListener getFocused() {
		GuiEventListener focused = super.getFocused();
		if (focused instanceof AbstractWidget && !((AbstractWidget) focused).isFocused())
			focused = null;
		setFocused(focused);
		return focused;
	}

	/**
	 * Used for moving JEI out of the way of extra things like block renders.
	 *
	 * @return the space that the GUI takes up outside the normal rectangle defined
	 *         by {@link ContainerScreen}.
	 */
	public List<Rect2i> getExtraAreas() {
		return Collections.emptyList();
	}

	@Deprecated
	protected void debugWindowArea(GuiGraphicsExtractor graphics) {
		graphics.fill(leftPos + imageWidth, topPos + imageHeight, leftPos, topPos, 0xD3D3D3D3);
	}

	@Deprecated
	protected void debugExtraAreas(GuiGraphicsExtractor graphics) {
		for (Rect2i area : getExtraAreas()) {
			graphics.fill(area.getX() + area.getWidth(), area.getY() + area.getHeight(), area.getX(), area.getY(),
				0xD3D3D3D3);
		}
	}

	protected void playUiSound(SoundEvent sound, float volume, float pitch) {
		Minecraft.getInstance()
			.getSoundManager()
			.play(SimpleSoundInstance.forUI(sound, pitch, volume * 0.25f));
	}

}
