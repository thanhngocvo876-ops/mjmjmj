$ErrorActionPreference = 'Stop'

function New-Face([int]$texture = -1) {
    return [ordered]@{ uv = @(0, 0, 16, 16); texture = $texture }
}

function New-Cube([string]$name, [double[]]$from, [double[]]$to, [double[]]$origin = @(0, 0, 0), [double[]]$rotation = @(0, 0, 0), [int]$color = 0) {
    $faces = [ordered]@{
        north = New-Face
        east  = New-Face
        south = New-Face
        west  = New-Face
        up    = New-Face
        down  = New-Face
    }
    return [ordered]@{
        name = $name
        from = $from
        to = $to
        autouv = 0
        color = $color
        origin = $origin
        rotation = $rotation
        faces = $faces
        uuid = [guid]::NewGuid().ToString()
    }
}

$elements = [System.Collections.Generic.List[object]]::new()

# A faceted, suspended tear: the dark inner core narrows to a point.
$elements.Add((New-Cube 'void_core_lower' @(-2, 0, -2) @(2, 5, 2) @(0, 2.5, 0) @(0, 0, 0) 0))
$elements.Add((New-Cube 'void_core_mid' @(-3, 4, -3) @(3, 11, 3) @(0, 7.5, 0) @(0, 0, 0) 0))
$elements.Add((New-Cube 'void_core_upper' @(-2, 10, -2) @(2, 15, 2) @(0, 12.5, 0) @(0, 0, 0) 0))
$elements.Add((New-Cube 'void_core_tip' @(-1, 14, -1) @(1, 18, 1) @(0, 14, 0) @(0, 0, 0) 0))

# Broken rim shards frame the core like a cracked dimensional opening.
$elements.Add((New-Cube 'rim_left_lower' @(-5, 1, -1) @(-3, 8, 1) @(-4, 8, 0) @(0, 0, -16) 1))
$elements.Add((New-Cube 'rim_right_lower' @(3, 1, -1) @(5, 8, 1) @(4, 8, 0) @(0, 0, 16) 1))
$elements.Add((New-Cube 'rim_left_mid' @(-6, 6, -1) @(-4, 13, 1) @(-5, 13, 0) @(0, 0, -22) 1))
$elements.Add((New-Cube 'rim_right_mid' @(4, 6, -1) @(6, 13, 1) @(5, 13, 0) @(0, 0, 22) 1))
$elements.Add((New-Cube 'rim_left_upper' @(-4, 12, -1) @(-2, 17, 1) @(-3, 17, 0) @(0, 0, -28) 1))
$elements.Add((New-Cube 'rim_right_upper' @(2, 12, -1) @(4, 17, 1) @(3, 17, 0) @(0, 0, 28) 1))
$elements.Add((New-Cube 'rim_top_left' @(-3, 15, -1) @(-1, 19, 1) @(-2, 18, 0) @(0, 0, -38) 1))
$elements.Add((New-Cube 'rim_top_right' @(1, 15, -1) @(3, 19, 1) @(2, 18, 0) @(0, 0, 38) 1))

# A few detached fragments sell the unstable, tearing-space silhouette.
$elements.Add((New-Cube 'fragment_left' @(-8, 7, -1) @(-6, 10, 1) @(-7, 9, 0) @(0, 0, -30) 1))
$elements.Add((New-Cube 'fragment_right' @(6, 9, -1) @(8, 12, 1) @(7, 11, 0) @(0, 0, 34) 1))
$elements.Add((New-Cube 'fragment_top' @(-1, 20, -1) @(1, 23, 1) @(0, 21, 0) @(0, 0, 0) 1))

$outliner = @($elements | ForEach-Object { $_.uuid })
$model = [ordered]@{
    meta = [ordered]@{
        format_version = '4.10'
        model_format = 'free'
        box_uv = $false
        source = 'mjmod'
    }
    name = '虚空之泪'
    model_identifier = 'mjmod:void_tears'
    on皮 = $null
    resolution = [ordered]@{ width = 64; height = 64 }
    elements = @($elements)
    outliner = $outliner
    textures = @()
    display = [ordered]@{}
}
$model.Remove('on皮')
$json = $model | ConvertTo-Json -Depth 20
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'void_tears.bbmodel'), $json, [Text.UTF8Encoding]::new($false))

# Bedrock geometry export for direct GeckoLib/Bedrock-style inspection.
$bones = @(
    [ordered]@{ name = 'root'; pivot = @(0, 0, 0); cubes = @(
        [ordered]@{ origin = @(-3, 0, -3); size = @(6, 15, 6); uv = @(0, 0) },
        [ordered]@{ origin = @(-1, 14, -1); size = @(2, 9, 2); uv = @(16, 0) }
    ) },
    [ordered]@{ name = 'cracked_rim'; pivot = @(0, 9, 0); cubes = @(
        [ordered]@{ origin = @(-6, 1, -1); size = @(2, 12, 2); uv = @(24, 0) },
        [ordered]@{ origin = @(4, 1, -1); size = @(2, 12, 2); uv = @(24, 0) },
        [ordered]@{ origin = @(-4, 12, -1); size = @(2, 8, 2); uv = @(32, 0) },
        [ordered]@{ origin = @(2, 12, -1); size = @(2, 8, 2); uv = @(32, 0) }
    ) }
)
$geo = [ordered]@{
    format_version = '1.12.0'
    'minecraft:geometry' = @([ordered]@{
        description = [ordered]@{ identifier = 'geometry.void_tears'; texture_width = 64; texture_height = 64; visible_bounds_width = 3; visible_bounds_height = 3; visible_bounds_offset = @(0, 1.5, 0) }
        bones = $bones
    })
}
$geo | ConvertTo-Json -Depth 20 | Set-Content (Join-Path $PSScriptRoot 'void_tears.geo.json') -Encoding utf8
Write-Output 'Generated void_tears.bbmodel and void_tears.geo.json'
