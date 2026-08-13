package com.simibubi.create.content.equipment.symmetryWand;

import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.symmetryWand.mirror.CrossPlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.EmptyMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.PlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.TriplePlaneMirror;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class SymmetryWandScreen extends AbstractSimiScreen {
	private final AllGuiTextures background = AllGuiTextures.WAND_OF_SYMMETRY;
	private final ItemStack wand;
	private final InteractionHand hand;
	private SymmetryMirror currentElement;
	private ScrollInput areaAlign;
	private Label labelAlign;
	private int left;
	private int top;

	public SymmetryWandScreen(ItemStack wand, InteractionHand hand) {
		super(wand.getHoverName());
		this.wand = wand;
		this.hand = hand;
		currentElement = SymmetryWandItem.getMirror(wand);
		if (currentElement instanceof EmptyMirror)
			currentElement = new PlaneMirror(Vec3.ZERO);
	}

	@Override
	protected void init() {
		layout();
		super.init();
		Label labelType = new Label(left + 51, top + 28, CommonComponents.EMPTY).colored(0xffffffff).withShadow();
		labelAlign = new Label(left + 51, top + 50, CommonComponents.EMPTY).colored(0xffffffff).withShadow();
		int state = currentElement instanceof TriplePlaneMirror ? 2 : currentElement instanceof CrossPlaneMirror ? 1 : 0;
		ScrollInput areaType = new SelectionScrollInput(left + 45, top + 21, 109, 18)
			.forOptions(SymmetryMirror.getMirrors())
			.titled(CreateLang.translateDirect("gui.symmetryWand.mirrorType"))
			.writingTo(labelType)
			.setState(state)
			.calling(this::setMirrorType);
		initAlign();
		addRenderableWidget(labelAlign);
		addRenderableWidget(areaType);
		addRenderableWidget(labelType);
		IconButton confirm = new IconButton(left + background.getWidth() - 33,
			top + background.getHeight() - 24, AllIcons.I_CONFIRM);
		confirm.withCallback(this::onClose);
		addRenderableWidget(confirm);
	}

	private void layout() {
		left = (width - background.getWidth()) / 2 - 20;
		top = (height - background.getHeight()) / 2;
	}

	private void setMirrorType(int state) {
		Vec3 position = currentElement.getPosition();
		currentElement = switch (state) {
			case 1 -> new CrossPlaneMirror(position);
			case 2 -> new TriplePlaneMirror(position);
			default -> new PlaneMirror(position);
		};
		initAlign();
	}

	private void initAlign() {
		if (areaAlign != null)
			removeWidget(areaAlign);
		areaAlign = new SelectionScrollInput(left + 45, top + 43, 109, 18)
			.forOptions(currentElement.getAlignToolTips())
			.titled(CreateLang.translateDirect("gui.symmetryWand.orientation"))
			.writingTo(labelAlign)
			.setState(currentElement.getOrientationIndex())
			.calling(this::setMirrorOrientation);
		addRenderableWidget(areaAlign);
	}

	private void setMirrorOrientation(int orientation) {
		currentElement.setOrientation(orientation);
		wand.set(com.simibubi.create.AllDataComponents.SYMMETRY_WAND_ORIENTATION, orientation);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (areaAlign != null && areaAlign.isMouseOver(mouseX, mouseY))
			return areaAlign.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		layout();
		background.render(graphics, left, top);
		graphics.text(font, title, left + (background.getWidth() - font.width(title)) / 2, top + 4,
			0xff592424, false);
		renderMirrorPreview(graphics);
		GuiGameElement.of(wand).scale(4).rotate(-70, 20, 20)
			// 26.2 anchors scaled GUI items at their top-left instead of their centre.
			.at(left + background.getWidth() + 8, top + 18, 100)
			.submit(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	private void renderMirrorPreview(GuiGraphicsExtractor graphics) {
		final int selectorX = left + 20;
		final int selectorY = top + 24;
		final int selectorSize = 20;
		final int itemSize = 16;
		String type = currentElement instanceof TriplePlaneMirror ? "tripleplane"
			: currentElement instanceof CrossPlaneMirror ? "crossplane" : "plane";
		int orientation = currentElement instanceof TriplePlaneMirror ? 0 : currentElement.getOrientationIndex();
		ItemStack preview = new ItemStack(Items.STONE);
		preview.set(DataComponents.ITEM_MODEL,
			Create.asResource("symmetry_" + type + "_preview_" + orientation));
		GuiGameElement.of(preview)
			.at(selectorX + (selectorSize - itemSize) / 2,
				selectorY + (selectorSize - itemSize) / 2 - 1, 100)
			.submit(graphics);
	}

	@Override
	public void removed() {
		SymmetryWandItem.configureSettings(wand, currentElement);
		ClientNetworkHelper.INSTANCE.sendToServer(new ConfigureSymmetryWandPacket(hand, currentElement));
		super.removed();
	}
}
