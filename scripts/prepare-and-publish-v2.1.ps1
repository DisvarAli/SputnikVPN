# One-shot prepare + publish Sputnik VPN 2.1.0
# Usage (when user gives the go-ahead):
#   powershell -File scripts/prepare-and-publish-v2.1.ps1
# Options:
#   -SkipCleanup  do not delete old GitHub releases/tags
#   -SkipApk      skip rebuild
#   -DryRun       show actions only
param(
    [switch]$SkipCleanup,
    [switch]$SkipApk,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

$Tag = "v2.1.0"
$ApkName = "SputnikVPN-2.1.0.apk"

Write-Host "== Prepare Sputnik VPN $Tag =="

if (-not $SkipApk) {
    if (-not (Test-Path "app\libs\libv2ray.aar")) {
        & "$PSScriptRoot\download-libs.ps1"
    }
    Write-Host "Building APK..."
    & "$root\gradlew.bat" assembleStableDebug --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "APK build failed" }
}

$apkSrc = Join-Path $root "app\build\outputs\apk\stable\debug\app-stable-debug.apk"
if (-not (Test-Path $apkSrc)) { throw "APK not found: $apkSrc" }
$apkOut = Join-Path $root $ApkName
Copy-Item $apkSrc $apkOut -Force
Write-Host "APK ready: $apkOut"

if (-not (Test-Path ".git")) {
    Write-Host "Initializing git..."
    if ($DryRun) {
        Write-Host "[DryRun] git init / remote / commit"
    } else {
        git init
        git remote add origin "https://github.com/DisvarAli/SputnikVPN.git"
        git branch -M main
        git add -A
        git -c user.name="DisvarAli" -c user.email="DisvarAli@users.noreply.github.com" `
            commit -m "Sputnik VPN 2.1.0`n`nConfig picker, subscription mirrors, faster server checks."
    }
} else {
    $dirty = git status --porcelain
    if ($dirty) {
        if ($DryRun) {
            Write-Host "[DryRun] commit dirty tree"
        } else {
            git add -A
            git -c user.name="DisvarAli" -c user.email="DisvarAli@users.noreply.github.com" `
                commit -m "Sputnik VPN 2.1.0`n`nConfig picker, subscription mirrors, faster server checks."
        }
    }
}

if (-not $SkipCleanup) {
    Write-Host "Cleaning old GitHub releases/tags..."
    if ($DryRun) {
        & "$PSScriptRoot\cleanup-old-github-releases.ps1" -KeepTag $Tag -WhatIf
    } else {
        & "$PSScriptRoot\cleanup-old-github-releases.ps1" -KeepTag $Tag
    }
}

if ($DryRun) {
    Write-Host "[DryRun] would: git push -u origin main --force"
    Write-Host "[DryRun] would: create release $Tag + upload $ApkName"
    Write-Host "Dry run complete."
    exit 0
}

Write-Host "Pushing + creating release $Tag..."
& "$PSScriptRoot\publish-release-v2.ps1" -Tag $Tag -SkipApk

# Prefer branded APK asset name on the release
& "$PSScriptRoot\publish-github-release.ps1" -Tag $Tag -ApkPath $apkOut -AssetName $ApkName

Write-Host "Published: https://github.com/DisvarAli/SputnikVPN/releases/tag/$Tag"
