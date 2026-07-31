# Push main + create GitHub Release v2.1.0 with stable APK (no gh CLI required).
param(
    [string]$Tag = "v2.1.0",
    [switch]$SkipPush,
    [switch]$SkipApk
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

if (-not $SkipApk) {
    if (-not (Test-Path "app\libs\libv2ray.aar")) {
        & "$PSScriptRoot\download-libs.ps1"
    }
    & "$root\gradlew.bat" assembleStableDebug --no-daemon -q
}

if (-not $SkipPush) {
    git remote remove origin 2>$null
    git remote add origin "https://github.com/DisvarAli/SputnikVPN.git"
    git branch -M main
    $staged = git diff --cached --quiet 2>$null; $dirty = git status --porcelain
    if ($dirty) {
        git -c user.name="DisvarAli" -c user.email="DisvarAli@users.noreply.github.com" `
            add -A
        git -c user.name="DisvarAli" -c user.email="DisvarAli@users.noreply.github.com" `
            commit -m "Sputnik VPN 2.1.0`n`nConfig picker, subscription mirrors, faster server checks."
    }
    git push -u origin main --force
    Write-Host "Pushed: https://github.com/DisvarAli/SputnikVPN"
}

$notesPath = Join-Path $root "docs\release-notes-$($Tag.TrimStart('v')).md"
$body = if (Test-Path $notesPath) {
    [IO.File]::ReadAllText($notesPath, [Text.UTF8Encoding]::new($false))
} else {
    "Sputnik VPN $($Tag.TrimStart('v')). See CHANGELOG.md."
}

$cred = "protocol=https`nhost=github.com`n`n" | git credential fill
$token = ($cred | Select-String '^password=').ToString().Substring(9)
$headers = @{
    Authorization = "Bearer $token"
    Accept        = 'application/vnd.github+json'
    'X-GitHub-Api-Version' = '2022-11-28'
}

$releaseJson = @{
    tag_name   = $Tag
    name       = "Sputnik VPN $($Tag.TrimStart('v'))"
    body       = $body
    draft      = $false
    prerelease = $false
} | ConvertTo-Json -Compress

try {
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/releases/tags/$Tag" -Headers $headers
    Write-Host "Release $Tag already exists - updating notes"
    $patch = @{ body = $body } | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/releases/$($release.id)" `
        -Method Patch -Headers $headers -Body ([Text.Encoding]::UTF8.GetBytes($patch)) `
        -ContentType 'application/json; charset=utf-8' | Out-Null
} catch {
    Write-Host "Creating release $Tag..."
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/releases" `
        -Method Post -Headers $headers -Body ([Text.Encoding]::UTF8.GetBytes($releaseJson)) `
        -ContentType 'application/json; charset=utf-8'
}

if (-not $SkipApk) {
    $apk = Join-Path $root "app\build\outputs\apk\stable\debug\app-stable-debug.apk"
    if (-not (Test-Path $apk)) { throw "APK missing: $apk" }
    foreach ($asset in @($release.assets)) {
        if ($asset.name -eq "app-stable-debug.apk") {
            Invoke-RestMethod -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/releases/assets/$($asset.id)" `
                -Method Delete -Headers $headers | Out-Null
        }
    }
    $uploadUrl = ($release.upload_url -split '\{')[0] + "?name=app-stable-debug.apk"
    $bytes = [IO.File]::ReadAllBytes((Resolve-Path $apk))
    $newAsset = Invoke-RestMethod -Uri $uploadUrl -Method Post -Headers @{
        Authorization = "Bearer $token"
        'Content-Type' = 'application/vnd.android.package-archive'
    } -Body $bytes
    Write-Host "APK: $($newAsset.browser_download_url)"
}

Write-Host "Release: $($release.html_url)"
