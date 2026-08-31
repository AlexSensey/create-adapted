package net.createmod.ponder.impl.client.registration;

import java.util.function.Consumer;

import net.createmod.ponder.api.client.registration.MultiSceneBuilder;
import net.createmod.ponder.api.client.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.client.registration.StoryBoardEntry;
import net.createmod.ponder.api.client.scene.PonderStoryBoard;
import net.minecraft.resources.Identifier;

public class GenericMultiSceneBuilder<T> implements MultiSceneBuilder {
	protected Iterable<? extends T> components;
	protected PonderSceneRegistrationHelper<T> helper;

	protected GenericMultiSceneBuilder(PonderSceneRegistrationHelper<T> helper, Iterable<? extends T> components) {
		this.helper = helper;
		this.components = components;
	}

	@Override
	public MultiSceneBuilder addStoryBoard(Identifier schematicIdentifier,
										   PonderStoryBoard storyBoard) {
		return addStoryBoard(schematicIdentifier, storyBoard, $ -> {
		});
	}

	@Override
	public MultiSceneBuilder addStoryBoard(Identifier schematicIdentifier,
										   PonderStoryBoard storyBoard,
										   Identifier... tags) {
		return addStoryBoard(schematicIdentifier, storyBoard, sb -> sb.highlightTags(tags));
	}

	@Override
	public MultiSceneBuilder addStoryBoard(Identifier schematicIdentifier,
										   PonderStoryBoard storyBoard,
										   Consumer<StoryBoardEntry> extras) {
		components.forEach(c -> extras.accept(helper.addStoryBoard(c, schematicIdentifier, storyBoard)));
		return this;
	}

	@Override
	public MultiSceneBuilder addStoryBoard(String schematicPath, PonderStoryBoard storyBoard) {
		return addStoryBoard(helper.asIdentifier(schematicPath), storyBoard);
	}

	@Override
	public MultiSceneBuilder addStoryBoard(String schematicPath, PonderStoryBoard storyBoard,
										   Identifier... tags) {
		return addStoryBoard(helper.asIdentifier(schematicPath), storyBoard, tags);
	}

	@Override
	public MultiSceneBuilder addStoryBoard(String schematicPath, PonderStoryBoard storyBoard,
										   Consumer<StoryBoardEntry> extras) {
		return addStoryBoard(helper.asIdentifier(schematicPath), storyBoard, extras);
	}
}
