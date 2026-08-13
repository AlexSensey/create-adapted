package com.simibubi.create.content.logistics.factoryBoard;

import static com.simibubi.create.foundation.gui.AllGuiTextures.FACTORY_GAUGE_BOTTOM;
import static com.simibubi.create.foundation.gui.AllGuiTextures.FACTORY_GAUGE_RECIPE;
import static com.simibubi.create.foundation.gui.AllGuiTextures.FACTORY_GAUGE_RESTOCK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The original Factory Gauge configuration screen, adapted to the 26.2
 * extract/submit GUI API.
 */
public class FactoryPanelScreen extends AbstractSimiScreen {

	private final FactoryPanelBehaviour behaviour;
	private final boolean restocker;

	private AddressEditBox addressBox;
	private ScrollInput promiseExpiration;
	private BigItemStack outputConfig;
	private List<BigItemStack> inputConfig;
	private List<FactoryPanelConnection> connections;

	private int left;
	private int top;
	private int windowWidth;
	private int windowHeight;
	private boolean sendReset;
	private boolean sendRedstoneReset;

	public FactoryPanelScreen(FactoryPanelBehaviour behaviour) {
		super(CreateLang.translateDirect("gui.factory_panel.title"));
		this.behaviour = behaviour;
		restocker = behaviour.panelBE().restocker;
		updateConfigs();
	}

	private void layout() {
		windowWidth = FACTORY_GAUGE_BOTTOM.getWidth();
		windowHeight =
			(restocker ? FACTORY_GAUGE_RESTOCK : FACTORY_GAUGE_RECIPE).getHeight()
				+ FACTORY_GAUGE_BOTTOM.getHeight();
		left = (width - windowWidth) / 2;
		top = (height - windowHeight) / 2;
	}

	private void updateConfigs() {
		connections = new ArrayList<>(behaviour.targetedBy.values());
		outputConfig = new BigItemStack(behaviour.getFilter(), behaviour.recipeOutput);
		inputConfig = connections.stream()
			.map(connection -> {
				FactoryPanelBehaviour source = FactoryPanelBehaviour.at((Level) minecraft.level, connection.from);
				return source == null
					? new BigItemStack(ItemStack.EMPTY, 0)
					: new BigItemStack(source.getFilter(), connection.amount);
			})
			.toList();
	}

	@Override
	protected void init() {
		layout();
		super.init();
		clearWidgets();

		String frogAddress = behaviour.getFrogAddress();
		addressBox = new AddressEditBox(this, font, left + 36,
			top + windowHeight - 51, 108, 10, false, frogAddress);
		addressBox.setValue(behaviour.recipeAddress);
		addressBox.setTextColor(0xFF555555);
		addressBox.setTextColorUneditable(0xFF777777);
		addressBox.setTextShadow(false);
		addRenderableWidget(addressBox);

		IconButton confirmButton =
			new IconButton(left + windowWidth - 33, top + windowHeight - 25, AllIcons.I_CONFIRM);
		confirmButton.withCallback(this::onClose);
		confirmButton.setToolTip(CreateLang.translate("gui.factory_panel.save_and_close").component());
		addRenderableWidget(confirmButton);

		IconButton deleteButton =
			new IconButton(left + windowWidth - 55, top + windowHeight - 25, AllIcons.I_TRASH);
		deleteButton.withCallback(() -> {
			sendReset = true;
			onClose();
		});
		deleteButton.setToolTip(CreateLang.translate("gui.factory_panel.reset").component());
		addRenderableWidget(deleteButton);

		promiseExpiration =
			new ScrollInput(left + 97, top + windowHeight - 24, 28, 16).withRange(-1, 31)
				.titled(CreateLang.translate("gui.factory_panel.promises_expire_title").component());
		promiseExpiration.setState(behaviour.promiseClearingInterval);
		addRenderableWidget(promiseExpiration);

		if (!restocker) {
			IconButton newInputButton = new IconButton(left + 31, top + 47, AllIcons.I_ADD);
			newInputButton.withCallback(() -> {
				sendConfiguration(null, false);
				FactoryPanelConnectionHandler.startConnection(behaviour);
				super.onClose();
			});
			newInputButton.setToolTip(CreateLang.translate("gui.factory_panel.connect_input").component());
			addRenderableWidget(newInputButton);

			IconButton relocateButton = new IconButton(left + 31, top + 67, AllIcons.I_MOVE_GAUGE);
			relocateButton.withCallback(() -> {
				sendConfiguration(null, false);
				FactoryPanelConnectionHandler.startRelocating(behaviour);
				super.onClose();
			});
			relocateButton.setToolTip(CreateLang.translate("gui.factory_panel.relocate").component());
			addRenderableWidget(relocateButton);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (inputConfig.size() != behaviour.targetedBy.size()) {
			updateConfigs();
			init();
		}
		addressBox.tick();
		promiseExpiration.titled(CreateLang
			.translate(promiseExpiration.getState() == -1
				? "gui.factory_panel.promises_do_not_expire"
				: "gui.factory_panel.promises_expire_title")
			.component());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		layout();
		renderWindow(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	private void renderWindow(GuiGraphicsExtractor graphics) {
		AllGuiTextures background = restocker ? FACTORY_GAUGE_RESTOCK : FACTORY_GAUGE_RECIPE;
		if (restocker)
			FACTORY_GAUGE_RECIPE.render(graphics, left, top - 16);
		background.render(graphics, left, top);
		FACTORY_GAUGE_BOTTOM.render(graphics, left, top + background.getHeight());

		Component screenTitle = CreateLang
			.translate(restocker ? "gui.factory_panel.title_as_restocker" : "gui.factory_panel.title_as_recipe")
			.component();
		graphics.text(font, screenTitle, left + 97 - font.width(screenTitle) / 2,
			top + (restocker ? -12 : 4), 0xFF3D3C48, false);

		for (int slot = 0; slot < inputConfig.size(); slot++)
			renderInput(graphics, slot, inputConfig.get(slot));

		if (restocker) {
			graphics.item(behaviour.getFilter(), left + 88, top + 12);
		} else {
			int outputX = left + 160;
			int outputY = top + 48;
			graphics.item(outputConfig.stack, outputX, outputY);
			graphics.itemDecorations(font, outputConfig.stack, outputX, outputY,
				Integer.toString(outputConfig.count));
		}

		int expiration = promiseExpiration.getState();
		graphics.text(font,
			Component.literal(expiration == -1 ? " /" : expiration == 0 ? "30s" : expiration + "m"),
			promiseExpiration.getX() + 3, promiseExpiration.getY() + 4, 0xFFEEEEEE, true);

		ItemStack box = PackageStyles.getDefaultBox();
		int promiseX = left + 68;
		int promiseY = top + windowHeight - 24;
		graphics.item(box, promiseX, promiseY);
		graphics.itemDecorations(font, box, promiseX, promiseY,
			Integer.toString(behaviour.getPromised()));

		if (!behaviour.targetedByLinks.isEmpty()) {
			int linkX = left + 9;
			int linkY = top + windowHeight - 24;
			AllGuiTextures.FROGPORT_SLOT.render(graphics, linkX - 1, linkY - 1);
			graphics.item(AllBlocks.REDSTONE_LINK.asStack(), linkX, linkY);
		}

	}

	private void renderInput(GuiGraphicsExtractor graphics, int slot, BigItemStack input) {
		int inputX = left + (restocker ? 88 : 68 + slot % 3 * 20);
		int inputY = top + (restocker ? 12 : 28) + slot / 3 * 20;
		graphics.item(input.stack, inputX, inputY);
		if (!restocker && !input.stack.isEmpty())
			graphics.itemDecorations(font, input.stack, inputX, inputY, Integer.toString(input.count));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			if (!restocker)
				for (int i = 0; i < connections.size(); i++) {
					int inputX = left + 68 + i % 3 * 20;
					int inputY = top + 28 + i / 3 * 20;
					if (inside(mouseX, mouseY, inputX, inputY, 16, 16)) {
						sendConfiguration(connections.get(i).from, false);
						return true;
					}
				}

			if (inside(mouseX, mouseY, left + 68, top + windowHeight - 24, 16, 16)) {
				sendConfiguration(null, true);
				return true;
			}

			if (!behaviour.targetedByLinks.isEmpty()
				&& inside(mouseX, mouseY, left + 9, top + windowHeight - 24, 16, 16)) {
				sendRedstoneReset = true;
				sendConfiguration(null, false);
				sendRedstoneReset = false;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (addressBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
			return true;

		int step = minecraft.player != null && minecraft.player.isShiftKeyDown() ? 10 : 1;
		if (!restocker)
			for (int i = 0; i < inputConfig.size(); i++) {
				int inputX = left + 68 + i % 3 * 20;
				int inputY = top + 28 + i / 3 * 20;
				if (inside(mouseX, mouseY, inputX, inputY, 16, 16)) {
					BigItemStack input = inputConfig.get(i);
					if (!input.stack.isEmpty())
						input.count = Mth.clamp(input.count + (int) Math.signum(scrollY) * step, 1, 64);
					return true;
				}
			}

		if (!restocker && inside(mouseX, mouseY, left + 160, top + 48, 16, 16)) {
			outputConfig.count = Mth.clamp(outputConfig.count + (int) Math.signum(scrollY) * step, 1, 64);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	@Override
	public void onClose() {
		sendConfiguration(null, false);
		super.onClose();
	}

	private void sendConfiguration(@Nullable FactoryPanelPosition removeConnection, boolean clearPromises) {
		Map<FactoryPanelPosition, Integer> inputs = new HashMap<>();
		for (int i = 0; i < inputConfig.size() && i < connections.size(); i++)
			inputs.put(connections.get(i).from, inputConfig.get(i).count);

		ClientNetworkHelper.INSTANCE.sendToServer(new FactoryPanelConfigurationPacket(
			behaviour.getPanelPosition(),
			addressBox == null ? behaviour.recipeAddress : addressBox.getValue(),
			inputs,
			behaviour.activeCraftingArrangement,
			outputConfig.count,
			promiseExpiration == null ? behaviour.promiseClearingInterval : promiseExpiration.getState(),
			removeConnection,
			clearPromises,
			sendReset,
			sendRedstoneReset));
	}
}
