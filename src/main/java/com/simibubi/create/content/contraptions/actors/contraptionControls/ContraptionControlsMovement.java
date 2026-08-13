package com.simibubi.create.content.contraptions.actors.contraptionControls;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.createmod.catnip.api.animation.LerpedFloat;
import net.createmod.catnip.api.animation.LerpedFloat.Chaser;
import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.data.IntAttached;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;


public class ContraptionControlsMovement implements MovementBehaviour {

	@Override
	public ItemStack canBeDisabledVia(MovementContext context) {
		return null;
	}

	@Override
	public void startMoving(MovementContext context) {
		if (context.contraption instanceof ElevatorContraption && context.blockEntityData != null)
			context.blockEntityData.remove("Filter");
	}

	@Override
	public void stopMoving(MovementContext context) {
		ItemStack filter = getFilter(context);
		if (filter != null)
			context.blockEntityData.putBoolean("Disabled", context.contraption.isActorTypeDisabled(filter)
				|| context.contraption.isActorTypeDisabled(ItemStack.EMPTY));
	}

	public static boolean isSameFilter(ItemStack stack1, ItemStack stack2) {
		if (stack1.isEmpty() && stack2.isEmpty())
			return true;
		return ItemStack.isSameItemSameComponents(stack1, stack2);
	}

	public static ItemStack getFilter(MovementContext ctx) {
		CompoundTag blockEntityData = ctx.blockEntityData;
		if (blockEntityData == null)
			return null;
		Tag filterTag = blockEntityData.get("Filter");
		if (filterTag == null)
			return ItemStack.EMPTY;
		return ItemStack.OPTIONAL_CODEC.decode(ctx.world.registryAccess().createSerializationContext(NbtOps.INSTANCE), filterTag)
			.resultOrPartial(error -> Create.LOGGER.error("Failed to deserialize contraption controls filter: {}", error))
			.map(result -> result.getFirst())
			.orElse(ItemStack.EMPTY);
	}

	public static boolean isDisabledInitially(MovementContext ctx) {
		return ctx.blockEntityData != null && ctx.blockEntityData.getBooleanOr("Disabled", false);
	}

	@Override
	public void tick(MovementContext ctx) {
		if (!ctx.world.isClientSide())
			return;

		Contraption contraption = ctx.contraption;
		var blockEntity = contraption.getBlockEntityClientSide(ctx.localPos);

		if (!(contraption instanceof ElevatorContraption ec)) {
			if (!(blockEntity instanceof ContraptionControlsBlockEntity cbe))
				return;
			ItemStack filter = getFilter(ctx);
			int value =
				contraption.isActorTypeDisabled(filter) || contraption.isActorTypeDisabled(ItemStack.EMPTY) ? 4 * 45
					: 0;
			cbe.indicator.setValue(value);
			cbe.indicator.updateChaseTarget(value);
			cbe.tickAnimations();
			return;
		}

		if (!(ctx.temporaryData instanceof ElevatorFloorSelection)) {
			ElevatorFloorSelection selection = new ElevatorFloorSelection();
			initFloorSelectionAtCurrentTarget(selection, ec);
			ctx.temporaryData = selection;
		}

		ElevatorFloorSelection efs = (ElevatorFloorSelection) ctx.temporaryData;
		tickFloorSelection(efs, ec);

		if (!(blockEntity instanceof ContraptionControlsBlockEntity cbe))
			return;

		cbe.tickAnimations();

		int currentY = (int) Math.round(contraption.entity.getY() + ec.getContactYOffset());
		boolean atTargetY = ec.clientYTarget == currentY;

		LerpedFloat indicator = cbe.indicator;
		float currentIndicator = indicator.getChaseTarget();
		boolean below = atTargetY ? currentIndicator > 0 : ec.clientYTarget <= currentY;

		if (currentIndicator == 0 && !atTargetY) {
			int startingPoint = below ? 181 : -181;
			indicator.setValue(startingPoint);
			indicator.updateChaseTarget(startingPoint);
			cbe.tickAnimations();
			return;
		}

		int currentStage = Mth.floor(((currentIndicator % 360) + 360) % 360);
		if (!atTargetY || currentStage / 45 != 0) {
			float increment = currentStage / 45 == (below ? 4 : 3) ? 2.25f : 33.75f;
			indicator.chase(currentIndicator + (below ? increment : -increment), 45f, Chaser.LINEAR);
			return;
		}

		indicator.setValue(0);
		indicator.updateChaseTarget(0);
		return;
	}

	public static void tickFloorSelection(ElevatorFloorSelection efs, ElevatorContraption ec) {
		if (ec.namesList.isEmpty()) {
			efs.currentShortName = "X";
			efs.currentLongName = "No Floors";
			efs.currentIndex = 0;
			efs.targetYEqualsSelection = true;
			return;
		}

		efs.currentIndex = Mth.clamp(efs.currentIndex, 0, ec.namesList.size() - 1);
		IntAttached<Couple<String>> entry = ec.namesList.get(efs.currentIndex);
		efs.currentTargetY = entry.getFirst();
		efs.currentShortName = entry.getSecond()
			.getFirst();
		efs.currentLongName = entry.getSecond()
			.getSecond();
		efs.targetYEqualsSelection = efs.currentTargetY == ec.clientYTarget;

		if (ec.isTargetUnreachable(efs.currentTargetY))
			efs.currentLongName = CreateLang.translate("contraption.controls.floor_unreachable")
				.component()
				.getString();
	}

	public static void initFloorSelectionAtCurrentTarget(ElevatorFloorSelection efs, ElevatorContraption ec) {
		for (int i = 0; i < ec.namesList.size(); i++) {
			if (ec.namesList.get(i)
				.getFirst() != ec.clientYTarget)
				continue;
			efs.currentIndex = i;
			return;
		}
		efs.currentIndex = 0;
	}

	@Override
	public void renderInContraption(MovementContext ctx, VirtualRenderWorld renderWorld, ContraptionMatrices matrices,
		MultiBufferSource buffer) {
		ContraptionControlsRenderer.renderInContraption(ctx, renderWorld, matrices, buffer);
	}

	public static class ElevatorFloorSelection {
		public int currentIndex = 0;
		public int currentTargetY = 0;
		public boolean targetYEqualsSelection = true;
		public String currentShortName = "";
		public String currentLongName = "";
	}

}
