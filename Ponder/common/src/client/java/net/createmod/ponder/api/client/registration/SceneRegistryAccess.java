package net.createmod.ponder.api.client.registration;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.createmod.ponder.api.client.scene.PonderScene;
import net.minecraft.resources.Identifier;

public interface SceneRegistryAccess {

	boolean doScenesExistForId(Identifier id);

	Collection<Map.Entry<Identifier, StoryBoardEntry>> getRegisteredEntries();

	List<PonderScene> compile(Identifier id);

	List<PonderScene> compile(Collection<StoryBoardEntry> entries);

}
