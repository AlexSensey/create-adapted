package com.simibubi.create.content.kinetics.chainConveyor;

import java.util.List;

import com.simibubi.create.foundation.utility.RaycastHelper;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChainPackageInteractionHandler {

	public static boolean onUse() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null)
			return false;

		double range = minecraft.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1;
		Vec3 from = minecraft.player.getEyePosition();
		Vec3 to = RaycastHelper.getTraceTarget(minecraft.player, range, from);
		boolean[] success = { false };

		ChainConveyorPackage.physicsDataCache.get(minecraft.level)
			.asMap()
			.forEach((netId, data) -> {
				if (success[0] || data == null || data.targetPos == null || data.beReference == null)
					return;

				AABB bounds = new AABB(data.targetPos, data.targetPos).move(0, -.25, 0)
					.expandTowards(0, .5, 0)
					.inflate(.45);
				if (bounds.clip(from, to).isEmpty())
					return;

				ChainConveyorBlockEntity conveyor = data.beReference.get();
				if (conveyor == null || conveyor.isRemoved())
					return;

				for (ChainConveyorPackage pckg : conveyor.getLoopingPackages()) {
					if (pckg.netId != netId)
						continue;
					removePackage(conveyor, null, pckg);
					success[0] = true;
					return;
				}

				for (BlockPos connection : conveyor.connections) {
					List<ChainConveyorPackage> packages = conveyor.travellingPackages.get(connection);
					if (packages == null)
						continue;
					for (ChainConveyorPackage pckg : packages) {
						if (pckg.netId != netId)
							continue;
						removePackage(conveyor, connection, pckg);
						success[0] = true;
						return;
					}
				}
			});

		return success[0];
	}

	private static void removePackage(ChainConveyorBlockEntity conveyor, BlockPos connection,
		ChainConveyorPackage pckg) {
		ClientNetworkHelper.INSTANCE.sendToServer(new ChainPackageInteractionPacket(conveyor.getBlockPos(), connection,
			pckg.chainPosition, true));
	}
}
