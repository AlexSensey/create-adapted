package com.simibubi.create.compat.jei;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.blueprint.BlueprintScreen;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItemComponent;
import com.simibubi.create.compat.jei.category.animations.AnimatedCrafter;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.compat.jei.category.animations.AnimatedCrushingWheels;
import com.simibubi.create.compat.jei.category.animations.AnimatedDeployer;
import com.simibubi.create.compat.jei.category.animations.AnimatedItemDrain;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.compat.jei.category.animations.AnimatedMillstone;
import com.simibubi.create.compat.jei.category.animations.AnimatedMixer;
import com.simibubi.create.compat.jei.category.animations.AnimatedPress;
import com.simibubi.create.compat.jei.category.animations.AnimatedSaw;
import com.simibubi.create.compat.jei.category.animations.AnimatedSpout;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.content.fluids.potion.PotionMixingRecipes;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.fluids.transfer.EmptyingRecipe;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSetItemScreen;
import com.simibubi.create.content.logistics.filter.AttributeFilterScreen;
import com.simibubi.create.content.logistics.filter.FilterScreen;
import com.simibubi.create.content.logistics.filter.PackageFilterScreen;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerScreen;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.data.recipe.LogStrippingFakeRecipes;
import com.simibubi.create.foundation.mixin.accessor.ConcretePowderBlockAccessor;
import com.simibubi.create.foundation.recipe.CreateRecipeClientCache;
import com.simibubi.create.infrastructure.config.AllConfigs;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import net.createmod.catnip.api.data.Pair;

import net.createmod.catnip.api.client.gui.element.GuiGameElement;

import com.simibubi.create.foundation.gui.AllGuiTextures;

@JeiPlugin
@SuppressWarnings("unused")
public class CreateJEI implements IModPlugin {

	private static final Identifier ID = Create.asResource("jei_plugin");
	private static final List<AllRecipeTypes> PROCESSING_TYPES = List.of(
		AllRecipeTypes.CONVERSION,
		AllRecipeTypes.CRUSHING,
		AllRecipeTypes.CUTTING,
		AllRecipeTypes.MILLING,
		AllRecipeTypes.BASIN,
		AllRecipeTypes.MIXING,
		AllRecipeTypes.COMPACTING,
		AllRecipeTypes.PRESSING,
		AllRecipeTypes.SANDPAPER_POLISHING,
		AllRecipeTypes.SPLASHING,
		AllRecipeTypes.HAUNTING,
		AllRecipeTypes.DEPLOYING,
		AllRecipeTypes.FILLING,
		AllRecipeTypes.EMPTYING,
		AllRecipeTypes.ITEM_APPLICATION
	);

	public static IJeiRuntime runtime;

	private final List<ProcessingCategory<?>> categories = new ArrayList<>();
	private FanCookingCategory<AbstractCookingRecipe> fanBlastingCategory;
	private FanCookingCategory<SmokingRecipe> fanSmokingCategory;
	private BlockCuttingCategory blockCuttingCategory;
	private MechanicalCraftingCategory mechanicalCraftingCategory;
	private SequencedAssemblyCategory sequencedAssemblyCategory;
	private final List<AutomaticBasinCategory> automaticBasinCategories = new ArrayList<>();
	private AutomaticCraftingCategory automaticCraftingCategory;

	@Override
	public Identifier getPluginUid() {
		return ID;
	}

	@Override
	public <T> void registerFluidSubtypes(ISubtypeRegistration registration, IPlatformFluidHelper<T> platformFluidHelper) {
		PotionFluid potionFluid = AllFluids.POTION.get();
		PotionFluidSubtypeInterpreter interpreter = new PotionFluidSubtypeInterpreter();
		registration.registerSubtypeInterpreter(NeoForgeTypes.FLUID_STACK, potionFluid.getSource(), interpreter);
		registration.registerSubtypeInterpreter(NeoForgeTypes.FLUID_STACK, potionFluid.getFlowing(), interpreter);
	}

	@Override
	public void registerExtraIngredients(IExtraIngredientRegistration registration) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;
		List<Reference<Potion>> potions = minecraft.level.registryAccess()
			.lookupOrThrow(Registries.POTION)
			.listElements()
			.toList();
		Collection<FluidStack> potionFluids = new ArrayList<>(potions.size());
		Set<Set<Holder<MobEffect>>> visitedEffects = new HashSet<>();
		for (Reference<Potion> potion : potions) {
			PotionContents contents = new PotionContents(potion);
			if (contents.hasEffects()) {
				Set<Holder<MobEffect>> effects = new HashSet<>();
				contents.forEachEffect(effect -> effects.add(effect.getEffect()), 1);
				if (!visitedEffects.add(effects))
					continue;
			}
			potionFluids.add(PotionFluid.of(1000, contents, BottleType.REGULAR));
		}
		registration.addExtraIngredients(NeoForgeTypes.FLUID_STACK, potionFluids);
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper guiHelper = registration.getJeiHelpers()
			.getGuiHelper();
		categories.clear();
		for (AllRecipeTypes recipeType : PROCESSING_TYPES)
			categories.add(new ProcessingCategory<>(recipeType, guiHelper));
		fanBlastingCategory = FanCookingCategory.blasting(guiHelper);
		fanSmokingCategory = FanCookingCategory.smoking(guiHelper);
		blockCuttingCategory = new BlockCuttingCategory(guiHelper);
		mechanicalCraftingCategory = new MechanicalCraftingCategory(guiHelper);
		sequencedAssemblyCategory = new SequencedAssemblyCategory(guiHelper);
		automaticBasinCategories.clear();
		automaticBasinCategories.add(new AutomaticBasinCategory(AutomaticBasinCategory.Mode.SHAPELESS, guiHelper));
		automaticBasinCategories.add(new AutomaticBasinCategory(AutomaticBasinCategory.Mode.BREWING, guiHelper));
		automaticBasinCategories.add(new AutomaticBasinCategory(AutomaticBasinCategory.Mode.PACKING, guiHelper));
		automaticCraftingCategory = new AutomaticCraftingCategory(guiHelper);
		List<IRecipeCategory<?>> allCategories = new ArrayList<>(categories);
		allCategories.add(fanBlastingCategory);
		allCategories.add(fanSmokingCategory);
		allCategories.add(blockCuttingCategory);
		allCategories.add(mechanicalCraftingCategory);
		allCategories.add(sequencedAssemblyCategory);
		allCategories.addAll(automaticBasinCategories);
		allCategories.add(automaticCraftingCategory);
		registration.addRecipeCategories(allCategories.toArray(IRecipeCategory[]::new));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		Collection<RecipeHolder<?>> recipes = getLoadedRecipes();
		Create.LOGGER.info("Create JEI: loaded {} recipes for JEI registration", recipes.size());
		for (ProcessingCategory<?> category : categories)
			category.registerRecipes(registration, recipes, registration.getIngredientManager());
		if (fanBlastingCategory != null)
			fanBlastingCategory.registerRecipes(registration, recipes);
		if (fanSmokingCategory != null)
			fanSmokingCategory.registerRecipes(registration, recipes);
		if (blockCuttingCategory != null)
			blockCuttingCategory.registerRecipes(registration, recipes);
		if (mechanicalCraftingCategory != null)
			mechanicalCraftingCategory.registerRecipes(registration, recipes);
		if (sequencedAssemblyCategory != null)
			sequencedAssemblyCategory.registerRecipes(registration, recipes);
		for (AutomaticBasinCategory category : automaticBasinCategories)
			category.registerRecipes(registration, recipes);
		if (automaticCraftingCategory != null)
			automaticCraftingCategory.registerRecipes(registration, recipes);
		registration.addRecipes(RecipeTypes.CRAFTING, ToolboxColoringRecipeMaker.createRecipes().toList());
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		for (ProcessingCategory<?> category : categories)
			category.registerCatalysts(registration);
		if (fanBlastingCategory != null)
			fanBlastingCategory.registerCatalysts(registration);
		if (fanSmokingCategory != null)
			fanSmokingCategory.registerCatalysts(registration);
		if (blockCuttingCategory != null)
			blockCuttingCategory.registerCatalysts(registration);
		if (mechanicalCraftingCategory != null)
			mechanicalCraftingCategory.registerCatalysts(registration);
		if (sequencedAssemblyCategory != null)
			sequencedAssemblyCategory.registerCatalysts(registration);
		for (AutomaticBasinCategory category : automaticBasinCategories)
			category.registerCatalysts(registration);
		if (automaticCraftingCategory != null)
			automaticCraftingCategory.registerCatalysts(registration);
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		registration.addRecipeTransferHandler(new BlueprintTransferHandler(), RecipeTypes.CRAFTING);
		registration.addUniversalRecipeTransferHandler(
			new StockKeeperTransferHandler(registration.getJeiHelpers()));
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		addGhostHandler(registration, AttributeFilterScreen.class);
		addGhostHandler(registration, FilterScreen.class);
		addGhostHandler(registration, PackageFilterScreen.class);
		addGhostHandler(registration, BlueprintScreen.class);
		addGhostHandler(registration, FactoryPanelSetItemScreen.class);
		addGhostHandler(registration, RedstoneRequesterScreen.class);
		addGhostHandler(registration, LinkedControllerScreen.class);
		addGhostHandler(registration, ScheduleScreen.class);
		registration.addGenericGuiContainerHandler((Class) AbstractSimiContainerScreen.class, new SlotMover());
		registration.addGuiContainerHandler(StockKeeperRequestScreen.class,
			new StockKeeperGuiContainerHandler(registration.getJeiHelpers()
				.getIngredientManager()));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void addGhostHandler(IGuiHandlerRegistration registration, Class<?> screenClass) {
		registration.addGhostIngredientHandler((Class) screenClass, new GhostIngredientHandler<>());
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		runtime = jeiRuntime;
	}

	@Override
	public void onRuntimeUnavailable() {
		runtime = null;
	}

	private static Collection<RecipeHolder<?>> getLoadedRecipes() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null)
			return minecraft.getSingleplayerServer()
				.getRecipeManager()
				.getRecipes();
		return CreateRecipeClientCache.getRecipes();
	}

	private static List<RecipeHolder<FillingRecipe>> createGeneratedFillingRecipes(IIngredientManager ingredientManager) {
		List<RecipeHolder<FillingRecipe>> recipes = new ArrayList<>();
		int potionIndex = 0;
		int containerIndex = 0;
		Minecraft minecraft = Minecraft.getInstance();
		for (ItemStack stack : ingredientManager.getAllIngredients(VanillaTypes.ITEM_STACK)) {
			if (PotionFluidHandler.isPotionItem(stack)) {
				FluidStack potionFluid = PotionFluidHandler.getFluidFromPotionItem(stack);
				Identifier id = Create.asResource("potion_filling_" + potionIndex++);
				SizedFluidIngredient fluidIngredient = new SizedFluidIngredient(
					DataComponentFluidIngredient.of(false, potionFluid), potionFluid.getAmount());
				FillingRecipe recipe = new StandardProcessingRecipe.Builder<>(FillingRecipe::new, id)
					.withItemIngredients(Ingredient.of(Items.GLASS_BOTTLE))
					.withFluidIngredients(fluidIngredient)
					.withSingleItemOutput(stack)
					.build();
				recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe));
				continue;
			}

			// Potion bottles are generated above from their exact output stacks. Iterating the
			// empty bottle here would generate the same entries a second time.
			if (minecraft.level == null || stack.is(Items.GLASS_BOTTLE))
				continue;
			for (FluidStack available : ingredientManager.getAllIngredients(NeoForgeTypes.FLUID_STACK)) {
				FluidStack fluid = available.copy();
				fluid.setAmount(1000);
				ItemStack input = stack.copy();
				int required = GenericItemFilling.getRequiredAmountForItem(minecraft.level, input, fluid);
				if (required <= 0)
					continue;
				ItemStack output = GenericItemFilling.fillItem(minecraft.level, required, input, fluid.copy());
				if (output.isEmpty() || ItemHelper.sameItem(output, stack))
					continue;
				Identifier id = Create.asResource("generated_filling_" + containerIndex++);
				FluidStack requiredFluid = available.copy();
				requiredFluid.setAmount(required);
				FillingRecipe recipe = new StandardProcessingRecipe.Builder<>(FillingRecipe::new, id)
					.withItemIngredients(DataComponentIngredient.of(false, stack))
					.withFluidIngredients(new SizedFluidIngredient(
						DataComponentFluidIngredient.of(false, requiredFluid), required))
					.withSingleItemOutput(output)
					.build();
				recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe));
			}
		}
		return recipes;
	}

	private static List<RecipeHolder<EmptyingRecipe>> createGeneratedEmptyingRecipes(IIngredientManager ingredientManager) {
		List<RecipeHolder<EmptyingRecipe>> recipes = new ArrayList<>();
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return recipes;
		int index = 0;
		for (ItemStack stack : ingredientManager.getAllIngredients(VanillaTypes.ITEM_STACK)) {
			// Loaded data recipes are already registered in this category. Only synthesize
			// the runtime-only potion/container recipes which JEI cannot discover itself.
			if (!PotionFluidHandler.isPotionItem(stack)
				&& AllRecipeTypes.EMPTYING.find(new SingleRecipeInput(stack), minecraft.level).isPresent())
				continue;
			Pair<FluidStack, ItemStack> result = GenericItemEmptying.emptyItem(minecraft.level, stack.copy(), true);
			FluidStack fluid = result.getFirst();
			ItemStack output = result.getSecond();
			if (fluid.isEmpty() || output.isEmpty())
				continue;
			Identifier id = Create.asResource("generated_emptying_" + index++);
			EmptyingRecipe recipe = new StandardProcessingRecipe.Builder<>(EmptyingRecipe::new, id)
				.withItemIngredients(DataComponentIngredient.of(false, stack))
				.withFluidOutputs(fluid)
				.withSingleItemOutput(output)
				.build();
			recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe));
		}
		return recipes;
	}

	private static List<FluidStack> matchingFluidStacks(SizedFluidIngredient ingredient) {
		return ingredient.ingredient()
			.fluids()
			.stream()
			.map(fluid -> {
				FluidStack stack = new FluidStack(fluid, ingredient.amount());
				if (ingredient.ingredient() instanceof DataComponentFluidIngredient componentIngredient)
					stack.applyComponents(componentIngredient.components().asPatch());
				return stack;
			})
			.toList();
	}

	private static void drawArrow(GuiGraphicsExtractor graphics, int x, int y, int width) {
		int color = 0xff5f5f5f;
		graphics.fill(x, y, x + width, y + 3, color);
		graphics.fill(x + width - 6, y - 4, x + width - 3, y + 7, color);
		graphics.fill(x + width - 3, y - 3, x + width, y + 6, color);
		graphics.fill(x + width, y - 2, x + width + 3, y + 5, color);
	}

	private static void drawDownArrow(GuiGraphicsExtractor graphics, int x, int y, int height) {
		int color = 0xff5f5f5f;
		graphics.fill(x, y, x + 3, y + height, color);
		graphics.fill(x - 4, y + height - 6, x + 7, y + height - 3, color);
		graphics.fill(x - 3, y + height - 3, x + 6, y + height, color);
		graphics.fill(x - 2, y + height, x + 5, y + height + 3, color);
	}

	private static void renderAnimatedFan(GuiGraphicsExtractor graphics, int x, int y, BlockState attachedBlock) {
		GuiGameElement.beginModelBatch(-12.5f, 22.5f, 0);
		try {
			AnimatedKinetics.defaultBlockElement(com.simibubi.create.AllPartialModels.ENCASED_FAN_INNER)
				.at(x, y)
				.rotateBlock(180, 0, AnimatedKinetics.getCurrentAngle() * 16)
				.scale(24)
				.submit(graphics);
			AnimatedKinetics.defaultBlockElement(AllBlocks.ENCASED_FAN.getDefaultState())
				.at(x, y)
				.rotateBlock(0, 180, 0)
				.scale(24)
				.submit(graphics);
			if (attachedBlock != null)
				AnimatedKinetics.defaultBlockElement(attachedBlock)
					.at(x, y)
					.atLocal(0, 0, 2)
					.scale(24)
					.submit(graphics);
		} finally {
			GuiGameElement.endModelBatch(graphics);
		}
	}

	private static class BlockCuttingCategory implements IRecipeCategory<RecipeHolder<CondensedBlockCuttingRecipe>> {
		private static final int WIDTH = 177;
		private static final int HEIGHT = 70;
		private static final int MAX_OUTPUT_SLOTS = 15;

		private final RecipeType<RecipeHolder<CondensedBlockCuttingRecipe>> jeiType;
		private final IDrawable icon;
		private final AnimatedSaw sawAnimation = new AnimatedSaw();

		private BlockCuttingCategory(IGuiHelper guiHelper) {
			jeiType = RecipeType.createRecipeHolderType(Create.asResource("block_cutting"));
			icon = new DoubleItemIcon(() -> new ItemStack(AllBlocks.MECHANICAL_SAW.get()),
				() -> new ItemStack(Items.STONE_BRICK_STAIRS));
		}

		@Override
		public RecipeType<RecipeHolder<CondensedBlockCuttingRecipe>> getRecipeType() {
			return jeiType;
		}

		@Override
		public Component getTitle() {
			return Component.translatable("create.recipe.block_cutting");
		}

		@Override
		public int getWidth() {
			return WIDTH;
		}

		@Override
		public int getHeight() {
			return HEIGHT;
		}

		@Override
		public IDrawable getIcon() {
			return icon;
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CondensedBlockCuttingRecipe> holder, IFocusGroup focuses) {
			CondensedBlockCuttingRecipe recipe = holder.value();
			ProcessingCategory.addSlot(builder, RecipeIngredientRole.INPUT, 5, 5)
				.add(recipe.input());

			List<List<ItemStack>> outputs = recipe.condensedOutputs();
			for (int outputIndex = 0; outputIndex < outputs.size(); outputIndex++) {
				int x = 78 + outputIndex % 5 * 19;
				int y = 48 - outputIndex / 5 * 19;
				ProcessingCategory.addSlot(builder, RecipeIngredientRole.OUTPUT, x, y)
					.addItemStacks(outputs.get(outputIndex));
			}
		}

		@Override
		public void draw(RecipeHolder<CondensedBlockCuttingRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics,
			double mouseX, double mouseY) {
			AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 31, 6);
			AllGuiTextures.JEI_SHADOW.render(graphics, 16, 50);
			sawAnimation.draw(graphics, 33, 37);
		}

		@Override
		public Identifier getIdentifier(RecipeHolder<CondensedBlockCuttingRecipe> holder) {
			return holder.id()
				.identifier();
		}

		private void registerRecipes(IRecipeRegistration registration, Collection<RecipeHolder<?>> loadedRecipes) {
			List<RecipeHolder<CondensedBlockCuttingRecipe>> recipes = condenseRecipes(loadedRecipes);
			if (!recipes.isEmpty())
				Create.LOGGER.info("Create JEI: registered {} {} recipes", recipes.size(), jeiType.getUid());
			registration.addRecipes(jeiType, recipes);
		}

		private void registerCatalysts(IRecipeCatalystRegistration registration) {
			registration.addCraftingStation(jeiType, new ItemStack(AllBlocks.MECHANICAL_SAW.get()));
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private static List<RecipeHolder<CondensedBlockCuttingRecipe>> condenseRecipes(Collection<RecipeHolder<?>> loadedRecipes) {
			List<RecipeHolder<CondensedBlockCuttingRecipe>> condensedRecipes = new ArrayList<>();
			for (RecipeHolder<?> holder : loadedRecipes) {
				Recipe<?> recipe = holder.value();
				if (recipe.getType() != net.minecraft.world.item.crafting.RecipeType.STONECUTTING || !(recipe instanceof StonecutterRecipe stonecutting))
					continue;
				if (AllRecipeTypes.shouldIgnoreInAutomation(holder))
					continue;

				Ingredient input = stonecutting.input();
				ItemStack output = resultItem(stonecutting, input);
				if (input.isEmpty() || output.isEmpty())
					continue;

				CondensedBlockCuttingRecipe matchingRecipe = null;
				for (RecipeHolder<CondensedBlockCuttingRecipe> condensedHolder : condensedRecipes) {
					CondensedBlockCuttingRecipe condensedRecipe = condensedHolder.value();
					if (ItemHelper.matchIngredients(condensedRecipe.input(), input)) {
						matchingRecipe = condensedRecipe;
						break;
					}
				}

				if (matchingRecipe != null) {
					matchingRecipe.addOutput(output);
					continue;
				}

				CondensedBlockCuttingRecipe condensedRecipe = new CondensedBlockCuttingRecipe((RecipeHolder<StonecutterRecipe>) (RecipeHolder) holder, input);
				condensedRecipe.addOutput(output);
				condensedRecipes.add(new RecipeHolder<>(holder.id(), condensedRecipe));
			}
			return condensedRecipes;
		}

		private static ItemStack resultItem(StonecutterRecipe recipe, Ingredient input) {
			ItemStack stack = input.items()
				.map(holder -> new ItemStack(holder.value()))
				.findFirst()
				.orElse(ItemStack.EMPTY);
			if (stack.isEmpty())
				return ItemStack.EMPTY;
			return recipe.assemble(new SingleRecipeInput(stack));
		}
	}

	private static class CondensedBlockCuttingRecipe implements Recipe<SingleRecipeInput> {
		private final RecipeHolder<StonecutterRecipe> firstRecipe;
		private final Ingredient input;
		private final List<ItemStack> outputs;

		private CondensedBlockCuttingRecipe(RecipeHolder<StonecutterRecipe> firstRecipe, Ingredient input) {
			this.firstRecipe = firstRecipe;
			this.input = input;
			this.outputs = new ArrayList<>();
		}

		private Ingredient input() {
			return input;
		}

		private void addOutput(ItemStack stack) {
			for (ItemStack existing : outputs) {
				if (ItemStack.isSameItemSameComponents(existing, stack)) {
					existing.grow(stack.getCount());
					return;
				}
			}
			outputs.add(stack.copy());
		}

		private List<List<ItemStack>> condensedOutputs() {
			List<List<ItemStack>> stacksPerSlot = new ArrayList<>();
			if (outputs.size() <= BlockCuttingCategory.MAX_OUTPUT_SLOTS) {
				for (ItemStack output : outputs)
					stacksPerSlot.add(List.of(output));
				return stacksPerSlot;
			}

			for (int slot = 0; slot < BlockCuttingCategory.MAX_OUTPUT_SLOTS; slot++)
				stacksPerSlot.add(new ArrayList<>());
			for (int outputIndex = 0; outputIndex < outputs.size(); outputIndex++)
				stacksPerSlot.get(outputIndex * BlockCuttingCategory.MAX_OUTPUT_SLOTS / outputs.size())
					.add(outputs.get(outputIndex));
			return stacksPerSlot;
		}

		@Override
		public boolean matches(SingleRecipeInput input, net.minecraft.world.level.Level level) {
			return firstRecipe.value()
				.matches(input, level);
		}

		@Override
		public ItemStack assemble(SingleRecipeInput input) {
			return firstRecipe.value()
				.assemble(input);
		}

		@Override
		public boolean showNotification() {
			return firstRecipe.value()
				.showNotification();
		}

		@Override
		public String group() {
			return firstRecipe.value()
				.group();
		}

		@Override
		public net.minecraft.world.item.crafting.RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
			return firstRecipe.value()
				.getSerializer();
		}

		@Override
		public net.minecraft.world.item.crafting.RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
			return firstRecipe.value()
				.getType();
		}

		@Override
		public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
			return firstRecipe.value()
				.placementInfo();
		}

		@Override
		public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
			return firstRecipe.value()
				.display();
		}

		@Override
		public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
			return firstRecipe.value()
				.recipeBookCategory();
		}
	}

	private static class MechanicalCraftingCategory implements IRecipeCategory<RecipeHolder<MechanicalCraftingRecipe>> {
		private static final int WIDTH = 177;
		private static final int HEIGHT = 107;
		private static final int GRID_CENTRE = 53;
		private static final int SLOT_SPACING = 19;

		private final RecipeType<RecipeHolder<MechanicalCraftingRecipe>> jeiType;
		private final IDrawable icon;
		private final AnimatedCrafter crafterAnimation = new AnimatedCrafter();

		private MechanicalCraftingCategory(IGuiHelper guiHelper) {
			jeiType = RecipeType.createRecipeHolderType(AllRecipeTypes.MECHANICAL_CRAFTING.getId());
			icon = guiHelper.createDrawableItemStack(new ItemStack(AllBlocks.MECHANICAL_CRAFTER.get()));
		}

		@Override
		public RecipeType<RecipeHolder<MechanicalCraftingRecipe>> getRecipeType() {
			return jeiType;
		}

		@Override
		public Component getTitle() {
			return Component.translatable("create.recipe.mechanical_crafting");
		}

		@Override
		public int getWidth() {
			return WIDTH;
		}

		@Override
		public int getHeight() {
			return HEIGHT;
		}

		@Override
		public IDrawable getIcon() {
			return icon;
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MechanicalCraftingRecipe> holder, IFocusGroup focuses) {
			MechanicalCraftingRecipe recipe = holder.value();
			ShapedRecipePattern pattern = recipe.getPattern();
			List<java.util.Optional<Ingredient>> ingredients = pattern.ingredients();
			int xPadding = GRID_CENTRE - pattern.width() * SLOT_SPACING / 2;
			int yPadding = GRID_CENTRE - pattern.height() * SLOT_SPACING / 2;

			for (int row = 0; row < pattern.height(); row++) {
				for (int col = 0; col < pattern.width(); col++) {
					int index = col + row * pattern.width();
					if (index >= ingredients.size())
						continue;
					java.util.Optional<Ingredient> ingredient = ingredients.get(index);
					if (ingredient.isEmpty() || ingredient.get().isEmpty())
						continue;
					ProcessingCategory.addSlot(builder, RecipeIngredientRole.INPUT,
						xPadding + 1 + col * SLOT_SPACING, yPadding + 1 + row * SLOT_SPACING)
						.add(ingredient.get());
				}
			}

			ProcessingCategory.addSlot(builder, RecipeIngredientRole.OUTPUT, 134, 81)
				.add(recipe.getResultStack());
		}

		@Override
		public void draw(RecipeHolder<MechanicalCraftingRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
			AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 128, 59);
			crafterAnimation.draw(graphics, 129, 25);
			long amount = holder.value().getPattern().ingredients().stream()
				.filter(java.util.Optional::isPresent)
				.filter(ingredient -> !ingredient.get().isEmpty())
				.count();
			graphics.text(Minecraft.getInstance().font, Long.toString(amount), 142, 39, 0xffffffff);
		}

		@Override
		public Identifier getIdentifier(RecipeHolder<MechanicalCraftingRecipe> holder) {
			return holder.id()
				.identifier();
		}

		private void registerRecipes(IRecipeRegistration registration, Collection<RecipeHolder<?>> loadedRecipes) {
			List<RecipeHolder<MechanicalCraftingRecipe>> recipes = new ArrayList<>();
			for (RecipeHolder<?> holder : loadedRecipes) {
				Recipe<?> recipe = holder.value();
				if (recipe instanceof MechanicalCraftingRecipe mechanicalRecipe)
					recipes.add(new RecipeHolder<>(holder.id(), mechanicalRecipe));
			}
			if (!recipes.isEmpty())
				Create.LOGGER.info("Create JEI: registered {} {} recipes", recipes.size(), AllRecipeTypes.MECHANICAL_CRAFTING.getId());
			registration.addRecipes(jeiType, recipes);
		}

		private void registerCatalysts(IRecipeCatalystRegistration registration) {
			registration.addCraftingStation(jeiType, new ItemStack(AllBlocks.MECHANICAL_CRAFTER.get()));
		}
	}

	private static class SequencedAssemblyCategory implements IRecipeCategory<RecipeHolder<SequencedAssemblyRecipe>> {
		private static final int WIDTH = 180;
		private static final int HEIGHT = 115;

		private final RecipeType<RecipeHolder<SequencedAssemblyRecipe>> jeiType;
		private final IDrawable icon;
		private final AnimatedPress pressAnimation = new AnimatedPress(false);
		private final AnimatedSpout spoutAnimation = new AnimatedSpout();
		private final AnimatedDeployer deployerAnimation = new AnimatedDeployer();
		private final AnimatedSaw sawAnimation = new AnimatedSaw();

		private SequencedAssemblyCategory(IGuiHelper guiHelper) {
			jeiType = RecipeType.createRecipeHolderType(AllRecipeTypes.SEQUENCED_ASSEMBLY.getId());
			icon = guiHelper.createDrawableItemStack(new ItemStack(AllItems.PRECISION_MECHANISM.get()));
		}

		@Override
		public RecipeType<RecipeHolder<SequencedAssemblyRecipe>> getRecipeType() {
			return jeiType;
		}

		@Override
		public Component getTitle() {
			return Component.translatable("create.recipe.sequenced_assembly");
		}

		@Override
		public int getWidth() {
			return WIDTH;
		}

		@Override
		public int getHeight() {
			return HEIGHT;
		}

		@Override
		public IDrawable getIcon() {
			return icon;
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<SequencedAssemblyRecipe> holder, IFocusGroup focuses) {
			SequencedAssemblyRecipe recipe = holder.value();
			float outputChance = recipe.getOutputChance();
			boolean noRandomOutput = outputChance == 1;
			int resultOffset = noRandomOutput ? 0 : -7;
			ProcessingCategory.addSlot(builder, RecipeIngredientRole.INPUT, 27 + resultOffset, 91)
				.addItemStacks(recipe.getIngredient()
					.items()
					.map(itemHolder -> new ItemStack(itemHolder.value()))
					.toList());

			if (!recipe.resultPool.isEmpty()) {
				ItemStack stack = recipe.resultPool.getFirst().getStack();
				builder.addSlot(RecipeIngredientRole.OUTPUT, 132 + resultOffset, 91)
					.setStandardSlotBackground()
					.add(stack)
					.addRichTooltipCallback((view, tooltip) -> {
						if (!noRandomOutput)
							tooltip.add(Component.translatable("create.recipe.processing.chance",
								outputChance < 0.01 ? "<1" : Math.round(outputChance * 100))
								.withStyle(ChatFormatting.GOLD));
					});
			}

			int sequenceWidth = recipe.getSequence().size() * 28 - 3;
			int sequenceX = (WIDTH - sequenceWidth) / 2;
			int stepIndex = 0;
			for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence()) {
				IAssemblyRecipe assemblyRecipe = sequencedRecipe.getAsAssemblyRecipe();
				List<Ingredient> ingredients = new ArrayList<>();
				assemblyRecipe.addAssemblyIngredients(ingredients);
				if (sequencedRecipe.getRecipe() instanceof FillingRecipe
					&& !sequencedRecipe.getRecipe().getFluidIngredients().isEmpty())
					ProcessingCategory.addFluidSlot(builder, RecipeIngredientRole.INPUT,
						sequenceX + 4, 15, sequencedRecipe.getRecipe().getFluidIngredients().getFirst());
				else if (sequencedRecipe.getRecipe() instanceof DeployerApplicationRecipe && !ingredients.isEmpty())
					ProcessingCategory.addSlot(builder, RecipeIngredientRole.INPUT, sequenceX + 4, 15)
						.add(ingredients.getFirst());
				for (Ingredient ingredient : ingredients)
					builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
						.add(ingredient);
				for (SizedFluidIngredient ingredient : sequencedRecipe.getRecipe()
					.getFluidIngredients())
					builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
						.addIngredients(NeoForgeTypes.FLUID_STACK, matchingFluidStacks(ingredient));
				stepIndex++;
				sequenceX += 28;
				if (stepIndex >= 6)
					break;
			}
		}

		@Override
		public void draw(RecipeHolder<SequencedAssemblyRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
			SequencedAssemblyRecipe recipe = holder.value();
			boolean noRandomOutput = recipe.getOutputChance() == 1;
			int resultOffset = noRandomOutput ? 0 : -7;
			AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52 + resultOffset, 94);
			if (!noRandomOutput) {
				AllGuiTextures.JEI_CHANCE_SLOT.render(graphics, 150 + resultOffset, 90);
				var question = Component.literal("?").withStyle(ChatFormatting.BOLD);
				graphics.text(Minecraft.getInstance().font, question,
					158 + resultOffset - Minecraft.getInstance().font.width(question) / 2, 95, 0xffefefef, false);
			}

			int sequenceWidth = recipe.getSequence().size() * 28 - 3;
			int sequenceX = (WIDTH - sequenceWidth) / 2;
			var pose = graphics.pose();
			var font = Minecraft.getInstance().font;
			String[] romans = {"I", "II", "III", "IV", "V", "VI", "-"};
			int stepIndex = 0;
			for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence()) {
				String roman = romans[Math.min(stepIndex, 6)];
				graphics.text(font, roman, sequenceX + (25 - font.width(roman)) / 2, 2, 0xff888888, false);
				var step = sequencedRecipe.getRecipe();
				if (step instanceof FillingRecipe) {
					spoutAnimation.offset = stepIndex;
					var fluidIngredient = step.getFluidIngredients().getFirst();
					spoutAnimation.withFluids(matchingFluidStacks(fluidIngredient));
					pose.pushMatrix();
					pose.translate(sequenceX - 7, 50);
					pose.scale(.75f, .75f);
					spoutAnimation.draw(graphics, 25 / 2, 0);
					pose.popMatrix();
				} else if (step instanceof PressingRecipe) {
					pressAnimation.offset = stepIndex;
					pose.pushMatrix();
					pose.translate(sequenceX - 5, 50);
					pose.scale(.6f, .6f);
					pressAnimation.draw(graphics, 25 / 2, 0);
					pose.popMatrix();
				} else if (step instanceof DeployerApplicationRecipe) {
					deployerAnimation.offset = stepIndex;
					pose.pushMatrix();
					pose.translate(sequenceX - 7, 50);
					pose.scale(.75f, .75f);
					deployerAnimation.draw(graphics, 25 / 2, 0);
					pose.popMatrix();
				} else if (step instanceof CuttingRecipe) {
					pose.pushMatrix();
					pose.translate(sequenceX, 51.5f);
					pose.scale(.6f, .6f);
					sawAnimation.draw(graphics, 25 / 2, 30);
					pose.popMatrix();
				}
				stepIndex++;
				sequenceX += 28;
				if (stepIndex >= 6)
					break;
			}

			if (recipe.getLoops() > 1)
				drawLoopIndicator(graphics, recipe.getLoops(), 80, 104);
		}

		private static void drawLoopIndicator(GuiGraphicsExtractor graphics, int loops, int x, int y) {
			int color = 0xff303030;
			int scale = 1;
			drawPixel(graphics, x, y, 0, 0, scale, color);
			drawPixel(graphics, x, y, 1, 1, scale, color);
			drawPixel(graphics, x, y, 2, 2, scale, color);
			drawPixel(graphics, x, y, 0, 2, scale, color);
			drawPixel(graphics, x, y, 2, 0, scale, color);
			drawDigit(graphics, x + 5, y, loops, scale, color);
		}

		private static void drawDigit(GuiGraphicsExtractor graphics, int x, int y, int value, int scale, int color) {
			int digit = Math.max(0, Math.min(9, value));
			boolean[][] pixels = switch (digit) {
				case 0 -> new boolean[][] {{true, true, true}, {true, false, true}, {true, false, true}, {true, false, true}, {true, true, true}};
				case 1 -> new boolean[][] {{false, true, false}, {true, true, false}, {false, true, false}, {false, true, false}, {true, true, true}};
				case 2 -> new boolean[][] {{true, true, true}, {false, false, true}, {true, true, true}, {true, false, false}, {true, true, true}};
				case 3 -> new boolean[][] {{true, true, true}, {false, false, true}, {false, true, true}, {false, false, true}, {true, true, true}};
				case 4 -> new boolean[][] {{true, false, true}, {true, false, true}, {true, true, true}, {false, false, true}, {false, false, true}};
				case 5 -> new boolean[][] {{true, true, true}, {true, false, false}, {true, true, true}, {false, false, true}, {true, true, true}};
				case 6 -> new boolean[][] {{true, true, true}, {true, false, false}, {true, true, true}, {true, false, true}, {true, true, true}};
				case 7 -> new boolean[][] {{true, true, true}, {false, false, true}, {false, true, false}, {false, true, false}, {false, true, false}};
				case 8 -> new boolean[][] {{true, true, true}, {true, false, true}, {true, true, true}, {true, false, true}, {true, true, true}};
				default -> new boolean[][] {{true, true, true}, {true, false, true}, {true, true, true}, {false, false, true}, {true, true, true}};
			};
			for (int row = 0; row < pixels.length; row++)
				for (int col = 0; col < pixels[row].length; col++)
					if (pixels[row][col])
						drawPixel(graphics, x, y, col, row, scale, color);
		}

		private static void drawPixel(GuiGraphicsExtractor graphics, int x, int y, int col, int row, int scale, int color) {
			graphics.fill(x + col * scale, y + row * scale, x + col * scale + scale, y + row * scale + scale, color);
		}

		@Override
		public Identifier getIdentifier(RecipeHolder<SequencedAssemblyRecipe> holder) {
			return holder.id()
				.identifier();
		}

		private void registerRecipes(IRecipeRegistration registration, Collection<RecipeHolder<?>> loadedRecipes) {
			List<RecipeHolder<SequencedAssemblyRecipe>> recipes = new ArrayList<>();
			for (RecipeHolder<?> holder : loadedRecipes) {
				Recipe<?> recipe = holder.value();
				if (recipe.getType() == AllRecipeTypes.SEQUENCED_ASSEMBLY.getType() && recipe instanceof SequencedAssemblyRecipe sequencedRecipe)
					recipes.add(new RecipeHolder<>(holder.id(), sequencedRecipe));
			}
			if (!recipes.isEmpty())
				Create.LOGGER.info("Create JEI: registered {} {} recipes", recipes.size(), AllRecipeTypes.SEQUENCED_ASSEMBLY.getId());
			registration.addRecipes(jeiType, recipes);
		}

		private void registerCatalysts(IRecipeCatalystRegistration registration) {
			registration.addCraftingStation(jeiType, new ItemStack(AllBlocks.DEPLOYER.get()));
			registration.addCraftingStation(jeiType, new ItemStack(AllBlocks.MECHANICAL_PRESS.get()));
			registration.addCraftingStation(jeiType, new ItemStack(AllBlocks.SPOUT.get()));
			registration.addCraftingStation(jeiType, new ItemStack(AllBlocks.MECHANICAL_SAW.get()));
		}

	}

	private static class FanCookingCategory<T extends AbstractCookingRecipe> implements IRecipeCategory<RecipeHolder<T>> {
		private static final int WIDTH = 178;
		private static final int HEIGHT = 72;

		private final RecipeType<RecipeHolder<T>> jeiType;
		private final List<net.minecraft.world.item.crafting.RecipeType<?>> vanillaTypes;
		private final Class<T> recipeClass;
		private final Component title;
		private final IDrawable icon;
		private final BlockState attachedBlock;

		private FanCookingCategory(Identifier id, List<net.minecraft.world.item.crafting.RecipeType<?>> vanillaTypes, Class<T> recipeClass,
			Component title, ItemLike iconSecondary, BlockState attachedBlock) {
			this.jeiType = RecipeType.createRecipeHolderType(id);
			this.vanillaTypes = vanillaTypes;
			this.recipeClass = recipeClass;
			this.title = title;
			this.icon = new DoubleItemIcon(AllItems.PROPELLER::asStack, () -> new ItemStack(iconSecondary));
			this.attachedBlock = attachedBlock;
		}

		private static FanCookingCategory<AbstractCookingRecipe> blasting(IGuiHelper guiHelper) {
			return new FanCookingCategory<>(Create.asResource("fan_blasting"), List.of(
				net.minecraft.world.item.crafting.RecipeType.SMELTING,
				net.minecraft.world.item.crafting.RecipeType.BLASTING),
				AbstractCookingRecipe.class, Component.translatable("create.recipe.fan_blasting"), Items.LAVA_BUCKET,
				Fluids.LAVA.defaultFluidState().createLegacyBlock());
		}

		private static FanCookingCategory<SmokingRecipe> smoking(IGuiHelper guiHelper) {
			return new FanCookingCategory<>(Create.asResource("fan_smoking"), List.of(net.minecraft.world.item.crafting.RecipeType.SMOKING),
				SmokingRecipe.class, Component.translatable("create.recipe.fan_smoking"), Items.CAMPFIRE,
				Blocks.FIRE.defaultBlockState());
		}

		@Override
		public RecipeType<RecipeHolder<T>> getRecipeType() {
			return jeiType;
		}

		@Override
		public Component getTitle() {
			return title;
		}

		@Override
		public int getWidth() {
			return WIDTH;
		}

		@Override
		public int getHeight() {
			return HEIGHT;
		}

		@Override
		public IDrawable getIcon() {
			return icon;
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<T> holder, IFocusGroup focuses) {
			T recipe = holder.value();
			Ingredient input = recipe.input();
			if (!input.isEmpty())
				ProcessingCategory.addSlot(builder, RecipeIngredientRole.INPUT, 21, 48)
					.add(input);
			ItemStack result = resultItem(recipe, input);
			if (!result.isEmpty())
				ProcessingCategory.addSlot(builder, RecipeIngredientRole.OUTPUT, 141, 48)
					.add(result);
		}

		@Override
		public void draw(RecipeHolder<T> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
			AllGuiTextures.JEI_SHADOW.render(graphics, 46, 29);
			AllGuiTextures.JEI_LIGHT.render(graphics, 65, 39);
			AllGuiTextures.JEI_LONG_ARROW.render(graphics, 54, 51);
			renderAnimatedFan(graphics, 56, 33, attachedBlock);
		}

		@Override
		public Identifier getIdentifier(RecipeHolder<T> holder) {
			return holder.id()
				.identifier();
		}

		@SuppressWarnings("unchecked")
		private void registerRecipes(IRecipeRegistration registration, Collection<RecipeHolder<?>> loadedRecipes) {
			List<RecipeHolder<T>> recipes = new ArrayList<>();
			Set<Identifier> ids = new LinkedHashSet<>();
			for (RecipeHolder<?> holder : loadedRecipes) {
				Recipe<?> recipe = holder.value();
				if (vanillaTypes.contains(recipe.getType()) && recipeClass.isInstance(recipe)
					&& ids.add(holder.id().identifier()))
					recipes.add((RecipeHolder<T>) holder);
			}
			if (!recipes.isEmpty())
				Create.LOGGER.info("Create JEI: registered {} {} recipes", recipes.size(), jeiType.getUid());
			registration.addRecipes(jeiType, recipes);
		}

		private void registerCatalysts(IRecipeCatalystRegistration registration) {
			registration.addCraftingStation(jeiType, new ItemStack(AllBlocks.ENCASED_FAN.get()));
		}

		private static ItemStack resultItem(AbstractCookingRecipe recipe, Ingredient input) {
			ItemStack stack = input.items()
				.map(holder -> new ItemStack(holder.value()))
				.findFirst()
				.orElse(ItemStack.EMPTY);
			if (stack.isEmpty())
				return ItemStack.EMPTY;
			return recipe.assemble(new SingleRecipeInput(stack));
		}
	}

	private static class AutomaticBasinCategory implements IRecipeCategory<RecipeHolder<BasinRecipe>> {
		private enum Mode {
			SHAPELESS("automatic_shapeless", "create.recipe.automatic_shapeless", false),
			BREWING("automatic_brewing", "create.recipe.automatic_brewing", false),
			PACKING("automatic_packing", "create.recipe.automatic_packing", true);

			private final Identifier id;
			private final String title;
			private final boolean press;

			Mode(String id, String title, boolean press) {
				this.id = Create.asResource(id);
				this.title = title;
				this.press = press;
			}
		}

		private final Mode mode;
		private final RecipeType<RecipeHolder<BasinRecipe>> jeiType;
		private final IDrawable icon;
		private final AnimatedMixer mixer = new AnimatedMixer();
		private final AnimatedPress press = new AnimatedPress(true);
		private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();

		private AutomaticBasinCategory(Mode mode, IGuiHelper guiHelper) {
			this.mode = mode;
			this.jeiType = RecipeType.createRecipeHolderType(mode.id);
			ItemLike primary = mode.press ? AllBlocks.MECHANICAL_PRESS.get() : AllBlocks.MECHANICAL_MIXER.get();
			ItemLike secondary = switch (mode) {
				case SHAPELESS, PACKING -> Blocks.CRAFTING_TABLE;
				case BREWING -> Blocks.BREWING_STAND;
			};
			this.icon = new DoubleItemIcon(() -> new ItemStack(primary), () -> new ItemStack(secondary));
		}

		@Override
		public RecipeType<RecipeHolder<BasinRecipe>> getRecipeType() {
			return jeiType;
		}

		@Override
		public Component getTitle() {
			return Component.translatable(mode.title);
		}

		@Override
		public int getWidth() {
			return 177;
		}

		@Override
		public int getHeight() {
			return mode == Mode.BREWING ? 103 : 85;
		}

		@Override
		public IDrawable getIcon() {
			return icon;
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BasinRecipe> holder, IFocusGroup focuses) {
			ProcessingCategory.layoutBasin(builder, holder.value());
		}

		@Override
		public void draw(RecipeHolder<BasinRecipe> holder, IRecipeSlotsView recipeSlotsView,
			GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
			BasinRecipe recipe = holder.value();
			ProcessingCategory.drawBasinWidgets(recipe, graphics, mode == Mode.BREWING);
			if (recipe.getRequiredHeat() != HeatCondition.NONE)
				heater.withHeat(recipe.getRequiredHeat().visualizeAsBlazeBurner())
					.draw(graphics, getWidth() / 2 + 3, 55);
			if (mode.press)
				press.draw(graphics, getWidth() / 2 + 3, 34);
			else
				mixer.draw(graphics, getWidth() / 2 + 3, 34);
		}

		@Override
		public Identifier getIdentifier(RecipeHolder<BasinRecipe> holder) {
			return holder.id().identifier();
		}

		private void registerRecipes(IRecipeRegistration registration, Collection<RecipeHolder<?>> loadedRecipes) {
			List<RecipeHolder<BasinRecipe>> recipes = new ArrayList<>();
			if (mode == Mode.BREWING) {
				if (AllConfigs.server().recipes.allowBrewingInMixer.get() && Minecraft.getInstance().level != null)
					for (RecipeHolder<? extends BasinRecipe> holder : PotionMixingRecipes.createRecipes(Minecraft.getInstance().level))
						recipes.add(new RecipeHolder<>(holder.id(), holder.value()));
			} else {
				boolean enabled = mode == Mode.PACKING
					? AllConfigs.server().recipes.allowShapedSquareInPress.get()
					: AllConfigs.server().recipes.allowShapelessInMixer.get();
				if (enabled)
					for (RecipeHolder<?> holder : loadedRecipes) {
						if (!(holder.value() instanceof CraftingRecipe crafting)
							|| holder.value() instanceof MechanicalCraftingRecipe
							|| AllRecipeTypes.shouldIgnoreInAutomation(holder))
							continue;
						boolean canPack = MechanicalPressBlockEntity.canCompress(crafting);
						boolean accepted = mode == Mode.PACKING ? canPack
							: !(crafting instanceof ShapedRecipe) && crafting.placementInfo().ingredients().size() > 1 && !canPack;
						if (accepted && Minecraft.getInstance().level != null)
							recipes.add(BasinRecipe.convertShapeless(holder, Minecraft.getInstance().level));
					}
			}
			Create.LOGGER.info("Create JEI: registered {} {} recipes", recipes.size(), mode.id);
			registration.addRecipes(jeiType, recipes);
		}

		private void registerCatalysts(IRecipeCatalystRegistration registration) {
			registration.addCraftingStation(jeiType, new ItemStack(mode.press
				? AllBlocks.MECHANICAL_PRESS.get() : AllBlocks.MECHANICAL_MIXER.get()));
			registration.addCraftingStation(jeiType, new ItemStack(AllBlocks.BASIN.get()));
		}
	}

	private static class AutomaticCraftingCategory implements IRecipeCategory<RecipeHolder<CraftingRecipe>> {
		private static final int WIDTH = 177;
		private static final int HEIGHT = 107;
		private final RecipeType<RecipeHolder<CraftingRecipe>> jeiType =
			RecipeType.createRecipeHolderType(Create.asResource("automatic_shaped"));
		private final IDrawable icon;
		private final AnimatedCrafter crafter = new AnimatedCrafter();

		private AutomaticCraftingCategory(IGuiHelper guiHelper) {
			icon = guiHelper.createDrawableItemStack(new ItemStack(AllBlocks.MECHANICAL_CRAFTER.get()));
		}

		@Override
		public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
			return jeiType;
		}

		@Override
		public Component getTitle() {
			return Component.translatable("create.recipe.automatic_shaped");
		}

		@Override public int getWidth() { return WIDTH; }
		@Override public int getHeight() { return HEIGHT; }
		@Override public IDrawable getIcon() { return icon; }

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CraftingRecipe> holder, IFocusGroup focuses) {
			CraftingRecipe recipe = holder.value();
			int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 1;
			int height = recipe instanceof ShapedRecipe shaped ? shaped.getHeight() : 1;
			int x = 53 - width * 19 / 2;
			int y = 53 - height * 19 / 2;
			if (recipe instanceof ShapedRecipe shaped) {
				List<java.util.Optional<Ingredient>> ingredients = shaped.getIngredients();
				for (int i = 0; i < ingredients.size(); i++) {
					java.util.Optional<Ingredient> ingredient = ingredients.get(i);
					if (ingredient.isPresent() && !ingredient.get().isEmpty())
						ProcessingCategory.addSlot(builder, RecipeIngredientRole.INPUT,
							x + i % width * 19, y + i / width * 19).add(ingredient.get());
				}
			} else {
				List<Ingredient> ingredients = recipe.placementInfo().ingredients();
				for (int i = 0; i < ingredients.size(); i++)
					ProcessingCategory.addSlot(builder, RecipeIngredientRole.INPUT, x, y + i * 19)
						.add(ingredients.get(i));
			}
			ProcessingCategory.addSlot(builder, RecipeIngredientRole.OUTPUT, 134, 81)
				.add(recipe.display().stream().findFirst()
					.map(display -> display.result().resolveForFirstStack(
						net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(Minecraft.getInstance().level)))
					.orElse(ItemStack.EMPTY));
		}

		@Override
		public void draw(RecipeHolder<CraftingRecipe> holder, IRecipeSlotsView recipeSlotsView,
			GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
			AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 128, 59);
			crafter.draw(graphics, 129, 25);
			CraftingRecipe recipe = holder.value();
			int amount = recipe instanceof ShapedRecipe shaped
				? (int) shaped.getIngredients().stream().filter(java.util.Optional::isPresent).count()
				: (int) recipe.placementInfo().ingredients().stream().filter(ingredient -> !ingredient.isEmpty()).count();
			graphics.text(Minecraft.getInstance().font, Integer.toString(amount), 142, 39, 0xffffffff);
		}

		@Override
		public Identifier getIdentifier(RecipeHolder<CraftingRecipe> holder) {
			return holder.id().identifier();
		}

		private void registerRecipes(IRecipeRegistration registration, Collection<RecipeHolder<?>> loadedRecipes) {
			List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
			if (AllConfigs.server().recipes.allowRegularCraftingInCrafter.get())
				for (RecipeHolder<?> holder : loadedRecipes) {
					if (!(holder.value() instanceof CraftingRecipe crafting)
						|| holder.value() instanceof MechanicalCraftingRecipe
						|| AllRecipeTypes.shouldIgnoreInAutomation(holder))
						continue;
					if (crafting instanceof ShapedRecipe || crafting.placementInfo().ingredients().size() == 1)
						recipes.add(new RecipeHolder<>(holder.id(), crafting));
				}
			Create.LOGGER.info("Create JEI: registered {} automatic_shaped recipes", recipes.size());
			registration.addRecipes(jeiType, recipes);
		}

		private void registerCatalysts(IRecipeCatalystRegistration registration) {
			registration.addCraftingStation(jeiType, new ItemStack(AllBlocks.MECHANICAL_CRAFTER.get()));
		}
	}

	private static class ProcessingCategory<T extends ProcessingRecipe<?, ?>> implements IRecipeCategory<RecipeHolder<T>> {
		private static final int WIDTH = 177;
		private static final int HEIGHT = 70;
		private static final int SLOT_SIZE = 18;

		private final AllRecipeTypes createType;
		private final RecipeType<RecipeHolder<T>> jeiType;
		private final Component title;
		private final IDrawable icon;
		private final List<ItemStack> catalysts;
		private final AnimatedDeployer deployerAnimation = new AnimatedDeployer();
		private final AnimatedSpout spoutAnimation = new AnimatedSpout();
		private final AnimatedPress pressAnimation = new AnimatedPress(false);
		private final AnimatedPress basinPressAnimation = new AnimatedPress(true);
		private final AnimatedItemDrain drainAnimation = new AnimatedItemDrain();
		private final AnimatedCrushingWheels crushingAnimation = new AnimatedCrushingWheels();
		private final AnimatedMillstone millstoneAnimation = new AnimatedMillstone();
		private final AnimatedSaw sawAnimation = new AnimatedSaw();
		private final AnimatedMixer mixerAnimation = new AnimatedMixer();
		private final AnimatedBlazeBurner heaterAnimation = new AnimatedBlazeBurner();

		@SuppressWarnings("unchecked")
		private ProcessingCategory(AllRecipeTypes createType, IGuiHelper guiHelper) {
			this.createType = createType;
			this.jeiType = (RecipeType<RecipeHolder<T>>) (RecipeType<?>) RecipeType.createRecipeHolderType(createType.getId());
			this.title = Component.translatable(titleKey(createType));
			this.catalysts = catalystsFor(createType);
			this.icon = iconFor(createType, guiHelper, catalysts);
		}

		@Override
		public RecipeType<RecipeHolder<T>> getRecipeType() {
			return jeiType;
		}

		@Override
		public Component getTitle() {
			return title;
		}

		@Override
		public int getWidth() {
			return switch (createType) {
				case SPLASHING, HAUNTING -> 178;
				default -> WIDTH;
			};
		}

		@Override
		public int getHeight() {
			return switch (createType) {
				case CRUSHING, MIXING, COMPACTING, BASIN -> 103;
				case MILLING -> 53;
				case SANDPAPER_POLISHING -> 55;
				case SPLASHING, HAUNTING -> 72;
				case EMPTYING -> 50;
				default -> HEIGHT;
			};
		}

		@Override
		public IDrawable getIcon() {
			return icon;
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<T> holder, IFocusGroup focuses) {
			T recipe = holder.value();
			switch (createType) {
				case DEPLOYING -> layoutDeploying(builder, recipe);
				case ITEM_APPLICATION -> layoutItemApplication(builder, recipe);
				case PRESSING -> layoutPressing(builder, recipe);
				case FILLING -> layoutFilling(builder, recipe);
				case EMPTYING -> layoutEmptying(builder, recipe);
				case CRUSHING -> layoutCrushing(builder, recipe);
				case MILLING -> layoutMilling(builder, recipe);
				case CUTTING -> layoutCutting(builder, recipe);
				case SANDPAPER_POLISHING -> layoutPolishing(builder, recipe);
				case SPLASHING, HAUNTING -> layoutFan(builder, recipe);
				case MIXING, COMPACTING, BASIN -> layoutBasin(builder, recipe);
				case CONVERSION -> layoutConversion(builder, recipe);
				default -> layoutGeneric(builder, recipe);
			}
		}

		@Override
		public void draw(RecipeHolder<T> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
			T recipe = holder.value();
			switch (createType) {
				case DEPLOYING -> {
					AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
					AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29 + (recipe.getRollableResults()
						.size() > 2 ? -19 : 0));
					deployerAnimation.draw(graphics, getWidth() / 2 - 13, 22);
				}
				case FILLING -> {
					AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
					AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29);
					if (!recipe.getFluidIngredients().isEmpty()) {
						SizedFluidIngredient ingredient = recipe.getFluidIngredients().getFirst();
						spoutAnimation.withFluids(matchingFluidStacks(ingredient));
					}
					spoutAnimation.draw(graphics, getWidth() / 2 - 13, 22);
				}
				case ITEM_APPLICATION -> {
					AllGuiTextures.JEI_SHADOW.render(graphics, 62, 47);
					AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 74, 10);
					renderMachineIcon(graphics, new ItemStack(AllItems.BRASS_HAND.get()), 82, 38);
				}
				case PRESSING -> {
					AllGuiTextures.JEI_SHADOW.render(graphics, 61, 41);
					AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52, 54);
					pressAnimation.draw(graphics, getWidth() / 2 - 17, 32);
				}
				case EMPTYING -> {
					AllGuiTextures.JEI_SHADOW.render(graphics, 62, 37);
					AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 73, 4);
					if (!recipe.getFluidResults().isEmpty())
						drainAnimation.withFluid(recipe.getFluidResults().getFirst());
					drainAnimation.draw(graphics, getWidth() / 2 - 13, 40);
				}
				case CRUSHING -> {
					AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 72, 7);
					crushingAnimation.draw(graphics, 62, 59);
				}
				case MILLING -> {
					AllGuiTextures.JEI_ARROW.render(graphics, 85, 32);
					AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 43, 4);
					millstoneAnimation.draw(graphics, 48, 27);
				}
				case CUTTING -> {
					AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 70, 6);
					AllGuiTextures.JEI_SHADOW.render(graphics, 55, 55);
					sawAnimation.draw(graphics, 72, 42);
				}
				case SANDPAPER_POLISHING -> {
					AllGuiTextures.JEI_SHADOW.render(graphics, 61, 21);
					AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52, 32);
					renderPolishingTool(holder.value(), graphics);
				}
				case SPLASHING, HAUNTING -> {
					int size = recipe.getRollableResultsAsItemStacks()
						.size();
					int xOffsetAmount = 1 - Math.min(3, size);
					AllGuiTextures.JEI_SHADOW.render(graphics, 46, 29);
					(createType == AllRecipeTypes.HAUNTING ? AllGuiTextures.JEI_LIGHT : AllGuiTextures.JEI_SHADOW)
						.render(graphics, 65, 39);
					AllGuiTextures.JEI_LONG_ARROW.render(graphics, 7 * xOffsetAmount + 54, 51);
					BlockState attached = createType == AllRecipeTypes.HAUNTING
						? Blocks.SOUL_FIRE.defaultBlockState()
						: Fluids.WATER.defaultFluidState().createLegacyBlock();
					renderAnimatedFan(graphics, 56, 33, attached);
				}
				case MIXING, COMPACTING, BASIN -> {
					drawBasinWidgets(recipe, graphics, true);
					if (recipe.getRequiredHeat() != HeatCondition.NONE)
						heaterAnimation.withHeat(recipe.getRequiredHeat().visualizeAsBlazeBurner())
							.draw(graphics, getWidth() / 2 + 3, 55);
					if (createType == AllRecipeTypes.COMPACTING)
						basinPressAnimation.draw(graphics, getWidth() / 2 + 3, 34);
					else
						mixerAnimation.draw(graphics, getWidth() / 2 + 3, 34);
				}
				case CONVERSION -> {
					AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52, 20);
					AllGuiTextures.JEI_QUESTION_MARK.render(graphics, 77, 5);
				}
				default -> AllGuiTextures.JEI_ARROW.render(graphics, 63, 35);
			}
		}

		private static void layoutConversion(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			if (!recipe.getIngredients().isEmpty())
				addSlot(builder, RecipeIngredientRole.INPUT, 27, 17).add(recipe.getIngredients().getFirst());
			if (!recipe.getRollableResults().isEmpty())
				addSlot(builder, RecipeIngredientRole.OUTPUT, 132, 17)
					.add(recipe.getRollableResults().getFirst().getStack());
		}

		private static void layoutGeneric(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			int inputIndex = 0;
			for (Ingredient ingredient : recipe.getIngredients())
				if (!ingredient.isEmpty())
					addInput(builder, inputIndex++).add(ingredient);
			for (SizedFluidIngredient ingredient : recipe.getFluidIngredients())
				addFluidInput(builder, inputIndex++, ingredient);
			addOutputs(builder, recipe, 90, 12, 3);
		}

		private static void layoutDeploying(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			if (recipe instanceof DeployerApplicationRecipe deployingRecipe) {
				addSlot(builder, RecipeIngredientRole.INPUT, 27, 51).add(deployingRecipe.getProcessedItem());
				addSlot(builder, RecipeIngredientRole.INPUT, 51, 5).add(deployingRecipe.getRequiredHeldItem());
			} else {
				List<Ingredient> ingredients = recipe.getIngredients();
				if (!ingredients.isEmpty())
					addSlot(builder, RecipeIngredientRole.INPUT, 27, 51).add(ingredients.get(0));
				if (ingredients.size() > 1)
					addSlot(builder, RecipeIngredientRole.INPUT, 51, 5).add(ingredients.get(1));
			}
			addRollableOutputs(builder, recipe, 132, 51, 2, 132);
		}

		private static void layoutItemApplication(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			if (recipe instanceof ItemApplicationRecipe applicationRecipe) {
				addSlot(builder, RecipeIngredientRole.INPUT, 27, 38).add(applicationRecipe.getProcessedItem());
				addSlot(builder, RecipeIngredientRole.INPUT, 51, 5).add(applicationRecipe.getRequiredHeldItem());
			} else {
				List<Ingredient> ingredients = recipe.getIngredients();
				if (!ingredients.isEmpty())
					addSlot(builder, RecipeIngredientRole.INPUT, 27, 38).add(ingredients.get(0));
				if (ingredients.size() > 1)
					addSlot(builder, RecipeIngredientRole.INPUT, 51, 5).add(ingredients.get(1));
			}
			addRollableOutputs(builder, recipe, 132, 38, 2, 132);
		}

		private static void layoutPressing(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			List<Ingredient> ingredients = recipe.getIngredients();
			if (!ingredients.isEmpty())
				addSlot(builder, RecipeIngredientRole.INPUT, 27, 51).add(ingredients.get(0));
			addRollableOutputs(builder, recipe, 131, 50, 2, -1);
		}

		private static void layoutFilling(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			List<Ingredient> ingredients = recipe.getIngredients();
			if (!ingredients.isEmpty())
				addSlot(builder, RecipeIngredientRole.INPUT, 27, 51).add(ingredients.get(0));
			if (!recipe.getFluidIngredients().isEmpty())
				addFluidSlot(builder, RecipeIngredientRole.INPUT, 27, 32, recipe.getFluidIngredients().getFirst());
			addRollableOutputs(builder, recipe, 132, 51, 1, -1);
		}

		private static void layoutEmptying(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			List<Ingredient> ingredients = recipe.getIngredients();
			if (!ingredients.isEmpty())
				addSlot(builder, RecipeIngredientRole.INPUT, 27, 8).add(ingredients.get(0));
			if (!recipe.getFluidResults().isEmpty()) {
				FluidStack fluidStack = recipe.getFluidResults().getFirst();
				if (!fluidStack.isEmpty())
					addFluidSlot(builder, RecipeIngredientRole.OUTPUT, 132, 8, fluidStack);
			}
			addItemOutputs(builder, recipe, 132, 27, 1);
		}

		private static void layoutCrushing(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			List<Ingredient> ingredients = recipe.getIngredients();
			if (!ingredients.isEmpty())
				addSlot(builder, RecipeIngredientRole.INPUT, 51, 3).add(ingredients.get(0));
			addRollableOutputs(builder, recipe, 57, 84, 5, -1);
		}

		private static void layoutMilling(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			List<Ingredient> ingredients = recipe.getIngredients();
			if (!ingredients.isEmpty())
				addSlot(builder, RecipeIngredientRole.INPUT, 15, 9).add(ingredients.get(0));
			addRollableOutputs(builder, recipe, 133, 27, 2, 139);
		}

		private static void layoutCutting(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			List<Ingredient> ingredients = recipe.getIngredients();
			if (!ingredients.isEmpty())
				addSlot(builder, RecipeIngredientRole.INPUT, 44, 5).add(ingredients.get(0));
			addRollableOutputs(builder, recipe, 118, 48, 2, -1);
		}

		private static void layoutPolishing(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			List<Ingredient> ingredients = recipe.getIngredients();
			if (!ingredients.isEmpty())
				addSlot(builder, RecipeIngredientRole.INPUT, 27, 29).add(ingredients.get(0));
			addRollableOutputs(builder, recipe, 132, 29, 1, -1);
		}

		private static void renderPolishingTool(ProcessingRecipe<?, ?> recipe, GuiGraphicsExtractor graphics) {
			if (recipe.getIngredients().isEmpty())
				return;
			List<ItemStack> matchingStacks = recipe.getIngredients()
				.getFirst()
				.items()
				.map(itemHolder -> new ItemStack(itemHolder.value()))
				.toList();
			if (matchingStacks.isEmpty())
				return;
			ItemStack renderedSandpaper = AllItems.SAND_PAPER.asStack();
			renderedSandpaper.set(AllDataComponents.SAND_PAPER_POLISHING, new SandPaperItemComponent(matchingStacks.getFirst()));
			renderedSandpaper.set(AllDataComponents.SAND_PAPER_JEI, Unit.INSTANCE);
			GuiGameElement.of(renderedSandpaper)
				.scale(2)
				.at(72, 0, 0)
				.submit(graphics);
		}

		private static void renderMachineIcon(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
			GuiGameElement.of(stack)
				.at(x, y, 100)
				.submit(graphics);
		}

		private static void renderCrushingWheels(GuiGraphicsExtractor graphics, int x, int y) {
			BlockState wheel = AllBlocks.CRUSHING_WHEEL.getDefaultState()
				.setValue(BlockStateProperties.AXIS, Direction.Axis.X);
			GuiGameElement.of(wheel)
				.rotateBlock(0, 90, 0)
				.scale(24)
				.at(x, y, 100)
				.submit(graphics);
			GuiGameElement.of(wheel)
				.rotateBlock(0, -90, 0)
				.scale(24)
				.at(x + 26, y, 100)
				.submit(graphics);
		}

		private static void layoutFan(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			List<ProcessingOutput> results = recipe.getRollableResults();
			int xOffsetAmount = 1 - Math.min(3, results.size());
			List<Ingredient> ingredients = recipe.getIngredients();
			if (!ingredients.isEmpty())
				addSlot(builder, RecipeIngredientRole.INPUT, 5 * xOffsetAmount + 21, 48).add(ingredients.get(0));
			boolean excessive = results.size() > 9;
			for (int outputIndex = 0; outputIndex < results.size(); outputIndex++) {
				ProcessingOutput output = results.get(outputIndex);
				ItemStack stack = output.getStack();
				if (stack.isEmpty())
					continue;
				int xOffset = outputIndex % 3 * 19 + 9 * xOffsetAmount;
				int yOffset = outputIndex / 3 * -19 + (excessive ? 8 : 0);
				addSlot(builder, RecipeIngredientRole.OUTPUT, 141 + xOffset, 48 + yOffset)
					.add(stack)
					.addRichTooltipCallback((view, tooltip) -> {
						float chance = output.getChance();
						if (chance != 1)
							tooltip.add(Component.translatable("create.recipe.processing.chance", chance < 0.01 ? "<1" : (int) (chance * 100))
								.withStyle(ChatFormatting.GOLD));
					});
			}
		}

		private static void layoutBasin(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			int inputIndex = 0;
			int size = recipe.getIngredients()
				.size() + recipe.getFluidIngredients()
				.size();
			int xOffset = size < 3 ? (3 - size) * 19 / 2 : 0;
			for (Ingredient ingredient : recipe.getIngredients())
				if (!ingredient.isEmpty())
					addSlot(builder, RecipeIngredientRole.INPUT, 17 + xOffset + inputIndex % 3 * 19, 51 - inputIndex++ / 3 * 19)
						.add(ingredient);
			for (SizedFluidIngredient ingredient : recipe.getFluidIngredients())
				addFluidSlot(builder, RecipeIngredientRole.INPUT, 17 + xOffset + inputIndex % 3 * 19, 51 - inputIndex++ / 3 * 19, ingredient);
			addBasinOutputs(builder, recipe);
			HeatCondition requiredHeat = recipe.getRequiredHeat();
			if (!requiredHeat.testBlazeBurner(HeatLevel.NONE))
				builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81)
					.add(AllBlocks.BLAZE_BURNER.asStack());
			if (!requiredHeat.testBlazeBurner(HeatLevel.KINDLED))
				builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 153, 81)
					.add(AllItems.BLAZE_CAKE.asStack());
		}

		private static void drawBasinWidgets(ProcessingRecipe<?, ?> recipe, GuiGraphicsExtractor graphics, boolean needsHeating) {
			int vRows = (1 + recipe.getFluidResults()
				.size() + recipe.getRollableResults()
				.size()) / 2;
			if (vRows <= 2)
				AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, -19 * (vRows - 1) + 32);

			AllGuiTextures shadow = recipe.getRequiredHeat() == HeatCondition.NONE ? AllGuiTextures.JEI_SHADOW : AllGuiTextures.JEI_LIGHT;
			shadow.render(graphics, 81, 58 + (recipe.getRequiredHeat() == HeatCondition.NONE ? 10 : 30));
			if (!needsHeating)
				return;
			HeatCondition requiredHeat = recipe.getRequiredHeat();
			AllGuiTextures heatBar = requiredHeat == HeatCondition.NONE
				? AllGuiTextures.JEI_NO_HEAT_BAR : AllGuiTextures.JEI_HEAT_BAR;
			heatBar.render(graphics, 4, 80);
			graphics.text(Minecraft.getInstance().font, Component.translatable("create." + requiredHeat.getTranslationKey()),
				9, 86, 0xff000000 | requiredHeat.getColor(), false);
		}

		private static void addOutputs(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe, int x, int y, int columns) {
			int outputIndex = addItemOutputs(builder, recipe, x, y, columns);
			for (FluidStack fluidStack : recipe.getFluidResults()) {
				if (fluidStack.isEmpty() || fluidStack.getFluid() == Fluids.EMPTY)
					continue;
				addFluidSlot(builder, RecipeIngredientRole.OUTPUT, x + outputIndex % columns * SLOT_SIZE, y - outputIndex / columns * SLOT_SIZE, fluidStack);
				outputIndex++;
			}
		}

		private static void addBasinOutputs(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe) {
			int size = recipe.getRollableResults()
				.size() + recipe.getFluidResults()
				.size();
			int outputIndex = 0;
			for (ProcessingOutput output : recipe.getRollableResults()) {
				ItemStack stack = output.getStack();
				if (stack.isEmpty())
					continue;
				int xPosition = 142 - (size % 2 != 0 && outputIndex == size - 1 ? 0 : outputIndex % 2 == 0 ? 10 : -9);
				int yPosition = 51 - 19 * (outputIndex / 2);
				addSlot(builder, RecipeIngredientRole.OUTPUT, xPosition, yPosition)
					.add(stack)
					.addRichTooltipCallback((view, tooltip) -> {
						float chance = output.getChance();
						if (chance != 1)
							tooltip.add(Component.translatable("create.recipe.processing.chance", chance < 0.01 ? "<1" : (int) (chance * 100))
								.withStyle(ChatFormatting.GOLD));
					});
				outputIndex++;
			}
			for (FluidStack fluidStack : recipe.getFluidResults()) {
				if (fluidStack.isEmpty() || fluidStack.getFluid() == Fluids.EMPTY)
					continue;
				int xPosition = 142 - (size % 2 != 0 && outputIndex == size - 1 ? 0 : outputIndex % 2 == 0 ? 10 : -9);
				int yPosition = 51 - 19 * (outputIndex / 2);
				addFluidSlot(builder, RecipeIngredientRole.OUTPUT, xPosition, yPosition, fluidStack);
				outputIndex++;
			}
		}

		private static void addRollableOutputs(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe, int x, int y, int columns, int singleX) {
			List<ProcessingOutput> results = recipe.getRollableResults();
			boolean single = results.size() == 1 && singleX >= 0;
			for (int outputIndex = 0; outputIndex < results.size(); outputIndex++) {
				ProcessingOutput output = results.get(outputIndex);
				ItemStack stack = output.getStack();
				if (stack.isEmpty())
					continue;
				int slotX = single ? singleX : x + outputIndex % columns * 19;
				int slotY = y - outputIndex / columns * 19;
				addSlot(builder, RecipeIngredientRole.OUTPUT, slotX, slotY)
					.add(stack)
					.addRichTooltipCallback((view, tooltip) -> {
						float chance = output.getChance();
						if (chance != 1)
							tooltip.add(Component.translatable("create.recipe.processing.chance", chance < 0.01 ? "<1" : (int) (chance * 100))
								.withStyle(ChatFormatting.GOLD));
					});
			}
		}

		private static int addItemOutputs(IRecipeLayoutBuilder builder, ProcessingRecipe<?, ?> recipe, int x, int y, int columns) {
			int outputIndex = 0;
			for (ProcessingOutput output : recipe.getRollableResults()) {
				ItemStack stack = output.getStack();
				if (stack.isEmpty())
					continue;
				addSlot(builder, RecipeIngredientRole.OUTPUT, x + outputIndex % columns * SLOT_SIZE, y - outputIndex / columns * SLOT_SIZE)
					.add(stack)
					.addRichTooltipCallback((view, tooltip) -> {
						float chance = output.getChance();
						if (chance != 1)
							tooltip.add(Component.translatable("create.recipe.processing.chance", chance < 0.01 ? "<1" : (int) (chance * 100))
								.withStyle(ChatFormatting.GOLD));
					});
				outputIndex++;
			}
			return outputIndex;
		}

		@Override
		public Identifier getIdentifier(RecipeHolder<T> holder) {
			return holder.id()
				.identifier();
		}

		@SuppressWarnings("unchecked")
		private void registerRecipes(IRecipeRegistration registration, Collection<RecipeHolder<?>> loadedRecipes,
			IIngredientManager ingredientManager) {
			List<RecipeHolder<T>> recipes = new ArrayList<>();
			net.minecraft.world.item.crafting.RecipeType<?> minecraftType = createType.getType();
			for (RecipeHolder<?> holder : loadedRecipes) {
				Recipe<?> recipe = holder.value();
				if (recipe.getType() == minecraftType && recipe instanceof ProcessingRecipe<?, ?>)
					recipes.add((RecipeHolder<T>) holder);
				if (createType == AllRecipeTypes.DEPLOYING && recipe.getType() == AllRecipeTypes.SANDPAPER_POLISHING.getType())
					recipes.add((RecipeHolder<T>) DeployerApplicationRecipe.convert(holder));
			}
			if (createType == AllRecipeTypes.CONVERSION)
				recipes.addAll((List<RecipeHolder<T>>) (List<?>) createConversionRecipes());
			if (createType == AllRecipeTypes.FILLING)
				recipes.addAll((List<RecipeHolder<T>>) (List<?>) createGeneratedFillingRecipes(ingredientManager));
			if (createType == AllRecipeTypes.EMPTYING)
				recipes.addAll((List<RecipeHolder<T>>) (List<?>) createGeneratedEmptyingRecipes(ingredientManager));
			if (createType == AllRecipeTypes.SPLASHING)
				recipes.addAll((List<RecipeHolder<T>>) (List<?>) createConcreteSplashingRecipes());
			if (createType == AllRecipeTypes.ITEM_APPLICATION)
				recipes.addAll((List<RecipeHolder<T>>) (List<?>) LogStrippingFakeRecipes.createRecipes());
			if (createType == AllRecipeTypes.DEPLOYING)
				for (RecipeHolder<?> holder : LogStrippingFakeRecipes.createRecipes())
					recipes.add((RecipeHolder<T>) DeployerApplicationRecipe.convert(holder));
			if (!recipes.isEmpty())
				Create.LOGGER.info("Create JEI: registered {} {} recipes", recipes.size(), createType.getId());
			registration.addRecipes(jeiType, recipes);
		}

		private void registerCatalysts(IRecipeCatalystRegistration registration) {
			for (ItemStack catalyst : catalysts)
				registration.addCraftingStation(jeiType, catalyst);
		}

		private static List<ItemStack> catalystsFor(AllRecipeTypes type) {
			return switch (type) {
				case CRUSHING -> List.of(new ItemStack(AllBlocks.CRUSHING_WHEEL.get()));
				case CUTTING -> List.of(new ItemStack(AllBlocks.MECHANICAL_SAW.get()));
				case MILLING -> List.of(new ItemStack(AllBlocks.MILLSTONE.get()));
				case BASIN -> List.of(new ItemStack(AllBlocks.BASIN.get()));
				case MIXING -> List.of(new ItemStack(AllBlocks.MECHANICAL_MIXER.get()), new ItemStack(AllBlocks.BASIN.get()));
				case COMPACTING -> List.of(new ItemStack(AllBlocks.MECHANICAL_PRESS.get()), new ItemStack(AllBlocks.BASIN.get()));
				case PRESSING -> List.of(new ItemStack(AllBlocks.MECHANICAL_PRESS.get()));
				case SANDPAPER_POLISHING -> List.of(new ItemStack(AllItems.SAND_PAPER.get()), new ItemStack(AllItems.RED_SAND_PAPER.get()));
				case SPLASHING, HAUNTING -> List.of(new ItemStack(AllBlocks.ENCASED_FAN.get()));
				case DEPLOYING -> List.of(new ItemStack(AllBlocks.DEPLOYER.get()), new ItemStack(AllBlocks.DEPOT.get()), new ItemStack(AllItems.BELT_CONNECTOR.get()));
				case FILLING -> List.of(new ItemStack(AllBlocks.SPOUT.get()));
				case EMPTYING -> List.of(new ItemStack(AllBlocks.ITEM_DRAIN.get()));
				case ITEM_APPLICATION -> List.of(new ItemStack(AllItems.BRASS_HAND.get()));
				default -> List.of();
			};
		}

		private static IDrawable iconFor(AllRecipeTypes type, IGuiHelper guiHelper, List<ItemStack> catalysts) {
			return switch (type) {
				case MILLING -> doubleIcon(AllBlocks.MILLSTONE.get(), AllItems.WHEAT_FLOUR.get());
				case CRUSHING -> doubleIcon(AllBlocks.CRUSHING_WHEEL.get(), AllItems.CRUSHED_GOLD.get());
				case PRESSING -> doubleIcon(AllBlocks.MECHANICAL_PRESS.get(), AllItems.IRON_SHEET.get());
				case SPLASHING -> doubleIcon(AllItems.PROPELLER.get(), Items.WATER_BUCKET);
				case HAUNTING -> doubleIcon(AllItems.PROPELLER.get(), Items.SOUL_CAMPFIRE);
				case MIXING -> doubleIcon(AllBlocks.MECHANICAL_MIXER.get(), AllBlocks.BASIN.get());
				case COMPACTING -> doubleIcon(AllBlocks.MECHANICAL_PRESS.get(), AllBlocks.BASIN.get());
				case CUTTING -> doubleIcon(AllBlocks.MECHANICAL_SAW.get(), Items.OAK_LOG);
				case FILLING -> doubleIcon(AllBlocks.SPOUT.get(), Items.WATER_BUCKET);
				case EMPTYING -> doubleIcon(AllBlocks.ITEM_DRAIN.get(), Items.WATER_BUCKET);
				default -> guiHelper.createDrawableItemStack(catalysts.isEmpty()
					? new ItemStack(Items.CRAFTING_TABLE) : catalysts.getFirst());
			};
		}

		private static IDrawable doubleIcon(ItemLike primary, ItemLike secondary) {
			return new DoubleItemIcon(() -> new ItemStack(primary), () -> new ItemStack(secondary));
		}

		private static String titleKey(AllRecipeTypes type) {
			return switch (type) {
				case CONVERSION -> "create.recipe.mystery_conversion";
				case BASIN -> "create.recipe.mixing";
				case SPLASHING -> "create.recipe.fan_washing";
				case HAUNTING -> "create.recipe.fan_haunting";
				case CUTTING -> "create.recipe.sawing";
				case FILLING -> "create.recipe.spout_filling";
				case EMPTYING -> "create.recipe.draining";
				case COMPACTING -> "create.recipe.packing";
				default -> type.getId()
					.getNamespace() + ".recipe." + type.getId()
					.getPath();
			};
		}

		private static mezz.jei.api.gui.builder.IRecipeSlotBuilder addInput(IRecipeLayoutBuilder builder, int index) {
			return builder.addSlot(RecipeIngredientRole.INPUT, 6 + index % 3 * SLOT_SIZE, 12 + index / 3 * SLOT_SIZE)
				.setStandardSlotBackground();
		}

		private static void addFluidInput(IRecipeLayoutBuilder builder, int index, SizedFluidIngredient ingredient) {
			addFluidSlot(builder, RecipeIngredientRole.INPUT, 6 + index % 3 * SLOT_SIZE, 12 + index / 3 * SLOT_SIZE, ingredient);
		}

		private static IRecipeSlotBuilder addSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
			return builder.addSlot(role, x, y)
				.setStandardSlotBackground();
		}

		private static IRecipeSlotBuilder addRenderOnly(IRecipeLayoutBuilder builder, int x, int y) {
			return builder.addSlot(RecipeIngredientRole.RENDER_ONLY, x, y);
		}

		private static IRecipeSlotBuilder addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, SizedFluidIngredient ingredient) {
			List<FluidStack> fluids = matchingFluidStacks(ingredient);
			return addSlot(builder, role, x, y)
				.addIngredients(NeoForgeTypes.FLUID_STACK, fluids)
				.setFluidRenderer(ingredient.amount(), false, 16, 16);
		}

		private static IRecipeSlotBuilder addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, FluidStack fluidStack) {
			return addSlot(builder, role, x, y)
				.addIngredient(NeoForgeTypes.FLUID_STACK, fluidStack)
				.setFluidRenderer(fluidStack.getAmount(), false, 16, 16);
		}
	}

	private static List<RecipeHolder<com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe>> createConcreteSplashingRecipes() {
		List<RecipeHolder<com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe>> recipes = new ArrayList<>();
		for (var entry : BuiltInRegistries.ITEM.entrySet()) {
			if (!(entry.getValue() instanceof BlockItem blockItem)
				|| !(blockItem.getBlock() instanceof ConcretePowderBlock powder))
				continue;
			var concrete = ((ConcretePowderBlockAccessor) powder).create$getConcrete();
			Identifier id = Create.asResource("runtime_generated/concrete/" + entry.getKey().identifier().getPath());
			var recipe = new StandardProcessingRecipe.Builder<>(
				com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe::new, id)
				.withItemIngredients(Ingredient.of(entry.getValue()))
				.withSingleItemOutput(new ItemStack(concrete))
				.build();
			recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe));
		}
		return recipes;
	}

	private static List<RecipeHolder<ConversionRecipe>> createConversionRecipes() {
		return List.of(
			ConversionRecipe.create(AllItems.EMPTY_BLAZE_BURNER.asStack(), AllBlocks.BLAZE_BURNER.asStack()),
			ConversionRecipe.create(AllBlocks.PECULIAR_BELL.asStack(), AllBlocks.HAUNTED_BELL.asStack())
		);
	}

}
