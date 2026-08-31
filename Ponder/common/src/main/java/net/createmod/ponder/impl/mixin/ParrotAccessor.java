package net.createmod.ponder.impl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.parrot.Parrot.Variant;

@Mixin(Parrot.class)
public interface ParrotAccessor {
	@Invoker("setVariant")
	void ponder$setVariant(Variant variant);
}
