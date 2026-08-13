package com.simibubi.create.foundation.mixin.client;

import java.lang.ref.Reference;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.util.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.ContraptionHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public abstract class EntityContraptionInteractionMixin {
	@Shadow
	private Level level;

	@Shadow
	private Vec3 position;

	@Unique
	private Stream<AbstractContraptionEntity> create$getIntersectionContraptionsStream() {
		return ContraptionHandler.loadedContraptions.get(level)
			.values()
			.stream()
			.map(Reference::get)
			.filter(cEntity -> cEntity != null && cEntity.collidingEntities.containsKey((Entity) (Object) this));
	}

	@Unique
	private Set<AbstractContraptionEntity> create$getIntersectingContraptions() {
		Set<AbstractContraptionEntity> contraptions = create$getIntersectionContraptionsStream().collect(Collectors.toSet());

		contraptions.addAll(level.getEntitiesOfClass(AbstractContraptionEntity.class, ((Entity) (Object) this).getBoundingBox()
			.inflate(1f)));
		return contraptions;
	}

	@Unique
	private void create$forCollision(Vec3 worldPos, TriConsumer<Contraption, BlockState, BlockPos> action) {
		create$getIntersectingContraptions().forEach(cEntity -> {
			Vec3 localPos = ContraptionCollider.worldToLocalPos(worldPos, cEntity);

			BlockPos blockPos = BlockPos.containing(localPos);
			Contraption contraption = cEntity.getContraption();
			StructureTemplate.StructureBlockInfo info = contraption.getBlocks()
				.get(blockPos);

			if (info != null) {
				BlockState blockstate = info.state();
				action.accept(contraption, blockstate, blockPos);
			}
		});
	}

	// involves client-side view bobbing animation on contraptions
	@Inject(method = "move", at = @At(value = "TAIL"))
	private void create$onMove(MoverType mover, Vec3 movement, CallbackInfo ci) {
		if (!level.isClientSide())
			return;
		Entity self = (Entity) (Object) this;
		if (self.onGround())
			return;
		if (self.isPassenger())
			return;

		Vec3 worldPos = position.add(0, -0.2, 0);
		boolean onAtLeastOneContraption = create$getIntersectionContraptionsStream().anyMatch(cEntity -> {
			Vec3 localPos = ContraptionCollider.worldToLocalPos(worldPos, cEntity);

			BlockPos blockPos = BlockPos.containing(localPos);
			Contraption contraption = cEntity.getContraption();
			StructureTemplate.StructureBlockInfo info = contraption.getBlocks()
				.get(blockPos);

			if (info == null)
				return false;

			cEntity.registerColliding(self);
			return true;
		});

		if (!onAtLeastOneContraption)
			return;

		self.setOnGround(true);
		self.getPersistentData()
			.putBoolean("ContraptionGrounded", true);
	}
}
