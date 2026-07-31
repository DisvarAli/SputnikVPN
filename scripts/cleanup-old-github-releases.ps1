# Delete GitHub Releases/tags except KeepTag (default v2.1.0).
# If KeepTag does not exist yet, all current releases/tags are removed.
param(
    [string]$KeepTag = "v2.1.0",
    [string]$Owner = "DisvarAli",
    [string]$Repo = "SputnikVPN",
    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

function Get-GitHubToken {
    $cred = "protocol=https`nhost=github.com`n`n" | git credential fill 2>$null
    if ($cred) {
        $line = ($cred | Select-String '^password=').ToString()
        if ($line) { return $line.Substring(9) }
    }
    if ($env:GITHUB_TOKEN) { return $env:GITHUB_TOKEN }
    if ($env:GH_TOKEN) { return $env:GH_TOKEN }
    throw "No GitHub token (git credential / GITHUB_TOKEN)"
}

$token = Get-GitHubToken
$headers = @{
    Authorization = "Bearer $token"
    Accept        = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
}

$base = "https://api.github.com/repos/$Owner/$Repo"
$releases = Invoke-RestMethod -Uri "$base/releases?per_page=100" -Headers $headers

Write-Host "Releases found: $($releases.Count); keep=$KeepTag"
foreach ($rel in $releases) {
    if ($KeepTag -and $rel.tag_name -eq $KeepTag) {
        Write-Host "KEEP release: $($rel.tag_name)"
        continue
    }
    Write-Host "DELETE release: $($rel.tag_name) (id $($rel.id))"
    if (-not $WhatIf) {
        Invoke-RestMethod -Method Delete -Uri "$base/releases/$($rel.id)" -Headers $headers | Out-Null
    }
}

$tags = Invoke-RestMethod -Uri "$base/tags?per_page=100" -Headers $headers
Write-Host "Tags found: $($tags.Count)"
foreach ($t in $tags) {
    if ($KeepTag -and $t.name -eq $KeepTag) {
        Write-Host "KEEP tag: $($t.name)"
        continue
    }
    Write-Host "DELETE tag: $($t.name)"
    if (-not $WhatIf) {
        Invoke-RestMethod -Method Delete -Uri "$base/git/refs/tags/$($t.name)" -Headers $headers | Out-Null
    }
}

Write-Host "Done. KeepTag=$KeepTag WhatIf=$WhatIf"
