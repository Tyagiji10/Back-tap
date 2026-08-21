Add-Type -AssemblyName System.Drawing

$src = "C:\Users\shaur\.gemini\antigravity-ide\brain\00636412-27eb-4f1d-bcf1-68bb5023bf74\.user_uploaded\media_1787342444463.png"
$res_dir = "d:\VS Code\Android\Back-tap\app\src\main\res"

$sizes = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

$img = [System.Drawing.Image]::FromFile($src)

foreach ($dpi in $sizes.Keys) {
    $size = $sizes[$dpi]
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    
    $g.Clear([System.Drawing.Color]::Black)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    
    # Draw original image scaled to fill entirely
    $g.DrawImage($img, 0, 0, $size, $size)
    $g.Dispose()
    
    $dir = "$res_dir\mipmap-$dpi"
    if (!(Test-Path -Path $dir)) {
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
    }
    
    $bmp.Save("$dir\ic_launcher.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save("$dir\ic_launcher_round.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "Generated $dpi icons"
}

$img.Dispose()
Write-Host "Icons generated successfully!"
