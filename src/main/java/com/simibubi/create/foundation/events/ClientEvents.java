package com.simibubi.create.foundation.events;

import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.actors.seat.ContraptionPlayerPassengerRotation;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchHandler;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorConnectionHandler;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionHandler;
import com.simibubi.create.content.logistics.depot.EjectorTargetHandler;
import com.simibubi.create.content.logistics.packagePort.PackagePortTargetSelectionHandler;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedClientHandler;
import com.simibubi.create.content.logistics.tableCloth.TableClothOverlayRenderer;
import com.simibubi.create.content.contraptions.chassis.ChassisRangeDisplay;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.minecart.CouplingHandlerClient;
import com.simibubi.create.content.contraptions.minecart.CouplingRenderer;
import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.content.equipment.clipboard.ClipboardValueSettingsHandler;
import com.simibubi.create.content.equipment.armor.RemainingAirOverlay;
import com.simibubi.create.content.equipment.armor.NetheriteBacktankFirstPersonRenderer;
import com.simibubi.create.content.equipment.armor.CardboardArmorStealthOverlay;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripRenderHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryHandler;
import com.simibubi.create.content.equipment.zapper.terrainzapper.WorldshaperRenderHandler;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorHandler;
import com.simibubi.create.content.kinetics.turntable.TurntableHandler;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointHandler;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyClient;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler;
import com.simibubi.create.content.redstone.link.LinkRenderer;
import com.simibubi.create.content.redstone.displayLink.ClickToLinkBlockItem;
import com.simibubi.create.content.redstone.displayLink.ClickToLinkClient;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.content.trains.TrainHUD;
import com.simibubi.create.content.trains.entity.CarriageCouplingRenderer;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.content.trains.track.CurvedTrackInteraction;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.content.trains.track.TrackPlacementClient;
import com.simibubi.create.content.trains.track.TrackPlacementOverlay;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueHandler;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueLabelRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueRenderer;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.recipe.CreateRecipeClientCache;
import com.simibubi.create.foundation.render.SelectionBoxRenderer;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import com.simibubi.create.CreateClient;
import com.simibubi.create.infrastructure.command.AllCommands;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.outliner.Outliner;
import net.createmod.catnip.api.client.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.api.client.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {

	@SubscribeEvent
	public static void registerClientCommands(RegisterClientCommandsEvent event) {
		AllCommands.registerClient(event.getDispatcher());
	}

	@SubscribeEvent
	public static void preTick(ClientTickEvent.Pre event) {
		ControlsHandler.tick();
		LinkedControllerClientHandler.tick();
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		ServerSpeedProvider.clientTick();
		Outliner.getInstance().tickOutlines();
		BlueprintOverlayRenderer.tick();
		TableClothOverlayRenderer.tick();
		CreateClient.VALUE_SETTINGS_HANDLER.tick();
		CreateClient.SCHEMATIC_SENDER.tick();
		CreateClient.SCHEMATIC_AND_QUILL_HANDLER.tick();
		CreateClient.SCHEMATIC_HANDLER.tick();
		CreateClient.GLUE_HANDLER.tick();
		CreateClient.ZAPPER_RENDER_HANDLER.tick();
		WorldshaperRenderHandler.tick();
		CreateClient.POTATO_CANNON_RENDER_HANDLER.tick();
		NetheriteBacktankFirstPersonRenderer.clientTick();
		CardboardArmorStealthOverlay.clientTick();
		ExtendoGripRenderHandler.tick();
		if (Minecraft.getInstance().level != null)
			ContraptionHandler.tick(Minecraft.getInstance().level);
		if (Minecraft.getInstance().level != null)
			CapabilityMinecartController.tick(Minecraft.getInstance().level);
		if (Minecraft.getInstance().level != null)
			CreateClient.SOUL_PULSE_EFFECT_HANDLER.tick(Minecraft.getInstance().level);
		FilteringRenderer.tick();
		LogisticallyLinkedClientHandler.tick();
		LinkRenderer.tick();
		ScrollValueHandler.tick();
		ScrollValueRenderer.tick();
		ClipboardValueSettingsHandler.ClientEvents.clientTick();
		BeltConnectorHandler.tick();
		ChassisRangeDisplay.tick();
		ArmInteractionPointHandler.tick();
		TrackPlacementClient.clientTick();
		CurvedTrackInteraction.clientTick();
		TrackTargetingClient.clientTick();
		TrainHUD.tick();
		TrainRelocator.clientTick();
		ChainConveyorInteractionHandler.clientTick();
		ChainConveyorRidingHandler.clientTick();
		ChainConveyorConnectionHandler.clientTick();
		PackagePortTargetSelectionHandler.tick();
		FactoryPanelConnectionHandler.clientTick();
		EjectorTargetHandler.tick();
		ClickToLinkBlockItem.clientTick();
		ToolboxHandlerClient.clientTick();
		RadialWrenchHandler.clientTick();
		CouplingHandlerClient.tick();
		ContraptionPlayerPassengerRotation.tick();
	}

	@SubscribeEvent
	public static void renderFrame(RenderFrameEvent.Pre event) {
		TurntableHandler.gameRenderFrame(event.getPartialTick());
		ContraptionPlayerPassengerRotation.frame();
	}

	@SubscribeEvent
	public static void onUnloadWorld(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
		if (event.getLevel().isClientSide()) {
			CreateClient.SOUL_PULSE_EFFECT_HANDLER.refresh();
			CreateRecipeClientCache.clear();
		}
	}

	@SubscribeEvent
	public static void recipesReceived(RecipesReceivedEvent event) {
		CreateRecipeClientCache.onRecipesReceived(event);
	}

	@SubscribeEvent
	public static void addToItemTooltip(ItemTooltipEvent event) {
		SequencedAssemblyRecipe.addToTooltip(event);
		TooltipModifier modifier = TooltipModifier.REGISTRY.get(event.getItemStack()
			.getItem());
		if (modifier != null)
			modifier.modify(event);
	}

	@SubscribeEvent
	public static void renderLevel(RenderLevelStageEvent.AfterTranslucentFeatures event) {
		SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
		Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera()
			.position();
		ScrollValueRenderer.render(event.getPoseStack(), buffer, camera);
		Outliner.getInstance().renderOutlines(event.getPoseStack(), buffer, camera, AnimationTickHolder.getPartialTicks());
		ArmInteractionPointHandler.renderSelection(event.getPoseStack(), buffer, camera);
		CreateClient.SCHEMATIC_HANDLER.render(event.getPoseStack(), buffer, camera);
		ChainConveyorConnectionHandler.drawConnectionPreview(event.getPoseStack(), buffer, camera);
		ChainConveyorInteractionHandler.drawCustomBlockSelection(event.getPoseStack(), buffer, camera);
		buffer.draw();
	}

	@SubscribeEvent
	public static void submitCustomGeometry(SubmitCustomGeometryEvent event) {
		CreateClient.SCHEMATIC_AND_QUILL_HANDLER.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState);
		CreateClient.SCHEMATIC_HANDLER.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState);
		WorldshaperRenderHandler.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		SymmetryHandler.Client.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		CreateClient.ZAPPER_RENDER_HANDLER.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		ChainConveyorRenderer.submitConnectionPreview(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		PackagePortTargetSelectionHandler.submitPlacementPreview(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		LogisticallyLinkedClientHandler.submitPreviewBounds(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		FilteringRenderer.submitFactoryPanelValueBox(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		FilteringRenderer.submitTableClothValueBox(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		FilteringRenderer.submitTrackObserverValueBox(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		FilteringRenderer.submitSmartObserverValueBox(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		FilteringRenderer.submitSmartChuteValueBox(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		ScrollValueLabelRenderer.submitMotorValueBox(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		CreateClient.GLUE_HANDLER.submitPreview(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
		int panelColor = AnimationTickHolder.getTicks() % 16 > 8 ? 0xFF38B764 : 0xFFA7F070;
		SelectionBoxRenderer.submitExtrudedFrame(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos, FactoryPanelConnectionHandler.getSelectedPreviewBox(),
			FactoryPanelConnectionHandler.getSelectedPreviewOutwardDirection(), panelColor, 1 / 16f);
		SelectionBoxRenderer.submitExtrudedFrame(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos, FactoryPanelConnectionHandler.getRelocationPreviewBox(),
			FactoryPanelConnectionHandler.getRelocationPreviewOutwardDirection(), 0xFFEEEEEE, 1 / 16f);
		SelectionBoxRenderer.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos, ClickToLinkClient.getPreviewBounds(), 0xffcb74);
		SelectionBoxRenderer.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos, EjectorTargetHandler.getTargetPreviewBounds(), 0xffcb74);
		SelectionBoxRenderer.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos, EjectorTargetHandler.getPlacementPreviewBounds(),
			EjectorTargetHandler.getPlacementPreviewColor());
		TrackBlockOutline.submitCurveSelection(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState);
		TrackPlacementClient.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState);
		TrackTargetingClient.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState);
		TrainRelocator.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState);
		CouplingRenderer.submit(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState);
		CarriageCouplingRenderer.submitAll(event.getPoseStack(), event.getSubmitNodeCollector(),
			event.getLevelRenderState().cameraRenderState.pos);
	}

	public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.HOTBAR, Create.asResource("goggle_overlay"),
			GoggleOverlayRenderer.OVERLAY);
		event.registerAbove(VanillaGuiLayers.AIR_LEVEL, Create.asResource("remaining_air"),
			RemainingAirOverlay.OVERLAY);
		event.registerAbove(VanillaGuiLayers.HOTBAR, Create.asResource("train_hud"),
			TrainHUD.OVERLAY);
		event.registerAbove(VanillaGuiLayers.HOTBAR, Create.asResource("track_placement"),
			TrackPlacementOverlay.INSTANCE);
		event.registerAbove(VanillaGuiLayers.HOTBAR, Create.asResource("value_settings"),
			CreateClient.VALUE_SETTINGS_HANDLER);
		event.registerAbove(VanillaGuiLayers.HOTBAR, Create.asResource("linked_controller"),
			LinkedControllerClientHandler.OVERLAY);
		event.registerAbove(VanillaGuiLayers.HOTBAR, Create.asResource("blueprint_overlay"),
			BlueprintOverlayRenderer.OVERLAY);
		event.registerAbove(VanillaGuiLayers.HOTBAR, Create.asResource("schematic"),
			CreateClient.SCHEMATIC_HANDLER);
		event.registerAbove(VanillaGuiLayers.HOTBAR, Create.asResource("toolbox"),
			ToolboxHandlerClient.OVERLAY);
	}
}
