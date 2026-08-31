package net.createmod.ponder.impl.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.createmod.ponder.impl.client.tooltip.PonderTooltipHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

@Mixin(TooltipRenderUtil.class)
public final class TooltipRenderUtilMixin {
	@WrapOperation(
		method = "extractTooltipBackground",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
			ordinal = 1
		)
	)
	private static void modifyFrameWhenOpeningPonder(GuiGraphicsExtractor graphics, RenderPipeline renderPipeline, Identifier location,
													 int x, int y, int width, int height, Operation<Void> original) {
		float progress = PonderTooltipHandler.getVisualProgress();

		if (progress > 0) {
			Identifier initialSprite = initialFrameTexture(location);
			Identifier finalSprite = finalFrameTexture(location);
			if (spriteExists(graphics, initialSprite) && spriteExists(graphics, finalSprite)) {
				original.call(graphics, renderPipeline, initialSprite, x, y, width, height);
				graphics.blitSprite(renderPipeline, finalSprite, x, y, width, height, progress);
				return;
			}
		}

		original.call(graphics, renderPipeline, location, x, y, width, height);
	}

	@Unique
	private static Identifier initialFrameTexture(Identifier style) {
		return style.withPath(path -> path + "_ponder_initial");
	}

	@Unique
	private static Identifier finalFrameTexture(Identifier style) {
		return style.withPath(path -> path + "_ponder_final");
	}

	@Unique
	private static boolean spriteExists(GuiGraphicsExtractor graphics, Identifier sprite) {
		TextureAtlas atlas = ((GuiGraphicsExtractorAccessor) graphics).ponder$getGuiSprites();
		return atlas.getSprite(sprite) != atlas.missingSprite();
	}
}
