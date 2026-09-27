$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$path = Join-Path $PSScriptRoot '..\src\main\resources\assets\mjmod\textures\item\onion.png'
$bitmap = [System.Drawing.Bitmap]::new(16, 16)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::Transparent)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

function Fill([System.Drawing.Graphics]$g, [System.Drawing.Brush]$brush, [int]$x, [int]$y, [int]$w, [int]$h) {
    $g.FillRectangle($brush, $x, $y, $w, $h)
}

$outline = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 74, 36, 103))
$skin = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 174, 72, 177))
$light = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 225, 143, 224))
$shadow = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 111, 43, 137))
$green = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 74, 145, 58))
$greenLight = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 121, 190, 72))

# Pixel-art onion bulb with a single tear highlight, sized for the inventory icon.
Fill $graphics $outline 5 3 6 1
Fill $graphics $outline 3 4 10 2
Fill $graphics $outline 2 6 12 5
Fill $graphics $outline 3 11 10 2
Fill $graphics $outline 5 13 6 1
Fill $graphics $skin 4 5 8 7
Fill $graphics $skin 3 7 10 3
Fill $graphics $light 5 5 3 5
Fill $graphics $light 4 8 2 2
Fill $graphics $shadow 9 7 3 5
Fill $graphics $green 7 2 2 3
Fill $graphics $greenLight 8 1 1 3
Fill $graphics ([System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 130, 226, 255))) 10 10 1 2
Fill $graphics ([System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 75, 161, 230))) 11 12 1 1

$bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$bitmap.Dispose()
$outline.Dispose(); $skin.Dispose(); $light.Dispose(); $shadow.Dispose(); $green.Dispose(); $greenLight.Dispose()
Write-Output "Generated $path"
