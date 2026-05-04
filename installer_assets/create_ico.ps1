Add-Type -AssemblyName System.Drawing
$pngPath = "d:\eclipse_workspace\facturacao_electronica\installer_assets\wizard_small_image.png"
$icoPath = "d:\eclipse_workspace\facturacao_electronica\installer_assets\KwanzaERP.ico"

$img = [System.Drawing.Bitmap]::FromFile($pngPath)
$hIcon = $img.GetHicon()
$icon = [System.Drawing.Icon]::FromHandle($hIcon)

$stream = [System.IO.File]::Create($icoPath)
$icon.Save($stream)
$stream.Close()

$icon.Dispose()
$img.Dispose()

Write-Host "Ícone criado em $icoPath"
