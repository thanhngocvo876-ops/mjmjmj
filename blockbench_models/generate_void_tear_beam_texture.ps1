$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$path = Join-Path $PSScriptRoot '..\src\main\resources\assets\mjmod\textures\entity\void_tear_beam.png'
$bitmap = [System.Drawing.Bitmap]::new(32, 32)
$g = [System.Drawing.Graphics]::FromImage($bitmap)
$g.Clear([System.Drawing.Color]::Transparent)
$base = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(215, 184, 231, 255))
$flowA = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(225, 216, 243, 255))
$flowB = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(205, 137, 207, 255))
$g.FillRectangle($base, 0, 0, 32, 32)
$g.FillRectangle($flowA, 0, 5, 32, 3)
$g.FillRectangle($flowB, 0, 15, 32, 2)
$g.FillRectangle($flowA, 0, 25, 32, 3)
$bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bitmap.Dispose(); $base.Dispose(); $flowA.Dispose(); $flowB.Dispose()
Write-Output "Generated $path"
