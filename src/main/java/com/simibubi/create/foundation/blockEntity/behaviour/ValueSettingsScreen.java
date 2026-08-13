package com.simibubi.create.foundation.blockEntity.behaviour;

import java.util.function.Consumer;

import com.mojang.blaze3d.platform.Window;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter.ScrollOptionSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueHandler;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

public class ValueSettingsScreen extends AbstractSimiScreen {

	private final BlockPos pos;
	private final ValueSettingsBoard board;
	private final Consumer<ValueSettings> onHover;
	private final int netId;

	private int ticksOpen;
	private int left;
	private int top;
	private int windowWidth;
	private int windowHeight;
	private int labelWidth;
	private int barWidth;
	private int soundCoolDown;
	private final boolean iconMode;
	private final int milestoneSize;
	private final ValueSettings initialSettings;
	private ValueSettings lastHovered = new ValueSettings(-1, -1);

	public ValueSettingsScreen(BlockPos pos, ValueSettingsBoard board, ValueSettings valueSettings,
		Consumer<ValueSettings> onHover, int netId) {
		super(Component.empty());
		this.pos = pos;
		this.board = board;
		this.onHover = onHover;
		this.netId = netId;
		this.initialSettings = valueSettings;
		this.iconMode = board.formatter() instanceof ScrollOptionSettingsFormatter;
		this.milestoneSize = iconMode ? 8 : 4;
		layout();
	}

	private void layout() {
		labelWidth = 0;
		for (Component component : board.rows())
			labelWidth = Math.max(labelWidth, minecraft.font.width(component));
		if (iconMode)
			labelWidth = -18;

		int scale = board.maxValue() > 128 ? 1 : 2;
		int milestones = board.maxValue() / board.milestoneInterval() + 1;
		barWidth = (board.maxValue() + 1) * scale + 1 + milestones * milestoneSize;
		windowWidth = labelWidth + 14 + barWidth + 10;
		windowHeight = board.rows()
			.size() * 11;
		left = (width - windowWidth) / 2;
		top = (height - windowHeight) / 2;
	}

	@Override
	protected void init() {
		layout();
		super.init();
		setCursor(getCoordinateOfValue(initialSettings.row(), initialSettings.value()));
	}

	private void setCursor(Vec2 coordinate) {
		Window window = minecraft.getWindow();
		double scale = window.getGuiScale();
		org.lwjgl.glfw.GLFW.glfwSetCursorPos(window.handle(), coordinate.x * scale, coordinate.y * scale);
	}

	private ValueSettings getClosestCoordinate(int mouseX, int mouseY) {
		int row = 0;
		int value = 0;
		boolean milestonesOnly = AllKeys.shiftDown();

		double bestDiff = Double.MAX_VALUE;
		for (; row < board.rows()
			.size(); row++) {
			Vec2 coordinate = getCoordinateOfValue(row, 0);
			double diff = Math.abs(coordinate.y - mouseY);
			if (bestDiff < diff)
				break;
			bestDiff = diff;
		}
		row -= 1;

		bestDiff = Double.MAX_VALUE;
		for (; value <= board.maxValue(); value++) {
			Vec2 coordinate = getCoordinateOfValue(row, milestonesOnly ? value * board.milestoneInterval() : value);
			double diff = Math.abs(coordinate.x - mouseX);
			if (bestDiff < diff)
				break;
			bestDiff = diff;
		}
		value -= 1;

		if (AllKeys.shiftDown())
			value = Math.min(value * board.milestoneInterval(), board.maxValue());
		return new ValueSettings(row, value);
	}

	private Vec2 getCoordinateOfValue(int row, int value) {
		int scale = board.maxValue() > 128 ? 1 : 2;
		float x = left + ((Math.max(1, value) - 1) / board.milestoneInterval()) * milestoneSize + value * scale + 1.5f;
		x += labelWidth + 14 + 4;

		if (value % board.milestoneInterval() == 0)
			x += milestoneSize / 2f;
		if (value > 0)
			x += milestoneSize;

		float y = top + (row + .5f) * 11 - .5f;
		return new Vec2(x, y);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		layout();

		int milestoneCount = board.maxValue() / board.milestoneInterval() + 1;
		int scale = board.maxValue() > 128 ? 1 : 2;
		double fadeIn = Mth.clamp((ticksOpen + partialTicks) / 4.0, 0, 1);

		Component title = board.title();
		Component tip = CreateLang.translateDirect("gui.value_settings.release_to_confirm", Component.keybind("key.use"));
		int fattestLabel = Math.max(minecraft.font.width(tip), minecraft.font.width(title));
		if (iconMode)
			for (int i = 0; i <= board.maxValue(); i++)
				fattestLabel = Math.max(fattestLabel, minecraft.font.width(board.formatter()
					.format(new ValueSettings(0, i))));

		int extraHeight = iconMode ? 46 : 33;
		int fatTipOffset = Math.max(0, fattestLabel + 10 - (windowWidth + 13)) / 2;
		int bgWidth = Math.max(windowWidth + 13, fattestLabel + 10);
		int fadeInWidth = (int) (bgWidth * fadeIn);
		int fadeInStart = (bgWidth - fadeInWidth) / 2 - fatTipOffset;

		drawStretched(graphics, left - 11 + fadeInStart, top - 17, fadeInWidth, windowHeight + extraHeight,
			AllGuiTextures.VALUE_SETTINGS_OUTER_BG);
		drawStretched(graphics, left - 10 + fadeInStart, top - 18, Math.max(0, fadeInWidth - 2), 1,
			AllGuiTextures.VALUE_SETTINGS_OUTER_BG);
		drawStretched(graphics, left - 10 + fadeInStart, top - 17 + windowHeight + extraHeight,
			Math.max(0, fadeInWidth - 2), 1, AllGuiTextures.VALUE_SETTINGS_OUTER_BG);

		if (fadeInWidth > fattestLabel) {
			int textX = left - 11 - fatTipOffset + bgWidth / 2;
			graphics.text(minecraft.font, title, textX - minecraft.font.width(title) / 2, top - 14, 0xffdddddd, false);
			graphics.text(minecraft.font, tip, textX - minecraft.font.width(tip) / 2, top + windowHeight + extraHeight - 27,
				0xffdddddd, false);
		}

		renderBrassFrame(graphics, left + labelWidth + 14, top - 3, barWidth + 8, board.rows()
			.size() * 11 + 5);
		drawStretched(graphics, left + labelWidth + 17, top, barWidth + 2, board.rows()
			.size() * 11 - 1, AllGuiTextures.VALUE_SETTINGS_BAR_BG);

		int originalY = top;
		int y = top;
		for (Component component : board.rows()) {
			int valueBarX = left + labelWidth + 14 + 4;

			if (!iconMode) {
				drawCropped(graphics, left - 4, y, labelWidth + 8, 11, AllGuiTextures.VALUE_SETTINGS_LABEL_BG);
				for (int w = 0; w < barWidth; w += AllGuiTextures.VALUE_SETTINGS_BAR.getWidth() - 1)
					drawCropped(graphics, valueBarX + w, y + 1,
						Math.min(AllGuiTextures.VALUE_SETTINGS_BAR.getWidth() - 1, barWidth - w),
						8, AllGuiTextures.VALUE_SETTINGS_BAR);
				graphics.text(minecraft.font, component, left, y + 1, 0xff442000, false);
			}

			int milestoneX = valueBarX;
			for (int milestone = 0; milestone < milestoneCount; milestone++) {
				(iconMode ? AllGuiTextures.VALUE_SETTINGS_WIDE_MILESTONE : AllGuiTextures.VALUE_SETTINGS_MILESTONE)
					.render(graphics, milestoneX, y + 1);
				milestoneX += milestoneSize + board.milestoneInterval() * scale;
			}

			y += 11;
		}

		if (!iconMode)
			renderBrassFrame(graphics, left - 7, originalY - 3, labelWidth + 14, board.rows()
				.size() * 11 + 5);

		if (ticksOpen < 1)
			return;

		ValueSettings hovered = getClosestCoordinate(mouseX, mouseY);
		if (!hovered.equals(lastHovered)) {
			onHover.accept(hovered);
			if (soundCoolDown == 0) {
				float pitch = hovered.value() / (float) Math.max(1, board.maxValue());
				pitch = Mth.lerp(pitch, 1.15f, 1.5f);
				minecraft.getSoundManager()
					.play(SimpleSoundInstance.forUI(AllSoundEvents.SCROLL_VALUE.getMainEvent(), pitch, 0.25F));
				ScrollValueHandler.wrenchCog.bump(3, -(hovered.value() - lastHovered.value()) * 10);
				soundCoolDown = 1;
			}
		}
		lastHovered = hovered;

		Vec2 cursor = getCoordinateOfValue(hovered.row(), hovered.value());
		Component cursorText = board.formatter()
			.format(hovered);

		AllIcons cursorIcon = null;
		if (board.formatter() instanceof ScrollOptionSettingsFormatter formatter)
			cursorIcon = formatter.getIcon(hovered);

		int cursorWidth = ((cursorIcon != null ? 16 : minecraft.font.width(cursorText)) / 2) * 2 + 3;
		int cursorX = (int) cursor.x - cursorWidth / 2;
		int cursorY = (int) cursor.y - 7;

		if (cursorIcon != null) {
			AllGuiTextures.VALUE_SETTINGS_CURSOR_ICON.render(graphics, cursorX - 2, cursorY - 3);
			cursorIcon.render(graphics, cursorX + 1, cursorY - 1);
			if (fadeInWidth > fattestLabel)
				graphics.text(minecraft.font, cursorText,
					left - 11 - fatTipOffset + (bgWidth - minecraft.font.width(cursorText)) / 2,
					originalY + windowHeight + extraHeight - 40, 0xfffbdc7d, false);
			return;
		}

		AllGuiTextures.VALUE_SETTINGS_CURSOR_LEFT.render(graphics, cursorX - 3, cursorY);
		drawCropped(graphics, cursorX, cursorY, cursorWidth, 14, AllGuiTextures.VALUE_SETTINGS_CURSOR);
		AllGuiTextures.VALUE_SETTINGS_CURSOR_RIGHT.render(graphics, cursorX + cursorWidth, cursorY);

		graphics.text(minecraft.font, cursorText, cursorX + 2, cursorY + 3, 0xff442000, false);
	}

	private void renderBrassFrame(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		AllGuiTextures.BRASS_FRAME_TL.render(graphics, x, y);
		AllGuiTextures.BRASS_FRAME_TR.render(graphics, x + w - 4, y);
		AllGuiTextures.BRASS_FRAME_BL.render(graphics, x, y + h - 4);
		AllGuiTextures.BRASS_FRAME_BR.render(graphics, x + w - 4, y + h - 4);

		if (h > 8) {
			drawStretched(graphics, x, y + 4, 3, h - 8, AllGuiTextures.BRASS_FRAME_LEFT);
			drawStretched(graphics, x + w - 3, y + 4, 3, h - 8, AllGuiTextures.BRASS_FRAME_RIGHT);
		}

		if (w > 8) {
			drawCropped(graphics, x + 4, y, w - 8, 3, AllGuiTextures.BRASS_FRAME_TOP);
			drawCropped(graphics, x + 4, y + h - 3, w - 8, 3, AllGuiTextures.BRASS_FRAME_BOTTOM);
		}
	}

	private void drawCropped(GuiGraphicsExtractor graphics, int x, int y, int width, int height, AllGuiTextures texture) {
		if (width <= 0 || height <= 0)
			return;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture.getId(), x, y, texture.getStartX(), texture.getStartY(),
			width, height, 256, 256);
	}

	private void drawStretched(GuiGraphicsExtractor graphics, int x, int y, int width, int height, AllGuiTextures texture) {
		if (width <= 0 || height <= 0)
			return;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture.getId(), x, y, texture.getStartX(), texture.getStartY(),
			width, height, texture.getWidth(), texture.getHeight(), 256, 256);
	}

	@Override
	public void tick() {
		ticksOpen++;
		if (soundCoolDown > 0)
			soundCoolDown--;
		super.tick();
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (minecraft.options.keyUse.matchesMouse(event)) {
			saveAndClose(event.x(), event.y());
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (minecraft.options.keyUse.matches(event)) {
			Window window = minecraft.getWindow();
			double x = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
			double y = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
			saveAndClose(x, y);
			return true;
		}
		return super.keyReleased(event);
	}

	private void saveAndClose(double mouseX, double mouseY) {
		ValueSettings closest = getClosestCoordinate((int) mouseX, (int) mouseY);
		ClientNetworkHelper.INSTANCE.sendToServer(new ValueSettingsPacket(pos, closest.row(), closest.value(), null, null,
			Direction.UP, AllKeys.ctrlDown(), netId));
		onClose();
	}
}
