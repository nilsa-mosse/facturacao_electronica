Add-Type -AssemblyName System.Drawing
$inputPath = "d:\eclipse_workspace\facturacao_electronica\installer_assets\wizard_small_image.png"
$png48Path = "d:\eclipse_workspace\facturacao_electronica\installer_assets\temp_48.png"
$icoPath = "d:\eclipse_workspace\facturacao_electronica\installer_assets\KwanzaERP.ico"

# Resize to 48x48 para máxima compatibilidade
$img = [System.Drawing.Image]::FromFile($inputPath)
$bmp = New-Object System.Drawing.Bitmap(48, 48)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.DrawImage($img, 0, 0, 48, 48)
$bmp.Save($png48Path, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
$img.Dispose()

# Construir estrutura de arquivo ICO (PNG-compressed)
$pngBytes = [System.IO.File]::ReadAllBytes($png48Path)
$icoBytes = New-Object byte[] (22 + $pngBytes.Length)

# ICONDIR
$icoBytes[0] = 0; $icoBytes[1] = 0; # Reserved
$icoBytes[2] = 1; $icoBytes[3] = 0; # Type (1=ICO)
$icoBytes[4] = 1; $icoBytes[5] = 0; # Count (1)

# ICONDIRENTRY
$icoBytes[6] = 48; # Width
$icoBytes[7] = 48; # Height
$icoBytes[8] = 0;  # Color count
$icoBytes[9] = 0;  # Reserved
$icoBytes[10] = 1; $icoBytes[11] = 0; # Planes
$icoBytes[12] = 32; $icoBytes[13] = 0; # Bits per pixel

# Data size
$size = $pngBytes.Length
$icoBytes[14] = $size -band 0xFF
$icoBytes[15] = ($size -shr 8) -band 0xFF
$icoBytes[16] = ($size -shr 16) -band 0xFF
$icoBytes[17] = ($size -shr 24) -band 0xFF

# Data offset (22 bytes)
$icoBytes[18] = 22; $icoBytes[19] = 0; $icoBytes[20] = 0; $icoBytes[21] = 0

# Copiar dados do PNG
[System.Array]::Copy($pngBytes, 0, $icoBytes, 22, $pngBytes.Length)

# Salvar arquivo final
[System.IO.File]::WriteAllBytes($icoPath, $icoBytes)
Write-Host "Ícone 48x48 gerado com sucesso em $icoPath ($($icoBytes.Length) bytes)"
