# Patch GitHub repo About + release notes for v2.1.0
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

$cred = "protocol=https`nhost=github.com`n`n" | git credential fill
$token = ($cred | Select-String '^password=').ToString().Substring(9)
$headers = @{
    Authorization = "Bearer $token"
    Accept        = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
}

$desc = "Open-source Android VPN client (Kotlin, Jetpack Compose, Xray). Mirrors, config picker, VLESS/VMess/Trojan/SS/Hysteria2."
$homepage = "https://github.com/DisvarAli/SputnikVPN/releases/latest"

$patchRepo = @{
    description = $desc
    homepage    = $homepage
    has_downloads = $true
} | ConvertTo-Json -Compress

Write-Host "Updating repo About..."
Invoke-RestMethod -Method Patch -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN" `
    -Headers $headers -Body ([Text.Encoding]::UTF8.GetBytes($patchRepo)) `
    -ContentType "application/json; charset=utf-8" | Out-Null

$topicsBody = @{
    names = @(
        "android", "vpn", "kotlin", "jetpack-compose", "xray",
        "vless", "vmess", "trojan", "shadowsocks", "hysteria2"
    )
} | ConvertTo-Json -Compress

Write-Host "Updating topics..."
$topicHeaders = @{
    Authorization = "Bearer $token"
    Accept        = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
}
Invoke-RestMethod -Method Put -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/topics" `
    -Headers $topicHeaders `
    -Body ([Text.Encoding]::UTF8.GetBytes($topicsBody)) `
    -ContentType "application/json; charset=utf-8" | Out-Null

$notesPath = Join-Path $root "docs\release-notes-v2.1.0.md"
$notes = [IO.File]::ReadAllText($notesPath, [Text.UTF8Encoding]::new($false))
$release = Invoke-RestMethod -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/releases/tags/v2.1.0" -Headers $headers
$patchRel = @{
    name = "Sputnik VPN 2.1.0"
    body = $notes
} | ConvertTo-Json -Compress

Write-Host "Updating release notes..."
Invoke-RestMethod -Method Patch -Uri "https://api.github.com/repos/DisvarAli/SputnikVPN/releases/$($release.id)" `
    -Headers $headers -Body ([Text.Encoding]::UTF8.GetBytes($patchRel)) `
    -ContentType "application/json; charset=utf-8" | Out-Null

# Ensure branded APK is present
$apk = Join-Path $root "SputnikVPN-2.1.0.apk"
if (-not (Test-Path $apk)) {
    $apk = Join-Path $root "app\build\outputs\apk\stable\debug\app-stable-debug.apk"
}
$hasBranded = @($release.assets | Where-Object { $_.name -eq "SputnikVPN-2.1.0.apk" }).Count -gt 0
if (-not $hasBranded -and (Test-Path $apk)) {
    Write-Host "Uploading SputnikVPN-2.1.0.apk..."
    $uploadUrl = ($release.upload_url -split '\{')[0] + "?name=SputnikVPN-2.1.0.apk"
    $bytes = [IO.File]::ReadAllBytes((Resolve-Path $apk))
    Invoke-RestMethod -Uri $uploadUrl -Method Post -Headers @{
        Authorization = "Bearer $token"
        "Content-Type" = "application/vnd.android.package-archive"
    } -Body $bytes | Out-Null
}

Write-Host "Done."
Write-Host "Repo: https://github.com/DisvarAli/SputnikVPN"
Write-Host "Release: https://github.com/DisvarAli/SputnikVPN/releases/tag/v2.1.0"
