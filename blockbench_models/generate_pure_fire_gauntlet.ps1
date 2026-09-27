# 生成 纯质阳炎·火焰拳套 的 Blockbench 工程、Bedrock 几何导出、贴图与 Java 数组片段。
# 坐标约定与 PureFireFistModel 一致：原点在拳心，+Y 朝指尖，-Y 沿小臂向上（火焰向上升腾），
# -Z 为拳面正前方。左手为基准定义，右手按 x 镜像。
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# 字段: 名称, x, y, z, dx, dy, dz, texU, texV, 层(0拳套 1焰芯 2中焰 3外焰 4火星), curlDeg
$cubeDefs = @(
    @('band',       -3,  0, -2, 6, 2, 4,  0,  0, 0,  0),
    @('knuckle',    -3, -1, -3, 6, 1, 2,  0,  8, 0,  0),
    @('cuff',       -3, -2, -2, 6, 1, 4,  0, 12, 0,  0),
    @('fingerband', -3,  2, -2, 6, 1, 4,  0, 18, 0,  0),
    @('trim_l',     -4,  0, -1, 1, 2, 2, 24,  0, 0,  0),
    @('trim_r',      3,  0, -1, 1, 2, 2, 24,  4, 0,  0),
    @('core_a',     -2, -7,  0, 2, 5, 2, 24,  8, 1,  0),
    @('core_b',      0, -6, -1, 2, 4, 2, 24,  8, 1,  0),
    @('core_c',     -1, -5, -3, 2, 3, 2, 24,  8, 1,  0),
    @('core_d',     -1, -5,  1, 2, 3, 2, 24,  8, 1,  0),
    @('core_tip_a', -1.5, -8.5,  0.5, 1, 2, 1, 24, 24, 1,  0),
    @('core_tip_b',  0.5, -7.5, -0.5, 1, 1, 1, 24, 24, 1,  0),
    @('core_tip_c', -0.5, -7.5, -2.5, 1, 1, 1, 24, 26, 1,  0),
    @('mid_a',      -4, -9, -2, 2, 7, 2, 40,  0, 2, -5),
    @('mid_b',       2, -8, -2, 2, 6, 2, 40,  0, 2,  5),
    @('mid_c',      -4, -7,  1, 2, 5, 2, 40,  0, 2,  0),
    @('mid_d',       2, -6,  1, 2, 4, 2, 40,  0, 2,  0),
    @('mid_e',      -1, -8, -4, 2, 6, 1, 40,  0, 2,  0),
    @('mid_f',      -1, -7,  3, 2, 5, 1, 40,  0, 2,  0),
    @('mid_tip_a', -3.5, -10.5, -1.5, 1, 2, 1, 40, 12, 2, -9),
    @('mid_tip_b',  2.5, -9.5, -1.5, 1, 1, 1, 40, 12, 2,  8),
    @('mid_tip_c', -0.5, -9.5, -4.5, 1, 1, 1, 40, 14, 2,  0),
    @('outer_a',    -6, -11, -2, 2, 9, 2,  0, 32, 3, -7),
    @('outer_b',     4, -10, -1, 2, 8, 2,  0, 32, 3,  7),
    @('outer_c',    -5, -8,  2, 2, 6, 2,  0, 32, 3,  0),
    @('outer_d',     3, -7,  2, 2, 5, 2,  0, 32, 3,  0),
    @('outer_e',    -2, -10, -5, 2, 8, 2,  0, 32, 3,  0),
    @('outer_f',    -2, -8,  4, 2, 6, 2,  0, 32, 3,  0),
    @('outer_tip_a', -6.5, -12.5, -1.5, 1, 2, 1, 16, 32, 3, -11),
    @('outer_tip_b',  4.5, -11.5, -0.5, 1, 1, 1, 16, 32, 3, 10),
    @('spark_a',    -7, -14, -1, 1, 2, 1, 16, 36, 4,  0),
    @('spark_b',     5, -13,  2, 1, 1, 1, 16, 36, 4,  0)
)

# ---- vanilla 盒子 UV：与 ModelPart texOffs 完全一致，保证 bbmodel 与游戏内像素对齐 ----
function FaceUv([double]$u, [double]$v, [double]$w, [double]$h, [double]$d, [string]$face) {
    switch ($face) {
        'up'    { return @([double]($u + $d), [double]$v, [double]($u + $d + $w), [double]($v + $d)) }
        'down'  { return @([double]($u + $d + $w), [double]$v, [double]($u + $d + 2 * $w), [double]($v + $d)) }
        'west'  { return @([double]$u, [double]($v + $d), [double]($u + $d), [double]($v + $d + $h)) }
        'north' { return @([double]($u + $d), [double]($v + $d), [double]($u + $d + $w), [double]($v + $d + $h)) }
        'east'  { return @([double]($u + $d + $w), [double]($v + $d), [double]($u + $d + $w + $d), [double]($v + $d + $h)) }
        'south' { return @([double]($u + $d + $w + $d), [double]($v + $d), [double]($u + 2 * $d + $w), [double]($v + $d + $h)) }
    }
}

function BuildCube([string]$name, $def, [bool]$mirror) {
    $x = [double]$def[1]; $y = [double]$def[2]; $z = [double]$def[3]
    $dx = [double]$def[4]; $dy = [double]$def[5]; $dz = [double]$def[6]
    $u = [double]$def[7]; $v = [double]$def[8]
    $layer = [int]$def[9]; $curlDeg = [double]$def[10]
    if ($mirror) {
        $x = -($x + $dx)
        $curlDeg = -$curlDeg
        $name = 'r_' + $name
    } else {
        $name = 'l_' + $name
    }
    $faces = [ordered]@{}
    foreach ($f in @('north', 'east', 'south', 'west', 'up', 'down')) {
        $faces[$f] = [ordered]@{ uv = FaceUv $u $v $dx $dy $dz $f; texture = 0 }
    }
    return [ordered]@{
        name = $name
        from = @($x, $y, $z)
        to = @([double]($x + $dx), [double]($y + $dy), [double]($z + $dz))
        autouv = 0
        color = $layer
        origin = @(0, 0, 0)
        rotation = @(0, 0, $curlDeg)
        faces = $faces
        type = 'cube'
        uuid = [guid]::NewGuid().ToString()
    }
}

$leftElements  = [System.Collections.Generic.List[object]]::new()
$rightElements = [System.Collections.Generic.List[object]]::new()
foreach ($def in $cubeDefs) {
    $leftElements.Add((BuildCube $def[0] $def $false))
    $rightElements.Add((BuildCube $def[0] $def $true))
}

$outliner = @(
    [ordered]@{ name = 'left_gauntlet';  origin = @(0, 0, 0); color = 0; uuid = [guid]::NewGuid().ToString(); export = $true; isOpen = $true; children = @($leftElements | ForEach-Object { $_.uuid }) },
    [ordered]@{ name = 'right_gauntlet'; origin = @(0, 0, 0); color = 1; uuid = [guid]::NewGuid().ToString(); export = $true; isOpen = $true; children = @($rightElements | ForEach-Object { $_.uuid }) }
)

# ---- 64x64 贴图：拳套金甲 + 三层火焰渐变（白炽焰芯/炽橙中焰/赤红外焰）+ 流动光痕 ----
$bmp = [System.Drawing.Bitmap]::new(64, 64)
function FillVGrad([System.Drawing.Bitmap]$b, [int]$x, [int]$y, [int]$w, [int]$h,
                   [System.Drawing.Color]$top, [System.Drawing.Color]$bot) {
    for ($row = 0; $row -lt $h; $row++) {
        $t = if ($h -le 1) { 0 } else { $row / [double]($h - 1) }
        $c = [System.Drawing.Color]::FromArgb(
            [int][Math]::Round($top.A + ($bot.A - $top.A) * $t),
            [int][Math]::Round($top.R + ($bot.R - $top.R) * $t),
            [int][Math]::Round($top.G + ($bot.G - $top.G) * $t),
            [int][Math]::Round($top.B + ($bot.B - $top.B) * $t))
        for ($col = 0; $col -lt $w; $col++) { $b.SetPixel($x + $col, $y + $row, $c) }
    }
}
function Streaks([System.Drawing.Bitmap]$b, [int]$x, [int]$y, [int]$w, [int]$h, [int]$boost) {
    # 断续的斜向亮痕：模型摆动时会呈现火舌向上流动的错觉
    for ($row = 1; $row -lt $h; $row += 3) {
        for ($col = 0; $col -lt $w; $col++) {
            if ((($col * 3 + $row * 5) % 11) -ne 0) { continue }
            $c = $b.GetPixel($x + $col, $y + $row)
            $b.SetPixel($x + $col, $y + $row, [System.Drawing.Color]::FromArgb($c.A,
                [Math]::Min(255, $c.R + $boost), [Math]::Min(255, $c.G + $boost),
                [Math]::Min(255, $c.B + [int]($boost * 0.6))))
        }
    }
}

# 拳套金甲（区域 0,0 - 24,24）
FillVGrad $bmp 0 0 24 24 ([System.Drawing.Color]::FromArgb(255, 255, 201, 77)) ([System.Drawing.Color]::FromArgb(255, 156, 74, 12))
Streaks $bmp 0 0 24 24 18
for ($i = 0; $i -lt 10; $i++) { $bmp.SetPixel((4 + $i * 2), (3 + ($i % 4) * 5), [System.Drawing.Color]::FromArgb(255, 255, 236, 170)) }

# 焰芯（24,8 16x16 白炽->金黄）+ 焰芯尖端（24,24 8x4）
FillVGrad $bmp 24 8 16 16 ([System.Drawing.Color]::FromArgb(250, 255, 253, 232)) ([System.Drawing.Color]::FromArgb(250, 255, 195, 0))
Streaks $bmp 24 8 16 16 22
FillVGrad $bmp 24 24 8 4 ([System.Drawing.Color]::FromArgb(250, 255, 255, 244)) ([System.Drawing.Color]::FromArgb(250, 255, 236, 160))

# 中焰（40,0 16x16 金黄->炽橙）+ 尖端（40,12 8x4）
FillVGrad $bmp 40 0 16 16 ([System.Drawing.Color]::FromArgb(240, 255, 225, 77)) ([System.Drawing.Color]::FromArgb(240, 235, 90, 0))
Streaks $bmp 40 0 16 16 20
FillVGrad $bmp 40 12 8 4 ([System.Drawing.Color]::FromArgb(240, 255, 200, 90)) ([System.Drawing.Color]::FromArgb(240, 250, 120, 20))

# 外焰（0,32 16x16 橙红->深赤，半透明）+ 尖端与火星（16,32 8x8）
FillVGrad $bmp 0 32 16 16 ([System.Drawing.Color]::FromArgb(215, 255, 106, 42)) ([System.Drawing.Color]::FromArgb(215, 138, 15, 46))
Streaks $bmp 0 32 16 16 18
FillVGrad $bmp 16 32 8 8 ([System.Drawing.Color]::FromArgb(230, 255, 170, 90)) ([System.Drawing.Color]::FromArgb(230, 220, 40, 30))

$pngPath = Join-Path $PSScriptRoot 'pure_fire_gauntlet.png'
$bmp.Save($pngPath, [System.Drawing.Imaging.ImageFormat]::Png)
$resPngDir = Join-Path $PSScriptRoot '..\src\main\resources\assets\mjmod\textures\effect'
if (-not (Test-Path $resPngDir)) { New-Item -ItemType Directory -Path $resPngDir -Force | Out-Null }
$bmp.Save((Join-Path $resPngDir 'pure_fire_gauntlet.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

$source = 'data:image/png;base64,' + [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($pngPath))

# ---- Blockbench 工程 ----
$model = [ordered]@{
    meta = [ordered]@{ format_version = '4.10'; model_format = 'modded_entity'; box_uv = $false; source = 'mjmod' }
    name = '纯质阳炎·火焰拳套'
    model_identifier = 'mjmod:pure_fire_gauntlet'
    resolution = [ordered]@{ width = 64; height = 64 }
    elements = @($leftElements + $rightElements)
    outliner = $outliner
    textures = @([ordered]@{
        path = 'pure_fire_gauntlet.png'; name = 'pure_fire_gauntlet.png'; folder = ''; namespace = ''
        id = 'pure_fire_gauntlet'; particle = $false; render_mode = 'translucent'; visible = $true
        internal = $false; double_sided = $true; saved = $true; uuid = [guid]::NewGuid().ToString()
        source = $source
    })
    display = [ordered]@{}
}
$model | ConvertTo-Json -Depth 30 | Set-Content (Join-Path $PSScriptRoot 'pure_fire_gauntlet.bbmodel') -Encoding utf8

# ---- Bedrock/GeckoLib 风格几何导出 ----
function GeoCubes($elements) {
    return @($elements | ForEach-Object {
        $from = $_.from; $to = $_.to; $rot = $_.rotation
        [ordered]@{
            origin = @([double]$from[0], [double]$from[1], [double]$from[2])
            size = @([double]($to[0] - $from[0]), [double]($to[1] - $from[1]), [double]($to[2] - $from[2]))
            uv = @(0, 0)
            rotation = @([double]$rot[0], [double]$rot[1], [double]$rot[2])
            pivot = @(0, 0, 0)
        }
    })
}
$geo = [ordered]@{
    format_version = '1.12.0'
    'minecraft:geometry' = @([ordered]@{
        description = [ordered]@{ identifier = 'geometry.pure_fire_gauntlet'; texture_width = 64; texture_height = 64; visible_bounds_width = 3; visible_bounds_height = 3; visible_bounds_offset = @(0, 1, 0) }
        bones = @(
            [ordered]@{ name = 'left_gauntlet';  pivot = @(0, 0, 0); cubes = GeoCubes $leftElements },
            [ordered]@{ name = 'right_gauntlet'; pivot = @(0, 0, 0); cubes = GeoCubes $rightElements }
        )
    })
}
$geo | ConvertTo-Json -Depth 20 | Set-Content (Join-Path $PSScriptRoot 'pure_fire_gauntlet.geo.json') -Encoding utf8

# ---- Java 模型数组片段（PureFireGauntletModel 直接粘贴使用） ----
$java = [System.Collections.Generic.List[string]]::new()
$java.Add('// 由 generate_pure_fire_gauntlet.ps1 生成：Box(x, y, z, dx, dy, dz, u, v, layer, rise, curlRad)')
foreach ($mirror in @($false, $true)) {
    $side = if ($mirror) { 'RIGHT' } else { 'LEFT' }
    $java.Add(("private static final Box[] {0}_BOXES = {{" -f $side))
    foreach ($def in $cubeDefs) {
        $x = [double]$def[1]; $y = [double]$def[2]; $z = [double]$def[3]
        $dx = [double]$def[4]; $dy = [double]$def[5]; $dz = [double]$def[6]
        $u = [int]$def[7]; $v = [int]$def[8]; $layer = [int]$def[9]
        $curlDeg = [double]$def[10]
        if ($mirror) { $x = -($x + $dx); $curlDeg = -$curlDeg }
        $topAbove = -($y)
        $rise = [Math]::Max(0.0, [Math]::Min(1.0, $topAbove / 12.0))
        if ($layer -eq 0) { $rise = 0.0 }
        $curlRad = [Math]::Round($curlDeg * [Math]::PI / 180.0, 4)
        $fmt = '    new Box({0}F, {1}F, {2}F, {3}F, {4}F, {5}F, {6}, {7}, {8}, {9}F, {10}F),'
        $java.Add(($fmt -f $x, $y, $z, $dx, $dy, $dz, $u, $v, $layer, $rise, $curlRad))
    }
    $java.Add('};')
    $java.Add('')
}
$java | Set-Content (Join-Path $PSScriptRoot 'pure_fire_gauntlet_java_snippet.txt') -Encoding utf8

Write-Output ('Generated pure_fire_gauntlet.bbmodel / .geo.json / .png / java snippet. Cubes per hand: ' + $cubeDefs.Count)
