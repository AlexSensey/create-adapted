package com.simibubi.create.content.equipment.blueprint;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.filter.AttributeFilterWhitelistMode;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute.ItemAttributeEntry;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.InTagAttribute;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.items.ItemStackHandler;

public class BlueprintItem extends Item {

	public BlueprintItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Direction face = ctx.getClickedFace();
		Player player = ctx.getPlayer();
		ItemStack stack = ctx.getItemInHand();
		BlockPos pos = ctx.getClickedPos()
			.relative(face);

		if (player != null && !player.mayUseItemAt(pos, face, stack))
			return InteractionResult.FAIL;

		Level world = ctx.getLevel();
		HangingEntity hangingentity = new BlueprintEntity(world, pos, face, face.getAxis()
			.isHorizontal() ? Direction.DOWN : ctx.getHorizontalDirection());
		TypedEntityData<EntityType<?>> entityData = stack.get(DataComponents.ENTITY_DATA);
		if (entityData != null)
			EntityType.updateCustomEntityTag(world, player, hangingentity, entityData);

		if (!hangingentity.survives())
			return InteractionResult.CONSUME;
		if (!world.isClientSide()) {
			hangingentity.playPlacementSound();
			world.addFreshEntity(hangingentity);
		}

		stack.shrink(1);
		return InteractionResult.SUCCESS;
	}

	public static void assignCompleteRecipe(Level level, ItemStackHandler inv, Recipe<?> recipe) {
		if (!(recipe instanceof CraftingRecipe craftingRecipe))
			return;

		ItemStack result = craftingRecipe.display()
			.stream()
			.map(display -> display.result()
				.resolveForFirstStack(SlotDisplayContext.fromLevel(level)))
			.filter(stack -> !stack.isEmpty())
			.findFirst()
			.orElse(ItemStack.EMPTY);
		if (result.isEmpty())
			return;

		if (craftingRecipe instanceof ShapedRecipe shapedRecipe) {
			if (shapedRecipe.getWidth() > 3 || shapedRecipe.getHeight() > 3)
				return;

			for (int i = 0; i < 9; i++)
				inv.setStackInSlot(i, ItemStack.EMPTY);

			List<Optional<Ingredient>> ingredients = shapedRecipe.getIngredients();
			for (int row = 0; row < shapedRecipe.getHeight(); row++)
				for (int column = 0; column < shapedRecipe.getWidth(); column++) {
					int ingredientIndex = row * shapedRecipe.getWidth() + column;
					if (ingredientIndex >= ingredients.size())
						continue;
					Optional<Ingredient> ingredient = ingredients.get(ingredientIndex);
					if (ingredient.isPresent())
						inv.setStackInSlot(row * 3 + column, convertIngredientToFilter(ingredient.get()));
				}
		} else {
			List<Ingredient> ingredients = craftingRecipe.placementInfo()
				.ingredients();
			if (craftingRecipe.placementInfo()
				.isImpossibleToPlace() || ingredients.size() > 9)
				return;

			for (int i = 0; i < 9; i++)
				inv.setStackInSlot(i, ItemStack.EMPTY);
			for (int i = 0; i < ingredients.size(); i++)
				inv.setStackInSlot(i, convertIngredientToFilter(ingredients.get(i)));
		}

		inv.setStackInSlot(9, result);
	}

	private static ItemStack convertIngredientToFilter(Ingredient ingredient) {
		Optional<TagKey<Item>> tag = ingredient.values.unwrapKey();
		if (tag.isPresent()) {
			ItemStack result = AllItems.ATTRIBUTE_FILTER.asStack();
			result.set(AllDataComponents.ATTRIBUTE_FILTER_WHITELIST_MODE,
				AttributeFilterWhitelistMode.WHITELIST_DISJ);
			result.set(AllDataComponents.ATTRIBUTE_FILTER_MATCHED_ATTRIBUTES,
				List.of(new ItemAttributeEntry(new InTagAttribute(tag.get()), false)));
			return result;
		}

		boolean isCompoundIngredient = ingredient.getCustomIngredient() instanceof CompoundIngredient;
		List<ItemStack> acceptedItems = ingredient.items()
			.map(holder -> new ItemStack(holder.value()))
			.limit(19)
			.toList();
		if (acceptedItems.size() > 18)
			return ItemStack.EMPTY;
		if (acceptedItems.isEmpty())
			return ItemStack.EMPTY;
		if (acceptedItems.size() == 1)
			return acceptedItems.get(0);

		ItemStack result = AllItems.FILTER.asStack();
		ItemStackHandler filterItems = AllItems.FILTER.get().getFilterItemHandler(result);
		for (int i = 0; i < acceptedItems.size(); i++)
			filterItems.setStackInSlot(i, acceptedItems.get(i));
		result.set(AllDataComponents.FILTER_ITEMS, ItemHelper.containerContentsFromHandler(filterItems));
		if (isCompoundIngredient)
			result.set(AllDataComponents.FILTER_ITEMS_RESPECT_NBT, true);
		return result;
	}

}
