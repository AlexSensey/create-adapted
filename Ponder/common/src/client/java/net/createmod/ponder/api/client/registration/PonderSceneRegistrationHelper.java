package net.createmod.ponder.api.client.registration;

import java.util.function.Function;

import net.createmod.ponder.api.client.scene.PonderStoryBoard;
import net.minecraft.resources.Identifier;

public interface PonderSceneRegistrationHelper<T> {
	<S> PonderSceneRegistrationHelper<S> withKeyFunction(Function<S, T> keyGen);

	StoryBoardEntry addStoryBoard(T component, Identifier schematicIdentifier, PonderStoryBoard storyBoard,
								  Identifier... tags);

	StoryBoardEntry addStoryBoard(T component, String schematicPath, PonderStoryBoard storyBoard,
								  Identifier... tags);

	MultiSceneBuilder forComponents(T... components);

	MultiSceneBuilder forComponents(Iterable<? extends T> components);

	Identifier asIdentifier(String path);
}
