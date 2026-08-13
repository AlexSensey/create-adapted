package com.simibubi.create.content.trains.station;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.Consumer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.decoration.slidingDoor.DoorControl;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public class StationScreen extends AbstractStationScreen {

	private EditBox nameBox;
	private EditBox trainNameBox;
	private IconButton newTrainButton;
	private IconButton disassembleTrainButton;
	private IconButton dropScheduleButton;

	private int leavingAnimation;
	private LerpedFloat trainPosition;
	private DoorControl doorControl;

	private ScrollInput colorTypeScroll;
	private int messedWithColors;

	private boolean switchingToAssemblyMode;

	public StationScreen(StationBlockEntity be, GlobalStation station) {
		super(be, station);
		background = AllGuiTextures.STATION;
		leavingAnimation = 0;
		trainPosition = LerpedFloat.linear()
			.startWithValue(0);
		switchingToAssemblyMode = false;
		doorControl = be.doorControls.mode;
	}

	@Override
	protected void init() {
		super.init();

		Consumer<String> onTextChanged = s -> nameBox.setX(nameBoxX(s, nameBox));
		nameBox = new EditBox(font, left + 23, top + 4, background.getWidth() - 20, 10,
			Component.literal(station.name));
		nameBox.setBordered(false);
		nameBox.setMaxLength(25);
		nameBox.setTextColor(opaque(0x592424));
		nameBox.setTextShadow(false);
		nameBox.setValue(station.name);
		nameBox.setFocused(false);
		nameBox.setResponder(onTextChanged);
		nameBox.setX(nameBoxX(nameBox.getValue(), nameBox));
		addRenderableWidget(nameBox);

		Runnable assemblyCallback = () -> {
			switchingToAssemblyMode = true;
			ClientNetworkHelper.INSTANCE.sendToServer(
				StationEditPacket.configure(blockEntity.getBlockPos(), true, nameBox.getValue(), doorControl));
			ScreenOpener.open(new AssemblyScreen(blockEntity, station));
		};

		newTrainButton = new WideIconButton(left + 84, top + 65, AllGuiTextures.I_NEW_TRAIN);
		newTrainButton.withCallback(assemblyCallback);
		addRenderableWidget(newTrainButton);

		disassembleTrainButton = new WideIconButton(left + 94, top + 65, AllGuiTextures.I_DISASSEMBLE_TRAIN);
		disassembleTrainButton.active = false;
		disassembleTrainButton.visible = false;
		disassembleTrainButton.withCallback(assemblyCallback);
		addRenderableWidget(disassembleTrainButton);

		dropScheduleButton = new IconButton(left + 73, top + 65, AllIcons.I_VIEW_SCHEDULE);
		dropScheduleButton.active = false;
		dropScheduleButton.visible = false;
		dropScheduleButton.withCallback(() ->
			ClientNetworkHelper.INSTANCE.sendToServer(StationEditPacket.dropSchedule(blockEntity.getBlockPos())));
		addRenderableWidget(dropScheduleButton);

		colorTypeScroll = new ScrollInput(left + 166, top + 17, 22, 14)
			.titled(CreateLang.translateDirect("station.train_map_color"));
		colorTypeScroll.withRange(0, 16);
		colorTypeScroll.withStepFunction(ctx -> colorTypeScroll.standardStep()
			.apply(ctx));
		colorTypeScroll.calling(s -> {
			Train train = displayedTrain.get();
			if (train != null) {
				train.mapColorIndex = s;
				messedWithColors = 10;
			}
		});
		colorTypeScroll.active = colorTypeScroll.visible = false;
		addRenderableWidget(colorTypeScroll);

		onTextChanged = s -> trainNameBox.setX(nameBoxX(s, trainNameBox));
		trainNameBox = new EditBox(font, left + 23, top + 47, background.getWidth() - 75, 10,
			CommonComponents.EMPTY);
		trainNameBox.setBordered(false);
		trainNameBox.setMaxLength(35);
		trainNameBox.setTextColor(opaque(0xC6C6C6));
		trainNameBox.setTextShadow(false);
		trainNameBox.setFocused(false);
		trainNameBox.setResponder(onTextChanged);
		trainNameBox.active = false;

		tickTrainDisplay();

		Pair<ScrollInput, Label> doorControlWidgets =
			DoorControl.createWidget(left + 35, top + 102, mode -> doorControl = mode, doorControl);
		addRenderableWidget(doorControlWidgets.getFirst());
		addRenderableWidget(doorControlWidgets.getSecond());
	}

	@Override
	public void tick() {
		tickTrainDisplay();
		if (getFocused() != nameBox) {
			nameBox.setCursorPosition(nameBox.getValue()
				.length());
			nameBox.setHighlightPos(nameBox.getCursorPosition());
		}
		if (getFocused() != trainNameBox || !trainNameBox.active) {
			trainNameBox.setCursorPosition(trainNameBox.getValue()
				.length());
			trainNameBox.setHighlightPos(trainNameBox.getCursorPosition());
		}

		if (messedWithColors > 0) {
			messedWithColors--;
			if (messedWithColors == 0)
				syncTrainNameAndColor();
		}

		trainPosition.tickChaser();
		super.tick();

		updateAssemblyTooltip(blockEntity.edgePoint.isOnCurve() ? "no_assembly_curve"
			: !blockEntity.edgePoint.isOrthogonal() ? "no_assembly_diagonal"
				: trainPresent() && !blockEntity.trainCanDisassemble ? "train_not_aligned" : null);
	}

	private void tickTrainDisplay() {
		Train train = displayedTrain.get();

		if (train == null) {
			if (trainNameBox.active) {
				trainNameBox.active = false;
				removeWidget(trainNameBox);
			}

			leavingAnimation = 0;
			newTrainButton.active = blockEntity.edgePoint.isOrthogonal();
			newTrainButton.visible = true;
			colorTypeScroll.visible = false;
			colorTypeScroll.active = false;
			Train imminentTrain = getImminent();

			if (imminentTrain != null) {
				displayedTrain = new WeakReference<>(imminentTrain);
				newTrainButton.active = false;
				newTrainButton.visible = false;
				disassembleTrainButton.active = false;
				disassembleTrainButton.visible = true;
				dropScheduleButton.active = blockEntity.trainHasSchedule;
				dropScheduleButton.visible = true;
				if (mapModsPresent()) {
					colorTypeScroll.setState(imminentTrain.mapColorIndex);
					colorTypeScroll.visible = true;
					colorTypeScroll.active = true;
				}
				trainNameBox.active = true;
				trainNameBox.setValue(imminentTrain.name.getString());
				trainNameBox.setX(nameBoxX(trainNameBox.getValue(), trainNameBox));
				addRenderableWidget(trainNameBox);

				int trainIconWidth = getTrainIconWidth(imminentTrain);
				int targetPos = background.getWidth() / 2 - trainIconWidth / 2;
				if (trainIconWidth > 130)
					targetPos -= trainIconWidth - 130;
				float f = (float) (imminentTrain.navigation.distanceToDestination / 15f);
				if (trainPresent())
					f = 0;
				trainPosition.startWithValue(targetPos - (targetPos + 5) * f);
			}
			return;
		}

		int trainIconWidth = getTrainIconWidth(train);
		int targetPos = background.getWidth() / 2 - trainIconWidth / 2;
		if (trainIconWidth > 130)
			targetPos -= trainIconWidth - 130;

		if (leavingAnimation > 0) {
			colorTypeScroll.visible = false;
			colorTypeScroll.active = false;
			disassembleTrainButton.active = false;
			float f = 1 - (leavingAnimation / 80f);
			trainPosition.setValue(targetPos + f * f * f * (background.getWidth() - targetPos + 5));
			leavingAnimation--;
			if (leavingAnimation > 0)
				return;

			displayedTrain = new WeakReference<>(null);
			disassembleTrainButton.visible = false;
			dropScheduleButton.active = false;
			dropScheduleButton.visible = false;
			return;
		}

		if (getImminent() != train) {
			leavingAnimation = 80;
			return;
		}

		boolean trainAtStation = trainPresent();
		disassembleTrainButton.active =
			trainAtStation && blockEntity.trainCanDisassemble && blockEntity.edgePoint.isOrthogonal();
		dropScheduleButton.active = blockEntity.trainHasSchedule;

		if (blockEntity.trainHasSchedule)
			dropScheduleButton.setToolTip(CreateLang.translateDirect(
				blockEntity.trainHasAutoSchedule ? "station.remove_auto_schedule" : "station.remove_schedule"));
		else
			dropScheduleButton.getToolTip()
				.clear();

		float f = trainAtStation ? 0 : (float) (train.navigation.distanceToDestination / 30f);
		trainPosition.setValue(targetPos - (targetPos + trainIconWidth) * f);
	}

	private int nameBoxX(String s, EditBox nameBox) {
		return left + background.getWidth() / 2 - (Math.min(font.width(s), nameBox.getWidth()) + 10) / 2;
	}

	private void updateAssemblyTooltip(String key) {
		if (key == null) {
			disassembleTrainButton.setToolTip(CreateLang.translateDirect("station.disassemble_train"));
			newTrainButton.setToolTip(CreateLang.translateDirect("station.create_train"));
			return;
		}
		for (IconButton ib : new IconButton[]{disassembleTrainButton, newTrainButton}) {
			List<Component> toolTip = ib.getToolTip();
			toolTip.clear();
			toolTip.add(CreateLang.translateDirect("station." + key)
				.withStyle(ChatFormatting.GRAY));
			toolTip.add(CreateLang.translateDirect("station." + key + "_1")
				.withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	protected void renderWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		String text = nameBox.getValue();

		if (!nameBox.isFocused())
			AllGuiTextures.STATION_EDIT_NAME.render(graphics, nameBoxX(text, nameBox) + font.width(text) + 5, top + 1);

		graphics.item(AllBlocks.TRAIN_DOOR.asStack(), left + 14, top + 103);

		Train train = displayedTrain.get();
		if (train == null) {
			MutableComponent header = CreateLang.translateDirect("station.idle");
			graphics.text(font, header, left + 97 - font.width(header) / 2, top + 47, opaque(0x7A7A7A), false);
			return;
		}

		float position = trainPosition.getValue(partialTicks);
		TrainIconType icon = train.icon;
		int offset = 0;

		List<Carriage> carriages = train.carriages;
		for (int i = carriages.size() - 1; i > 0; i--) {
			Carriage carriage = carriages.get(blockEntity.trainBackwards ? carriages.size() - i - 1 : i);
			offset += icon.render(carriage.bogeySpacing, graphics, left + Math.round(position) + offset, top + 20) + 1;
		}

		offset += icon.render(TrainIconType.ENGINE, graphics, left + Math.round(position) + offset, top + 20);

		AllGuiTextures.STATION_TEXTBOX_TOP.render(graphics, left + 21, top + 42);
		UIRenderHelper.drawStretched(graphics, left + 21, top + 60, 150, 26, AllGuiTextures.STATION_TEXTBOX_MIDDLE);
		AllGuiTextures.STATION_TEXTBOX_BOTTOM.render(graphics, left + 21, top + 86);
		AllGuiTextures.STATION_TEXTBOX_SPEECH.render(graphics, left + Mth.clamp(Math.round(position) + offset - 13, 25, 159),
			top + 38);

		text = trainNameBox.getValue();
		if (!trainNameBox.isFocused()) {
			int buttonX = nameBoxX(text, trainNameBox) + font.width(text) + 5;
			AllGuiTextures.STATION_EDIT_TRAIN_NAME.render(graphics, Math.min(buttonX, left + 156), top + 44);
			if (font.width(text) > trainNameBox.getWidth())
				graphics.text(font, "...", left + 26, top + 47, opaque(0xa6a6a6));
		}

		if (!mapModsPresent())
			return;

		AllGuiTextures sprite = AllGuiTextures.TRAINMAP_SPRITES;
		int trainColorIndex = colorTypeScroll.getState();
		int colorRow = trainColorIndex / 4;
		int colorCol = trainColorIndex % 4;
		int rotation = (AnimationTickHolder.getTicks() / 5) % 8;

		for (int slice = 0; slice < 3; slice++) {
			int row = slice == 0 ? 1 : slice == 2 ? 2 : 3;
			int col = rotation;
			int positionX = colorTypeScroll.getX() + 4;
			int positionY = colorTypeScroll.getY() - 1;
			int sheetX = col * 16 + colorCol * 128;
			int sheetY = row * 16 + colorRow * 64;

			graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.location, positionX, positionY, sheetX, sheetY, 16, 16,
				sprite.getWidth(), sprite.getHeight());
		}
	}

	public boolean mapModsPresent() {
		return Mods.FTBCHUNKS.isLoaded() || Mods.JOURNEYMAP.isLoaded() || Mods.XAEROWORLDMAP.isLoaded();
	}

	private static int opaque(int color) {
		return (color & 0xff000000) == 0 ? color | 0xff000000 : color;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean consumed = super.mouseClicked(event, doubleClick);
		double mouseX = event.x();
		double mouseY = event.y();
		if (!nameBox.isFocused() && mouseY > top && mouseY < top + 14 && mouseX > left
			&& mouseX < left + background.getWidth()) {
			nameBox.setFocused(true);
			nameBox.setHighlightPos(0);
			setFocused(nameBox);
			return true;
		}
		if (trainNameBox.active && !trainNameBox.isFocused() && mouseY > top + 45 && mouseY < top + 58
			&& mouseX > left + 25 && mouseX < left + 168) {
			trainNameBox.setFocused(true);
			trainNameBox.setHighlightPos(0);
			setFocused(trainNameBox);
			return true;
		}
		return consumed;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		boolean hitEnter = getFocused() instanceof EditBox && (event.key() == 257 || event.key() == 335);

		if (hitEnter && nameBox.isFocused()) {
			nameBox.setFocused(false);
			syncStationName();
			return true;
		}

		if (hitEnter && trainNameBox.isFocused()) {
			trainNameBox.setFocused(false);
			syncTrainNameAndColor();
			return true;
		}

		return super.keyPressed(event);
	}

	private void syncTrainNameAndColor() {
		Train train = displayedTrain.get();
		if (train != null)
			ClientNetworkHelper.INSTANCE.sendToServer(
				new TrainEditPacket.Serverbound(train.id, trainNameBox.getValue(), train.icon.getId(),
					colorTypeScroll.getState()));
	}

	private void syncStationName() {
		String value = nameBox.getValue();
		if (!value.equals(station.name)) {
			station.name = value;
			ClientNetworkHelper.INSTANCE.sendToServer(
				StationEditPacket.configure(blockEntity.getBlockPos(), false, value, doorControl));
		}
	}

	@Override
	public void removed() {
		super.removed();
		if (nameBox == null || trainNameBox == null)
			return;
		station.name = nameBox.getValue();
		ClientNetworkHelper.INSTANCE.sendToServer(StationEditPacket.configure(blockEntity.getBlockPos(),
			switchingToAssemblyMode, nameBox.getValue(), doorControl));
		Train train = displayedTrain.get();
		if (train == null)
			return;
		if (!switchingToAssemblyMode)
			ClientNetworkHelper.INSTANCE.sendToServer(
				new TrainEditPacket.Serverbound(train.id, trainNameBox.getValue(), train.icon.getId(),
					train.mapColorIndex));
		else
			blockEntity.imminentTrain = null;
	}

	@Override
	protected PartialModel getFlag(float partialTicks) {
		return blockEntity.flag.getValue(partialTicks) > 0.75f ? AllPartialModels.STATION_ON
			: AllPartialModels.STATION_OFF;
	}

}
