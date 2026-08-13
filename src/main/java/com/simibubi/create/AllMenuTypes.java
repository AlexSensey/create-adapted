package com.simibubi.create;

import com.simibubi.create.content.equipment.blueprint.BlueprintMenu;
import com.simibubi.create.content.equipment.blueprint.BlueprintScreen;
import com.simibubi.create.content.equipment.toolbox.ToolboxMenu;
import com.simibubi.create.content.equipment.toolbox.ToolboxScreen;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSetItemMenu;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSetItemScreen;
import com.simibubi.create.content.logistics.filter.AttributeFilterMenu;
import com.simibubi.create.content.logistics.filter.AttributeFilterScreen;
import com.simibubi.create.content.logistics.filter.FilterMenu;
import com.simibubi.create.content.logistics.filter.FilterScreen;
import com.simibubi.create.content.logistics.filter.PackageFilterMenu;
import com.simibubi.create.content.logistics.filter.PackageFilterScreen;
import com.simibubi.create.content.logistics.packagePort.PackagePortMenu;
import com.simibubi.create.content.logistics.packagePort.PackagePortScreen;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerMenu;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerScreen;
import com.simibubi.create.content.schematics.cannon.SchematicannonMenu;
import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;
import com.simibubi.create.content.schematics.table.SchematicTableMenu;
import com.simibubi.create.content.schematics.table.SchematicTableScreen;
import com.simibubi.create.content.trains.schedule.ScheduleMenu;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;
import com.tterrag.registrate.builders.MenuBuilder.ForgeMenuFactory;
import com.tterrag.registrate.builders.MenuBuilder.ScreenFactory;
import com.tterrag.registrate.util.entry.MenuEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class AllMenuTypes {

	public static final MenuEntry<SchematicTableMenu> SCHEMATIC_TABLE =
		register("schematic_table", SchematicTableMenu::new, "com.simibubi.create.content.schematics.table.SchematicTableScreen");

	public static final MenuEntry<SchematicannonMenu> SCHEMATICANNON =
		register("schematicannon", SchematicannonMenu::new, "com.simibubi.create.content.schematics.cannon.SchematicannonScreen");

	public static final MenuEntry<FilterMenu> FILTER =
		register("filter", FilterMenu::new, "com.simibubi.create.content.logistics.filter.FilterScreen");

	public static final MenuEntry<AttributeFilterMenu> ATTRIBUTE_FILTER =
		register("attribute_filter", AttributeFilterMenu::new, "com.simibubi.create.content.logistics.filter.AttributeFilterScreen");

	public static final MenuEntry<PackageFilterMenu> PACKAGE_FILTER =
		register("package_filter", PackageFilterMenu::new, "com.simibubi.create.content.logistics.filter.PackageFilterScreen");

	public static final MenuEntry<BlueprintMenu> CRAFTING_BLUEPRINT =
		register("crafting_blueprint", BlueprintMenu::new, "com.simibubi.create.content.equipment.blueprint.BlueprintScreen");

	public static final MenuEntry<LinkedControllerMenu> LINKED_CONTROLLER =
		register("linked_controller", LinkedControllerMenu::new, "com.simibubi.create.content.redstone.link.controller.LinkedControllerScreen");

	public static final MenuEntry<ToolboxMenu> TOOLBOX =
		register("toolbox", ToolboxMenu::new, "com.simibubi.create.content.equipment.toolbox.ToolboxScreen");

	public static final MenuEntry<ScheduleMenu> SCHEDULE =
		register("schedule", ScheduleMenu::new, "com.simibubi.create.content.trains.schedule.ScheduleScreen");

	public static final MenuEntry<StockKeeperCategoryMenu> STOCK_KEEPER_CATEGORY =
		register("stock_keeper_category", StockKeeperCategoryMenu::new, "com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryScreen");

	public static final MenuEntry<StockKeeperRequestMenu> STOCK_KEEPER_REQUEST =
		register("stock_keeper_request", StockKeeperRequestMenu::new, "com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen");

	public static final MenuEntry<PackagePortMenu> PACKAGE_PORT =
		register("package_port", PackagePortMenu::new, "com.simibubi.create.content.logistics.packagePort.PackagePortScreen");

	public static final MenuEntry<RedstoneRequesterMenu> REDSTONE_REQUESTER =
		register("redstone_requester", RedstoneRequesterMenu::new, "com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen");

	public static final MenuEntry<FactoryPanelSetItemMenu> FACTORY_PANEL_SET_ITEM =
		register("factory_panel_set_item", FactoryPanelSetItemMenu::new, "com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSetItemScreen");

	private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> MenuEntry<C> register(
		String name, ForgeMenuFactory<C> factory, String screenClassName) {
		return Create.registrate()
			.menu(name, factory, lazyScreen(screenClassName))
			.register();
	}

	@SuppressWarnings("unchecked")
	private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>>
		NonNullSupplier<ScreenFactory<C, S>> lazyScreen(String screenClassName) {
		return () -> (menu, inventory, title) -> {
			try {
				Class<?> screenClass = Class.forName(screenClassName);
				return (S) screenClass.getConstructor(menu.getClass(), Inventory.class, Component.class)
					.newInstance(menu, inventory, title);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Could not open Create menu screen " + screenClassName, e);
			}
		};
	}

	public static void register() {
	}

}
