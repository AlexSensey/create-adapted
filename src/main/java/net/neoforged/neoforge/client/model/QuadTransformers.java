package net.neoforged.neoforge.client.model;

import java.util.List;

import net.minecraft.client.renderer.block.model.BakedQuad;

public final class QuadTransformers {
	private QuadTransformers() {
	}

	public static QuadTransformer settingMaxEmissivity() {
		return quads -> {
		};
	}

	public interface QuadTransformer {
		void processInPlace(List<BakedQuad> quads);
	}
}
