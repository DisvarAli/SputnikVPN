# Скачивает geoip.dat и geosite.dat в app/src/main/assets/xray/
$assetsDir = Join-Path $PSScriptRoot "..\app\src\main\assets\xray"
New-Item -ItemType Directory -Force -Path $assetsDir | Out-Null

$files = @{
    "geoip.dat"   = "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geoip.dat"
    "geosite.dat" = "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat"
}

foreach ($entry in $files.GetEnumerator()) {
    $out = Join-Path $assetsDir $entry.Key
    if ((Test-Path $out) -and (Get-Item $out).Length -gt 100000) {
        Write-Host "$($entry.Key) already present"
        continue
    }
    Write-Host "Downloading $($entry.Key)..."
    Invoke-WebRequest -Uri $entry.Value -OutFile $out -UseBasicParsing
}
Write-Host "Done: $assetsDir"
