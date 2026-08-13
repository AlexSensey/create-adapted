package com.simibubi.create.content.kinetics.deployer;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class DeployerMovingInteraction extends MovingInteractionBehaviour {

	@Override
	public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
		AbstractContraptionEntity contraptionEntity) {
		MutablePair<StructureBlockInfo, MovementContext> actor = contraptionEntity.getContraption().getActorAt(localPos);
		if (actor == null || actor.right == null)
			return false;

		MovementContext context = actor.right;
		ItemStack heldStack = player.getItemInHand(activeHand);
		if (heldStack.is(AllItems.WRENCH.get())) {
			String modeName = context.blockEntityData.getStringOr("Mode", DeployerBlockEntity.Mode.USE.name());
			DeployerBlockEntity.Mode mode;
			try {
				mode = DeployerBlockEntity.Mode.valueOf(modeName);
			} catch (IllegalArgumentException ignored) {
				mode = DeployerBlockEntity.Mode.USE;
			}
			context.blockEntityData.putString("Mode",
				(mode == DeployerBlockEntity.Mode.PUNCH ? DeployerBlockEntity.Mode.USE : DeployerBlockEntity.Mode.PUNCH).name());
			return true;
		}

		if (context.world.isClientSide())
			return true;
		DeployerFakePlayer fake = DeployerMovementBehaviour.getPlayer(context);
		if (fake == null)
			return false;

		ItemStack deployerItem = fake.getMainHandItem();
		player.setItemInHand(activeHand, deployerItem.copy());
		fake.setItemInHand(InteractionHand.MAIN_HAND, heldStack.copy());
		context.blockEntityData.put("HeldItem", DeployerMovementBehaviour.writeItemStack(heldStack, context.world));
		context.data.put("HeldItem", DeployerMovementBehaviour.writeItemStack(heldStack, context.world));
		return true;
	}
}
