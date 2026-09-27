$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$path = Join-Path $PSScriptRoot '..\src\main\resources\assets\mjmod\textures\item\void_tears_helmet.png'
$bitmap = [System.Drawing.Bitmap]::new(16, 16)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::Transparent)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

function Fill([System.Drawing.Graphics]$g, [System.Drawing.Brush]$b, [int]$x, [int]$y, [int]$w, [int]$h) {
    $g.FillRectangle($b, $x, $y, $w, $h)
}

$outline = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 22, 35, 67))
$white = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 220, 245, 255))
$whiteHi = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
$iris = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 67, 185, 238))
$irisDark = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 31, 93, 177))
$tear = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 81, 207, 255))
$tearHi = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 179, 244, 255))

# Pixel-art almond eye.
Fill $graphics $outline 3 5 10 1
Fill $graphics $outline 2 6 12 2
Fill $graphics $outline 3 8 10 2
Fill $graphics $outline 5 10 6 1
Fill $graphics $white 4 6 8 3
Fill $graphics $whiteHi 5 6 3 2
Fill $graphics $iris 7 5 4 5
Fill $graphics $irisDark 8 6 3 4
Fill $graphics $outline 9 6 2 3
Fill $graphics $whiteHi 7 6 1 1

# A tear hangs from the lower-right edge of the eye.
Fill $graphics $tear 10 9 2 2
Fill $graphics $tear 11 10 1 3
Fill $graphics $tearHi 10 9 1 1
Fill $graphics $tear 11 13 1 1

$bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$bitmap.Dispose()
$outline.Dispose(); $white.Dispose(); $whiteHi.Dispose(); $iris.Dispose(); $irisDark.Dispose(); $tear.Dispose(); $tearHi.Dispose()
Write-Output "Generated $path"
