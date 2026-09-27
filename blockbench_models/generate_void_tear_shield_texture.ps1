$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$path = Join-Path $PSScriptRoot '..\src\main\resources\assets\mjmod\textures\entity\void_tear_shield.png'
$bitmap = [System.Drawing.Bitmap]::new(64, 64)
$g = [System.Drawing.Graphics]::FromImage($bitmap)
$g.Clear([System.Drawing.Color]::Transparent)
$outer = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(92, 55, 173, 229))
$mid = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(105, 120, 213, 255))
$light = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(140, 188, 240, 255))
$core = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(175, 224, 250, 255))
$g.FillRectangle($outer, 0, 0, 64, 64)
for ($y = 0; $y -lt 64; $y += 8) {
    if ((($y / 8) % 2) -eq 0) { $g.FillRectangle($mid, 0, $y, 64, 3) }
    else { $g.FillRectangle($light, 0, $y + 2, 64, 2) }
}
$g.FillRectangle($core, 5, 27, 54, 3)
$g.FillRectangle($light, 10, 43, 44, 2)
$bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bitmap.Dispose(); $outer.Dispose(); $mid.Dispose(); $light.Dispose(); $core.Dispose()
Write-Output "Generated $path"
