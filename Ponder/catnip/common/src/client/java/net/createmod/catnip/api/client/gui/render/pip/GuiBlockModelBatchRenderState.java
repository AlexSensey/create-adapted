package net.createmod.catnip.api.client.gui.render.pip;

import java.util.List;

import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.state.BlockState;

public record GuiBlockModelBatchRenderState(
	List<Entry> entries,
	Matrix3x2f pose,
	int x0, int y0, int x1, int y1,
	float scale,
	float globalXRot, float globalYRot, float globalZRot,
	boolean rotateAroundBlockCenter,
	@Nullable ScreenRectangle scissorArea,
	@Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
	public record Entry(
		@Nullable BlockState state,
		@Nullable BlockStateModelPart part,
		@Nullable TypedInstance<Fluid> fluid,
		float xLocal, float yLocal, float zLocal,
		float xRot, float yRot, float zRot,
		float localScale,
		float postX, float postY, float postZ,
		float minX, float minY, float minZ,
		float maxX, float maxY, float maxZ,
		boolean cullBackFaces,
		int color
	) {}
}
