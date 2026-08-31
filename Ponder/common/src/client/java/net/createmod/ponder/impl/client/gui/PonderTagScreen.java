package net.createmod.ponder.impl.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.Window;

import net.createmod.catnip.api.client.gui.NavigatableSimiScreen;
import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.client.gui.element.BoxElement;
import net.createmod.catnip.api.client.gui.layout.LayoutHelper;
import net.createmod.catnip.api.client.gui.widget.BoxWidget;
import net.createmod.catnip.api.client.lang.ClientFontHelper;
import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.api.client.PonderIndex;
import net.createmod.ponder.api.client.registration.PonderTag;
import net.createmod.ponder.impl.client.gui.element.PonderButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class PonderTagScreen extends AbstractPonderScreen {
	private static final float MAIN_YMULT = 0.15f;

	private final PonderTag tag;
	protected final List<ItemEntry> items = new ArrayList<>();
	private final double itemXmult = 0.5;
	@Nullable
	protected Rect2i itemArea;

	private ItemStack hoveredItem = ItemStack.EMPTY;

	public PonderTagScreen(Identifier tagId) {
		this(PonderIndex.getTagAccess().getRegisteredTag(tagId).orElseThrow(
			() -> new NoSuchElementException("PonderTag " + tagId.toString())
		));
	}

	public PonderTagScreen(PonderTag tag) {
		// FIXME: title lang
		super(Component.literal(tag.getTitle()));
		this.tag = tag;
	}

	@Override
	protected void init() {
		super.init();

		// items
		items.clear();
		PonderIndex.getTagAccess()
			.getItems(tag)
			.stream()
			.map(key -> new ItemEntry(RegisteredObjectsHelper.getItemOrBlock(key), key))
			.filter(entry -> entry.item != null)
			.forEach(items::add);

		if (!tag.getMainItem().isEmpty())
			items.removeIf(entry -> entry.item == tag.getMainItem().getItem());

		int rowCount = Mth.clamp((int) Math.ceil(items.size() / 11d), 1, 3);
		LayoutHelper layout = LayoutHelper.centeredHorizontal(items.size(), rowCount, 28, 28, 8);
		itemArea = layout.getArea();
		int itemCenterX = (int) (width * itemXmult);
		int itemCenterY = getItemsY();

		for (ItemEntry entry : items) {
			PonderButton b = new PonderButton(itemCenterX + layout.getX() + 4, itemCenterY + layout.getY() + 4)
				.showing(new ItemStack(entry.item));

			if (PonderIndex.getSceneAccess().doScenesExistForId(entry.key)) {
				b.withCallback((mouseX, mouseY) -> {
					centerScalingOn(mouseX, mouseY);
					ScreenOpener.transitionTo(PonderUI.of(new ItemStack(entry.item), tag));
				});
			} else {
				b.withBorderColors(
					entry.key.getNamespace().equals("minecraft") ?
						PonderUI.MISSING_VANILLA_ENTRY :
						PonderUI.MISSING_MODDED_ENTRY
				).animateColors(false);
			}

			addRenderableWidget(b);
			layout.next();
		}

		if (!tag.getMainItem().isEmpty()) {
			Identifier registryName = RegisteredObjectsHelper.getKeyOrThrow(tag.getMainItem().getItem());

			PonderButton b = new PonderButton(itemCenterX - layout.getTotalWidth() / 2 - 48, itemCenterY - 10)
				.showing(tag.getMainItem());
			//b.withCustomBackground(PonderTheme.Key.PONDER_BACKGROUND_IMPORTANT.c());

			if (PonderIndex.getSceneAccess().doScenesExistForId(registryName)) {
				b.withCallback((mouseX, mouseY) -> {
					centerScalingOn(mouseX, mouseY);
					ScreenOpener.transitionTo(PonderUI.of(tag.getMainItem(), tag));
				});
			} else {
				b.withBorderColors(
					registryName.getNamespace().equals("minecraft") ?
						PonderUI.MISSING_VANILLA_ENTRY :
						PonderUI.MISSING_MODDED_ENTRY
				).animateColors(false);
			}

			addRenderableWidget(b);
		}

	}

	@Override
	protected void initBackTrackIcon(BoxWidget backTrack) {
		backTrack.showing(tag);
	}

	@Override
	public void tick() {
		super.tick();
		PonderUI.ponderTicks++;

		hoveredItem = ItemStack.EMPTY;
		Window w = minecraft.getWindow();
		int mX = (int) (this.minecraft.mouseHandler.xpos() * (double) w.getGuiScaledWidth() / (double) w.getScreenWidth());
		int mY = (int) (this.minecraft.mouseHandler.ypos() * (double) w.getGuiScaledHeight() / (double) w.getScreenHeight());
		for (GuiEventListener child : children()) {
			if (child == backTrack)
				continue;
			if (child instanceof PonderButton button)
				if (button.isMouseOver(mX, mY)) {
					hoveredItem = button.getItem();
				}
		}
	}

	@Override
	public void extractScaledRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractScaledRenderState(graphics, mouseX, mouseY, partialTicks);

		renderItems(graphics, mouseX, mouseY, partialTicks);

		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();
		poseStack.translate(width / 2f - 120f, height * MAIN_YMULT - 40f);

		int x = 31 + 20 + 8;
		int y = 31;

		String title = tag.getTitle();

		int streakHeight = 35;
		UIRenderHelper.streak(graphics, 0, x - 4, y - 12 + streakHeight / 2, streakHeight, 240);
		//PonderUI.renderBox(poseStack, 21, 21, 30, 30, false);
		new BoxElement()
			.withBackground(PonderUI.BACKGROUND_FLAT)
			.gradientBorder(PonderUI.COLOR_IDLE)
			.at(21, 21, 100)
			.withBounds(30, 30)
			.submit(graphics);

		graphics.text(font, Ponder.lang().translate(AbstractPonderScreen.PONDERING_TAG).component(), x, y - 6, UIRenderHelper.COLOR_TEXT_DARKER.getFirst().getRGB(), false);
		y += 8;
		x += 0;
		poseStack.pushMatrix();
		poseStack.translate(x, y);
		graphics.text(font, title, 0, 0, UIRenderHelper.COLOR_TEXT.getFirst().getRGB(), false);
		poseStack.popMatrix();

		poseStack.pushMatrix();
		poseStack.translate(23, 23);
		poseStack.scale(1.66f, 1.66f);
		tag.render(graphics, 0, 0);
		poseStack.popMatrix();
		poseStack.popMatrix();

		poseStack.pushMatrix();
		int w = (int) (width * .45);
		x = (width - w) / 2;
		y = getItemsY() - 10 + Math.max(itemArea.getHeight(), 48);

		FormattedText desc = FormattedText.of(tag.getDescription());
		int h = font.wordWrapHeight(desc, w);

		//PonderUI.renderBox(poseStack, x - 3, y - 3, w + 6, h + 6, false);
		new BoxElement()
			.withBackground(PonderUI.BACKGROUND_FLAT)
			.gradientBorder(PonderUI.COLOR_IDLE)
			.at(x - 3, y - 3, 90)
			.withBounds(w + 6, h + 6)
			.submit(graphics);

		ClientFontHelper.drawSplitString(graphics, font, desc, x, y, w, UIRenderHelper.COLOR_TEXT.getFirst().getRGB());
		poseStack.popMatrix();

		if (!hoveredItem.isEmpty()) {
			graphics.setTooltipForNextFrame(font, hoveredItem, mouseX, mouseY);
		}
	}

	protected void renderItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		if (items.isEmpty())
			return;

		int x = (int) (width * itemXmult);
		int y = getItemsY();

		String relatedTitle = Ponder.lang().translate(AbstractPonderScreen.ASSOCIATED).string();
		int stringWidth = font.width(relatedTitle);

		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();
		poseStack.translate(x, y);
		new BoxElement()
			.withBackground(PonderUI.BACKGROUND_FLAT)
			.gradientBorder(PonderUI.COLOR_IDLE)
			.at((-stringWidth) / 2f - 5, itemArea.getY() - 21, 100)
			.withBounds(stringWidth + 10, 10)
			.submit(graphics);

//		UIRenderHelper.streak(0, itemArea.getX() - 10, itemArea.getY() - 20, 20, 180, 0x101010);
		graphics.centeredText(font, relatedTitle, 0, itemArea.getY() - 20, UIRenderHelper.COLOR_TEXT.getFirst().getRGB());

		UIRenderHelper.streak(graphics, 0, 0, 0, itemArea.getHeight() + 10, itemArea.getWidth() / 2 + 75);
		UIRenderHelper.streak(graphics, 180, 0, 0, itemArea.getHeight() + 10, itemArea.getWidth() / 2 + 75);

		poseStack.popMatrix();

	}

	public int getItemsY() {
		return (int) (MAIN_YMULT * height + 85);
	}

	public ItemStack getHoveredTooltipItem() {
		return hoveredItem;
	}

	@Override
	public boolean isEquivalentTo(NavigatableSimiScreen other) {
		if (other instanceof PonderTagScreen)
			return tag == ((PonderTagScreen) other).tag;
		return super.isEquivalentTo(other);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	public PonderTag getTag() {
		return tag;
	}

	@Override
	public void removed() {
		super.removed();
		hoveredItem = ItemStack.EMPTY;
	}

	public record ItemEntry(@Nullable ItemLike item, Identifier key) {
	}
}
