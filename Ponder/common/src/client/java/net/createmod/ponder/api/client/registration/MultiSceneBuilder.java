package net.createmod.ponder.api.client.registration;

import java.util.function.Consumer;

import net.createmod.ponder.api.client.scene.PonderStoryBoard;
import net.minecraft.resources.Identifier;

public interface MultiSceneBuilder {
	MultiSceneBuilder addStoryBoard(Identifier schematicIdentifier,
									PonderStoryBoard storyBoard);

	MultiSceneBuilder addStoryBoard(Identifier schematicIdentifier,
									PonderStoryBoard storyBoard, Identifier... tags);

	MultiSceneBuilder addStoryBoard(Identifier schematicIdentifier,
									PonderStoryBoard storyBoard,
									Consumer<StoryBoardEntry> extras);

	MultiSceneBuilder addStoryBoard(String schematicPath, PonderStoryBoard storyBoard);

	MultiSceneBuilder addStoryBoard(String schematicPath, PonderStoryBoard storyBoard,
									Identifier... tags);

	MultiSceneBuilder addStoryBoard(String schematicPath, PonderStoryBoard storyBoard,
									Consumer<StoryBoardEntry> extras);
}
