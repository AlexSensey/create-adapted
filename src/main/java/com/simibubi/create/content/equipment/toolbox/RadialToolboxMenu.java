package com.simibubi.create.content.equipment.toolbox;

import static com.simibubi.create.content.equipment.toolbox.ToolboxInventory.STACKS_PER_COMPARTMENT;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public class RadialToolboxMenu extends AbstractSimiScreen {
	private static final int DEPOSIT = -7;
	private static final int UNEQUIP = -5;

	private State state;
	private int ticksOpen;
	private int hoveredSlot = -1;
	private boolean scrollMode;
	private int scrollSlot;
	private final List<ToolboxBlockEntity> toolboxes;
	private ToolboxBlockEntity selectedBox;

	public RadialToolboxMenu(List<ToolboxBlockEntity> toolboxes, State state,
		@Nullable ToolboxBlockEntity selectedBox) {
		super(Component.empty());
		this.toolboxes = toolboxes;
		this.state = state;
		this.selectedBox = selectedBox;
	}

	public void prevSlot(int slot) {
		scrollSlot = slot;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		float fade = Mth.clamp((ticksOpen + AnimationTickHolder.getPartialTicks()) / 10f, 1 / 512f, 1);
		int alpha = (int) (Math.min(1, (ticksOpen + AnimationTickHolder.getPartialTicks()) / 20f) * 112);
		graphics.fillGradient(0, 0, width, height, alpha << 24, alpha << 24);

		int cx = width / 2;
		int cy = height / 2;
		float dx = mouseX - cx;
		float dy = mouseY - cy;
		float distance = dx * dx + dy * dy;
		hoveredSlot = -1;
		if (distance > 25 && distance < 10000)
			hoveredSlot = Mth.floor((Math.toDegrees(Math.atan2(dy, dx)) + 517.5) % 360) / 45;
		boolean centerSlot = state == State.SELECT_ITEM_UNEQUIP;
		if (scrollMode && distance > 150)
			scrollMode = false;
		if (centerSlot && distance <= 150)
			hoveredSlot = UNEQUIP;

		Component tip = state == State.DETACH ? CreateLang.translateDirect("toolbox.outOfRange") : null;
		if (state == State.DETACH) {
			if (dx > -20 && dx < 20 && dy > -80 && dy < -20)
				hoveredSlot = UNEQUIP;
			AllGuiTextures.TOOLBELT_INACTIVE_SLOT.render(graphics, cx - 12, cy - 12);
			GuiGameElement.of(AllBlocks.TOOLBOXES.get(DyeColor.BROWN).asStack()).at(cx - 9, cy - 9, 100).submit(graphics);
			int y = Math.round(cy - 40 + 10 * (1 - fade) * (1 - fade));
			AllGuiTextures.TOOLBELT_SLOT.render(graphics, cx - 12, y - 12);
			AllIcons.I_DISABLE.render(graphics, cx - 9, y - 9);
			if (!scrollMode && hoveredSlot == UNEQUIP) {
				AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, cx - 13, y - 13);
				tip = CreateLang.translateDirect("toolbox.detach").withStyle(ChatFormatting.GOLD);
			}
		} else {
			if (dx > 60 && dx < 100 && dy > -20 && dy < 20)
				hoveredSlot = DEPOSIT;
			int depositX = Math.round(cx + 80 - 5 * (1 - fade) * (1 - fade));
			AllGuiTextures.TOOLBELT_SLOT.render(graphics, depositX - 12, cy - 12);
			AllIcons.I_TOOLBOX.render(graphics, depositX - 9, cy - 9);
			if (!scrollMode && hoveredSlot == DEPOSIT) {
				AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, depositX - 13, cy - 13);
				tip = CreateLang.translateDirect(state == State.SELECT_BOX ? "toolbox.depositAll" : "toolbox.depositBox")
					.withStyle(ChatFormatting.GOLD);
			}

			float radius = 40 - 10 * (1 - fade) * (1 - fade);
			for (int slot = 0; slot < 8; slot++) {
				double angle = Math.toRadians(slot * 45 - 45);
				int x = Math.round(cx + (float) Math.sin(angle) * radius) - 12;
				int y = Math.round(cy - (float) Math.cos(angle) * radius) - 12;
				if (state == State.SELECT_ITEM || state == State.SELECT_ITEM_UNEQUIP) {
					ToolboxInventory inv = selectedBox.inventory;
					ItemStack filter = inv.filters.get(slot);
					if (filter.isEmpty())
						AllGuiTextures.TOOLBELT_EMPTY_SLOT.render(graphics, x, y);
					else {
						boolean empty = inv.getStackInSlot(slot * STACKS_PER_COMPARTMENT).isEmpty();
						(empty ? AllGuiTextures.TOOLBELT_INACTIVE_SLOT : AllGuiTextures.TOOLBELT_SLOT)
							.render(graphics, x, y);
						GuiGameElement.of(filter).at(x + 3, y + 3, 100).submit(graphics);
						if (slot == (scrollMode ? scrollSlot : hoveredSlot) && !empty) {
							AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, x - 1, y - 1);
							tip = filter.getHoverName();
						}
					}
				} else if (slot < toolboxes.size()) {
					AllGuiTextures.TOOLBELT_SLOT.render(graphics, x, y);
					ToolboxBlockEntity box = toolboxes.get(slot);
					GuiGameElement.of(AllBlocks.TOOLBOXES.get(box.getColor()).asStack()).at(x + 3, y + 3, 100).submit(graphics);
					if (slot == (scrollMode ? scrollSlot : hoveredSlot)) {
						AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, x - 1, y - 1);
						tip = box.getDisplayName();
					}
				} else
					AllGuiTextures.TOOLBELT_EMPTY_SLOT.render(graphics, x, y);
			}

			if (centerSlot) {
				AllGuiTextures.TOOLBELT_SLOT.render(graphics, cx - 12, cy - 12);
				(scrollMode ? AllIcons.I_REFRESH : AllIcons.I_FLIP).render(graphics, cx - 9, cy - 9);
				if (!scrollMode && hoveredSlot == UNEQUIP) {
					AllGuiTextures.TOOLBELT_SLOT_HIGHLIGHT.render(graphics, cx - 13, cy - 13);
					tip = CreateLang.translateDirect("toolbox.unequip", minecraft.player.getMainHandItem().getHoverName())
						.withStyle(ChatFormatting.GOLD);
				}
			}
		}

		if (tip != null) {
			int color = ((int) (fade * 255) << 24) | 0xffffff;
			graphics.text(font, tip, cx - font.width(tip) / 2, height - 72, color, false);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	@Override public void tick() { ticksOpen++; super.tick(); }

	@Override
	public void removed() {
		super.removed();
		if (minecraft.player == null)
			return;
		int selected = scrollMode ? scrollSlot : hoveredSlot;
		if (selected == DEPOSIT) {
			if (state == State.SELECT_BOX)
				toolboxes.forEach(be -> ClientNetworkHelper.INSTANCE.sendToServer(new ToolboxDisposeAllPacket(be.getBlockPos())));
			else if (state != State.DETACH)
				ClientNetworkHelper.INSTANCE.sendToServer(new ToolboxDisposeAllPacket(selectedBox.getBlockPos()));
			return;
		}
		if (state == State.SELECT_BOX)
			return;
		if (state == State.DETACH) {
			if (selected == UNEQUIP)
				ClientNetworkHelper.INSTANCE.sendToServer(new ToolboxEquipPacket(null, selected,
					minecraft.player.getInventory().getSelectedSlot()));
			return;
		}
		if (selected == UNEQUIP) {
			ClientNetworkHelper.INSTANCE.sendToServer(new ToolboxEquipPacket(selectedBox.getBlockPos(), selected,
				minecraft.player.getInventory().getSelectedSlot()));
			return;
		}
		if (selected < 0)
			return;
		ToolboxInventory inv = selectedBox.inventory;
		if (inv.filters.get(selected).isEmpty() || inv.getStackInSlot(selected * STACKS_PER_COMPARTMENT).isEmpty())
			return;
		ClientNetworkHelper.INSTANCE.sendToServer(new ToolboxEquipPacket(selectedBox.getBlockPos(), selected,
			minecraft.player.getInventory().getSelectedSlot()));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		double dx = mouseX - width / 2d;
		double dy = mouseY - height / 2d;
		if (dx * dx + dy * dy > 150)
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		scrollMode = true;
		scrollSlot = (scrollSlot - Mth.sign(scrollY) + 8) % 8;
		for (int i = 0; i < 8; i++) {
			if ((state == State.SELECT_ITEM || state == State.SELECT_ITEM_UNEQUIP)
				&& !selectedBox.inventory.filters.get(scrollSlot).isEmpty()
				&& !selectedBox.inventory.getStackInSlot(scrollSlot * STACKS_PER_COMPARTMENT).isEmpty())
				break;
			if (state == State.SELECT_BOX && scrollSlot < toolboxes.size() || state == State.DETACH)
				break;
			scrollSlot = (scrollSlot - Mth.sign(scrollY) + 8) % 8;
		}
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int selected = scrollMode ? scrollSlot : hoveredSlot;
		if (event.button() == 0) {
			if (selected == DEPOSIT) { closeWithCooldown(); return true; }
			if (state == State.SELECT_BOX && selected >= 0 && selected < toolboxes.size()) {
				state = State.SELECT_ITEM; selectedBox = toolboxes.get(selected); return true;
			}
			if ((state == State.DETACH || state == State.SELECT_ITEM || state == State.SELECT_ITEM_UNEQUIP)
				&& (selected == UNEQUIP || selected >= 0)) { closeWithCooldown(); return true; }
		}
		if (event.button() == 1 && state == State.SELECT_ITEM && toolboxes.size() > 1) {
			state = State.SELECT_BOX; return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void closeWithCooldown() {
		onClose();
		ToolboxHandlerClient.COOLDOWN = 2;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		KeyMapping[] binds = minecraft.options.keyHotbarSlots;
		for (int i = 0; i < binds.length && i < 8; i++)
			if (binds[i].matches(event)) {
				scrollMode = true;
				scrollSlot = i;
				mouseClicked(new MouseButtonEvent(0, 0,
					new net.minecraft.client.input.MouseButtonInfo(0, event.modifiers())), false);
				return true;
			}
		return super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (AllKeys.TOOLBELT.getKeybind().isActiveAndMatches(InputConstants.getKey(event))) {
			onClose();
			return true;
		}
		return super.keyReleased(event);
	}

	public enum State { SELECT_BOX, SELECT_ITEM, SELECT_ITEM_UNEQUIP, DETACH }
}
