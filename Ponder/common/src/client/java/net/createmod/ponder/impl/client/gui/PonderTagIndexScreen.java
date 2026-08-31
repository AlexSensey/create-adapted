package net.createmod.ponder.impl.client.gui;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.Window;

import net.createmod.catnip.api.client.gui.ScreenOpener;
import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.client.gui.element.BoxElement;
import net.createmod.catnip.api.client.gui.layout.LayoutHelper;
import net.createmod.catnip.api.client.gui.layout.PaginationState;
import net.createmod.catnip.api.client.gui.texture.CatnipGuiTextures;
import net.createmod.catnip.api.client.gui.widget.BoxWidget;
import net.createmod.catnip.api.client.lang.ClientFontHelper;
import net.createmod.catnip.api.client.lang.FontHelper;
import net.createmod.catnip.api.client.lang.FontHelper.Palette;
import net.createmod.catnip.api.platform.services.PlatformHelper;
import net.createmod.ponder.api.Ponder;
import net.createmod.ponder.api.client.PonderIndex;
import net.createmod.ponder.api.client.registration.PonderTag;
import net.createmod.ponder.impl.client.gui.element.PonderButton;
import net.createmod.ponder.impl.client.gui.element.PonderGuiTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class PonderTagIndexScreen extends AbstractPonderScreen {

	protected List<ModTagsEntry> currentModTagEntries = new LinkedList<>();
	protected List<Map.Entry<String, List<PonderTag>>> sortedModTags = List.of();
	protected PaginationState paginationState = new PaginationState();

	@Nullable
	protected PonderButton pageNext;
	@Nullable
	protected PonderButton pagePrev;

	@Nullable
	private PonderTag hoveredItem = null;

	// The main ponder entry point from menus.
	public PonderTagIndexScreen() {
		// FIXME: title lang
		super(Component.literal("Tag Index"));
	}

	@Override
	protected void init() {
		super.init();

		Map<String, List<PonderTag>> tagsByModID = PonderIndex.getTagAccess().getListedTags().stream().collect(Collectors.groupingBy(tag -> tag.getId().getNamespace()));
		sortedModTags = new TreeMap<>(tagsByModID).entrySet().stream().toList();

		int modCount = sortedModTags.size();
		int maxModsOnScreen = (height - 140 - 40) / 58;

		paginationState = new PaginationState(modCount > 1 && modCount > maxModsOnScreen, maxModsOnScreen, modCount);

		setupModTagEntries();

		if (!paginationState.usesPagination())
			return;

		int xOffset = (int) (width * 0.5);

		addRenderableWidget(pagePrev = new PonderButton(xOffset - 120, height - 32)
			.showing(CatnipGuiTextures.ICON_PONDER_LEFT)
			.withCallback(() -> {
				paginationState.previousPage();
				updateAfterPaginationChange();
			})
			.setActive(false)
		);

		pagePrev.updateGradientFromState();

		addRenderableWidget(pageNext = new PonderButton(xOffset + 100, height - 32)
			.showing(CatnipGuiTextures.ICON_PONDER_RIGHT)
			.withCallback(() -> {
				paginationState.nextPage();
				updateAfterPaginationChange();
			})
			.setActive(true)
		);

	}

	protected void setupModTagEntries() {
		this.children().stream().filter(widget -> {
			if (!(widget instanceof PonderButton ponderButton))
				return false;

			return ponderButton.getTag() != null;
		}).forEach(this::removeWidget);

		currentModTagEntries.clear();

		AtomicInteger yOffset = new AtomicInteger(140);
		int xOffset = (int) (width * 0.5);

		paginationState.iterateForCurrentPage((iPage, iOverall) -> {
			Map.Entry<String, List<PonderTag>> entry = sortedModTags.get(iOverall);
			String modName = PlatformHelper.INSTANCE.getModDisplayName(entry.getKey());
			List<PonderTag> tags = entry.getValue();

			LayoutHelper layout = LayoutHelper.centeredHorizontal(tags.size(), 1, 28, 28, 8);
			Rect2i layoutArea = layout.getArea();

			for (PonderTag tag : tags) {
				PonderButton button = new PonderButton(xOffset + layout.getX() + 4, yOffset.get() + layout.getY() + 18)
					.showingTag(tag)
					.withCallback((mouseX, mouseY) -> {
						centerScalingOn(mouseX, mouseY);
						ScreenOpener.transitionTo(new PonderTagScreen(tag));
					});
				addRenderableWidget(button);
				layout.next();
			}

			currentModTagEntries.add(new ModTagsEntry(
				modName,
				tags.size(),
				layoutArea,
				yOffset.get()
			));

			yOffset.addAndGet(58 + 10);
		});

		for (int i = 0; i < paginationState.getElementsPerPage(); i++) {
			if (paginationState.getStartIndex() + i >= sortedModTags.size())
				break;


		}
	}

	protected void updateAfterPaginationChange() {
		setupModTagEntries();

		pagePrev.<PonderButton>setActive(paginationState.hasPreviousPage()).animateGradientFromState();
		pageNext.<PonderButton>setActive(paginationState.hasNextPage()).animateGradientFromState();
	}

	@Override
	protected void initBackTrackIcon(BoxWidget backTrack) {
		backTrack.showing(CatnipGuiTextures.ICON_PONDER_IDENTIFY);
	}

	@Override
	public void tick() {
		super.tick();
		PonderUI.ponderTicks++;

		hoveredItem = null;
		Window w = minecraft.getWindow();
		double mouseX = minecraft.mouseHandler.xpos() * w.getGuiScaledWidth() / w.getScreenWidth();
		double mouseY = minecraft.mouseHandler.ypos() * w.getGuiScaledHeight() / w.getScreenHeight();
		for (GuiEventListener child : children()) {
			if (child == backTrack)
				continue;
			if (child instanceof PonderButton button)
				if (button.isMouseOver(mouseX, mouseY))
					hoveredItem = button.getTag();
		}
	}

	@Override
	public void extractScaledRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractScaledRenderState(graphics, mouseX, mouseY, partialTicks);

		if (hoveredItem != null) {
			List<Component> list = FontHelper.cutStringTextComponent(hoveredItem.getDescription(), Palette.ALL_GRAY);
			list.addFirst(Component.literal(hoveredItem.getTitle()));
			graphics.setComponentTooltipForNextFrame(font, list, mouseX, mouseY);
		}

		Matrix3x2fStack poseStack = graphics.pose();

		poseStack.pushMatrix();
		poseStack.translate(width / 2f, 30);

		//title, box for icon and streak
		poseStack.pushMatrix();
		poseStack.translate(-120, 0);

		String title = Ponder.lang().translate(AbstractPonderScreen.WELCOME).string();

		new BoxElement().withBackground(PonderUI.BACKGROUND_FLAT)
			.gradientBorder(PonderUI.COLOR_IDLE)
			.at(0, 0, 0)
			.withBounds(30, 30)
			.submit(graphics);

		PonderGuiTextures.LOGO.render(graphics, -1, -1);

		//34 = 30 bounds + 2 padding + 2 box width
		//-3 = 2 padding + 1 pixel of the box
		poseStack.translate(34, -3);

		int streakHeight = 36;
		UIRenderHelper.streak(graphics, 0, 0, (streakHeight / 2), streakHeight, 280);

		poseStack.scale(2f, 2f);
		graphics.text(font, title, 3, 5, UIRenderHelper.COLOR_TEXT.getFirst().getRGB(), false);

		poseStack.popMatrix();
		poseStack.translate(0, 50);
		poseStack.pushMatrix();
		//at the middle, 80px from the top now

		int maxWidth = (int) (width * .5f);
		Component desc = Ponder.lang().translate(AbstractPonderScreen.DESCRIPTION).component();

		int descWidth = font.width(desc);
		if (descWidth + 2 < maxWidth)
			maxWidth = descWidth + 2;

		int descHeight = font.wordWrapHeight(desc, maxWidth);

		poseStack.translate(-maxWidth / 2f, 0);

		new BoxElement().withBackground(PonderUI.BACKGROUND_FLAT)
			.gradientBorder(PonderUI.COLOR_IDLE)
			.at(-3, -3, 0)
			.withBounds(maxWidth + 6, descHeight + 5)
			.submit(graphics);

		ClientFontHelper.drawSplitString(graphics, font, desc, 0, 0, maxWidth, UIRenderHelper.COLOR_TEXT.getFirst().getRGB());
		poseStack.popMatrix();

		poseStack.translate(0, -80);
		//at the middle of top edge now

		for (ModTagsEntry entry : currentModTagEntries) {
			poseStack.pushMatrix();
			renderTagsEntry(graphics, entry);
			poseStack.popMatrix();
		}

		poseStack.popMatrix();

	}

	protected void renderTagsEntry(GuiGraphicsExtractor graphics, ModTagsEntry entry) {
		Matrix3x2fStack poseStack = graphics.pose();

		int layoutWidth = entry.layoutArea().getWidth();
		int layoutHeight = entry.layoutArea().getHeight();

		poseStack.translate(0, entry.yPos());

		String categories = Ponder.lang().translate(AbstractPonderScreen.CATEGORIES, entry.modName()).string();
		int stringWidth = font.width(categories);
		poseStack.pushMatrix();
		poseStack.translate(-stringWidth / 2f, -20);

		new BoxElement().withBackground(PonderUI.BACKGROUND_FLAT)
			.gradientBorder(PonderUI.COLOR_IDLE)
			.at(-3, -1, 0)
			.withBounds(stringWidth + 6, 10)
			.submit(graphics);

		graphics.text(font, categories, 0, 0, UIRenderHelper.COLOR_TEXT.getFirst().getRGB(), false);

		poseStack.popMatrix();

		int extraLength = Mth.clamp(entry.tagCount, 2, 8);

		UIRenderHelper.streak(graphics, 0, 0, layoutHeight / 2, layoutHeight + 6, layoutWidth / 2 + extraLength * 15);
		UIRenderHelper.streak(graphics, 180, 0, layoutHeight / 2, layoutHeight + 6, layoutWidth / 2 + extraLength * 15);

	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	@Override
	public void removed() {
		super.removed();
		hoveredItem = null;
	}

	public record ModTagsEntry(
		String modName,
		int tagCount,
		Rect2i layoutArea,
		int yPos
	) {
	}
}
