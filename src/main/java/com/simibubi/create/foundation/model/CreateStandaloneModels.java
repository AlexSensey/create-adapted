package com.simibubi.create.foundation.model;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.simibubi.create.Create;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes.ComponentPartials;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.box.PackageStyles.PackageStyle;

import net.createmod.catnip.api.data.Couple;
import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class CreateStandaloneModels {
	private static final Map<StandaloneModelKey<?>, Identifier> MODELS = new LinkedHashMap<>();
	private static final Map<StandaloneModelKey<QuadCollection>, Identifier> QUAD_MODELS = new LinkedHashMap<>();

	public static final StandaloneModelKey<BlockStateModelPart> WATER_WHEEL =
		blockPart("water_wheel", Create.asResource("block/water_wheel/wheel"));
	public static final StandaloneModelKey<BlockStateModelPart> LARGE_WATER_WHEEL =
		blockPart("large_water_wheel", Create.asResource("block/large_water_wheel/block"));
	public static final StandaloneModelKey<BlockStateModelPart> LARGE_WATER_WHEEL_EXTENSION =
		blockPart("large_water_wheel_extension", Create.asResource("block/large_water_wheel/block_extension"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_PRESS_HEAD =
		blockPart("mechanical_press_head", Create.asResource("block/mechanical_press/head"));
	public static final StandaloneModelKey<BlockStateModelPart> SCHEMATICANNON_CONNECTOR =
		blockPart("schematicannon_connector", Create.asResource("block/schematicannon/connector"));
	public static final StandaloneModelKey<BlockStateModelPart> SCHEMATICANNON_PIPE =
		blockPart("schematicannon_pipe", Create.asResource("block/schematicannon/pipe"));
	public static final StandaloneModelKey<BlockStateModelPart> COPPER_BACKTANK_SHAFT =
		blockPart("copper_backtank_shaft", Create.asResource("block/copper_backtank/block_shaft_input"));
	public static final StandaloneModelKey<BlockStateModelPart> COPPER_BACKTANK_BODY =
		blockPart("copper_backtank_body", Create.asResource("block/copper_backtank/block"));
	public static final StandaloneModelKey<BlockStateModelPart> COPPER_BACKTANK_COGS =
		blockPart("copper_backtank_cogs", Create.asResource("block/copper_backtank/block_cogs"));
	public static final StandaloneModelKey<BlockStateModelPart> NETHERITE_BACKTANK_SHAFT =
		blockPart("netherite_backtank_shaft", Create.asResource("block/netherite_backtank/block_shaft_input"));
	public static final StandaloneModelKey<BlockStateModelPart> NETHERITE_BACKTANK_BODY =
		blockPart("netherite_backtank_body", Create.asResource("block/netherite_backtank/block"));
	public static final StandaloneModelKey<BlockStateModelPart> NETHERITE_BACKTANK_COGS =
		blockPart("netherite_backtank_cogs", Create.asResource("block/netherite_backtank/block_cogs"));
	public static final StandaloneModelKey<BlockStateModelPart> PECULIAR_BELL =
		blockPart("peculiar_bell", Create.asResource("block/peculiar_bell"));
	public static final StandaloneModelKey<BlockStateModelPart> HAUNTED_BELL =
		blockPart("haunted_bell", Create.asResource("block/haunted_bell"));
	public static final StandaloneModelKey<BlockStateModelPart> TRAIN_HAT =
		blockPart("train_hat", Create.asResource("entity/train_hat"));
	public static final StandaloneModelKey<BlockStateModelPart> LOGISTICS_HAT =
		blockPart("logistics_hat", Create.asResource("entity/logistics_hat"));
	public static final StandaloneModelKey<BlockStateModelPart> MINECART_COUPLING_ATTACHMENT =
		blockPart("minecart_coupling_attachment", Create.asResource("entity/minecart_coupling/attachment"));
	public static final StandaloneModelKey<BlockStateModelPart> MINECART_COUPLING_RING =
		blockPart("minecart_coupling_ring", Create.asResource("entity/minecart_coupling/ring"));
	public static final StandaloneModelKey<BlockStateModelPart> MINECART_COUPLING_CONNECTOR =
		blockPart("minecart_coupling_connector", Create.asResource("entity/minecart_coupling/connector"));
	public static final StandaloneModelKey<BlockStateModelPart> CRAFTING_BLUEPRINT_SMALL =
		blockPart("crafting_blueprint_small", Create.asResource("entity/crafting_blueprint_small"));
	public static final StandaloneModelKey<BlockStateModelPart> CRAFTING_BLUEPRINT_MEDIUM =
		blockPart("crafting_blueprint_medium", Create.asResource("entity/crafting_blueprint_medium"));
	public static final StandaloneModelKey<BlockStateModelPart> CRAFTING_BLUEPRINT_LARGE =
		blockPart("crafting_blueprint_large", Create.asResource("entity/crafting_blueprint_large"));
	public static final StandaloneModelKey<BlockStateModelPart> SHAFTLESS_COGWHEEL =
		blockPart("shaftless_cogwheel", Create.asResource("block/cogwheel_shaftless"));
	public static final StandaloneModelKey<BlockStateModelPart> SHAFTLESS_LARGE_COGWHEEL =
		blockPart("shaftless_large_cogwheel", Create.asResource("block/large_cogwheel_shaftless"));
	public static final StandaloneModelKey<BlockStateModelPart> COGWHEEL_SHAFT =
		blockPart("cogwheel_shaft", Create.asResource("block/cogwheel_shaft"));
	public static final StandaloneModelKey<BlockStateModelPart> FACTORY_PANEL =
		blockPart("factory_panel", Create.asResource("block/factory_gauge/panel"));
	public static final StandaloneModelKey<BlockStateModelPart> FACTORY_PANEL_WITH_BULB =
		blockPart("factory_panel_with_bulb", Create.asResource("block/factory_gauge/panel_with_bulb"));
	public static final StandaloneModelKey<BlockStateModelPart> FACTORY_PANEL_RESTOCKER =
		blockPart("factory_panel_restocker", Create.asResource("block/factory_gauge/panel_restocker"));
	public static final StandaloneModelKey<BlockStateModelPart> FACTORY_PANEL_RESTOCKER_WITH_BULB =
		blockPart("factory_panel_restocker_with_bulb",
			Create.asResource("block/factory_gauge/panel_restocker_with_bulb"));
	public static final StandaloneModelKey<BlockStateModelPart> FACTORY_PANEL_LIGHT =
		blockPart("factory_panel_light", Create.asResource("block/factory_gauge/bulb_light"));
	public static final StandaloneModelKey<BlockStateModelPart> FACTORY_PANEL_RED_LIGHT =
		blockPart("factory_panel_red_light", Create.asResource("block/factory_gauge/bulb_red"));
	public static final Map<Direction, StandaloneModelKey<BlockStateModelPart>> FACTORY_PANEL_ARROWS =
		new EnumMap<>(Direction.class);
	public static final Map<Direction, StandaloneModelKey<BlockStateModelPart>> FACTORY_PANEL_LINES =
		new EnumMap<>(Direction.class);
	public static final Map<Direction, StandaloneModelKey<BlockStateModelPart>> FACTORY_PANEL_DOTTED =
		new EnumMap<>(Direction.class);
	public static final Map<Direction, StandaloneModelKey<BlockStateModelPart>> METAL_GIRDER_BRACKETS =
		new EnumMap<>(Direction.class);
	public static final StandaloneModelKey<BlockStateModelPart> DISPLAY_LINK_TUBE =
		blockPart("display_link_tube", Create.asResource("block/display_link/tube"));
	public static final StandaloneModelKey<BlockStateModelPart> DISPLAY_LINK_GLOW =
		blockPart("display_link_glow", Create.asResource("block/display_link/glow"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_CRAFTER_LID =
		blockPart("mechanical_crafter_lid", Create.asResource("block/mechanical_crafter/lid"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_CRAFTER_ARROW =
		blockPart("mechanical_crafter_arrow", Create.asResource("block/mechanical_crafter/arrow"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_CRAFTER_BELT_FRAME =
		blockPart("mechanical_crafter_belt_frame", Create.asResource("block/mechanical_crafter/belt"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_CRAFTER_BELT =
		blockPart("mechanical_crafter_belt", Create.asResource("block/mechanical_crafter/belt_animated"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_MIXER_POLE =
		blockPart("mechanical_mixer_pole", Create.asResource("block/mechanical_mixer/pole"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_MIXER_HEAD =
		blockPart("mechanical_mixer_head", Create.asResource("block/mechanical_mixer/head"));
	public static final StandaloneModelKey<BlockStateModelPart> MILLSTONE_COG =
		blockPart("millstone_cog", Create.asResource("block/millstone/inner"));
	public static final StandaloneModelKey<BlockStateModelPart> SYMMETRY_PLANE =
		blockPart("symmetry_plane", Create.asResource("block/symmetry_effect/plane"));
	public static final StandaloneModelKey<BlockStateModelPart> SYMMETRY_CROSSPLANE =
		blockPart("symmetry_crossplane", Create.asResource("block/symmetry_effect/crossplane"));
	public static final StandaloneModelKey<BlockStateModelPart> SYMMETRY_TRIPLEPLANE =
		blockPart("symmetry_tripleplane", Create.asResource("block/symmetry_effect/tripleplane"));
	public static final StandaloneModelKey<BlockStateModelPart> GAUGE_DIAL =
		blockPart("gauge_dial", Create.asResource("block/gauge/dial"));
	public static final StandaloneModelKey<BlockStateModelPart> ANALOG_LEVER_HANDLE =
		blockPart("analog_lever_handle", Create.asResource("block/analog_lever/handle"));
	public static final StandaloneModelKey<BlockStateModelPart> ANALOG_LEVER_INDICATOR =
		blockPart("analog_lever_indicator", Create.asResource("block/analog_lever/indicator"));
	public static final StandaloneModelKey<BlockStateModelPart> FLEXPEATER_INDICATOR =
		blockPart("flexpeater_indicator", Create.asResource("block/diodes/indicator"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_SIGNAL_PANEL =
		blockPart("nixie_signal_panel", Create.asResource("block/track_signal/panel"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_SIGNAL_WHITE_CUBE =
		blockPart("nixie_signal_white_cube", Create.asResource("block/track_signal/white_cube"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_SIGNAL_WHITE_GLOW =
		blockPart("nixie_signal_white_glow", Create.asResource("block/track_signal/white_glow"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_SIGNAL_WHITE =
		blockPart("nixie_signal_white", Create.asResource("block/track_signal/white_tube"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_SIGNAL_RED_GLOW =
		blockPart("nixie_signal_red_glow", Create.asResource("block/track_signal/red_glow"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_SIGNAL_RED =
		blockPart("nixie_signal_red", Create.asResource("block/track_signal/red_tube"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_SIGNAL_YELLOW_GLOW =
		blockPart("nixie_signal_yellow_glow", Create.asResource("block/track_signal/yellow_glow"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_SIGNAL_YELLOW =
		blockPart("nixie_signal_yellow", Create.asResource("block/track_signal/yellow_tube"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_COMPUTER_WHITE_CUBE =
		blockPart("nixie_computer_white_cube", Create.asResource("block/track_signal/computer_white_cube"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_COMPUTER_WHITE_GLOW =
		blockPart("nixie_computer_white_glow", Create.asResource("block/track_signal/computer_white_glow"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_COMPUTER_WHITE =
		blockPart("nixie_computer_white", Create.asResource("block/track_signal/computer_white_tube"));
	public static final StandaloneModelKey<BlockStateModelPart> NIXIE_COMPUTER_WHITE_BASE =
		blockPart("nixie_computer_white_base", Create.asResource("block/track_signal/computer_white_tube_base"));
	public static final StandaloneModelKey<BlockStateModelPart> DESK_BELL_PLUNGER =
		blockPart("desk_bell_plunger", Create.asResource("block/desk_bell/plunger"));
	public static final StandaloneModelKey<BlockStateModelPart> DESK_BELL_BELL =
		blockPart("desk_bell_bell", Create.asResource("block/desk_bell/bell"));
	public static final StandaloneModelKey<BlockStateModelPart> POSTBOX_FLAG =
		blockPart("postbox_flag", Create.asResource("block/package_postbox/flag"));
	public static final StandaloneModelKey<BlockStateModelPart> TABLE_CLOTH_PRICE_SIDE =
		blockPart("table_cloth_price_side", Create.asResource("block/table_cloth/price_tag_side"));
	public static final StandaloneModelKey<BlockStateModelPart> TABLE_CLOTH_PRICE_TOP =
		blockPart("table_cloth_price_top", Create.asResource("block/table_cloth/price_tag_top"));
	public static final StandaloneModelKey<BlockStateModelPart> TABLE_CLOTH_CORNER_SW =
		blockPart("table_cloth_corner_sw", Create.asResource("block/table_cloth/south_west"));
	public static final StandaloneModelKey<BlockStateModelPart> TABLE_CLOTH_CORNER_NW =
		blockPart("table_cloth_corner_nw", Create.asResource("block/table_cloth/north_west"));
	public static final StandaloneModelKey<BlockStateModelPart> TABLE_CLOTH_CORNER_NE =
		blockPart("table_cloth_corner_ne", Create.asResource("block/table_cloth/north_east"));
	public static final StandaloneModelKey<BlockStateModelPart> TABLE_CLOTH_CORNER_SE =
		blockPart("table_cloth_corner_se", Create.asResource("block/table_cloth/south_east"));
	public static final StandaloneModelKey<BlockStateModelPart> GAUGE_HEAD_SPEED =
		blockPart("gauge_head_speed", Create.asResource("block/gauge/speedometer/head"));
	public static final StandaloneModelKey<BlockStateModelPart> GAUGE_HEAD_STRESS =
		blockPart("gauge_head_stress", Create.asResource("block/gauge/stressometer/head"));
	public static final StandaloneModelKey<BlockStateModelPart> SAW_BLADE_HORIZONTAL_ACTIVE =
		blockPart("saw_blade_horizontal_active", Create.asResource("block/mechanical_saw/blade_horizontal_active"));
	public static final StandaloneModelKey<BlockStateModelPart> SAW_BLADE_HORIZONTAL_INACTIVE =
		blockPart("saw_blade_horizontal_inactive", Create.asResource("block/mechanical_saw/blade_horizontal_inactive"));
	public static final StandaloneModelKey<BlockStateModelPart> SAW_BLADE_HORIZONTAL_REVERSED =
		blockPart("saw_blade_horizontal_reversed", Create.asResource("block/mechanical_saw/blade_horizontal_reversed"));
	public static final StandaloneModelKey<BlockStateModelPart> SAW_BLADE_VERTICAL_ACTIVE =
		blockPart("saw_blade_vertical_active", Create.asResource("block/mechanical_saw/blade_vertical_active"));
	public static final StandaloneModelKey<BlockStateModelPart> SAW_BLADE_VERTICAL_INACTIVE =
		blockPart("saw_blade_vertical_inactive", Create.asResource("block/mechanical_saw/blade_vertical_inactive"));
	public static final StandaloneModelKey<BlockStateModelPart> SAW_BLADE_VERTICAL_REVERSED =
		blockPart("saw_blade_vertical_reversed", Create.asResource("block/mechanical_saw/blade_vertical_reversed"));
	public static final StandaloneModelKey<BlockStateModelPart> DEPLOYER_POLE =
		blockPart("deployer_pole", Create.asResource("block/deployer/pole"));
	public static final StandaloneModelKey<BlockStateModelPart> DEPLOYER_HAND_POINTING =
		blockPart("deployer_hand_pointing", Create.asResource("block/deployer/hand_pointing"));
	public static final StandaloneModelKey<BlockStateModelPart> DEPLOYER_HAND_PUNCHING =
		blockPart("deployer_hand_punching", Create.asResource("block/deployer/hand_punching"));
	public static final StandaloneModelKey<BlockStateModelPart> DEPLOYER_HAND_HOLDING =
		blockPart("deployer_hand_holding", Create.asResource("block/deployer/hand_holding"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_ARM_BLOCK =
		blockPart("mechanical_arm_block", Create.asResource("block/mechanical_arm/block"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_ARM_COG =
		blockPart("mechanical_arm_cog", Create.asResource("block/mechanical_arm/cog"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_ARM_BASE =
		blockPart("mechanical_arm_base", Create.asResource("block/mechanical_arm/base"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_ARM_LOWER_BODY =
		blockPart("mechanical_arm_lower_body", Create.asResource("block/mechanical_arm/lower_body"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_ARM_UPPER_BODY =
		blockPart("mechanical_arm_upper_body", Create.asResource("block/mechanical_arm/upper_body"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_ARM_CLAW_BASE =
		blockPart("mechanical_arm_claw_base", Create.asResource("block/mechanical_arm/claw_base"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_ARM_CLAW_BASE_GOGGLES =
		blockPart("mechanical_arm_claw_base_goggles", Create.asResource("block/mechanical_arm/claw_base_goggles"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_ARM_CLAW_GRIP_UPPER =
		blockPart("mechanical_arm_claw_grip_upper", Create.asResource("block/mechanical_arm/upper_claw_grip"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_ARM_CLAW_GRIP_LOWER =
		blockPart("mechanical_arm_claw_grip_lower", Create.asResource("block/mechanical_arm/lower_claw_grip"));
	public static final StandaloneModelKey<BlockStateModelPart> DRILL_HEAD =
		blockPart("drill_head", Create.asResource("block/mechanical_drill/head"));
	public static final StandaloneModelKey<BlockStateModelPart> HARVESTER_BLADE =
		blockPart("harvester_blade", Create.asResource("block/mechanical_harvester/blade"));
	public static final StandaloneModelKey<BlockStateModelPart> ROLLER_WHEEL =
		blockPart("roller_wheel", Create.asResource("block/mechanical_roller/wheel"));
	public static final StandaloneModelKey<BlockStateModelPart> ROLLER_FRAME =
		blockPart("roller_frame", Create.asResource("block/mechanical_roller/frame"));
	public static final StandaloneModelKey<BlockStateModelPart> CHAIN_CONVEYOR_GUARD =
		blockPart("chain_conveyor_guard", Create.asResource("block/chain_conveyor/guard"));
	public static final StandaloneModelKey<BlockStateModelPart> CHAIN_CONVEYOR_WHEEL =
		blockPart("chain_conveyor_wheel", Create.asResource("block/chain_conveyor/wheel"));
	public static final StandaloneModelKey<BlockStateModelPart> CHAIN_CONVEYOR_SHAFT =
		blockPart("chain_conveyor_shaft", Create.asResource("block/chain_conveyor/shaft"));
	public static final Map<Identifier, StandaloneModelKey<QuadCollection>> PACKAGE_QUADS = new LinkedHashMap<>();
	public static final Map<Identifier, StandaloneModelKey<QuadCollection>> PACKAGE_RIGGING_QUADS =
		new LinkedHashMap<>();
	public static final StandaloneModelKey<BlockStateModelPart> STICKER_HEAD =
		blockPart("sticker_head", Create.asResource("block/sticker/head"));
	public static final StandaloneModelKey<BlockStateModelPart> CONTRAPTION_CONTROLS_BUTTON =
		blockPart("contraption_controls_button", Create.asResource("block/contraption_controls/button"));
	public static final List<StandaloneModelKey<BlockStateModelPart>> CONTRAPTION_CONTROLS_INDICATOR =
		new ArrayList<>();
	public static final StandaloneModelKey<BlockStateModelPart> TRAIN_CONTROLS_COVER =
		blockPart("train_controls_cover", Create.asResource("block/controls/train/cover"));
	public static final StandaloneModelKey<BlockStateModelPart> TRAIN_CONTROLS_LEVER =
		blockPart("train_controls_lever", Create.asResource("block/controls/train/lever"));
	public static final StandaloneModelKey<BlockStateModelPart> STATION_FLAG_ON =
		blockPart("station_flag_on", Create.asResource("block/track_station/flag_on"));
	public static final StandaloneModelKey<BlockStateModelPart> STATION_FLAG_OFF =
		blockPart("station_flag_off", Create.asResource("block/track_station/flag_off"));
	public static final StandaloneModelKey<BlockStateModelPart> STATION_FLAG_ASSEMBLE =
		blockPart("station_flag_assemble", Create.asResource("block/track_station/flag_assemble"));
	public static final StandaloneModelKey<BlockStateModelPart> BELT_PULLEY =
		blockPart("belt_pulley", Create.asResource("block/belt_pulley"));
	public static final StandaloneModelKey<BlockStateModelPart> ANDESITE_BELT_COVER_X =
		blockPart("andesite_belt_cover_x", Create.asResource("block/belt_cover/andesite_belt_cover_x"));
	public static final StandaloneModelKey<BlockStateModelPart> ANDESITE_BELT_COVER_Z =
		blockPart("andesite_belt_cover_z", Create.asResource("block/belt_cover/andesite_belt_cover_z"));
	public static final StandaloneModelKey<BlockStateModelPart> BRASS_BELT_COVER_X =
		blockPart("brass_belt_cover_x", Create.asResource("block/belt_cover/brass_belt_cover_x"));
	public static final StandaloneModelKey<BlockStateModelPart> BRASS_BELT_COVER_Z =
		blockPart("brass_belt_cover_z", Create.asResource("block/belt_cover/brass_belt_cover_z"));
	public static final StandaloneModelKey<BlockStateModelPart> SHAFT_HALF =
		blockPart("shaft_half", Create.asResource("block/shaft_half"));
	public static final StandaloneModelKey<BlockStateModelPart> BEARING_TOP =
		blockPart("bearing_top", Create.asResource("block/bearing/top"));
	public static final StandaloneModelKey<BlockStateModelPart> BEARING_TOP_WOODEN =
		blockPart("bearing_top_wooden", Create.asResource("block/bearing/top_wooden"));
	public static final StandaloneModelKey<BlockStateModelPart> CUCKOO_MINUTE_HAND =
		blockPart("cuckoo_minute_hand", Create.asResource("block/cuckoo_clock/minute_hand"));
	public static final StandaloneModelKey<BlockStateModelPart> CUCKOO_HOUR_HAND =
		blockPart("cuckoo_hour_hand", Create.asResource("block/cuckoo_clock/hour_hand"));
	public static final StandaloneModelKey<BlockStateModelPart> CUCKOO_LEFT_DOOR =
		blockPart("cuckoo_left_door", Create.asResource("block/cuckoo_clock/left_door"));
	public static final StandaloneModelKey<BlockStateModelPart> CUCKOO_RIGHT_DOOR =
		blockPart("cuckoo_right_door", Create.asResource("block/cuckoo_clock/right_door"));
	public static final StandaloneModelKey<BlockStateModelPart> CUCKOO_PIG =
		blockPart("cuckoo_pig", Create.asResource("block/cuckoo_clock/pig"));
	public static final StandaloneModelKey<BlockStateModelPart> CUCKOO_CREEPER =
		blockPart("cuckoo_creeper", Create.asResource("block/cuckoo_clock/creeper"));
	public static final StandaloneModelKey<BlockStateModelPart> ENCASED_FAN_INNER =
		blockPart("encased_fan_inner", Create.asResource("block/encased_fan/propeller"));
	public static final StandaloneModelKey<BlockStateModelPart> MECHANICAL_PUMP_COG =
		blockPart("mechanical_pump_cog", Create.asResource("block/mechanical_pump/cog"));
	public static final StandaloneModelKey<BlockStateModelPart> FLUID_PIPE_CASING =
		blockPart("fluid_pipe_casing", Create.asResource("block/fluid_pipe/casing"));
	public static final StandaloneModelKey<BlockStateModelPart> FLUID_VALVE_POINTER =
		blockPart("fluid_valve_pointer", Create.asResource("block/fluid_valve/pointer"));
	public static final StandaloneModelKey<BlockStateModelPart> ROPE_COIL =
		blockPart("rope_coil", Create.asResource("block/rope_pulley/rope_coil"));
	public static final StandaloneModelKey<BlockStateModelPart> ROPE =
		blockPart("rope", Create.asResource("block/rope_pulley/rope"));
	public static final StandaloneModelKey<BlockStateModelPart> ROPE_HALF =
		blockPart("rope_half", Create.asResource("block/rope_pulley/rope_half"));
	public static final StandaloneModelKey<BlockStateModelPart> ROPE_PULLEY_MAGNET =
		blockPart("rope_pulley_magnet", Create.asResource("block/rope_pulley/pulley_magnet"));
	public static final StandaloneModelKey<BlockStateModelPart> ROPE_HALF_MAGNET =
		blockPart("rope_half_magnet", Create.asResource("block/rope_pulley/rope_half_magnet"));
	public static final StandaloneModelKey<BlockStateModelPart> ELEVATOR_COIL =
		blockPart("elevator_coil", Create.asResource("block/elevator_pulley/rope_coil"));
	public static final StandaloneModelKey<BlockStateModelPart> ELEVATOR_BELT =
		blockPart("elevator_belt", Create.asResource("block/elevator_pulley/rope"));
	public static final StandaloneModelKey<BlockStateModelPart> ELEVATOR_BELT_HALF =
		blockPart("elevator_belt_half", Create.asResource("block/elevator_pulley/rope_half"));
	public static final StandaloneModelKey<BlockStateModelPart> ELEVATOR_MAGNET =
		blockPart("elevator_magnet", Create.asResource("block/elevator_pulley/pulley_magnet"));
	public static final StandaloneModelKey<BlockStateModelPart> HOSE_COIL =
		blockPart("hose_coil", Create.asResource("block/hose_pulley/hose_coil"));
	public static final StandaloneModelKey<BlockStateModelPart> HOSE =
		blockPart("hose", Create.asResource("block/hose_pulley/rope"));
	public static final StandaloneModelKey<BlockStateModelPart> HOSE_HALF =
		blockPart("hose_half", Create.asResource("block/hose_pulley/rope_half"));
	public static final StandaloneModelKey<BlockStateModelPart> HOSE_MAGNET =
		blockPart("hose_magnet", Create.asResource("block/hose_pulley/pulley_magnet"));
	public static final StandaloneModelKey<BlockStateModelPart> HOSE_HALF_MAGNET =
		blockPart("hose_half_magnet", Create.asResource("block/hose_pulley/rope_half_magnet"));
	public static final StandaloneModelKey<BlockStateModelPart> HAND_CRANK_HANDLE =
		blockPart("hand_crank_handle", Create.asResource("block/hand_crank/handle"));
	public static final StandaloneModelKey<BlockStateModelPart> VALVE_HANDLE =
		blockPart("valve_handle", Create.asResource("block/valve_handle"));
	public static final StandaloneModelKey<BlockStateModelPart> GANTRY_COGS =
		blockPart("gantry_cogs", Create.asResource("block/gantry_carriage/wheels"));
	public static final Map<DyeColor, StandaloneModelKey<BlockStateModelPart>> DYED_VALVE_HANDLES = new EnumMap<>(DyeColor.class);
	public static final StandaloneModelKey<BlockStateModelPart> EJECTOR_TOP =
		blockPart("ejector_top", Create.asResource("block/weighted_ejector/top"));
	public static final StandaloneModelKey<BlockStateModelPart> TURNTABLE =
		blockPart("turntable", Create.asResource("block/turntable"));
	public static final StandaloneModelKey<BlockStateModelPart> TURNTABLE_TOP =
		blockPart("turntable_top", Create.asResource("block/turntable/top"));
	public static final StandaloneModelKey<BlockStateModelPart> SPEED_CONTROLLER_BRACKET =
		blockPart("speed_controller_bracket", Create.asResource("block/rotation_speed_controller/bracket"));
	public static final StandaloneModelKey<BlockStateModelPart> FUNNEL_FLAP =
		blockPart("funnel_flap", Create.asResource("block/funnel/flap"));
	public static final StandaloneModelKey<BlockStateModelPart> BELT_FUNNEL_FLAP =
		blockPart("belt_funnel_flap", Create.asResource("block/belt_funnel/flap"));
	public static final StandaloneModelKey<BlockStateModelPart> BELT_TUNNEL_FLAP =
		blockPart("belt_tunnel_flap", Create.asResource("block/belt_tunnel/flap"));
	public static final StandaloneModelKey<BlockStateModelPart> FROGPORT_BODY =
		blockPart("frogport_body", Create.asResource("block/package_frogport/body"));
	public static final StandaloneModelKey<BlockStateModelPart> FROGPORT_HEAD =
		blockPart("frogport_head", Create.asResource("block/package_frogport/head"));
	public static final StandaloneModelKey<BlockStateModelPart> FROGPORT_HEAD_GOGGLES =
		blockPart("frogport_head_goggles", Create.asResource("block/package_frogport/head_goggles"));
	public static final StandaloneModelKey<BlockStateModelPart> FROGPORT_TONGUE =
		blockPart("frogport_tongue", Create.asResource("block/package_frogport/tongue"));
	public static final StandaloneModelKey<BlockStateModelPart> PACKAGER_TRAY =
		blockPart("packager_tray", Create.asResource("block/packager/tray"));
	public static final StandaloneModelKey<BlockStateModelPart> REPACKAGER_TRAY =
		blockPart("repackager_tray", Create.asResource("block/repackager/tray"));
	public static final StandaloneModelKey<BlockStateModelPart> PACKAGER_HATCH_OPEN =
		blockPart("packager_hatch_open", Create.asResource("block/packager/hatch_open"));
	public static final StandaloneModelKey<BlockStateModelPart> PACKAGER_HATCH_CLOSED =
		blockPart("packager_hatch_closed", Create.asResource("block/packager/hatch_closed"));
	public static final StandaloneModelKey<BlockStateModelPart> TOOLBOX_DRAWER =
		blockPart("toolbox_drawer", Create.asResource("block/toolbox/drawer"));
	public static final Map<DyeColor, StandaloneModelKey<BlockStateModelPart>> TOOLBOX_LIDS = new EnumMap<>(DyeColor.class);
	public static final Map<Identifier, Couple<StandaloneModelKey<BlockStateModelPart>>> FOLDING_DOORS =
		new LinkedHashMap<>();
	public static final StandaloneModelKey<BlockStateModelPart> TRACK_SEGMENT_LEFT =
		blockPart("track_segment_left", Create.asResource("block/track/segment_left"));
	public static final StandaloneModelKey<BlockStateModelPart> TRACK_SEGMENT_RIGHT =
		blockPart("track_segment_right", Create.asResource("block/track/segment_right"));
	public static final StandaloneModelKey<BlockStateModelPart> TRACK_TIE =
		blockPart("track_tie", Create.asResource("block/track/tie"));
	public static final StandaloneModelKey<BlockStateModelPart> GIRDER_SEGMENT_TOP =
		blockPart("girder_segment_top", Create.asResource("block/metal_girder/segment_top"));
	public static final StandaloneModelKey<BlockStateModelPart> GIRDER_SEGMENT_MIDDLE =
		blockPart("girder_segment_middle", Create.asResource("block/metal_girder/segment_middle"));
	public static final StandaloneModelKey<BlockStateModelPart> GIRDER_SEGMENT_BOTTOM =
		blockPart("girder_segment_bottom", Create.asResource("block/metal_girder/segment_bottom"));
	public static final StandaloneModelKey<BlockStateModelPart> TRACK_STATION_OVERLAY =
		blockPart("track_station_overlay", Create.asResource("block/track_overlay/station"));
	public static final StandaloneModelKey<BlockStateModelPart> TRACK_ASSEMBLING_OVERLAY =
		blockPart("track_assembling_overlay", Create.asResource("block/track_overlay/assembling"));
	public static final StandaloneModelKey<BlockStateModelPart> TRACK_SIGNAL_OVERLAY =
		blockPart("track_signal_overlay", Create.asResource("block/track_overlay/signal"));
	public static final StandaloneModelKey<BlockStateModelPart> TRACK_SIGNAL_DUAL_OVERLAY =
		blockPart("track_signal_dual_overlay", Create.asResource("block/track_overlay/signal_dual"));
	public static final StandaloneModelKey<BlockStateModelPart> TRACK_OBSERVER_OVERLAY =
		blockPart("track_observer_overlay", Create.asResource("block/track_overlay/observer"));
	public static final StandaloneModelKey<BlockStateModelPart> SIGNAL_ON =
		blockPart("signal_on", Create.asResource("block/track_signal/indicator_on"));
	public static final StandaloneModelKey<BlockStateModelPart> SIGNAL_OFF =
		blockPart("signal_off", Create.asResource("block/track_signal/indicator_off"));
	public static final StandaloneModelKey<BlockStateModelPart> BOGEY_FRAME =
		blockPart("bogey_frame", Create.asResource("block/track/bogey/bogey_frame"));
	public static final StandaloneModelKey<BlockStateModelPart> SMALL_BOGEY_WHEELS =
		blockPart("small_bogey_wheels", Create.asResource("block/track/bogey/bogey_wheel"));
	public static final StandaloneModelKey<BlockStateModelPart> BOGEY_PIN =
		blockPart("bogey_pin", Create.asResource("block/track/bogey/bogey_drive_wheel_pin"));
	public static final StandaloneModelKey<BlockStateModelPart> BOGEY_PISTON =
		blockPart("bogey_piston", Create.asResource("block/track/bogey/bogey_drive_piston"));
	public static final StandaloneModelKey<BlockStateModelPart> BOGEY_DRIVE =
		blockPart("bogey_drive", Create.asResource("block/track/bogey/bogey_drive"));
	public static final StandaloneModelKey<BlockStateModelPart> LARGE_BOGEY_WHEELS =
		blockPart("large_bogey_wheels", Create.asResource("block/track/bogey/bogey_drive_wheel"));
	public static final StandaloneModelKey<BlockStateModelPart> BOGEY_DRIVE_BELT =
		blockPart("bogey_drive_belt", Create.asResource("block/track/bogey/bogey_drive_belt"));
	public static final StandaloneModelKey<BlockStateModelPart> TRAIN_COUPLING_HEAD =
		blockPart("train_coupling_head", Create.asResource("block/track/bogey/coupling_head"));
	public static final StandaloneModelKey<BlockStateModelPart> TRAIN_COUPLING_CABLE =
		blockPart("train_coupling_cable", Create.asResource("block/track/bogey/coupling_cable"));
	public static final StandaloneModelKey<BlockStateModelPart> GOGGLES =
		blockPart("goggles", Create.asResource("block/goggles"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_INERT =
		blockPart("blaze_inert", Create.asResource("block/blaze_burner/blaze/inert"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_BURNER_BLOCK =
		blockPart("blaze_burner_block", Create.asResource("block/blaze_burner/block"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_IDLE =
		blockPart("blaze_idle", Create.asResource("block/blaze_burner/blaze/idle"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_ACTIVE =
		blockPart("blaze_active", Create.asResource("block/blaze_burner/blaze/active"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_SUPER =
		blockPart("blaze_super", Create.asResource("block/blaze_burner/blaze/super"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_SUPER_ACTIVE =
		blockPart("blaze_super_active", Create.asResource("block/blaze_burner/blaze/super_active"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_GOGGLES =
		blockPart("blaze_goggles", Create.asResource("block/blaze_burner/goggles"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_GOGGLES_SMALL =
		blockPart("blaze_goggles_small", Create.asResource("block/blaze_burner/goggles_small"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_BURNER_RODS =
		blockPart("blaze_burner_rods", Create.asResource("block/blaze_burner/rods_small"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_BURNER_RODS_2 =
		blockPart("blaze_burner_rods_2", Create.asResource("block/blaze_burner/rods_large"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_BURNER_SUPER_RODS =
		blockPart("blaze_burner_super_rods", Create.asResource("block/blaze_burner/superheated_rods_small"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_BURNER_SUPER_RODS_2 =
		blockPart("blaze_burner_super_rods_2", Create.asResource("block/blaze_burner/superheated_rods_large"));
	public static final StandaloneModelKey<BlockStateModelPart> BLAZE_BURNER_FLAME =
		blockPart("blaze_burner_flame", Create.asResource("block/blaze_burner/flame"));
	public static final StandaloneModelKey<BlockStateModelPart> BOILER_GAUGE =
		blockPart("boiler_gauge", Create.asResource("block/steam_engine/gauge"));
	public static final StandaloneModelKey<BlockStateModelPart> BOILER_GAUGE_DIAL =
		blockPart("boiler_gauge_dial", Create.asResource("block/steam_engine/gauge_dial"));
	public static final StandaloneModelKey<BlockStateModelPart> SPOUT_TOP =
		blockPart("spout_top", Create.asResource("block/spout/top"));
	public static final StandaloneModelKey<BlockStateModelPart> SPOUT_MIDDLE =
		blockPart("spout_middle", Create.asResource("block/spout/middle"));
	public static final StandaloneModelKey<BlockStateModelPart> SPOUT_BOTTOM =
		blockPart("spout_bottom", Create.asResource("block/spout/bottom"));
	public static final StandaloneModelKey<BlockStateModelPart> ENGINE_PISTON =
		blockPart("engine_piston", Create.asResource("block/steam_engine/piston"));
	public static final StandaloneModelKey<BlockStateModelPart> ENGINE_LINKAGE =
		blockPart("engine_linkage", Create.asResource("block/steam_engine/linkage"));
	public static final StandaloneModelKey<BlockStateModelPart> ENGINE_CONNECTOR =
		blockPart("engine_connector", Create.asResource("block/steam_engine/shaft_connector"));
	public static final StandaloneModelKey<BlockStateModelPart> WHISTLE_MOUTH_LARGE =
		blockPart("whistle_mouth_large", Create.asResource("block/steam_whistle/large_mouth"));
	public static final StandaloneModelKey<BlockStateModelPart> WHISTLE_MOUTH_MEDIUM =
		blockPart("whistle_mouth_medium", Create.asResource("block/steam_whistle/medium_mouth"));
	public static final StandaloneModelKey<BlockStateModelPart> WHISTLE_MOUTH_SMALL =
		blockPart("whistle_mouth_small", Create.asResource("block/steam_whistle/small_mouth"));
	public static final StandaloneModelKey<BlockStateModelPart> PORTABLE_STORAGE_INTERFACE_MIDDLE =
		blockPart("portable_storage_interface_middle", Create.asResource("block/portable_storage_interface/block_middle"));
	public static final StandaloneModelKey<BlockStateModelPart> PORTABLE_STORAGE_INTERFACE_MIDDLE_POWERED =
		blockPart("portable_storage_interface_middle_powered", Create.asResource("block/portable_storage_interface/block_middle_powered"));
	public static final StandaloneModelKey<BlockStateModelPart> PORTABLE_STORAGE_INTERFACE_TOP =
		blockPart("portable_storage_interface_top", Create.asResource("block/portable_storage_interface/block_top"));
	public static final StandaloneModelKey<BlockStateModelPart> PORTABLE_FLUID_INTERFACE_MIDDLE =
		blockPart("portable_fluid_interface_middle", Create.asResource("block/portable_fluid_interface/block_middle"));
	public static final StandaloneModelKey<BlockStateModelPart> PORTABLE_FLUID_INTERFACE_MIDDLE_POWERED =
		blockPart("portable_fluid_interface_middle_powered", Create.asResource("block/portable_fluid_interface/block_middle_powered"));
	public static final StandaloneModelKey<BlockStateModelPart> PORTABLE_FLUID_INTERFACE_TOP =
		blockPart("portable_fluid_interface_top", Create.asResource("block/portable_fluid_interface/block_top"));

	public static final Map<ComponentPartials, Map<Direction, StandaloneModelKey<BlockStateModelPart>>> PIPE_ATTACHMENTS =
		new EnumMap<>(ComponentPartials.class);

	static {
		for (Direction direction : Iterate.horizontalDirections) {
			String name = direction.getSerializedName();
			METAL_GIRDER_BRACKETS.put(direction, blockPart("metal_girder_bracket_" + name,
				Create.asResource("block/metal_girder/bracket_" + name)));
			FACTORY_PANEL_ARROWS.put(direction, blockPart("factory_panel_arrow_" + name,
				Create.asResource("block/factory_gauge/connections/arrow_" + name)));
			FACTORY_PANEL_LINES.put(direction, blockPart("factory_panel_line_" + name,
				Create.asResource("block/factory_gauge/connections/line_" + name)));
			FACTORY_PANEL_DOTTED.put(direction, blockPart("factory_panel_dotted_" + name,
				Create.asResource("block/factory_gauge/connections/dotted_" + name)));
		}
		for (PackageStyle style : PackageStyles.STYLES) {
			Identifier itemId = style.getItemId();
			String modelName = itemId.getPath()
				.replace('/', '_');
			PACKAGE_QUADS.put(itemId,
				quadCollection("chain_package_" + modelName, Create.asResource("item/" + itemId.getPath())));
			PACKAGE_RIGGING_QUADS.put(itemId,
				quadCollection("chain_package_rigging_" + modelName, style.getRiggingModel()));
		}
		putFoldingDoor("andesite_door");
		putFoldingDoor("copper_door");
		for (int i = 0; i < 8; i++)
			CONTRAPTION_CONTROLS_INDICATOR.add(blockPart("contraption_controls_indicator_" + i,
				Create.asResource("block/contraption_controls/indicator_" + i)));
		for (DyeColor color : DyeColor.values()) {
			String colorName = color.getSerializedName();
			DYED_VALVE_HANDLES.put(color,
				blockPart(colorName + "_valve_handle", Create.asResource("block/" + colorName + "_valve_handle")));
			TOOLBOX_LIDS.put(color,
				blockPart(colorName + "_toolbox_lid", Create.asResource("block/toolbox/lid/" + colorName)));
		}
		for (ComponentPartials partial : ComponentPartials.values()) {
			Map<Direction, StandaloneModelKey<BlockStateModelPart>> map = new EnumMap<>(Direction.class);
			for (Direction direction : Iterate.directions) {
				String partialPath = partial.name()
					.toLowerCase(java.util.Locale.ROOT);
				map.put(direction, blockPart("fluid_pipe_" + partialPath + "_" + direction.getSerializedName(),
					Create.asResource("block/fluid_pipe/" + partialPath + "/" + direction.getSerializedName())));
			}
			PIPE_ATTACHMENTS.put(partial, map);
		}
	}

	private static void putFoldingDoor(String path) {
		FOLDING_DOORS.put(Create.asResource(path), Couple.create(
			blockPart(path + "_fold_left", Create.asResource("block/" + path + "/fold_left")),
			blockPart(path + "_fold_right", Create.asResource("block/" + path + "/fold_right"))));
	}

	private static StandaloneModelKey<BlockStateModelPart> blockPart(String name, Identifier model) {
		StandaloneModelKey<BlockStateModelPart> key = new StandaloneModelKey<>(debugName(name));
		MODELS.put(key, model);
		return key;
	}

	private static StandaloneModelKey<QuadCollection> quadCollection(String name, Identifier model) {
		StandaloneModelKey<QuadCollection> key = new StandaloneModelKey<>(debugName(name));
		QUAD_MODELS.put(key, model);
		return key;
	}

	private static ModelDebugName debugName(String name) {
		return () -> Create.ID + ":" + name;
	}

	public static void register(ModelEvent.RegisterStandalone event) {
		MODELS.forEach((key, model) -> registerBlockPart(event, key, model));
		QUAD_MODELS.forEach((key, model) -> event.register(key,
			SimpleUnbakedStandaloneModel.quadCollection(model)));
	}

	private static <T> void registerBlockPart(ModelEvent.RegisterStandalone event, StandaloneModelKey<T> key,
		Identifier model) {
		event.register(key, (net.neoforged.neoforge.client.model.standalone.UnbakedStandaloneModel<T>)
			SimpleUnbakedStandaloneModel.simpleModelWrapper(model));
	}
}
