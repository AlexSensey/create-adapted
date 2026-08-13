package com.simibubi.create.content.trains.station;

import java.lang.ref.WeakReference;
import java.util.List;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class AbstractStationScreen extends AbstractSimiScreen {

	protected AllGuiTextures background;
	protected StationBlockEntity blockEntity;
	protected GlobalStation station;
	protected WeakReference<Train> displayedTrain;
	protected int left;
	protected int top;

	private IconButton confirmButton;

	public AbstractStationScreen(StationBlockEntity be, GlobalStation station) {
		super(be.getBlockState()
			.getBlock()
			.getName());
		this.blockEntity = be;
		this.station = station;
		displayedTrain = new WeakReference<>(null);
	}

	@Override
	protected void init() {
		layout();
		super.init();
		clearWidgets();

		confirmButton =
			new IconButton(left + background.getWidth() - 33, top + background.getHeight() - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(this::onClose);
		addRenderableWidget(confirmButton);
	}

	protected void layout() {
		left = (width - background.getWidth()) / 2;
		top = (height - background.getHeight()) / 2;
	}

	public int getTrainIconWidth(Train train) {
		TrainIconType icon = train.icon;
		List<Carriage> carriages = train.carriages;

		int w = icon.getIconWidth(TrainIconType.ENGINE);
		if (carriages.size() == 1)
			return w;

		for (int i = 1; i < carriages.size(); i++) {
			if (i == carriages.size() - 1 && train.doubleEnded) {
				w += icon.getIconWidth(TrainIconType.FLIPPED_ENGINE) + 1;
				break;
			}
			Carriage carriage = carriages.get(i);
			w += icon.getIconWidth(carriage.bogeySpacing) + 1;
		}

		return w;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		layout();
		background.render(graphics, left, top);
		renderWindow(graphics, mouseX, mouseY, partialTicks);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	protected void renderWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
	}

	protected abstract PartialModel getFlag(float partialTicks);

	protected Train getImminent() {
		return blockEntity.imminentTrain == null ? null : CreateClient.RAILWAYS.trains.get(blockEntity.imminentTrain);
	}

	protected boolean trainPresent() {
		return blockEntity.trainPresent;
	}

}
