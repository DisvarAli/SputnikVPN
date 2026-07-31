# Publish SputnikVPN to GitHub (run from project root)
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

$gh = Get-Command gh -ErrorAction SilentlyContinue
if (-not $gh) {
    $portable = "$env:TEMP\gh-cli\bin\gh.exe"
    if (Test-Path $portable) { $gh = $portable } else {
        Write-Error "GitHub CLI (gh) not found. Install from https://cli.github.com/"
    }
}

& $gh auth status 2>$null
if ($LASTEXITCODE -ne 0) {
    & $gh auth login --hostname github.com --git-protocol https --web
}

$login = & $gh api user --jq ".login"
$repo = "SputnikVPN"

& $gh repo view "$login/$repo" 2>$null
if ($LASTEXITCODE -ne 0) {
    & $gh repo create $repo --public --source=. --remote=origin `
        --description "Educational Android VLESS VPN client. Use at your own risk."
} else {
    git remote remove origin 2>$null
    git remote add origin "https://github.com/$login/$repo.git"
}

git push -u origin main
Write-Host "Done: https://github.com/$login/$repo"
