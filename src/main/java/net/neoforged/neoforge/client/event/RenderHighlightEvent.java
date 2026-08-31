package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.createmod.catnip.impl.client.render.MultiBufferSource;
import net.minecraft.world.phys.BlockHitResult;

public class RenderHighlightEvent {
	public static class Block {
		private boolean canceled;

		public BlockHitResult getTarget() {
			return null;
		}

		public MultiBufferSource getMultiBufferSource() {
			return null;
		}

		public Camera getCamera() {
			return null;
		}

		public PoseStack getPoseStack() {
			return new PoseStack();
		}

		public void setCanceled(boolean canceled) {
			this.canceled = canceled;
		}

		public boolean isCanceled() {
			return canceled;
		}
	}
}
