param(
    [string]$Tag = "v2.1.0",
    [string]$ApkPath = "",
    [string]$AssetName = "app-debug.apk",
    [switch]$NotesOnly
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
if (-not $ApkPath) {
    $stable = Join-Path $root "app\build\outputs\apk\stable\debug\app-stable-debug.apk"
    $ApkPath = if (Test-Path $stable) { $stable } else { Join-Path $root "app\build\outputs\apk\debug\app-debug.apk" }
}
if (-not $NotesOnly -and -not (Test-Path $ApkPath)) {
    throw "APK not found: $ApkPath. Run: .\gradlew assembleStableDebug"
}

$cred = "protocol=https`nhost=github.com`n`n" | git -C $root credential fill
$token = ($cred | Select-String '^password=').ToString().Substring(9)
$headers = @{
    Authorization = "Bearer $token"
    Accept        = 'application/vnd.github+json'
    'X-GitHub-Api-Version' = '2022-11-28'
}

$release = Invoke-RestMethod -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/releases/tags/$Tag" -Headers $headers

$notesPath = Join-Path $root "docs\release-notes-$($Tag.TrimStart('v')).md"
if (Test-Path $notesPath) {
    $notes = [IO.File]::ReadAllText($notesPath, [Text.UTF8Encoding]::new($false))
    $patch = @{ body = $notes } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/releases/$($release.id)" -Method Patch -Headers $headers -Body ([Text.Encoding]::UTF8.GetBytes($patch)) -ContentType 'application/json; charset=utf-8' | Out-Null
}

if (-not $NotesOnly) {
    foreach ($asset in $release.assets) {
        Invoke-RestMethod -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/releases/assets/$($asset.id)" -Method Delete -Headers $headers | Out-Null
        Write-Host "Deleted asset: $($asset.name)"
    }
    $uploadUrl = ($release.upload_url -split '\{')[0] + "?name=$AssetName"
    $bytes = [IO.File]::ReadAllBytes((Resolve-Path $ApkPath))
    $newAsset = Invoke-RestMethod -Uri $uploadUrl -Method Post -Headers @{
        Authorization = "Bearer $token"
        'Content-Type' = 'application/vnd.android.package-archive'
    } -Body $bytes
    Write-Host "APK: $($newAsset.browser_download_url)"
}

Write-Host "Release: $($release.html_url)"
Write-Host "Notes updated from docs/release-notes-$($Tag.TrimStart('v')).md"
