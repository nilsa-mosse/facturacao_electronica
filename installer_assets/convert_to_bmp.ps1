Add-Type -AssemblyName System.Drawing
$sidebarPath = "d:\eclipse_workspace\facturacao_electronica\installer_assets\sidebar.png"
$sidebarBmp = "d:\eclipse_workspace\facturacao_electronica\installer_assets\sidebar.bmp"
$smallPath = "d:\eclipse_workspace\facturacao_electronica\installer_assets\small_logo.png"
$smallBmp = "d:\eclipse_workspace\facturacao_electronica\installer_assets\small_logo.bmp"

$sidebar = [System.Drawing.Image]::FromFile($sidebarPath)
$sidebar.Save($sidebarBmp, [System.Drawing.Imaging.ImageFormat]::Bmp)
$sidebar.Dispose()

$small = [System.Drawing.Image]::FromFile($smallPath)
$small.Save($smallBmp, [System.Drawing.Imaging.ImageFormat]::Bmp)
$small.Dispose()

Write-Host "Conversão concluída com sucesso!"
