# Скачивает libv2ray.aar (Xray/V2Ray core) перед сборкой
$libsDir = Join-Path $PSScriptRoot "..\app\libs"
$outFile = Join-Path $libsDir "libv2ray.aar"
$url = "https://github.com/2dust/AndroidLibXrayLite/releases/download/v26.5.19/libv2ray.aar"

New-Item -ItemType Directory -Force -Path $libsDir | Out-Null

if (Test-Path $outFile) {
    $size = (Get-Item $outFile).Length
    if ($size -gt 50000000) {
        Write-Host "libv2ray.aar already present ($size bytes)"
        exit 0
    }
}

Write-Host "Downloading libv2ray.aar..."
Invoke-WebRequest -Uri $url -OutFile $outFile -UseBasicParsing
Write-Host "Done: $outFile"
