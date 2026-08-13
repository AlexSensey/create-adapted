package dev.engine_room.flywheel.lib.model.baked;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.MapMaker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * A helper class for loading and accessing JSON models not directly used by any blocks or items.
 * <br>
 * Creating a PartialModel will make Minecraft automatically load the associated modelLocation.
 * <br>
 * Once Minecraft has finished baking all models, all PartialModels will have their bakedModel fields populated.
 */
public final class PartialModel {
	static final ConcurrentMap<Identifier, PartialModel> ALL = new MapMaker().weakValues().makeMap();
	private final Identifier modelLocation;
	final StandaloneModelKey<BlockStateModelPart> modelKey;

	private PartialModel(Identifier modelLocation) {
		this.modelLocation = modelLocation;
		ModelDebugName debugName = () -> "flywheel:partial/" + modelLocation;
		this.modelKey = new StandaloneModelKey<>(debugName);
	}

	public static PartialModel of(Identifier modelLocation) {
		return ALL.computeIfAbsent(modelLocation, PartialModel::new);
	}

	public BlockStateModelPart get() {
		return Minecraft.getInstance().getModelManager().getStandaloneModel(modelKey);
	}

	public Identifier modelLocation() {
		return modelLocation;
	}
}
