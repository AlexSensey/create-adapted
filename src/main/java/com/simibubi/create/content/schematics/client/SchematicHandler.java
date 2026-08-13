package com.simibubi.create.content.schematics.client;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.glue.SuperGlueRenderer;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.client.tools.ToolType;
import com.simibubi.create.content.schematics.packet.SchematicPlacePacket;
import com.simibubi.create.content.schematics.packet.SchematicSyncPacket;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.client.outliner.AABBOutline;
import net.createmod.catnip.api.client.render.SuperRenderTypeBuffer;
import net.createmod.catnip.api.level.wrapper.SchematicLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.gui.GuiLayer;

public class SchematicHandler implements GuiLayer {

	private static final int SYNC_DELAY = 10;

	private String displayedSchematic;
	private SchematicTransformation transformation;
	private AABB bounds;
	private boolean deployed;
	private boolean active;
	private ToolType currentTool;
	private int syncCooldown;
	private int activeHotbarSlot;
	private ItemStack activeSchematicItem;
	private AABBOutline outline;

	private final SchematicRenderer[] renderers = new SchematicRenderer[3];
	private final SchematicHotbarSlotOverlay overlay;
	private ToolSelectionScreen selectionScreen;

	public SchematicHandler() {
		overlay = new SchematicHotbarSlotOverlay();
		currentTool = ToolType.DEPLOY;
		selectionScreen = new ToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
		transformation = new SchematicTransformation();
		bounds = new AABB(0, 0, 0, 0, 0, 0);
		outline = createOutline(bounds);
		activeSchematicItem = ItemStack.EMPTY;
	}

	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameMode == null || mc.player == null)
			return;
		if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
			clearActiveState();
			return;
		}

		if (!activeSchematicItem.isEmpty())
			transformation.tick();

		LocalPlayer player = mc.player;
		ItemStack stack = findSchematicInHand(player);
		if (stack == null) {
			active = false;
			syncCooldown = 0;
			if (!activeSchematicItem.isEmpty() && itemLost(player)) {
				activeHotbarSlot = 0;
				activeSchematicItem = ItemStack.EMPTY;
				displayedSchematic = null;
			}
			return;
		}

		String schematicName = stack.get(AllDataComponents.SCHEMATIC_FILE);
		if (!active || !Objects.equals(schematicName, displayedSchematic))
			init(player, stack);
		if (!active)
			return;

		if (syncCooldown > 0)
			syncCooldown--;
		if (syncCooldown == 1)
			sync();

		selectionScreen.update();
		currentTool.getTool()
			.updateSelection();
	}

	private void clearActiveState() {
		active = false;
		deployed = false;
		syncCooldown = 0;
		activeHotbarSlot = 0;
		activeSchematicItem = ItemStack.EMPTY;
		displayedSchematic = null;
	}

	private void init(LocalPlayer player, ItemStack stack) {
		loadSettings(stack);
		displayedSchematic = stack.get(AllDataComponents.SCHEMATIC_FILE);
		active = true;
		if (deployed) {
			setupRenderer();
			ToolType previousTool = currentTool;
			selectionScreen = new ToolSelectionScreen(ToolType.getTools(player.isCreative()), this::equip);
			selectionScreen.setSelectedElement(previousTool);
			equip(previousTool);
		} else {
			selectionScreen = new ToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
			equip(ToolType.DEPLOY);
		}
	}

	private void setupRenderer() {
		Level clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null || activeSchematicItem.isEmpty())
			return;

		StructureTemplate schematic = SchematicItem.loadSchematic(clientLevel, activeSchematicItem);
		Vec3i size = schematic.getSize();
		if (size.equals(Vec3i.ZERO))
			return;

		SchematicLevel normal = new SchematicLevel(BlockPos.ZERO, clientLevel);
		SchematicLevel frontBack = new SchematicLevel(BlockPos.ZERO, clientLevel);
		SchematicLevel leftRight = new SchematicLevel(BlockPos.ZERO, clientLevel);
		StructurePlaceSettings settings = new StructurePlaceSettings();

		try {
			schematic.placeInWorld(normal, BlockPos.ZERO, BlockPos.ZERO, settings, normal.getRandom(),
				Block.UPDATE_CLIENTS);
			for (BlockEntity blockEntity : normal.getBlockEntities())
				blockEntity.setLevel(normal);
			fixControllerBlockEntities(normal);

			settings.setMirror(Mirror.FRONT_BACK);
			BlockPos frontBackOffset = BlockPos.ZERO.east(size.getX() - 1);
			schematic.placeInWorld(frontBack, frontBackOffset, frontBackOffset, settings, frontBack.getRandom(),
				Block.UPDATE_CLIENTS);
			transformBlockEntities(frontBack, settings);
			fixControllerBlockEntities(frontBack);

			settings.setMirror(Mirror.LEFT_RIGHT);
			BlockPos leftRightOffset = BlockPos.ZERO.south(size.getZ() - 1);
			schematic.placeInWorld(leftRight, leftRightOffset, leftRightOffset, settings, leftRight.getRandom(),
				Block.UPDATE_CLIENTS);
			transformBlockEntities(leftRight, settings);
			fixControllerBlockEntities(leftRight);
		} catch (Exception e) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null)
				player.sendSystemMessage(CreateLang.translateDirect("schematic.error"));
			Create.LOGGER.error("Failed to load schematic for previewing", e);
			return;
		}

		renderers[0] = new SchematicRenderer(normal);
		renderers[1] = new SchematicRenderer(frontBack);
		renderers[2] = new SchematicRenderer(leftRight);
	}

	private static void transformBlockEntities(SchematicLevel level, StructurePlaceSettings settings) {
		StructureTransform transform = new StructureTransform(settings.getRotationPivot(), Axis.Y,
			Rotation.NONE, settings.getMirror());
		for (BlockEntity blockEntity : level.getBlockEntities()) {
			blockEntity.setLevel(level);
			transform.apply(blockEntity);
		}
	}

	private void fixControllerBlockEntities(SchematicLevel level) {
		for (BlockEntity blockEntity : level.getBlockEntities()) {
			if (!(blockEntity instanceof IMultiBlockEntityContainer multiBlockEntity))
				continue;
			BlockPos lastKnown = multiBlockEntity.getLastKnownPos();
			BlockPos current = blockEntity.getBlockPos();
			if (lastKnown == null || current == null || multiBlockEntity.isController() || lastKnown.equals(current))
				continue;
			BlockPos newControllerPos = multiBlockEntity.getController()
				.offset(current.subtract(lastKnown));
			if (multiBlockEntity instanceof SmartBlockEntity smartBlockEntity)
				smartBlockEntity.markVirtual();
			multiBlockEntity.setController(newControllerPos);
		}
	}

	public void render(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera) {
		if (!active || activeSchematicItem.isEmpty())
			return;

		poseStack.pushPose();
		currentTool.getTool()
			.renderTool(poseStack, buffer, camera);
		poseStack.popPose();

		poseStack.pushPose();
		transformation.applyTransformations(poseStack, camera);
		currentTool.getTool()
			.renderOnSchematic(poseStack, buffer);
		poseStack.popPose();
	}

	public void submit(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
		if (!active || activeSchematicItem.isEmpty())
			return;

		currentTool.getTool()
			.submitTool(poseStack, collector, cameraRenderState);
		if (!deployed)
			return;

		poseStack.pushPose();
		transformation.applyTransformations(poseStack, cameraRenderState.pos);
		submitSchematic(poseStack, collector, cameraRenderState);
		collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
			(pose, consumer) -> SuperGlueRenderer.renderWireframe(
				pose, consumer, bounds, 0x6886c5, 235, 1 / 16f));
		poseStack.popPose();
	}

	public void submitSchematic(PoseStack poseStack, SubmitNodeCollector collector,
		CameraRenderState cameraRenderState) {
		float partialTicks = AnimationTickHolder.getPartialTicks();
		boolean leftRight = transformation.getScaleLR().getValue(partialTicks) < 0;
		boolean frontBack = transformation.getScaleFB().getValue(partialTicks) < 0;
		SchematicRenderer renderer = leftRight && !frontBack ? renderers[2]
			: frontBack && !leftRight ? renderers[1] : renderers[0];
		if (renderer != null)
			renderer.submit(poseStack, collector, cameraRenderState);
	}

	public void updateRenderers() {
		for (SchematicRenderer renderer : renderers)
			if (renderer != null)
				renderer.update();
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (!active)
			return;
		if (!activeSchematicItem.isEmpty())
			overlay.renderOn(graphics, activeHotbarSlot, activeSchematicItem);
		float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
		currentTool.getTool()
			.renderOverlay(mc.gui, graphics, partialTicks, graphics.guiWidth(), graphics.guiHeight());
		selectionScreen.renderPassive(graphics, partialTicks);
	}

	public boolean onMouseInput(int button, boolean pressed) {
		if (!active || !pressed || button != 1)
			return false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.player.isShiftKeyDown())
			return false;
		if (mc.hitResult instanceof BlockHitResult hit && mc.level != null) {
			BlockState clickedBlock = mc.level.getBlockState(hit.getBlockPos());
			if (AllBlocks.SCHEMATICANNON.has(clickedBlock) || AllBlocks.DEPLOYER.has(clickedBlock))
				return false;
		}
		return currentTool.getTool()
			.handleRightClick();
	}

	public void onKeyInput(int key, boolean pressed) {
		if (!active || !AllKeys.TOOL_MENU.doesModifierAndCodeMatch(key))
			return;
		if (pressed && !selectionScreen.focused)
			selectionScreen.focused = true;
		if (!pressed && selectionScreen.focused) {
			selectionScreen.focused = false;
			selectionScreen.onClose();
		}
	}

	public boolean mouseScrolled(double delta) {
		if (!active)
			return false;
		if (selectionScreen.focused) {
			selectionScreen.cycle((int) Math.signum(delta));
			return true;
		}
		return AllKeys.ctrlDown() && currentTool.getTool()
			.handleMouseWheel(delta);
	}

	private ItemStack findSchematicInHand(Player player) {
		ItemStack stack = player.getMainHandItem();
		if ((!SchematicItem.isSchematic(stack) && !SchematicItem.hasSchematicData(stack))
			|| !stack.has(AllDataComponents.SCHEMATIC_FILE))
			return null;
		activeSchematicItem = stack;
		activeHotbarSlot = player.getInventory()
			.getSelectedSlot();
		return stack;
	}

	private boolean itemLost(Player player) {
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack candidate = player.getInventory()
				.getItem(i);
			if (ItemStack.isSameItemSameComponents(candidate, activeSchematicItem))
				return false;
		}
		return true;
	}

	public void markDirty() {
		syncCooldown = SYNC_DELAY;
	}

	public void sync() {
		if (activeSchematicItem.isEmpty())
			return;
		ClientNetworkHelper.INSTANCE.sendToServer(new SchematicSyncPacket(activeHotbarSlot,
			transformation.toSettings(), transformation.getAnchor(), deployed));
	}

	public void equip(ToolType tool) {
		currentTool = tool == null ? ToolType.DEPLOY : tool;
		currentTool.getTool()
			.init();
	}

	public void loadSettings(ItemStack schematic) {
		activeSchematicItem = schematic;
		StructurePlaceSettings settings = SchematicItem.getSettings(schematic);
		transformation = new SchematicTransformation();
		deployed = schematic.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false);
		BlockPos anchor = schematic.getOrDefault(AllDataComponents.SCHEMATIC_ANCHOR, BlockPos.ZERO);
		Vec3i size = schematic.get(AllDataComponents.SCHEMATIC_BOUNDS);
		if (size == null)
			return;
		bounds = new AABB(0, 0, 0, size.getX(), size.getY(), size.getZ());
		outline = createOutline(bounds);
		transformation.init(anchor, settings, bounds);
	}

	private static AABBOutline createOutline(AABB bounds) {
		AABBOutline outline = new AABBOutline(bounds);
		outline.getParams()
			.colored(0x6886c5)
			.lineWidth(1 / 16f);
		return outline;
	}

	public void deploy() {
		if (!deployed) {
			LocalPlayer player = Minecraft.getInstance().player;
			selectionScreen = new ToolSelectionScreen(ToolType.getTools(player != null && player.isCreative()),
				this::equip);
		}
		deployed = true;
		setupRenderer();
	}

	public String getCurrentSchematicName() {
		return displayedSchematic != null ? displayedSchematic : "-";
	}

	public void printInstantly() {
		if (activeSchematicItem.isEmpty())
			return;
		ClientNetworkHelper.INSTANCE.sendToServer(new SchematicPlacePacket(activeSchematicItem.copy()));
		activeSchematicItem.set(AllDataComponents.SCHEMATIC_DEPLOYED, false);
		SchematicInstances.clearHash(activeSchematicItem);
		active = false;
		deployed = false;
		markDirty();
	}

	public boolean isActive() {
		return active;
	}

	public AABB getBounds() {
		return bounds;
	}

	public SchematicTransformation getTransformation() {
		return transformation;
	}

	public boolean isDeployed() {
		return deployed;
	}

	public ItemStack getActiveSchematicItem() {
		return activeSchematicItem;
	}

	public AABBOutline getOutline() {
		return outline;
	}

	public SchematicHotbarSlotOverlay getOverlay() {
		return overlay;
	}

	public ToolSelectionScreen getSelectionScreen() {
		return selectionScreen;
	}
}
