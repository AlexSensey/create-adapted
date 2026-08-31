param(
    [Parameter(Mandatory = $true)] [string] $TemplatePublicJar,
    [Parameter(Mandatory = $true)] [string] $NewFullJar,
    [Parameter(Mandatory = $true)] [string] $OutputJar
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

# The public package keeps the external-assets template resources so the
# original Create artwork is not accidentally reintroduced. All bytecode must
# come from the newly built full JAR; otherwise an old class can silently undo a
# dedicated-server fix in the final public artifact.
$replaceClassRoots = @(
    'com/simibubi/create/CreateBuildInfo',
    'com/simibubi/create/AllEntityTypes',
    'com/simibubi/create/infrastructure/assets/ExternalCreateAssets',
    'com/simibubi/create/infrastructure/assets/MissingCreateAssetsScreen',
    'com/simibubi/create/content/schematics/client/SchematicHandler',
    'com/simibubi/create/content/schematics/SchematicPrinter',
    'com/simibubi/create/content/schematics/cannon/ConfigureSchematicannonPacket',
    'com/simibubi/create/content/schematics/cannon/SchematicannonBlockEntity',
    'com/simibubi/create/content/fluids/FlowSource',
    'com/simibubi/create/content/fluids/FluidTransportBehaviour',
    'com/simibubi/create/content/fluids/PipeAttachmentModel',
    'com/simibubi/create/content/fluids/PipeConnection',
    'com/simibubi/create/content/fluids/pipes/FluidPipeBlockEntity',
    'com/simibubi/create/content/fluids/pipes/StraightPipeBlockEntity',
    'com/simibubi/create/content/fluids/pump/PumpBlockEntity',
    'com/simibubi/create/content/contraptions/AbstractContraptionEntity',
    'com/simibubi/create/content/contraptions/OrientedContraptionEntity',
    'com/simibubi/create/content/contraptions/ContraptionPacketClient',
    'com/simibubi/create/content/contraptions/ContraptionStallPacket',
    'com/simibubi/create/content/contraptions/ContraptionBlockChangedPacket',
    'com/simibubi/create/content/contraptions/ContraptionDisassemblyPacket',
    'com/simibubi/create/content/contraptions/ContraptionRelocationPacket',
    'com/simibubi/create/content/contraptions/gantry/GantryContraptionEntity',
    'com/simibubi/create/content/contraptions/gantry/GantryContraptionClient',
    'com/simibubi/create/content/contraptions/gantry/GantryContraptionUpdatePacket',
    'com/simibubi/create/content/contraptions/actors/seat/SeatEntity',
    'com/simibubi/create/content/contraptions/actors/seat/SeatEntityRenderer',
    'com/simibubi/create/content/contraptions/actors/trainControls/ControlsMovementBehaviour',
    'com/simibubi/create/content/contraptions/actors/trainControls/ControlsRenderer',
    'com/simibubi/create/content/contraptions/bearing/StabilizedBearingMovementBehaviour',
    'com/simibubi/create/content/contraptions/bearing/StabilizedBearingMovementClient',
    'com/simibubi/create/content/contraptions/render/ContraptionEntityRenderer',
    'com/simibubi/create/content/logistics/box/PackageEntity',
    'com/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour',
    'com/simibubi/create/content/logistics/factoryBoard/FactoryPanelClient',
    'com/simibubi/create/content/kinetics/belt/BeltSlicer',
    'com/simibubi/create/content/kinetics/base/KineticBlockEntity',
    'com/simibubi/create/content/kinetics/waterwheel/LargeWaterWheelBlockItem',
    'com/simibubi/create/content/kinetics/waterwheel/LargeWaterWheelClient',
    'com/simibubi/create/content/processing/burner/BlazeBurnerBlockEntity',
    'com/simibubi/create/content/processing/burner/BlazeBurnerClient',
    'com/simibubi/create/content/processing/burner/BlazeBurnerMovementBehaviour',
    'com/simibubi/create/content/processing/burner/BlazeBurnerMovementClient',
    'com/simibubi/create/content/equipment/armor/BacktankUtil',
    'com/simibubi/create/content/equipment/armor/BacktankClient',
    'com/simibubi/create/content/equipment/clipboard/ClipboardBlockEntity',
    'com/simibubi/create/content/equipment/clipboard/ClipboardClient',
    'com/simibubi/create/content/equipment/zapper/ShootGadgetPacket',
    'com/simibubi/create/content/equipment/zapper/ShootableGadgetRenderHandler',
    'com/simibubi/create/content/equipment/zapper/ZapperBeamPacket',
    'com/simibubi/create/content/equipment/potatoCannon/PotatoCannonPacket',
    'com/simibubi/create/content/redstone/displayLink/DisplayLinkBlockItem',
    'com/simibubi/create/content/redstone/displayLink/DisplayLinkClient',
    'com/simibubi/create/content/redstone/link/controller/LecternControllerBlockEntity',
    'com/simibubi/create/content/redstone/link/controller/LinkedControllerClientHandler',
    'com/simibubi/create/content/decoration/palettes/ConnectedGlassBlock',
    'com/simibubi/create/content/trains/track/TrackBlockItem',
    'com/simibubi/create/content/trains/track/TrackPlacement',
    'com/simibubi/create/content/trains/track/TrackPlacementClient',
    'com/simibubi/create/content/trains/track/TrackTargetingBehaviour',
    'com/simibubi/create/content/trains/track/TrackTargetingBlockItem',
    'com/simibubi/create/content/trains/track/TrackTargetingClient',
    'com/simibubi/create/content/trains/track/CurvedTrackInteraction',
    'com/simibubi/create/content/trains/station/StationRenderer',
    'com/simibubi/create/content/trains/signal/SignalRenderer',
    'com/simibubi/create/content/trains/observer/TrackObserverRenderer',
    'com/simibubi/create/infrastructure/debugInfo/ServerDebugInfoPacket',
    'com/simibubi/create/infrastructure/debugInfo/DebugInformationClient',
    'com/simibubi/create/foundation/utility/ServerSpeedProvider',
    'com/simibubi/create/foundation/model/ModelSwapper',
    'com/simibubi/create/foundation/utility/ServerSpeedClient',
    'com/simibubi/create/foundation/blockEntity/behaviour/inventory/InvManipulationBehaviour',
    'com/simibubi/create/foundation/events/ClientEvents',
    'com/simibubi/create/foundation/ponder/PonderLevelCompat',
    'com/simibubi/create/foundation/mixin/client/EntityRenderersThreadSafetyMixin'
)

$replaceEntries = @(
    'META-INF/MANIFEST.MF',
    'META-INF/neoforge.mods.toml',
    'create.mixins.json',
    'data/create/recipe/milling/compat/ae2/fluix_crystal.json',
    'data/create/recipe/milling/compat/ae2/sky_stone_block.json',
    'data/create/recipe/mixing/compat/ae2/fluix_crystal.json',
    'data/create/tags/item/sandpaper.json'
)

Copy-Item -LiteralPath $TemplatePublicJar -Destination $OutputJar -Force
$source = [System.IO.Compression.ZipFile]::OpenRead($NewFullJar)
$target = [System.IO.Compression.ZipFile]::Open($OutputJar, [System.IO.Compression.ZipArchiveMode]::Update)

try {
    $sourceClassEntries = @($source.Entries | Where-Object { $_.FullName.EndsWith('.class') })
    if ($sourceClassEntries.Count -eq 0) {
        throw "The newly built full JAR contains no class files: $NewFullJar"
    }

    $targetClassEntries = @($target.Entries | Where-Object { $_.FullName.EndsWith('.class') })
    foreach ($oldEntry in $targetClassEntries) {
        $oldEntry.Delete()
    }

    foreach ($sourceEntry in $sourceClassEntries) {
        $newEntry = $target.CreateEntry($sourceEntry.FullName, [System.IO.Compression.CompressionLevel]::Optimal)
        $input = $sourceEntry.Open()
        $output = $newEntry.Open()
        try {
            $input.CopyTo($output)
        }
        finally {
            $output.Dispose()
            $input.Dispose()
        }
    }

    foreach ($root in $replaceClassRoots) {
        $matchingSourceEntries = @($source.Entries | Where-Object {
            $_.FullName -eq "$root.class" -or
            ($_.FullName.StartsWith("$root`$") -and $_.FullName.EndsWith('.class'))
        })
        if ($matchingSourceEntries.Count -eq 0) {
            throw "Missing source class entry: $root.class"
        }

        $matchingTargetEntries = @($target.Entries | Where-Object {
            $_.FullName -eq "$root.class" -or
            ($_.FullName.StartsWith("$root`$") -and $_.FullName.EndsWith('.class'))
        })
        foreach ($oldEntry in $matchingTargetEntries) {
            $oldEntry.Delete()
        }

        foreach ($sourceEntry in $matchingSourceEntries) {
            $newEntry = $target.CreateEntry($sourceEntry.FullName, [System.IO.Compression.CompressionLevel]::Optimal)
            $input = $sourceEntry.Open()
            $output = $newEntry.Open()
            try {
                $input.CopyTo($output)
            }
            finally {
                $output.Dispose()
                $input.Dispose()
            }
        }
    }

    foreach ($path in $replaceEntries) {
        $sourceEntry = $source.GetEntry($path)
        if ($null -eq $sourceEntry) {
            throw "Missing source entry: $path"
        }

        $oldEntry = $target.GetEntry($path)
        if ($null -ne $oldEntry) {
            $oldEntry.Delete()
        }

        $newEntry = $target.CreateEntry($path, [System.IO.Compression.CompressionLevel]::Optimal)
        $input = $sourceEntry.Open()
        $output = $newEntry.Open()
        try {
            $input.CopyTo($output)
        }
        finally {
            $output.Dispose()
            $input.Dispose()
        }
    }
}
finally {
    $target.Dispose()
    $source.Dispose()
}

& (Join-Path $PSScriptRoot 'audit_server_client_refs.ps1') -JarPath $OutputJar

Write-Output "Created public external-assets JAR: $OutputJar"
