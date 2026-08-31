param(
    [string]$ClassesDirectory = "build/classes/java/main",
    [string]$JarPath
)

$ErrorActionPreference = "Stop"

# Classes loaded or executed by a dedicated server during normal interactions.
# Rendering, previews and input handlers belong in separate *Client classes.
$serverClasses = @(
    "com.simibubi.create.content.contraptions.AbstractContraptionEntity",
    "com.simibubi.create.content.contraptions.OrientedContraptionEntity",
    "com.simibubi.create.content.contraptions.actors.seat.SeatEntity",
    "com.simibubi.create.content.contraptions.actors.trainControls.ControlsMovementBehaviour",
    "com.simibubi.create.content.contraptions.bearing.StabilizedBearingMovementBehaviour",
    "com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity",
    "com.simibubi.create.content.contraptions.gantry.GantryContraptionUpdatePacket",
    "com.simibubi.create.content.contraptions.ContraptionBlockChangedPacket",
    "com.simibubi.create.content.contraptions.ContraptionDisassemblyPacket",
    "com.simibubi.create.content.contraptions.ContraptionRelocationPacket",
    "com.simibubi.create.content.contraptions.ContraptionStallPacket",
    "com.simibubi.create.content.decoration.palettes.ConnectedGlassBlock",
    "com.simibubi.create.content.equipment.armor.BacktankUtil",
    "com.simibubi.create.content.equipment.clipboard.ClipboardBlockEntity",
    "com.simibubi.create.content.equipment.potatoCannon.PotatoCannonPacket",
    "com.simibubi.create.content.equipment.zapper.ShootGadgetPacket",
    "com.simibubi.create.content.equipment.zapper.ZapperBeamPacket",
    "com.simibubi.create.content.fluids.FluidTransportBehaviour",
    "com.simibubi.create.content.fluids.PipeConnection",
    "com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity",
    "com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity",
    "com.simibubi.create.content.fluids.pump.PumpBlockEntity",
    "com.simibubi.create.content.kinetics.base.KineticBlockEntity",
    "com.simibubi.create.content.kinetics.belt.BeltBlock",
    "com.simibubi.create.content.kinetics.belt.BeltSlicer",
    "com.simibubi.create.content.kinetics.waterwheel.LargeWaterWheelBlockItem",
    "com.simibubi.create.content.logistics.box.PackageEntity",
    "com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour",
    "com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity",
    "com.simibubi.create.content.processing.burner.BlazeBurnerMovementBehaviour",
    "com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockItem",
    "com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity",
    "com.simibubi.create.content.trains.track.TrackBlockItem",
    "com.simibubi.create.content.trains.track.TrackPlacement",
    "com.simibubi.create.content.trains.track.TrackTargetingBehaviour",
    "com.simibubi.create.content.trains.track.TrackTargetingBlockItem",
    "com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour",
    "com.simibubi.create.foundation.utility.ServerSpeedProvider",
    "com.simibubi.create.infrastructure.debugInfo.ServerDebugInfoPacket"
)

$failures = [System.Collections.Generic.List[string]]::new()

if ($JarPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $resolvedJar = (Resolve-Path -LiteralPath $JarPath).Path
    $jar = [System.IO.Compression.ZipFile]::OpenRead($resolvedJar)
    try {
        foreach ($className in $serverClasses) {
            $entryName = $className.Replace('.', '/') + ".class"
            $entry = $jar.GetEntry($entryName)
            if ($null -eq $entry) {
                $failures.Add("MISSING: $className")
                continue
            }

            $input = $entry.Open()
            $memory = [System.IO.MemoryStream]::new()
            try {
                $input.CopyTo($memory)
                $bytecode = [System.Text.Encoding]::ASCII.GetString($memory.ToArray())
                if ($bytecode.Contains("net/minecraft/client/")) {
                    $failures.Add("CLIENT REF: $className")
                }
            }
            finally {
                $memory.Dispose()
                $input.Dispose()
            }
        }
    }
    finally {
        $jar.Dispose()
    }
}
else {
    $classesRoot = (Resolve-Path -LiteralPath $ClassesDirectory).Path
    foreach ($className in $serverClasses) {
        $relativePath = $className.Replace('.', [IO.Path]::DirectorySeparatorChar) + ".class"
        $classFile = Join-Path $classesRoot $relativePath
        if (-not (Test-Path -LiteralPath $classFile)) {
            $failures.Add("MISSING: $className")
            continue
        }

        $bytecode = & javap -verbose $classFile 2>&1
        if ($LASTEXITCODE -ne 0) {
            $failures.Add("JAVAP FAILED: $className")
            continue
        }

        if ($bytecode -match "net/minecraft/client/") {
            $failures.Add("CLIENT REF: $className")
        }
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    throw "Dedicated-server client-reference audit failed ($($failures.Count) issue(s))."
}

$auditedTarget = if ($JarPath) { "final JAR" } else { "compiled classes" }
Write-Host "Dedicated-server bytecode audit passed for ${auditedTarget}: $($serverClasses.Count) shared classes, 0 net/minecraft/client references."
