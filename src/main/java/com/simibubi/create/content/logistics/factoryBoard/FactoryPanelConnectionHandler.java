package com.simibubi.create.content.logistics.factoryBoard;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public class FactoryPanelConnectionHandler {

	static FactoryPanelPosition connectingFrom;
	static AABB connectingFromBox;
	static FactoryPanelBehaviour connectingBehaviour;
	static boolean relocating;
	static FactoryPanelPosition validRelocationTarget;

	public static boolean panelClicked(LevelAccessor level, Player player, FactoryPanelBehaviour panel) {
		if (connectingFrom == null)
			return false;

		FactoryPanelBehaviour at = getConnectingBehaviour(level);
		if (panel.getPanelPosition()
			.equals(connectingFrom) || at == null) {
			display(Component.empty());
			clearConnection();
			return true;
		}

		String issue = checkForIssues(at, panel);
		if (issue != null) {
			display(CreateLang.translate(issue)
				.style(ChatFormatting.RED)
				.component());
			clearConnection();
			AllSoundEvents.DENY.playAt(player.level(), player.blockPosition(), 1, 1, false);
			return true;
		}

		ItemStack filterFrom = panel.getFilter();
		ItemStack filterTo = at.getFilter();
		ClientNetworkHelper.INSTANCE.sendToServer(
			new FactoryPanelConnectionPacket(panel.getPanelPosition(), connectingFrom, false));
		display(CreateLang.translate("factory_panel.panels_connected",
				filterFrom.getHoverName().getString(), filterTo.getHoverName().getString())
			.style(ChatFormatting.GREEN)
			.component());
		clearConnection();
		player.level()
			.playLocalSound(player.blockPosition(), SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, .5f, .5f,
				false);
		return true;
	}

	@Nullable
	private static String checkForIssues(FactoryPanelBehaviour from, FactoryPanelBehaviour to) {
		if (from == null)
			return "factory_panel.connection_aborted";
		if (from.targetedBy.containsKey(to.getPanelPosition()))
			return "factory_panel.already_connected";
		if (from.targetedBy.size() >= 9)
			return "factory_panel.cannot_add_more_inputs";

		BlockState state1 = to.blockEntity.getBlockState();
		BlockState state2 = from.blockEntity.getBlockState();
		BlockPos diff = to.getPos().subtract(from.getPos());
		if (!sameOrientation(state1, state2))
			return "factory_panel.same_orientation";
		if (FactoryPanelBlock.connectedDirection(state1).getAxis()
			.choose(diff.getX(), diff.getY(), diff.getZ()) != 0)
			return "factory_panel.same_surface";
		if (!diff.closerThan(BlockPos.ZERO, 16))
			return "factory_panel.too_far_apart";
		if (to.panelBE().restocker)
			return "factory_panel.input_in_restock_mode";
		if (to.getFilter().isEmpty() || from.getFilter().isEmpty())
			return "factory_panel.no_item";
		return null;
	}

	@Nullable
	private static String checkForIssues(FactoryPanelBehaviour from, FactoryPanelSupportBehaviour to) {
		if (from == null)
			return "factory_panel.connection_aborted";
		BlockState state1 = from.blockEntity.getBlockState();
		BlockState state2 = to.blockEntity.getBlockState();
		BlockPos diff = to.getPos().subtract(from.getPos());
		Direction connectedDirection = FactoryPanelBlock.connectedDirection(state1);
		if (connectedDirection != state2.getOptionalValue(WrenchableDirectionalBlock.FACING)
			.orElse(connectedDirection))
			return "factory_panel.same_orientation";
		if (connectedDirection.getAxis().choose(diff.getX(), diff.getY(), diff.getZ()) != 0)
			return "factory_panel.same_surface";
		if (!diff.closerThan(BlockPos.ZERO, 16))
			return "factory_panel.too_far_apart";
		return null;
	}

	private static boolean sameOrientation(BlockState first, BlockState second) {
		return first.setValue(FactoryPanelBlock.WATERLOGGED, false)
			.setValue(FactoryPanelBlock.POWERED, false)
			.equals(second.setValue(FactoryPanelBlock.WATERLOGGED, false)
				.setValue(FactoryPanelBlock.POWERED, false));
	}

	public static void clientTick() {
		if (connectingFrom == null || connectingFromBox == null)
			return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			clearConnection();
			return;
		}
		if (!(mc.level.getBlockEntity(connectingFrom.pos()) instanceof FactoryPanelBlockEntity fpbe)
			|| !fpbe.panels.containsKey(connectingFrom.slot())
			|| !fpbe.panels.get(connectingFrom.slot()).isActive()) {
			clearConnection();
			display(Component.empty());
			return;
		}
		FactoryPanelBehaviour at = getConnectingBehaviour(mc.level);
		if (!connectingFrom.pos().closerThan(mc.player.blockPosition(), 16)) {
			clearConnection();
			display(Component.empty());
			return;
		}

		display(
			CreateLang.translate(relocating ? "factory_panel.click_to_relocate" : "factory_panel.click_second_panel")
				.component());

		if (!relocating)
			return;
		if (at == null)
			return;
		validRelocationTarget = null;
		if (!(mc.hitResult instanceof BlockHitResult bhr) || bhr.getType() == Type.MISS)
			return;

		Vec3 offsetPos = bhr.getLocation()
			.add(Vec3.atLowerCornerOf(bhr.getDirection().getUnitVec3i()).scale(1 / 32f));
		BlockPos pos = BlockPos.containing(offsetPos);
		BlockState blockState = at.blockEntity.getBlockState();
		PanelSlot slot = FactoryPanelBlock.getTargetedSlot(pos, blockState, offsetPos);
		BlockPos diff = pos.subtract(connectingFrom.pos());
		Direction facing = FactoryPanelBlock.connectedDirection(blockState);
		if (facing.getAxis().choose(diff.getX(), diff.getY(), diff.getZ()) != 0)
			return;
		if (!AllBlocks.FACTORY_GAUGE.get().canSurvive(blockState, mc.level, pos))
			return;
		if (AllBlocks.PACKAGER.has(mc.level.getBlockState(pos.relative(facing.getOpposite()))))
			return;

		validRelocationTarget = new FactoryPanelPosition(pos, slot);
	}

	public static boolean onRightClick() {
		if (connectingFrom == null || connectingFromBox == null)
			return false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null)
			return false;
		boolean missed = false;

		if (relocating) {
			if (mc.player.isShiftKeyDown())
				validRelocationTarget = null;
			if (validRelocationTarget != null)
				ClientNetworkHelper.INSTANCE.sendToServer(
					new FactoryPanelConnectionPacket(validRelocationTarget, connectingFrom, true));
			clearConnection();
			if (validRelocationTarget == null)
				display(CreateLang.translate("factory_panel.relocation_aborted")
					.component());
			relocating = false;
			validRelocationTarget = null;
			return true;
		}

		if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() != Type.MISS) {
			BlockEntity blockEntity = mc.level.getBlockEntity(bhr.getBlockPos());
			FactoryPanelSupportBehaviour behaviour =
				BlockEntityBehaviour.get(mc.level, bhr.getBlockPos(), FactoryPanelSupportBehaviour.TYPE);
			if (behaviour != null) {
				FactoryPanelBehaviour at = FactoryPanelBehaviour.at((Level) mc.level, connectingFrom);
				String issue = checkForIssues(at, behaviour);
				if (issue != null) {
					display(CreateLang.translate(issue)
						.style(ChatFormatting.RED)
						.component());
					clearConnection();
					AllSoundEvents.DENY.playAt(mc.level, mc.player.blockPosition(), 1, 1, false);
					return true;
				}

				FactoryPanelPosition bestPosition = null;
				double bestDistance = Double.POSITIVE_INFINITY;
				for (PanelSlot slot : PanelSlot.values()) {
					FactoryPanelPosition panelPosition = new FactoryPanelPosition(blockEntity.getBlockPos(), slot);
					FactoryPanelConnection connection = new FactoryPanelConnection(panelPosition, 1);
					Vec3 diff = connection.calculatePathDiff(mc.level.getBlockState(connectingFrom.pos()),
						connectingFrom);
					if (bestDistance < diff.lengthSqr())
						continue;
					bestDistance = diff.lengthSqr();
					bestPosition = panelPosition;
				}
				ClientNetworkHelper.INSTANCE.sendToServer(
					new FactoryPanelConnectionPacket(bestPosition, connectingFrom, false));
				display(CreateLang.translate("factory_panel.link_connected",
						blockEntity.getBlockState().getBlock().getName())
					.style(ChatFormatting.GREEN)
					.component());
				clearConnection();
				mc.player.level()
					.playLocalSound(mc.player.blockPosition(), SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS,
						.5f, .5f, false);
				return true;
			}
			if (!(blockEntity instanceof FactoryPanelBlockEntity))
				missed = true;
		}

		if (!mc.player.isShiftKeyDown() && !missed)
			return false;
		clearConnection();
		display(CreateLang.translate("factory_panel.connection_aborted")
			.component());
		return true;
	}

	private static void display(Component component) {
		Minecraft.getInstance().gui.hud.setOverlayMessage(component, false);
	}

	private static void clearConnection() {
		connectingFrom = null;
		connectingFromBox = null;
		connectingBehaviour = null;
	}

	public static void startRelocating(FactoryPanelBehaviour behaviour) {
		startConnection(behaviour);
		relocating = true;
	}

	public static void startConnection(FactoryPanelBehaviour behaviour) {
		relocating = false;
		connectingFrom = behaviour.getPanelPosition();
		connectingFromBox = getBB(behaviour.blockEntity.getBlockState(), connectingFrom);
		connectingBehaviour = behaviour;
	}

	public static AABB getSelectedPreviewBox() {
		Minecraft mc = Minecraft.getInstance();
		if (connectingFrom != null && (mc.level == null
			|| !(mc.level.getBlockEntity(connectingFrom.pos()) instanceof FactoryPanelBlockEntity fpbe)
			|| fpbe.panels.get(connectingFrom.slot()) == null
			|| !fpbe.panels.get(connectingFrom.slot()).isActive())) {
			clearConnection();
			return null;
		}
		return connectingFromBox;
	}

	public static Direction.Axis getSelectedPreviewDepthAxis() {
		Minecraft mc = Minecraft.getInstance();
		if (connectingFrom == null || mc.level == null)
			return null;
		return FactoryPanelBlock.connectedDirection(mc.level.getBlockState(connectingFrom.pos()))
			.getAxis();
	}

	public static Direction getSelectedPreviewOutwardDirection() {
		Minecraft mc = Minecraft.getInstance();
		if (connectingFrom == null || mc.level == null)
			return null;
		return FactoryPanelBlock.connectedDirection(mc.level.getBlockState(connectingFrom.pos()));
	}

	public static AABB getRelocationPreviewBox() {
		if (validRelocationTarget == null || connectingFrom == null || Minecraft.getInstance().level == null)
			return null;
		FactoryPanelBehaviour source = getConnectingBehaviour(Minecraft.getInstance().level);
		return source == null ? null : getBB(source.blockEntity.getBlockState(), validRelocationTarget);
	}

	public static Direction getRelocationPreviewOutwardDirection() {
		Minecraft mc = Minecraft.getInstance();
		if (validRelocationTarget == null || connectingFrom == null || mc.level == null)
			return null;
		FactoryPanelBehaviour source = getConnectingBehaviour(mc.level);
		return source == null ? null : FactoryPanelBlock.connectedDirection(source.blockEntity.getBlockState());
	}

	private static FactoryPanelBehaviour getConnectingBehaviour(LevelAccessor level) {
		if (connectingBehaviour != null && !connectingBehaviour.blockEntity.isRemoved()
			&& connectingBehaviour.getPanelPosition()
				.equals(connectingFrom))
			return connectingBehaviour;
		return level instanceof Level concreteLevel && connectingFrom != null
			? FactoryPanelBehaviour.at(concreteLevel, connectingFrom)
			: null;
	}

	public static AABB getBB(BlockState blockState, FactoryPanelPosition factoryPanelPosition) {
		Vec3 location = FactoryPanelSlotPositioning.getCenterOfSlot(blockState, factoryPanelPosition.slot())
			.add(Vec3.atLowerCornerOf(factoryPanelPosition.pos()));
		Vec3 normal = Vec3.atLowerCornerOf(FactoryPanelBlock.connectedDirection(blockState)
			.getUnitVec3i());
		return new AABB(location, location).inflate(
			(1 - Math.abs(normal.x)) * 3 / 16f,
			(1 - Math.abs(normal.y)) * 3 / 16f,
			(1 - Math.abs(normal.z)) * 3 / 16f);
	}
}
