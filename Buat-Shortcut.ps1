Add-Type -AssemblyName System.Drawing

$pngPath = "c:\Users\budia\Downloads\SIKAP\icon.png"
$icoPath = "c:\Users\budia\Downloads\SIKAP\icon.ico"

# Convert PNG to ICO
$png = [System.Drawing.Bitmap]::FromFile($pngPath)
$hIcon = $png.GetHicon()
$icon = [System.Drawing.Icon]::FromHandle($hIcon)
$fs = [System.IO.File]::OpenWrite($icoPath)
$icon.Save($fs)
$fs.Close()
$png.Dispose()

# Create Shortcut on Desktop
$WScriptShell = New-Object -ComObject WScript.Shell
$DesktopPath = [System.Environment]::GetFolderPath('Desktop')
$ShortcutPath = Join-Path -Path $DesktopPath -ChildPath "SIKAP Desktop.lnk"

$Shortcut = $WScriptShell.CreateShortcut($ShortcutPath)
$Shortcut.TargetPath = "c:\Users\budia\Downloads\SIKAP\Buka-SIKAP-Windows.bat"
$Shortcut.IconLocation = "$icoPath, 0"
$Shortcut.WorkingDirectory = "c:\Users\budia\Downloads\SIKAP"
$Shortcut.Description = "Sistem Informasi Karakter & Poin Siswa (SIKAP) - Aplikasi Windows Desktop"
$Shortcut.Save()

Write-Host "Shortcut SIKAP Desktop dengan ikon resmi berhasil dibuat di Desktop Windows Anda!"
