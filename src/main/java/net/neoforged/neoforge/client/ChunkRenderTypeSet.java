package net.neoforged.neoforge.client;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.renderer.rendertype.RenderType;

public class ChunkRenderTypeSet implements Iterable<RenderType> {
	public static ChunkRenderTypeSet all() {
		return new ChunkRenderTypeSet(List.of());
	}

	public static ChunkRenderTypeSet of(RenderType renderType) {
		return new ChunkRenderTypeSet(List.of(renderType));
	}

	public static ChunkRenderTypeSet union(Collection<ChunkRenderTypeSet> sets) {
		return new ChunkRenderTypeSet(sets.stream()
			.flatMap(set -> set.renderTypes.stream())
			.toList());
	}

	private final List<RenderType> renderTypes;

	private ChunkRenderTypeSet(List<RenderType> renderTypes) {
		this.renderTypes = renderTypes;
	}

	public boolean contains(RenderType renderType) {
		return renderTypes.isEmpty() || renderTypes.contains(renderType);
	}

	@Override
	public Iterator<RenderType> iterator() {
		return renderTypes.isEmpty() ? Collections.<RenderType>emptyList().iterator() : renderTypes.iterator();
	}
}
