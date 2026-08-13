package com.simibubi.create.compat.jei.category.animations;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.gui.CustomLightingSettings;
import com.simibubi.create.foundation.model.CreateStandaloneModels;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import mezz.jei.api.gui.drawable.IDrawable;
import net.createmod.catnip.api.client.gui.ILightingSettings;
import net.createmod.catnip.api.client.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public abstract class AnimatedKinetics implements IDrawable {

	public int offset = 0;

	public static final ILightingSettings DEFAULT_LIGHTING = CustomLightingSettings.builder()
			.firstLightRotation(12.5f, -45.0f)
			.secondLightRotation(-20.0f, -50.0f)
			.build();

	/**
	 * <b>Only use this method outside of subclasses.</b>
	 * Use {@link #blockElement(BlockState)} if calling from inside a subclass.
	 */
	public static GuiGameElement.GuiRenderBuilder defaultBlockElement(BlockState state) {
		return GuiGameElement.of(state)
				.lighting(DEFAULT_LIGHTING);
	}

	/**
	 * <b>Only use this method outside of subclasses.</b>
	 * Use {@link #blockElement(PartialModel)} if calling from inside a subclass.
	 */
	public static GuiGameElement.GuiRenderBuilder defaultBlockElement(PartialModel partial) {
		StandaloneModelKey<BlockStateModelPart> key = standaloneKey(partial);
		BlockStateModelPart part = Minecraft.getInstance().getModelManager().getStandaloneModel(key);
		return GuiGameElement.of(part)
				.lighting(DEFAULT_LIGHTING);
	}

	private static StandaloneModelKey<BlockStateModelPart> standaloneKey(PartialModel partial) {
		if (partial == AllPartialModels.SHAFTLESS_COGWHEEL) return CreateStandaloneModels.SHAFTLESS_COGWHEEL;
		if (partial == AllPartialModels.MECHANICAL_PRESS_HEAD) return CreateStandaloneModels.MECHANICAL_PRESS_HEAD;
		if (partial == AllPartialModels.MECHANICAL_MIXER_POLE) return CreateStandaloneModels.MECHANICAL_MIXER_POLE;
		if (partial == AllPartialModels.MECHANICAL_MIXER_HEAD) return CreateStandaloneModels.MECHANICAL_MIXER_HEAD;
		if (partial == AllPartialModels.MILLSTONE_COG) return CreateStandaloneModels.MILLSTONE_COG;
		if (partial == AllPartialModels.DEPLOYER_POLE) return CreateStandaloneModels.DEPLOYER_POLE;
		if (partial == AllPartialModels.DEPLOYER_HAND_HOLDING) return CreateStandaloneModels.DEPLOYER_HAND_HOLDING;
		if (partial == AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE) return CreateStandaloneModels.SAW_BLADE_VERTICAL_ACTIVE;
		if (partial == AllPartialModels.SPOUT_TOP) return CreateStandaloneModels.SPOUT_TOP;
		if (partial == AllPartialModels.SPOUT_MIDDLE) return CreateStandaloneModels.SPOUT_MIDDLE;
		if (partial == AllPartialModels.SPOUT_BOTTOM) return CreateStandaloneModels.SPOUT_BOTTOM;
		if (partial == AllPartialModels.BLAZE_ACTIVE) return CreateStandaloneModels.BLAZE_ACTIVE;
		if (partial == AllPartialModels.BLAZE_SUPER) return CreateStandaloneModels.BLAZE_SUPER;
		if (partial == AllPartialModels.BLAZE_BURNER_RODS_2) return CreateStandaloneModels.BLAZE_BURNER_RODS_2;
		if (partial == AllPartialModels.BLAZE_BURNER_SUPER_RODS_2) return CreateStandaloneModels.BLAZE_BURNER_SUPER_RODS_2;
		if (partial == AllPartialModels.BLAZE_BURNER_FLAME) return CreateStandaloneModels.BLAZE_BURNER_FLAME;
		if (partial == AllPartialModels.ENCASED_FAN_INNER) return CreateStandaloneModels.ENCASED_FAN_INNER;
		throw new IllegalArgumentException("No standalone model registered for " + partial);
	}

	protected static float getAnimationTime() {
		// JEI screens can pause client ticks. Wall-clock render time keeps GUI machines moving
		// just like the pre-26.2 immediate-mode renderer did.
		return (float) ((System.nanoTime() / 50_000_000.0) % 36_000.0);
	}

	public static float getCurrentAngle() {
		return (getAnimationTime() * 4) % 360;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		GuiGameElement.beginModelBatch(getGlobalXRotation(), getGlobalYRotation(), getGlobalZRotation());
		try {
			drawAnimation(graphics, xOffset, yOffset);
		} finally {
			GuiGameElement.endModelBatch(graphics);
		}
	}

	protected float getGlobalXRotation() {
		return 0;
	}

	protected float getGlobalYRotation() {
		return 0;
	}

	protected float getGlobalZRotation() {
		return 0;
	}

	protected abstract void drawAnimation(GuiGraphicsExtractor graphics, int xOffset, int yOffset);

	protected BlockState shaft(Axis axis) {
		return AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, axis);
	}

	protected PartialModel cogwheel() {
		return AllPartialModels.SHAFTLESS_COGWHEEL;
	}

	protected GuiGameElement.GuiRenderBuilder blockElement(BlockState state) {
		return defaultBlockElement(state);
	}

	protected GuiGameElement.GuiRenderBuilder blockElement(PartialModel partial) {
		return defaultBlockElement(partial);
	}

	@Override
	public int getWidth() {
		return 50;
	}

	@Override
	public int getHeight() {
		return 50;
	}

}
