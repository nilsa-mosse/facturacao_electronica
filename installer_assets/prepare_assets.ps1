Add-Type -AssemblyName System.Drawing

function Resize-And-Save-As-Bmp {
    param (
        [string]$InputPath,
        [string]$OutputPath,
        [int]$Width,
        [int]$Height
    )
    $img = [System.Drawing.Image]::FromFile($InputPath)
    $bmp = New-Object System.Drawing.Bitmap($Width, $Height)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($img, 0, 0, $Width, $Height)
    $bmp.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Bmp)
    $g.Dispose()
    $bmp.Dispose()
    $img.Dispose()
    Write-Host "Saved $OutputPath ($Width x $Height)"
}

$baseDir = "d:\eclipse_workspace\facturacao_electronica\installer_assets"
Resize-And-Save-As-Bmp -InputPath "$baseDir\wizard_image.png" -OutputPath "$baseDir\wizard_image.bmp" -Width 164 -Height 314
Resize-And-Save-As-Bmp -InputPath "$baseDir\wizard_small_image.png" -OutputPath "$baseDir\wizard_small_image.bmp" -Width 55 -Height 55
