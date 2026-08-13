package com.simibubi.create.content.schematics;

import java.util.Iterator;

import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;

import net.createmod.catnip.api.nbt.NBTHelper;
import net.createmod.catnip.api.registry.RegisteredObjectsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SchematicAndQuillItem extends Item {

	public SchematicAndQuillItem(Properties properties) {
		super(properties);
	}

	public static void replaceStructureVoidWithAir(CompoundTag nbt) {
		String air = RegisteredObjectsHelper.getKeyOrThrow(Blocks.AIR).toString();
		String structureVoid = RegisteredObjectsHelper.getKeyOrThrow(Blocks.STRUCTURE_VOID).toString();

		NBTHelper.iterateCompoundList(nbt.getListOrEmpty("palette"), entry -> {
			if (structureVoid.equals(entry.getStringOr("Name", "")))
				entry.putString("Name", air);
		});
	}

	public static void clampGlueBoxes(Level level, AABB aabb, CompoundTag nbt) {
		ListTag entities = nbt.getListOrEmpty("entities").copy();
		String glueEntityId = AllEntityTypes.SUPER_GLUE.getId().toString();

		for (Iterator<Tag> iterator = entities.iterator(); iterator.hasNext();) {
			Tag tag = iterator.next();
			if (!(tag instanceof CompoundTag entityEntry))
				continue;
			CompoundTag entityData = entityEntry.getCompoundOrEmpty("nbt");
			if (glueEntityId.equals(entityData.getStringOr("id", "")))
				iterator.remove();
		}

		for (SuperGlueEntity entity : SuperGlueEntity.collectCropped(level, aabb)) {
			Vec3 relativePosition =
				new Vec3(entity.getX() - aabb.minX, entity.getY() - aabb.minY, entity.getZ() - aabb.minZ);
			TagValueOutput output =
				TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			if (!entity.save(output))
				continue;

			BlockPos blockPos = BlockPos.containing(relativePosition);
			CompoundTag entityEntry = new CompoundTag();
			entityEntry.put("pos",
				newDoubleList(relativePosition.x, relativePosition.y, relativePosition.z));
			entityEntry.put("blockPos", newIntegerList(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
			entityEntry.put("nbt", output.buildResult());
			entities.add(entityEntry);
		}

		nbt.put("entities", entities);
	}

	private static ListTag newIntegerList(int... pValues) {
		ListTag listtag = new ListTag();
		for (int i : pValues)
			listtag.add(IntTag.valueOf(i));
		return listtag;
	}

	private static ListTag newDoubleList(double... pValues) {
		ListTag listtag = new ListTag();
		for (double d0 : pValues)
			listtag.add(DoubleTag.valueOf(d0));
		return listtag;
	}

}
