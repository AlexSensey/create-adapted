package net.createmod.ponder.api.client.scene;

@FunctionalInterface
public interface PonderStoryBoard {
	void program(SceneBuilder scene, SceneBuildingUtil util);
}
