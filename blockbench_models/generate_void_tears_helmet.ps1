$ErrorActionPreference = 'Stop'

function Face([int]$texture = 0) {
    # The model intentionally uses one translucent atlas.  Keep every face on
    # index 0 so Blockbench never resolves a non-existent second texture.
    [ordered]@{ uv = @(0, 0, 16, 16); texture = 0 }
}

function Cube([string]$name, [double[]]$from, [double[]]$to, [double[]]$origin, [double[]]$rotation, [int]$texture = 0) {
    [ordered]@{
        name = $name
        from = $from
        to = $to
        autouv = 0
        color = 0
        origin = $origin
        rotation = $rotation
        faces = [ordered]@{ north = Face $texture; east = Face $texture; south = Face $texture; west = Face $texture; up = Face $texture; down = Face $texture }
        uuid = [guid]::NewGuid().ToString()
    }
}

$parts = [System.Collections.Generic.List[object]]::new()
# Head-local coordinates: Minecraft head is roughly -4..4 on each axis.
# The front of the face is -Z. The effect sits on the right eye and rises above it.
$parts.Add((Cube 'tear_eye_core' @(-3.8, -4.8, -5.2) @(-0.2, -1.6, -3.1) @(-2.0, -3.2, -4.1) @(0, 0, -8) 0))
$parts.Add((Cube 'tear_eye_lobe' @(-4.9, -4.0, -4.8) @(-2.3, -0.8, -2.8) @(-3.6, -2.4, -3.8) @(0, 0, -22) 0))
$parts.Add((Cube 'tear_cheek_drop' @(-3.8, -2.8, -4.7) @(-1.8, 0.0, -3.0) @(-2.8, -1.4, -3.8) @(0, 0, -12) 1))
$parts.Add((Cube 'tear_brow' @(-4.0, -5.0, -3.8) @(-0.5, -3.2, -1.8) @(-2.25, -4.1, -2.8) @(0, 0, -18) 0))

# Crown tongues and plume now sweep into the visible upper-left/front quadrant.
# Negative X/Y is intentional: the flow direction is readable from the front,
# while negative Z keeps the effect in front of the player's head.
$parts.Add((Cube 'crown_spike_left' @(-6.0, -9.5, -4.15) @(-4.0, -5.0, -2.35) @(-5.0, -7.25, -3.25) @(0, -8, -42) 1))
$parts.Add((Cube 'crown_spike_center' @(-4.4, -10.0, -4.0) @(-2.5, -5.0, -2.2) @(-3.45, -7.5, -3.1) @(0, -4, -28) 0))
$parts.Add((Cube 'crown_spike_right' @(-2.8, -8.5, -3.85) @(-1.0, -4.7, -2.15) @(-1.9, -6.6, -3.0) @(0, 4, -16) 1))

# Arcing plume: staggered sections form a continuous diagonal trail toward -X/-Y.
$parts.Add((Cube 'tail_front' @(-3.5, -3.4, -4.35) @(-1.0, -0.9, -2.45) @(-2.25, -2.15, -3.40) @(-16, -10, -34) 0))
$parts.Add((Cube 'tail_mid' @(-5.0, -4.8, -4.15) @(-2.8, -2.3, -2.45) @(-3.9, -3.55, -3.30) @(-22, -14, -42) 1))
$parts.Add((Cube 'tail_back' @(-6.5, -6.1, -3.95) @(-4.6, -3.8, -2.45) @(-5.55, -4.95, -3.20) @(-28, -18, -50) 0))
$parts.Add((Cube 'tail_tip' @(-8.0, -7.4, -3.75) @(-6.5, -5.4, -2.45) @(-6.65, -6.4, -3.05) @(-34, -22, -58) 1))

# Detached luminous droplets trail along the same upper-left current. They
# remain asymmetric so the effect feels like escaping liquid rather than a halo.
$parts.Add((Cube 'drop_outer' @(-7.0, -5.3, -4.25) @(-6.0, -3.9, -3.15) @(-6.5, -4.6, -3.7) @(0, 0, -38) 1))
$parts.Add((Cube 'drop_lower' @(-8.4, -6.8, -4.05) @(-7.2, -5.4, -2.95) @(-7.8, -6.1, -3.50) @(0, 0, -50) 0))
$parts.Add((Cube 'drop_crown' @(-9.6, -8.4, -3.85) @(-8.6, -7.1, -2.85) @(-9.1, -7.75, -3.35) @(0, 0, -60) 1))

$headUuid = [guid]::NewGuid().ToString()
$outliner = @([ordered]@{
    name = 'head'
    origin = @(0, 0, 0)
    color = 0
    uuid = $headUuid
    export = $true
    isOpen = $true
    children = @($parts | ForEach-Object { $_.uuid })
})

$texturePath = Join-Path $PSScriptRoot 'void_tears_helmet.png'
if (-not ('System.Drawing.Bitmap' -as [type])) { Add-Type -AssemblyName System.Drawing }
$bmp = [System.Drawing.Bitmap]::new(64, 64)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$brushes = @(
    [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(220, 235, 255, 255)),
    [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(220, 150, 235, 255)),
    [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(220, 80, 175, 245)),
    [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(220, 48, 95, 180))
)
# Irregular liquid-flame silhouettes instead of a square icon: outer cyan edge, white core, blue folds.
$outer = [System.Drawing.Point[]]@([System.Drawing.Point]::new(8,0),[System.Drawing.Point]::new(10,3),[System.Drawing.Point]::new(14,2),[System.Drawing.Point]::new(12,6),[System.Drawing.Point]::new(15,8),[System.Drawing.Point]::new(12,10),[System.Drawing.Point]::new(13,14),[System.Drawing.Point]::new(9,11),[System.Drawing.Point]::new(6,16),[System.Drawing.Point]::new(6,11),[System.Drawing.Point]::new(1,13),[System.Drawing.Point]::new(4,8),[System.Drawing.Point]::new(0,6),[System.Drawing.Point]::new(5,5))
$inner = [System.Drawing.Point[]]@([System.Drawing.Point]::new(8,2),[System.Drawing.Point]::new(9,5),[System.Drawing.Point]::new(12,4),[System.Drawing.Point]::new(10,7),[System.Drawing.Point]::new(13,8),[System.Drawing.Point]::new(10,9),[System.Drawing.Point]::new(11,13),[System.Drawing.Point]::new(8,10),[System.Drawing.Point]::new(6,14),[System.Drawing.Point]::new(6,9),[System.Drawing.Point]::new(3,11),[System.Drawing.Point]::new(6,8),[System.Drawing.Point]::new(3,6),[System.Drawing.Point]::new(6,6))
$g.FillPolygon($brushes[2], $outer)
$g.FillPolygon($brushes[1], $inner)
$g.FillEllipse($brushes[0], 6, 5, 5, 5)
$g.FillEllipse($brushes[2], 7, 6, 2, 3)
$g.FillEllipse($brushes[0], 10, 9, 3, 3)
$bmp.Save($texturePath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose(); $brushes | ForEach-Object { $_.Dispose() }

# Vanilla still draws an armor layer for ArmorItem. A transparent layer keeps only
# the custom animated eye-flame model visible and prevents the purple missing texture.
$armorLayerPath = Join-Path $PSScriptRoot 'void_tears_layer_1.png'
$armorBmp = [System.Drawing.Bitmap]::new(64, 32)
$armorBmp.Save($armorLayerPath, [System.Drawing.Imaging.ImageFormat]::Png)
$armorBmp.Dispose()

$model = [ordered]@{
    # Blockbench's Java modded entity format supports a named head bone and armor-style export.
    meta = [ordered]@{ format_version = '4.10'; model_format = 'modded_entity'; box_uv = $false; source = 'mjmod' }
    name = '虚空之泪·头部护甲'
    model_identifier = 'mjmod:void_tears_helmet'
    resolution = [ordered]@{ width = 64; height = 64 }
    elements = @($parts)
    outliner = $outliner
    textures = @([ordered]@{
        path = 'void_tears_helmet.png'; name = 'void_tears_helmet.png'; folder = ''; namespace = ''; id = 'void_tears_helmet'; particle = $true; render_mode = 'translucent'; visible = $true; internal = $false; double_sided = $true; saved = $true; uuid = [guid]::NewGuid().ToString()
    })
    display = [ordered]@{}
}
$model | ConvertTo-Json -Depth 30 | Set-Content (Join-Path $PSScriptRoot 'void_tears_helmet.bbmodel') -Encoding utf8

# Keep the interchange export in lockstep with the Blockbench file.  Some
# importers ignore the Java-only model class, so exporting every authored cube
# here makes the upper-left silhouette available to those tools as well.
$geoCubes = @($parts | ForEach-Object {
    $from = @($_.from)
    $to = @($_.to)
    $rotation = @($_.rotation)
    $origin = @($_.origin)
    $size = @(
        ([double]$to[0] - [double]$from[0]),
        ([double]$to[1] - [double]$from[1]),
        ([double]$to[2] - [double]$from[2])
    )
    [ordered]@{
        origin = @([double]$from[0], [double]$from[1], [double]$from[2])
        size = $size
        uv = @(0, 0)
        rotation = @([double]$rotation[0], [double]$rotation[1], [double]$rotation[2])
        pivot = @([double]$origin[0], [double]$origin[1], [double]$origin[2])
    }
})

$geo = [ordered]@{
    format_version = '1.12.0'
    'minecraft:geometry' = @([ordered]@{
        description = [ordered]@{ identifier = 'geometry.void_tears_helmet'; texture_width = 64; texture_height = 64; visible_bounds_width = 3; visible_bounds_height = 2; visible_bounds_offset = @(0, 0.5, 0) }
        bones = @([ordered]@{ name = 'head'; pivot = @(0, 0, 0); cubes = $geoCubes })
    })
}
$geo | ConvertTo-Json -Depth 20 | Set-Content (Join-Path $PSScriptRoot 'void_tears_helmet.geo.json') -Encoding utf8
Write-Output 'Generated void_tears_helmet.bbmodel, void_tears_helmet.png and void_tears_helmet.geo.json'
