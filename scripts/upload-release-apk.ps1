# Upload app-debug.apk to an existing GitHub Release (uses git credential token).
param(
    [string]$Tag = "v2.1.0",
    [string]$ApkPath = "",
    [string]$AssetName = "app-stable-debug.apk"
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
if (-not $ApkPath) {
    $stable = Join-Path $root "app\build\outputs\apk\stable\debug"
    $named = Get-ChildItem -Path $stable -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($named) { $ApkPath = $named.FullName }
    else { $ApkPath = Join-Path $stable "app-stable-debug.apk" }
}
if (-not (Test-Path $ApkPath)) {
    throw "APK not found: $ApkPath. Build: .\gradlew assembleStableDebug"
}

$cred = "protocol=https`nhost=github.com`n`n" | git -C $root credential fill
$token = ($cred | Select-String '^password=').ToString().Substring(9)
$headers = @{
    Authorization = "Bearer $token"
    Accept        = 'application/vnd.github+json'
    'X-GitHub-Api-Version' = '2022-11-28'
}

$release = Invoke-RestMethod -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/releases/tags/$Tag" -Headers $headers
foreach ($asset in @($release.assets)) {
    if ($asset.name -eq $AssetName) {
        Invoke-RestMethod -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/releases/assets/$($asset.id)" `
            -Method Delete -Headers $headers | Out-Null
        Write-Host "Removed old asset: $AssetName"
    }
}

$uploadUrl = ($release.upload_url -split '\{')[0] + "?name=$AssetName"
Write-Host "Uploading $ApkPath ($([math]::Round((Get-Item $ApkPath).Length / 1MB)) MB)…"
$result = curl.exe -f -L -X POST `
    -H "Authorization: Bearer $token" `
    -H "Content-Type: application/vnd.android.package-archive" `
    --data-binary "@$ApkPath" `
    $uploadUrl
if ($LASTEXITCODE -ne 0) { throw "curl upload failed with exit $LASTEXITCODE" }
$json = $result | ConvertFrom-Json
Write-Host "Done: $($json.browser_download_url)"
Write-Host "Release: $($release.html_url)"
