package com.simibubi.create.content.kinetics.fan.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.AllTags.AllFluidTags;
import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.LitBlazeBurnerBlock;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.mixin.accessor.ConcretePowderBlockAccessor;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.createmod.catnip.api.math.VecHelper;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class AllFanProcessingTypes {
	public static final BlastingType BLASTING = register("blasting", new BlastingType());
	public static final HauntingType HAUNTING = register("haunting", new HauntingType());
	public static final SmokingType SMOKING = register("smoking", new SmokingType());
	public static final SplashingType SPLASHING = register("splashing", new SplashingType());

	private static final Map<String, FanProcessingType> LEGACY_NAME_MAP;

	static {
		Object2ReferenceOpenHashMap<String, FanProcessingType> map = new Object2ReferenceOpenHashMap<>();
		map.put("BLASTING", BLASTING);
		map.put("HAUNTING", HAUNTING);
		map.put("SMOKING", SMOKING);
		map.put("SPLASHING", SPLASHING);
		map.trim();
		LEGACY_NAME_MAP = map;
	}

	private static <T extends FanProcessingType> T register(String name, T type) {
		return Registry.register(CreateBuiltInRegistries.FAN_PROCESSING_TYPE, Create.asResource(name), type);
	}

	@Internal
	public static void init() {}

	@Nullable
	public static FanProcessingType ofLegacyName(String name) {
		return LEGACY_NAME_MAP.get(name);
	}

	@Nullable
	public static FanProcessingType parseLegacy(String str) {
		FanProcessingType type = ofLegacyName(str);
		return type != null ? type : FanProcessingType.parse(str);
	}

	public static class BlastingType implements FanProcessingType {
		@Override
		public boolean isValidAt(Level level, BlockPos pos) {
			FluidState fluidState = level.getFluidState(pos);
			if (AllFluidTags.FAN_PROCESSING_CATALYSTS_BLASTING.matches(fluidState))
				return true;
			BlockState blockState = level.getBlockState(pos);
			if (!AllBlockTags.FAN_PROCESSING_CATALYSTS_BLASTING.matches(blockState))
				return false;
			return !blockState.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)
				|| blockState.getValue(BlazeBurnerBlock.HEAT_LEVEL).isAtLeast(BlazeBurnerBlock.HeatLevel.FADING);
		}

		@Override
		public int getPriority() {
			return 100;
		}

		@Override
		public boolean canProcess(ItemStack stack, Level level) {
			Optional<RecipeHolder<SmeltingRecipe>> smeltingRecipe = getRecipe(level, RecipeType.SMELTING, stack)
				.filter(AllRecipeTypes.CAN_BE_AUTOMATED);

			if (smeltingRecipe.isPresent())
				return true;

			Optional<RecipeHolder<BlastingRecipe>> blastingRecipe = getRecipe(level, RecipeType.BLASTING, stack)
				.filter(AllRecipeTypes.CAN_BE_AUTOMATED);

			if (blastingRecipe.isPresent())
				return true;

			return stack.canBeHurtBy(level.damageSources().inFire());
		}

		@Override
		@Nullable
		public List<ItemStack> process(ItemStack stack, Level level) {
			Optional<RecipeHolder<SmokingRecipe>> smokingRecipe = getRecipe(level, RecipeType.SMOKING, stack)
				.filter(AllRecipeTypes.CAN_BE_AUTOMATED);

			Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> smeltingRecipe =
				getRecipe(level, RecipeType.SMELTING, stack)
				.filter(AllRecipeTypes.CAN_BE_AUTOMATED);

			if (smeltingRecipe.isEmpty()) {
				smeltingRecipe = getRecipe(level, RecipeType.BLASTING, stack)
					.filter(AllRecipeTypes.CAN_BE_AUTOMATED);
			}

			if (smeltingRecipe.isPresent()) {
				SingleRecipeInput input = new SingleRecipeInput(stack);
				if (smokingRecipe.isEmpty() || !ItemStack.isSameItem(smokingRecipe.get().value().assemble(input),
					smeltingRecipe.get().value().assemble(input)))
					return applyRecipeOn(level, stack, smeltingRecipe.get().value());
			}

			return Collections.emptyList();
		}

		@Override
		public void spawnProcessingParticles(Level level, Vec3 pos) {
			if (level.getRandom().nextInt(8) != 0)
				return;
			level.addParticle(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + .25f, pos.z, 0, 1 / 16f, 0);
		}

		@Override
		public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
			particleAccess.setColor(Color.mixColors(0xFF4400, 0xFF8855, random.nextFloat()));
			particleAccess.setAlpha(.5f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.FLAME, .25f);
			if (random.nextFloat() < 1 / 16f)
				particleAccess.spawnExtraParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LAVA.defaultBlockState()), .25f);
		}

		@Override
		public void affectEntity(Entity entity, Level level) {
			if (level.isClientSide())
				return;
			if (!entity.fireImmune()) {
				entity.igniteForSeconds(10);
				entity.hurt(CreateDamageSources.fanLava(level), 4);
			}
		}
	}

	public static class HauntingType implements FanProcessingType {
		@Override
		public boolean isValidAt(Level level, BlockPos pos) {
			FluidState fluidState = level.getFluidState(pos);
			if (AllFluidTags.FAN_PROCESSING_CATALYSTS_HAUNTING.matches(fluidState))
				return true;
			BlockState blockState = level.getBlockState(pos);
			if (!AllBlockTags.FAN_PROCESSING_CATALYSTS_HAUNTING.matches(blockState))
				return false;
			if (blockState.is(BlockTags.CAMPFIRES) && blockState.hasProperty(CampfireBlock.LIT)
				&& !blockState.getValue(CampfireBlock.LIT))
				return false;
			return !blockState.hasProperty(LitBlazeBurnerBlock.FLAME_TYPE)
				|| blockState.getValue(LitBlazeBurnerBlock.FLAME_TYPE) == LitBlazeBurnerBlock.FlameType.SOUL;
		}

		@Override
		public int getPriority() {
			return 300;
		}

		@Override
		public boolean canProcess(ItemStack stack, Level level) {
			return AllRecipeTypes.HAUNTING.find(new SingleRecipeInput(stack), level)
				.isPresent();
		}

		@Override
		@Nullable
		public List<ItemStack> process(ItemStack stack, Level level) {
			return AllRecipeTypes.HAUNTING.find(new SingleRecipeInput(stack), level)
				.map(RecipeHolder::value)
				.map(r -> RecipeApplier.applyRecipeOn(level, stack, r, true))
				.orElse(null);
		}

		@Override
		public void spawnProcessingParticles(Level level, Vec3 pos) {
			if (level.getRandom().nextInt(8) != 0)
				return;
			pos = pos.add(VecHelper.offsetRandomly(Vec3.ZERO, level.getRandom(), 1)
				.multiply(1, 0.05f, 1)
				.normalize()
				.scale(0.15f));
			level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + .45f, pos.z, 0, 0, 0);
			if (level.getRandom().nextInt(2) == 0)
				level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y + .25f, pos.z, 0, 0, 0);
		}

		@Override
		public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
			particleAccess.setColor(Color.mixColors(0x0, 0x126568, random.nextFloat()));
			particleAccess.setAlpha(1f);
			if (random.nextFloat() < 1 / 128f)
				particleAccess.spawnExtraParticle(ParticleTypes.SOUL_FIRE_FLAME, .125f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.SMOKE, .125f);
		}

		@Override
		public void affectEntity(Entity entity, Level level) {
			if (level.isClientSide()) {
				if (entity instanceof Horse) {
					Vec3 position = entity.position();
					Vec3 particlePos = position.add(0, .5f, 0)
						.add(VecHelper.offsetRandomly(Vec3.ZERO, level.getRandom(), 1)
							.multiply(1, .2f, 1)
							.normalize()
							.scale(1));
					level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, particlePos.x, particlePos.y, particlePos.z,
						0, .1f, 0);
					if (level.getRandom().nextInt(3) == 0)
						level.addParticle(ParticleTypes.LARGE_SMOKE, position.x, position.y + .5f, position.z,
							(level.getRandom().nextFloat() - .5f) * .5f, .1f,
							(level.getRandom().nextFloat() - .5f) * .5f);
				}
				return;
			}

			if (entity instanceof LivingEntity livingEntity) {
				livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
				livingEntity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1, false, false));
			}
			if (!(entity instanceof Horse horse) || !(level instanceof ServerLevel serverLevel))
				return;

			// Horses are wider than a block and in 26.2 can receive soul-fire contact
			// damage while standing beside the catalyst. Haunting itself is not meant
			// to damage them, so protect only while its airflow is actively applied.
			horse.clearFire();
			horse.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20, 0, false, false));

			int progress = horse.getPersistentData().getIntOr("CreateHaunting", 0);
			if (progress < 100) {
				if (progress % 10 == 0)
					level.playSound(null, entity.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL,
						1, 1.5f * progress / 100f);
				horse.getPersistentData().putInt("CreateHaunting", progress + 1);
				return;
			}

			level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE,
				SoundSource.NEUTRAL, 1.25f, .65f);
			ItemStack bodyArmor = horse.getItemBySlot(EquipmentSlot.BODY).copy();
			if (!bodyArmor.isEmpty())
				horse.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);

			SkeletonHorse skeletonHorse = EntityTypes.SKELETON_HORSE.create(serverLevel, EntitySpawnReason.CONVERSION);
			if (skeletonHorse == null)
				return;
			skeletonHorse.restoreFrom(horse);
			// restoreFrom also copies the UUID. The original implementation removed it
			// from the serialized data before adding the replacement entity.
			skeletonHorse.setUUID(UUID.randomUUID());
			skeletonHorse.snapTo(horse.getX(), horse.getY(), horse.getZ(), horse.getYRot(), horse.getXRot());
			if (serverLevel.addFreshEntity(skeletonHorse)) {
				if (!bodyArmor.isEmpty())
					horse.spawnAtLocation(serverLevel, bodyArmor);
				horse.discard();
			} else if (!bodyArmor.isEmpty()) {
				horse.setItemSlot(EquipmentSlot.BODY, bodyArmor);
			}
		}
	}

	public static class SmokingType implements FanProcessingType {
		@Override
		public boolean isValidAt(Level level, BlockPos pos) {
			FluidState fluidState = level.getFluidState(pos);
			if (AllFluidTags.FAN_PROCESSING_CATALYSTS_SMOKING.matches(fluidState))
				return true;
			BlockState blockState = level.getBlockState(pos);
			if (!AllBlockTags.FAN_PROCESSING_CATALYSTS_SMOKING.matches(blockState))
				return false;
			if (blockState.is(BlockTags.CAMPFIRES) && blockState.hasProperty(CampfireBlock.LIT)
				&& !blockState.getValue(CampfireBlock.LIT))
				return false;
			if (blockState.hasProperty(LitBlazeBurnerBlock.FLAME_TYPE)
				&& blockState.getValue(LitBlazeBurnerBlock.FLAME_TYPE) != LitBlazeBurnerBlock.FlameType.REGULAR)
				return false;
			return !blockState.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)
				|| blockState.getValue(BlazeBurnerBlock.HEAT_LEVEL) == BlazeBurnerBlock.HeatLevel.SMOULDERING;
		}

		@Override
		public int getPriority() {
			return 200;
		}

		@Override
		public boolean canProcess(ItemStack stack, Level level) {
			return getRecipe(level, RecipeType.SMOKING, stack)
				.filter(AllRecipeTypes.CAN_BE_AUTOMATED)
				.isPresent();
		}

		@Override
		@Nullable
		public List<ItemStack> process(ItemStack stack, Level level) {
			return getRecipe(level, RecipeType.SMOKING, stack)
				.filter(AllRecipeTypes.CAN_BE_AUTOMATED)
				.map(RecipeHolder::value)
				.map(r -> applyRecipeOn(level, stack, r))
				.orElse(null);
		}

		@Override
		public void spawnProcessingParticles(Level level, Vec3 pos) {
			if (level.getRandom().nextInt(8) != 0)
				return;
			level.addParticle(ParticleTypes.POOF, pos.x, pos.y + .25f, pos.z, 0, 1 / 16f, 0);
		}

		@Override
		public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
			particleAccess.setColor(Color.mixColors(0x0, 0x555555, random.nextFloat()));
			particleAccess.setAlpha(1f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.SMOKE, .125f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.LARGE_SMOKE, .125f);
		}

		@Override
		public void affectEntity(Entity entity, Level level) {
			if (level.isClientSide())
				return;
			if (!entity.fireImmune()) {
				entity.igniteForSeconds(2);
				entity.hurt(CreateDamageSources.fanFire(level), 2);
			}
		}
	}

	public static class SplashingType implements FanProcessingType {
		@Override
		public boolean isValidAt(Level level, BlockPos pos) {
			FluidState fluidState = level.getFluidState(pos);
			if (AllFluidTags.FAN_PROCESSING_CATALYSTS_SPLASHING.matches(fluidState))
				return true;
			return AllBlockTags.FAN_PROCESSING_CATALYSTS_SPLASHING.matches(level.getBlockState(pos));
		}

		@Override
		public int getPriority() {
			return 400;
		}

		@Override
		public boolean canProcess(ItemStack stack, Level level) {
			return AllRecipeTypes.SPLASHING.find(new SingleRecipeInput(stack), level)
				.isPresent() || !concreteResult(stack).isEmpty();
		}

		@Override
		@Nullable
		public List<ItemStack> process(ItemStack stack, Level level) {
			Optional<RecipeHolder<Recipe<SingleRecipeInput>>> recipe =
				AllRecipeTypes.SPLASHING.find(new SingleRecipeInput(stack), level);
			return recipe.map(RecipeHolder::value)
				.map(r -> RecipeApplier.applyRecipeOn(level, stack, r, true))
				.orElseGet(() -> {
					ItemStack concrete = concreteResult(stack);
					return concrete.isEmpty() ? null : List.of(concrete);
				});
		}

		private static ItemStack concreteResult(ItemStack stack) {
			if (!(stack.getItem() instanceof BlockItem blockItem)
				|| !(blockItem.getBlock() instanceof ConcretePowderBlock powder))
				return ItemStack.EMPTY;
			return new ItemStack(((ConcretePowderBlockAccessor) powder).create$getConcrete(), stack.getCount());
		}

		@Override
		public void spawnProcessingParticles(Level level, Vec3 pos) {
			if (level.getRandom().nextInt(8) != 0)
				return;
			RandomSource random = level.getRandom();
			level.addParticle(new DustParticleOptions(0x0055FF, 1), pos.x + (random.nextFloat() - .5f) * .5f,
				pos.y + .5f, pos.z + (random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
			level.addParticle(ParticleTypes.SPIT, pos.x + (random.nextFloat() - .5f) * .5f, pos.y + .5f,
				pos.z + (random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
		}

		@Override
		public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
			particleAccess.setColor(Color.mixColors(0x4499FF, 0x2277FF, random.nextFloat()));
			particleAccess.setAlpha(1f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.BUBBLE, .125f);
			if (random.nextFloat() < 1 / 32f)
				particleAccess.spawnExtraParticle(ParticleTypes.BUBBLE_POP, .125f);
		}

		@Override
		public void affectEntity(Entity entity, Level level) {
			if (level.isClientSide())
				return;

			String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
			if (entity instanceof EnderMan || entityId.equals("snow_golem") || entityId.equals("blaze"))
				entity.hurt(entity.damageSources().drown(), 2);

			if (entity.isOnFire()) {
				entity.clearFire();
				level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE,
					SoundSource.NEUTRAL, 0.7F,
					1.6F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.4F);
			}
		}
	}

	private static <R extends Recipe<SingleRecipeInput>> Optional<RecipeHolder<R>> getRecipe(Level level, RecipeType<R> type, ItemStack stack) {
		if (!(level instanceof ServerLevel serverLevel))
			return Optional.empty();
		return serverLevel.recipeAccess()
			.getRecipeFor(type, new SingleRecipeInput(stack), level);
	}

	private static List<ItemStack> applyRecipeOn(Level level, ItemStack stack, Recipe<SingleRecipeInput> recipe) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = 0; i < stack.getCount(); i++) {
			ItemStack result = recipe.assemble(new SingleRecipeInput(stack.copyWithCount(1)));
			if (!result.isEmpty())
				ItemHelper.addToList(result, stacks);
		}
		return stacks;
	}
}
