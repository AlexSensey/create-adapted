package net.createmod.ponder.api.client.registration;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.Identifier;

public interface TagRegistryAccess {

	Optional<PonderTag> getRegisteredTag(Identifier tagIdentifier);

	List<PonderTag> getListedTags();

	Set<PonderTag> getTags(Identifier item);

	Set<Identifier> getItems(Identifier tag);

	Set<Identifier> getItems(PonderTag tag);

}
