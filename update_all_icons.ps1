Add-Type -AssemblyName System.Drawing

$customLogoPath = "c:\Users\budia\Downloads\SIKAP\logo_custom.png"
$winIconPng = "c:\Users\budia\Downloads\SIKAP\icon.png"
$winAppIconPng = "c:\Users\budia\Downloads\SIKAP\windows-app\icon.png"
$winIconIco = "c:\Users\budia\Downloads\SIKAP\icon.ico"

# 1. Update Windows App PNGs
Copy-Item $customLogoPath $winIconPng -Force
Copy-Item $customLogoPath $winAppIconPng -Force

# 2. Convert custom logo to icon.ico for Windows
$srcImage = [System.Drawing.Bitmap]::FromFile($customLogoPath)

# Generate ICO with 256x256 high quality
$resizedBit = New-Object System.Drawing.Bitmap(256, 256)
$g = [System.Drawing.Graphics]::FromImage($resizedBit)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.DrawImage($srcImage, 0, 0, 256, 256)
$g.Dispose()

$hIcon = $resizedBit.GetHicon()
$icon = [System.Drawing.Icon]::FromHandle($hIcon)
$fs = [System.IO.File]::OpenWrite($winIconIco)
$icon.Save($fs)
$fs.Close()
$resizedBit.Dispose()

# Re-create Desktop Shortcut with new icon
$WScriptShell = New-Object -ComObject WScript.Shell
$DesktopPath = [System.Environment]::GetFolderPath('Desktop')
$ShortcutPath = Join-Path -Path $DesktopPath -ChildPath "SIKAP Desktop.lnk"

$Shortcut = $WScriptShell.CreateShortcut($ShortcutPath)
$Shortcut.TargetPath = "c:\Users\budia\Downloads\SIKAP\Buka-SIKAP-Windows.bat"
$Shortcut.IconLocation = "$winIconIco, 0"
$Shortcut.WorkingDirectory = "c:\Users\budia\Downloads\SIKAP"
$Shortcut.Description = "Sistem Informasi Karakter & Poin Siswa (SIKAP) - Aplikasi Windows Desktop"
$Shortcut.Save()

Write-Host "Windows Icon & Desktop Shortcut updated successfully with custom logo!"

# Helper function to resize and save PNG/WEBP
function Save-ResizedImage($srcBitmap, $width, $height, $targetPath) {
    $bmp = New-Object System.Drawing.Bitmap($width, $height)
    $graphics = [System.Drawing.Graphics]::FromImage($bmp)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.DrawImage($srcBitmap, 0, 0, $width, $height)
    $graphics.Dispose()

    # Save as PNG
    $bmp.Save($targetPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# 3. Update Android App Icons (Mipmap folders)
$resFolder = "c:\Users\budia\Downloads\SIKAP\app\src\main\res"

$resolutions = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($folder in $resolutions.Keys) {
    $size = $resolutions[$folder]
    $dir = Join-Path $resFolder $folder
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
    
    $pngFile = Join-Path $dir "ic_launcher.png"
    $roundPngFile = Join-Path $dir "ic_launcher_round.png"

    Save-ResizedImage $srcImage $size $size $pngFile
    Save-ResizedImage $srcImage $size $size $roundPngFile

    # If webp exists, overwrite with png or update
    $webpFile = Join-Path $dir "ic_launcher.webp"
    $roundWebpFile = Join-Path $dir "ic_launcher_round.webp"
    if (Test-Path $webpFile) { Remove-Item $webpFile -Force }
    if (Test-Path $roundWebpFile) { Remove-Item $roundWebpFile -Force }
}

# Copy logo to drawable for layout display
$drawableDir = Join-Path $resFolder "drawable"
if (!(Test-Path $drawableDir)) { New-Item -ItemType Directory -Path $drawableDir | Out-Null }
Save-ResizedImage $srcImage 512 512 (Join-Path $drawableDir "logo_app.png")

$srcImage.Dispose()
Write-Host "Android Launcher Icons updated successfully for all resolutions!"
