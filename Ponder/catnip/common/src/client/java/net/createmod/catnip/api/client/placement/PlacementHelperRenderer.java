package net.createmod.catnip.api.client.placement;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.ApiStatus.Internal;

import net.createmod.catnip.api.client.ghostblock.GhostBlocks;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.createmod.catnip.api.placement.IPlacementHelper;
import net.createmod.catnip.api.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/// Custom renderer for a [IPlacementHelper].
@FunctionalInterface
public interface PlacementHelperRenderer {
	@Internal
	Map<IPlacementHelper, PlacementHelperRenderer> REGISTRY = new IdentityHashMap<>();

	/// The default renderer, showing a ghost block. Fallback when no override is registered.
	PlacementHelperRenderer DEFAULT = (helper, _, _, _, offset) -> displayGhost(helper, offset);

	void render(IPlacementHelper helper, BlockPos pos, BlockState state, BlockHitResult ray, PlacementOffset offset);

	/// @return the renderer for the given helper, or [#DEFAULT] if one isn't registered
	static PlacementHelperRenderer get(IPlacementHelper helper) {
		return REGISTRY.getOrDefault(helper, DEFAULT);
	}

	/// Register a renderer override for the given helper.
	///
	/// This is not required. Helpers will use [#DEFAULT] when one isn't specifically registered.
	static void register(IPlacementHelper helper, PlacementHelperRenderer renderer) {
		PlacementHelperRenderer existing = REGISTRY.get(helper);
		if (existing != null) {
			throw new IllegalStateException("Duplicate renderer registration: " + existing + " / " + renderer);
		}

		REGISTRY.put(helper, renderer);
	}

	static void renderArrow(Vec3 center, Vec3 target, Direction arrowPlane) {
		renderArrow(center, target, arrowPlane, 1D);
	}

	static void renderArrow(Vec3 center, Vec3 target, Direction arrowPlane, double distanceFromCenter) {
		Vec3 direction = target.subtract(center).normalize();
		Vec3 facing = arrowPlane.getUnitVec3();
		Vec3 start = center.add(direction);
		Vec3 offset = direction.scale(distanceFromCenter - 1);
		Vec3 offsetA = direction.cross(facing).normalize().scale(.25);
		Vec3 offsetB = facing.cross(direction).normalize().scale(.25);
		Vec3 endA = center.add(direction.scale(.75)).add(offsetA);
		Vec3 endB = center.add(direction.scale(.75)).add(offsetB);
		Outliner.getInstance().showLine("placementArrowA" + center + target, start.add(offset), endA.add(offset)).lineWidth(1 / 16f);
		Outliner.getInstance().showLine("placementArrowB" + center + target, start.add(offset), endB.add(offset)).lineWidth(1 / 16f);
	}

	static void displayGhost(IPlacementHelper helper, PlacementOffset offset) {
		if (!offset.hasGhostState())
			return;

		GhostBlocks.getInstance().showGhostState(helper, offset.getTransform().apply(offset.getGhostState()))
			.at(offset.getBlockPos())
			.breathingAlpha();
	}
}
