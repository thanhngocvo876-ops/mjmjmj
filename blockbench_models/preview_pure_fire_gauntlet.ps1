# 将 pure_fire_gauntlet.bbmodel 渲染为 PNG 预览图（等距投影 + 贴图取色 + 简单光照）。
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$dir = $PSScriptRoot
$model = [System.IO.File]::ReadAllText((Join-Path $dir 'pure_fire_gauntlet.bbmodel')) | ConvertFrom-Json

$texture = [System.Drawing.Bitmap]::new((Join-Path $dir 'pure_fire_gauntlet.png'))

function TexColor([double[]]$uv) {
    $cx = [int][Math]::Round(($uv[0] + $uv[2]) / 2.0)
    $cy = [int][Math]::Round(($uv[1] + $uv[3]) / 2.0)
    $r = 0; $g = 0; $b = 0; $a = 0; $n = 0
    foreach ($off in @(@(0,0), @(2,0), @(-2,0), @(0,2), @(0,-2))) {
        $px = [Math]::Min(63, [Math]::Max(0, $cx + $off[0]))
        $py = [Math]::Min(63, [Math]::Max(0, $cy + $off[1]))
        $c = $texture.GetPixel($px, $py)
        $r += $c.R; $g += $c.G; $b += $c.B; $a += $c.A; $n++
    }
    $v0 = $r / $n / 255.0
    $v1 = $g / $n / 255.0
    $v2 = $b / $n / 255.0
    $v3 = $a / $n / 255.0
    return [double[]]@($v0, $v1, $v2, $v3)
}

# 元素 uuid -> 所属组
$uuidToGroup = @{}
foreach ($grp in $model.outliner) {
    foreach ($child in $grp.children) { $uuidToGroup[$child] = $grp.name }
}

function RotateZ([double]$x, [double]$y, [double]$deg) {
    $a = $deg * [Math]::PI / 180.0
    return [double[]]@(($x * [Math]::Cos($a) - $y * [Math]::Sin($a)), ($x * [Math]::Sin($a) + $y * [Math]::Cos($a)))
}

$faceCorners = @{
    north = @(@(0,0,0), @(1,0,0), @(1,1,0), @(0,1,0))
    south = @(@(0,0,1), @(1,0,1), @(1,1,1), @(0,1,1))
    west  = @(@(0,0,0), @(0,0,1), @(0,1,1), @(0,1,0))
    east  = @(@(1,0,0), @(1,0,1), @(1,1,1), @(1,1,0))
    up    = @(@(0,0,0), @(1,0,0), @(1,0,1), @(0,0,1))
    down  = @(@(0,1,0), @(1,1,0), @(1,1,1), @(0,1,1))
}
$faceNormals = @{
    north = @(0.0, 0.0, -1.0); south = @(0.0, 0.0, 1.0)
    west = @(-1.0, 0.0, 0.0); east = @(1.0, 0.0, 0.0)
    up = @(0.0, -1.0, 0.0); down = @(0.0, 1.0, 0.0)
}

function RenderView([System.Drawing.Bitmap]$canvas, [double]$yawDeg, [int]$cx, [int]$cy, [double]$scale) {
    $faces = [System.Collections.Generic.List[object]]::new()
    $yaw = $yawDeg * [Math]::PI / 180.0
    $pitch = 16.0 * [Math]::PI / 180.0
    foreach ($el in $model.elements) {
        $group = $uuidToGroup[$el.uuid]
        $gx = 0.0
        if ($group -eq 'left_gauntlet') { $gx = -9.5 }
        if ($group -eq 'right_gauntlet') { $gx = 9.5 }
        $f = @([double]($el.from[0]), [double]($el.from[1]), [double]($el.from[2]))
        $t = @([double]($el.to[0]), [double]($el.to[1]), [double]($el.to[2]))
        $curl = [double]($el.rotation[2])
        $corners = [System.Collections.Generic.List[object]]::new()
        foreach ($sel in @(@($f[0],$f[1],$f[2]), @($t[0],$f[1],$f[2]), @($f[0],$t[1],$f[2]), @($t[0],$t[1],$f[2]),
                          @($f[0],$f[1],$t[2]), @($t[0],$f[1],$t[2]), @($f[0],$t[1],$t[2]), @($t[0],$t[1],$t[2]))) {
            $rot = RotateZ $sel[0] $sel[1] $curl
            $x = $rot[0] + $gx
            $y = $rot[1]
            $z = $sel[2]
            # 偏航
            $rx = $x * [Math]::Cos($yaw) + $z * [Math]::Sin($yaw)
            $rz = -$x * [Math]::Sin($yaw) + $z * [Math]::Cos($yaw)
            # 俯仰
            $y2 = $y * [Math]::Cos($pitch) - $rz * [Math]::Sin($pitch)
            $z2 = $y * [Math]::Sin($pitch) + $rz * [Math]::Cos($pitch)
            $corners.Add([double[]]@($rx, $y2, $z2))
        }
        foreach ($faceName in @('north', 'south', 'west', 'east', 'up', 'down')) {
            $uv = [double[]]$el.faces.$faceName.uv
            $col = TexColor $uv
            if ($col[3] -le 0.01) { continue }
            $n = $faceNormals[$faceName]
            $nrx = $n[0] * [Math]::Cos($yaw) + $n[2] * [Math]::Sin($yaw)
            $nrz = -$n[0] * [Math]::Sin($yaw) + $n[2] * [Math]::Cos($yaw)
            $nrn = @($nrx, $n[1], $nrz)
            # 光来自上方偏前：模型 -Y 是上
            $lx = 0.30; $ly = -0.80; $lz = 0.52
            $dot = $nrn[0] * $lx + $nrn[1] * $ly + $nrn[2] * $lz
            $bright = 0.72 + 0.28 * [Math]::Max(0.0, $dot)
            $pts = [System.Collections.Generic.List[System.Drawing.PointF]]::new()
            $depth = 0.0
            foreach ($idx in $faceCorners[$faceName]) {
                $c = $corners[$idx[0] * 4 + $idx[1] * 2 + $idx[2]]
                $pts.Add([System.Drawing.PointF]::new([single]($cx + $c[0] * $scale), [single]($cy + $c[1] * $scale)))
                $depth += $c[2]
            }
            $faces.Add([ordered]@{ pts = $pts; depth = $depth; col = $col; bright = $bright })
        }
    }
    $sorted = $faces | Sort-Object { $_.depth }
    $g = [System.Drawing.Graphics]::FromImage($canvas)
    foreach ($face in $sorted) {
        $c = $face.col
        $alpha = [int][Math]::Round([Math]::Min(1.0, $c[3] * 1.15) * 255)
        $brush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb($alpha,
            [int][Math]::Min(255, $c[0] * 255 * $face.bright),
            [int][Math]::Min(255, $c[1] * 255 * $face.bright),
            [int][Math]::Min(255, $c[2] * 255 * $face.bright)))
        $g.FillPolygon($brush, $face.pts.ToArray())
        $brush.Dispose()
    }
    $g.Dispose()
}

$W = 980; $H = 700
$bmp = [System.Drawing.Bitmap]::new($W, $H)
$bg = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 24, 20, 26))
$gInit = [System.Drawing.Graphics]::FromImage($bmp)
$gInit.FillRectangle($bg, 0, 0, $W, $H)
$gInit.Dispose()
RenderView $bmp -34 300 400 15.5
RenderView $bmp 146 690 400 15.5
$previewPath = Join-Path $dir 'pure_fire_gauntlet_preview.png'
$bmp.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
$texture.Dispose()
Write-Output ('Saved ' + $previewPath)
