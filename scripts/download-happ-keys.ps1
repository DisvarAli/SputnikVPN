# Ключи RSA для happ://crypt5/ (из happ-decryptor)
$outDir = Join-Path $PSScriptRoot "..\app\src\main\assets\happ"
$outFile = Join-Path $outDir "expanded_rsa_keys.json"
$url = "https://raw.githubusercontent.com/LeeeeT/happ-decryptor/main/public/data/expanded_rsa_keys.json"

New-Item -ItemType Directory -Force -Path $outDir | Out-Null
Write-Host "Downloading Happ crypt5 keys..."
Invoke-WebRequest -Uri $url -OutFile $outFile -UseBasicParsing
Write-Host "Done: $outFile"
