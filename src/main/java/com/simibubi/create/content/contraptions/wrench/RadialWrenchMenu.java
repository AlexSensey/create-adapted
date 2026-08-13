package com.simibubi.create.content.contraptions.wrench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlock;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.UIRenderHelper;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Property;

public class RadialWrenchMenu extends AbstractSimiScreen {

	public static final Map<Property<?>, String> VALID_PROPERTIES = new HashMap<>();
	public static final Set<Identifier> BLOCK_BLACKLIST = new HashSet<>();

	static {
		registerRotationProperty(RotatedPillarKineticBlock.AXIS, "Axis");
		registerRotationProperty(DirectionalKineticBlock.FACING, "Facing");
		registerRotationProperty(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, "Axis");
		registerRotationProperty(HorizontalKineticBlock.HORIZONTAL_FACING, "Facing");
		registerRotationProperty(HopperBlock.FACING, "Facing");
		registerRotationProperty(DirectedDirectionalBlock.TARGET, "Target");
		registerRotationProperty(SequencedGearshiftBlock.VERTICAL, "Vertical");
		registerBlacklistedBlock(AllBlocks.LARGE_WATER_WHEEL.getId());
		registerBlacklistedBlock(AllBlocks.WATER_WHEEL_STRUCTURAL.getId());
	}

	public static void registerRotationProperty(Property<?> property, String label) {
		VALID_PROPERTIES.putIfAbsent(property, label);
	}

	public static void registerBlacklistedBlock(Identifier location) {
		BLOCK_BLACKLIST.add(location);
	}

	public static Optional<RadialWrenchMenu> tryCreateFor(BlockState state, BlockPos pos, Level level) {
		if (BLOCK_BLACKLIST.contains(RegisteredObjectsHelper.getKeyOrThrow(state.getBlock())))
			return Optional.empty();
		List<Map.Entry<Property<?>, String>> properties = VALID_PROPERTIES.entrySet()
			.stream()
			.filter(entry -> state.hasProperty(entry.getKey()))
			.toList();
		return properties.isEmpty() ? Optional.empty() : Optional.of(new RadialWrenchMenu(state, pos, level, properties));
	}

	private final BlockState originalState;
	private final BlockPos pos;
	private final Level level;
	private final List<Map.Entry<Property<?>, String>> properties;
	private List<BlockState> states = List.of();
	private int propertyIndex;
	private int selectedStateIndex;
	private int ticksOpen;

	private RadialWrenchMenu(BlockState state, BlockPos pos, Level level,
		List<Map.Entry<Property<?>, String>> properties) {
		super(CreateLang.translateDirect("radial_wrench_menu"));
		this.originalState = state;
		this.pos = pos;
		this.level = level;
		this.properties = properties;
		initProperty();
	}

	private void initProperty() {
		Property<?> property = properties.get(propertyIndex).getKey();
		List<BlockState> result = new ArrayList<>();
		BlockState current = originalState;
		for (int safety = 0; safety < 100 && !result.contains(current); safety++) {
			result.add(current);
			current = current.cycle(property);
		}
		states = result;
		selectedStateIndex = 0;
	}

	@Override
	public void tick() {
		ticksOpen++;
		if (!level.getBlockState(pos).is(originalState.getBlock()))
			onClose();
		super.tick();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		float fade = Mth.clamp((ticksOpen + AnimationTickHolder.getPartialTicks()) / 10f, 0, 1);
		int alpha = (int) (Math.min(1, (ticksOpen + AnimationTickHolder.getPartialTicks()) / 20f) * 144);
		graphics.fillGradient(0, 0, width, height, alpha << 24, alpha << 24);

		int cx = width / 2;
		int cy = height / 2;
		int count = states.size();
		float dx = mouseX - cx;
		float dy = mouseY - cy;
		if (dx * dx + dy * dy > 42 * 42 && count > 0) {
			float sector = 360f / count;
			double angle = Math.toDegrees(Math.atan2(dy, dx));
			selectedStateIndex = Mth.floor((angle + 90 + sector / 2 + 360) % 360 / sector);
		}

		float radius = 82 - 12 * (1 - fade) * (1 - fade);
		Property<?> property = properties.get(propertyIndex).getKey();
		float sector = 360f / count;
		var pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		for (int i = 0; i < count; i++) {
			boolean selected = i == selectedStateIndex;
			float start = i * sector - sector / 2 - 90;
			Color inner = selected ? new Color(118, 103, 31, 180) : new Color(255, 255, 255, 18);
			Color outer = selected ? new Color(238, 199, 55, 220) : new Color(255, 255, 255, 76);
			UIRenderHelper.drawRadialSector(graphics, 50, 110, start + .7f, sector - 1.4f, inner, outer);
			if (selected)
				UIRenderHelper.drawRadialSector(graphics, 112, 116, start + .7f, sector - 1.4f,
					new Color(255, 224, 86, 210), new Color(255, 224, 86, 120));
		}
		pose.popMatrix();

		// Block previews are pictures-in-picture in 26.2. Put them on a later GUI
		// stratum so the radial sectors cannot cover them.
		graphics.nextStratum();
		var player = Minecraft.getInstance().player;
		float previewXRot = player == null ? 30 : player.getXRot();
		float previewYRot = player == null ? 225 : player.getYRot() + 180;
		for (int i = 0; i < count; i++) {
			double angle = Math.toRadians(i * 360d / count);
			int x = Math.round(cx + (float) Math.sin(angle) * radius);
			int y = Math.round(cy - (float) Math.cos(angle) * radius);
			boolean selected = i == selectedStateIndex;
			BlockState previewState = states.get(i);
			if (previewState.getBlock() instanceof BedBlock) {
				var directionToOtherHalf = previewState.getValue(BedBlock.PART) == BedPart.FOOT
					? previewState.getValue(BedBlock.FACING)
					: previewState.getValue(BedBlock.FACING).getOpposite();
				float halfX = directionToOtherHalf.getStepX() * .5f;
				float halfZ = directionToOtherHalf.getStepZ() * .5f;
				BlockState otherHalf = previewState.setValue(BedBlock.PART,
					previewState.getValue(BedBlock.PART) == BedPart.FOOT ? BedPart.HEAD : BedPart.FOOT);
				GuiGameElement.beginBlockModelBatch(previewXRot, previewYRot, 0);
				GuiGameElement.of(previewState)
					.atLocal(-halfX, 0, -halfZ)
					.scale(24)
					.at(x - 12, y + 12, 100)
					.submit(graphics);
				GuiGameElement.of(otherHalf)
					.atLocal(halfX, 0, halfZ)
					.scale(24)
					.at(x - 12, y + 12, 100)
					.submit(graphics);
				GuiGameElement.endModelBatch(graphics);
			} else {
				BlockEntity previewBlockEntity = previewState.getBlock() instanceof EntityBlock entityBlock
					? entityBlock.newBlockEntity(pos, previewState)
					: null;
				if (previewBlockEntity != null)
					previewBlockEntity.setLevel(level);
				GuiGameElement.of(previewState, previewBlockEntity)
					.rotateBlock(previewXRot, previewYRot, 0)
					.scale(24)
					.at(x - 12, y + 12, 100)
					.submit(graphics);
			}
			if (selected) {
				String value = states.get(i).getValue(property).toString();
				graphics.centeredText(font, value, x, y + 23, 0xffffffff);
			}
		}

		graphics.centeredText(font, "Currently", cx, cy - 14, 0xffdddddd);
		graphics.centeredText(font, "Changing:", cx, cy - 4, 0xffdddddd);
		graphics.centeredText(font, properties.get(propertyIndex).getValue(), cx, cy + 7, 0xffffd966);
		if (propertyIndex > 0)
			graphics.centeredText(font, "↑  " + properties.get(propertyIndex - 1).getValue(), cx, cy - 39, 0xffaaaaaa);
		if (propertyIndex + 1 < properties.size())
			graphics.centeredText(font, "↓  " + properties.get(propertyIndex + 1).getValue(), cx, cy + 32, 0xffaaaaaa);

		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	private void submitChange() {
		BlockState selected = states.get(selectedStateIndex);
		if (selected != originalState)
			ClientNetworkHelper.INSTANCE.sendToServer(new RadialWrenchMenuSubmitPacket(pos, selected));
		onClose();
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (AllKeys.ROTATE_MENU.getKeybind().isActiveAndMatches(InputConstants.getKey(event))) {
			submitChange();
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			submitChange();
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			onClose();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (properties.size() < 2)
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		int next = propertyIndex + (int) Math.signum(-scrollY);
		if (next < 0 || next >= properties.size())
			return false;
		propertyIndex = next;
		initProperty();
		return true;
	}

	@Override
	public void removed() {
		RadialWrenchHandler.COOLDOWN = 2;
		super.removed();
	}
}
